package org.edu_sharing.alfresco.repository.server.authentication;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Context {

    /**
     * max number of nodes that are stored in the session for a temporary access via usage
     */
    private static final int MAX_SINGLE_USE_NODEIDS = 50;
    private static final ThreadLocal<Context> instance = new ThreadLocal<>();
    private static ServletContext globalContext;

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    /**
     * The single header B3 form. Spring Boot sends this one for
     * {@code management.tracing.propagation.type=b3} (rendering2 does), while {@code b3_multi}
     * and Istio/Envoy send the {@code X-B3-*} headers - both have to be understood here.
     */
    public static final String B3_SINGLE_HEADER = "b3";

    private B3 b3 = new B3() { };


    private Context(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Nullable
    public static Context getCurrentInstance() {
        return instance.get();
    }

    @Nullable
    public static Context getCurrentContextForCustomThreads(){
        Context context = getCurrentInstance();
        if(context != null){
            String serverName = (context.getRequest() != null) ? context.getRequest().getServerName() : null;
            String locale = (context.getRequest() != null && context.getRequest().getSession(false) != null)
                    ? (String)context.getRequest().getSession(false).getAttribute(CCConstants.AUTH_LOCALE)
                    : null;

            HttpSessionAdapter session = new HttpSessionAdapter(){
                @Override
                public Object getAttribute(String name) {
                    if(CCConstants.AUTH_LOCALE.equals(name)){
                        return locale;
                    }
                    return null;
                }
            };
            // the worker belongs to the request that started it - without this, init()
            // would roll a new id and its log lines would no longer join the request's
            String requestId = context.getB3().getRequestId();
            HttpServletRequestAdapter adapter = new HttpServletRequestAdapter(){
                @Override
                public HttpSession getSession(boolean create) {
                    return session;
                }

                @Override
                public String getHeader(String name) {
                    return REQUEST_ID_HEADER.equalsIgnoreCase(name) ? requestId : super.getHeader(name);
                }

                @Override
                public String getServerName() {
                    return serverName;
                }
            };

            Context newContext = new Context(adapter,null);
            newContext.init();
            return newContext;
        }
        return null;
    }

    @Nullable
    public static ServletContext getGlobalContext() {
        return globalContext;
    }

    @Nullable
    public static Context setInstance(Context context){
        instance.set(context);
        return context;
    }

    @NotNull
    public static Context newInstance(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) {
        Context context = new Context(request,response);
        if(globalContext==null)
            globalContext=servletContext;
        context.init();
        instance.set(context);
        return context;
    }

    private void init() {
        // decided once: generated when absent, so every accessor and every outgoing call sees the same id
        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
        // callers may send either B3 form; the multi header one wins when both are present
        SingleB3 single = SingleB3.parse(request.getHeader(B3_SINGLE_HEADER));
        b3 = new B3() {

            @Override
            public String getRequestId() {
                return requestId;
            }

            @Override
            public String getTraceId() {
                String multi = request.getHeader("X-B3-TraceId");
                return multi != null ? multi : single.traceId();
            }

            @Override
            public String getSpanId() {
                String multi = request.getHeader("X-B3-SpanId");
                return multi != null ? multi : single.spanId();
            }

            @Override
            public boolean isSampled() {
                String multi = request.getHeader("X-B3-Sampled");
                // "d" is debug, which implies sampled
                String sampled = multi != null ? multi : single.sampled();
                return "1".equals(sampled) || "d".equals(sampled);
            }

            @Override
            public String toString() {
                if (getTraceId() != null) {
                    return "TraceId: " + getTraceId();
                }
                return "";
            }

            private boolean isX3Header(String header) {
                return header.toUpperCase().startsWith("X-B3-") ||
                        header.toUpperCase().startsWith("X-OT-") ||
                        header.equalsIgnoreCase(B3_SINGLE_HEADER) ||
                        header.equalsIgnoreCase(REQUEST_ID_HEADER);
            }

            @Override
            public Map<String, String> getX3Headers() {
                Map<String, String> headers = new HashMap<>();
                try {
                    Collections.list(request.getHeaderNames()).stream()
                            .filter(this::isX3Header)
                            .filter(header -> !header.equalsIgnoreCase(REQUEST_ID_HEADER))
                            .forEach(header -> headers.put(header, request.getHeader(header)));
                } catch (Throwable t) {
                    Logger.getLogger(Context.class).warn("Unexpected error while fetching x3 headers", t);
                }
                // always the resolved one - generated, or the incoming one if it is a valid uuid
                headers.put(REQUEST_ID_HEADER, requestId);
                return headers;
            }
        };

        ThreadContext.put("RequestId", requestId);

        if(b3.getTraceId() != null) {
            ThreadContext.put("TraceId", b3.getTraceId());
        }
        if(b3.getSpanId() != null) {
            ThreadContext.put("SpanId", b3.getSpanId());
        }
    }

    /**
     * The `b3` single header: {@code {traceId}-{spanId}[-{samplingState}[-{parentSpanId}]]},
     * or just {@code 0} to deny sampling (no ids). Ids are 16 or 32 hex characters (trace) and
     * 16 (span); the sampling state is {@code 0}, {@code 1} or {@code d} (debug).
     * <p>
     * Anything that does not match is ignored rather than rejected - a malformed trace header
     * must not fail the request, it only costs the correlation.
     *
     * @see <a href="https://github.com/openzipkin/b3-propagation#single-header">b3 single header</a>
     */
    record SingleB3(@Nullable String traceId, @Nullable String spanId, @Nullable String sampled) {

        private static final SingleB3 NONE = new SingleB3(null, null, null);
        private static final Pattern PATTERN = Pattern.compile(
                "^([0-9a-fA-F]{16}|[0-9a-fA-F]{32})-([0-9a-fA-F]{16})(?:-([01d])(?:-[0-9a-fA-F]{16})?)?$");

        @NotNull
        static SingleB3 parse(@Nullable String header) {
            if (header == null || header.isBlank()) {
                return NONE;
            }
            String value = header.trim();
            // "b3: 0" is the sampling decision alone, without any ids
            if ("0".equals(value) || "1".equals(value) || "d".equals(value)) {
                return new SingleB3(null, null, value);
            }
            Matcher matcher = PATTERN.matcher(value);
            return matcher.matches()
                    ? new SingleB3(matcher.group(1), matcher.group(2), matcher.group(3))
                    : NONE;
        }
    }

    /**
     * The incoming id if it is a UUID, a new one otherwise - this server joins a trace,
     * it does not start one, but responses it produces itself must stay correlatable.
     * The id ends up in every log line, so a caller must not put arbitrary text or length there.
     */
    @NotNull
    static String resolveRequestId(@Nullable String incoming) {
        return isCanonicalUuid(incoming) ? incoming : UUID.randomUUID().toString();
    }

    /**
     * Only the canonical 8-4-4-4-12 hex form (any version, either case). {@link UUID#fromString}
     * alone is lenient ("1-2-3-4-5", a leading "+"), the round trip is not - so this matches
     * {@code isValidRequestId} in Frontend-Server/src/lib/tracing.ts exactly. Keep both in sync.
     */
    private static boolean isCanonicalUuid(@Nullable String value) {
        if (value == null) {
            return false;
        }
        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void release() {
        instance.remove();
    }

    @Nullable
    public HttpServletRequest getRequest() {
        return request;
    }

    @Nullable
    public B3 getB3() {
        return b3;
    }

    @Nullable
    public HttpServletResponse getResponse() {
        return response;
    }

    @Nullable
    public String getSessionAttribute(String key){
        String sessionAtt = null;
        if(this.getRequest() != null
                && this.getRequest().getSession() != null){
            sessionAtt = (String)this.getRequest().getSession().getAttribute(key);
        }
        return sessionAtt;
    }

    /**
     * is the given nodeId in the current context of an lms access?
     * @param nodeId
     * @return
     */
    public boolean isSingleUseNodeId(String nodeId) {
        List<String> list = null;
        if(this.getRequest() != null && this.getRequest().getSession() != null){
            list = (List<String>) this.getRequest().getSession().getAttribute(CCConstants.AUTH_SINGLE_USE_NODEIDS);
        }
        if(list == null) {
            return false;
        }
        return list.contains(nodeId);
    }

    /**
     * add the given node to the context of an lms access
     * @param nodeId
     */
    public synchronized void addSingleUseNode(String nodeId) {
        List<String> list = null;
        if (this.getRequest() == null) {
            log.error("addSingleUseNode was called without context; can not store id in session");
            return;
        }
        HttpSession session = this.getRequest().getSession();
        synchronized (session) {
            list = (List<String>) session.getAttribute(CCConstants.AUTH_SINGLE_USE_NODEIDS);
            if (list == null) {
                list = new ArrayList<>();
            }
            if (list.contains(nodeId)) {
                return;
            }
            list.add(nodeId);
            while (list.size() > MAX_SINGLE_USE_NODEIDS) {
                list.remove(0);
            }
            session.setAttribute(CCConstants.AUTH_SINGLE_USE_NODEIDS, list);
        }
    }

    @Nullable
    public String getSessionId(){
        if(this.getRequest() != null
                && this.getRequest().getSession() != null){
            return this.request.getSession().getId();
        }
        return null;
    }

    @Nullable
    public String getLocale(){
        return getSessionAttribute(CCConstants.AUTH_LOCALE);
    }

    @Nullable
    public String getAuthType(){
        return getSessionAttribute(CCConstants.AUTH_TYPE);
    }

    @Nullable
    public String getAccessToken(){
        return getSessionAttribute(CCConstants.AUTH_ACCESS_TOKEN);
    }

    @Nullable
    public String getAuthErrorStatus(){
        Object attribute = request.getAttribute(CCConstants.AUTH_ERROR_STATUS);
        if(attribute == null) {
            return null;
        }
        return attribute.toString();
    }

    public interface B3 {

        /** X-Request-Id of the current request, generated if absent or not a UUID */
        @Nullable
        default String getRequestId() {
            return null;
        }

        @Nullable
        default String getTraceId() {
            return null;
        }

        @Nullable
        default String getSpanId() {
            return null;
        }

        default boolean isSampled() {
            return false;
        }

        default void addToRequest(RequestBuilder request) {
            for (Map.Entry<String, String> header : getX3Headers().entrySet()) {
                request.setHeader(header.getKey(), header.getValue());
            }
        }
        default void addToRequest(HttpMethodBase get) {
            for (Map.Entry<String, String> header : getX3Headers().entrySet()) {
                get.addRequestHeader(header.getKey(), header.getValue());
            }
        }

        default Map<String, String> getX3Headers() {
            return Collections.emptyMap();
        };
    }
}

