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
        b3 = new B3() {

            @Override
            public String getRequestId() {
                return requestId;
            }

            @Override
            public String getTraceId() {
                return request.getHeader("X-B3-TraceId");
            }

            @Override
            public String getSpanId() {
                return request.getHeader("X-B3-SpanId");
            }

            @Override
            public boolean isSampled() {
                return "1".equals(request.getHeader("X-B3-Sampled"));
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

