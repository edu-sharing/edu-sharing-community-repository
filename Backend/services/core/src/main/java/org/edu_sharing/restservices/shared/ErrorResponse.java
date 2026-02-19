package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Data;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.EduSharingLockException;
import org.edu_sharing.restservices.*;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.usage.Usage2Service;
import org.edu_sharing.service.usage.UsageException;
import org.edu_sharing.webservices.util.AuthenticationUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;

@Data
public class ErrorResponse {
    /**
     * used in the lightbend config
     */
    private enum DisplayLevel {
        minimal,
        details,
        full,
    }
    public enum ErrorResponseLogging {
        strict, // default, log every error, including stacktrace
        relaxed, // stacktrace only for unknown errors, otherwise just one-line logging (recommended for get endpoints)
    }

    @JsonProperty(required = true)
    private String error;

    @JsonProperty(required = true)
    private String message;

    private String stacktrace;
    private String logLevel;

    private Map<String, Serializable> details;

    public ErrorResponse() {

    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "error='" + error + '\'' +
                ", message='" + message + '\'' +
                ", stacktrace='" + stacktrace + '\'' +
                ", logLevel='" + logLevel + '\'' +
                '}';
    }

    private static final Logger logger = Logger.getLogger(ErrorResponse.class);

    public static Response createResponse(Throwable t) {
        return createResponse(t, null, ErrorResponseLogging.strict);
    }

    public static Response createResponse(Throwable t, Response.Status errorCode) {
        return createResponse(t, errorCode, ErrorResponseLogging.strict);
    }

    public static Response createResponse(Throwable t, ErrorResponseLogging logging) {
        return createResponse(t, null, logging);
    }

    public static Response createResponse(Throwable t, Response.Status errorCode, ErrorResponseLogging logging) {
        handleLog(t, logging);

        if (errorCode != null) {
            return Response.status(errorCode)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new ErrorResponse(t))
                    .build();
        }

        // in case alfresco transaction exception, map to causing exception which is the DAO exception
        if (t instanceof AlfrescoRuntimeException && t.getCause() != null) {
            t = t.getCause();
        }
        while (t instanceof RuntimeException && !(t instanceof DAOException) && t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof DAOValidationException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new ErrorResponse(t))
                    .build();
        }
        if (t instanceof DAOSecurityException) {
            return Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new ErrorResponse(t))
                    .build();
        }
        if (t instanceof UsageException && Usage2Service.NO_CCPUBLISH_PERMISSION.equals(t.getMessage())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new ErrorResponse(t))
                    .build();
        }
        if (t instanceof DAOMissingException) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new ErrorResponse(t))
                    .build();
        }
        if (t instanceof DAOMimetypeVerificationException || t instanceof DAOFileExtensionVerificationException) {
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new ErrorResponse(t))
                    .build();
        }
        if (t instanceof DAOVirusDetectedException || t instanceof DAOVirusScanFailedException) {
            return Response.status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new ErrorResponse(t))
                    .build();
        }
        if (t instanceof DAOQuotaException) {

            logger.info(t.getMessage(), t);
            return Response.status(CCConstants.HTTP_INSUFFICIENT_STORAGE)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new ErrorResponse(t))
                    .build();
        }
        if (t instanceof DAODuplicateNodeNameException || t instanceof DAODuplicateNodeException || t instanceof DuplicateKeyException) {
            return Response.status(Response.Status.CONFLICT)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new ErrorResponse(t))
                    .build();
        }

        if(t instanceof EduSharingLockException){
            return Response.status(ExtendedStatus.LOCKED)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new ErrorResponse(t))
                    .build();
        }


        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    private static void handleLog(Throwable t, ErrorResponseLogging logging) {
        if (t instanceof DAOValidationException ||
                t instanceof DAOSecurityException ||
                t instanceof DAOMissingException ||
                t instanceof DAODuplicateNodeNameException) {
            if (logging.equals(ErrorResponseLogging.strict)) {
                logger.warn(t.getMessage(), t);
            } else if (logging.equals(ErrorResponseLogging.relaxed)) {
                logger.info(t.getMessage());
            }
        } else {
            // unknown error, log at highest level with stacktrace
            logger.error(t.getMessage(), t);
        }
    }

    public ErrorResponse(Throwable t) {
        if (t instanceof DAOException) {
            setDetails(((DAOException) t).getDetails());
        }

        setError(t.getClass().getName());
        DisplayLevel level = DisplayLevel.valueOf(LightbendConfigLoader.get().getString("security.logging.displayLevel." + (AuthorityServiceHelper.isAdmin() ? "admin" : "default")));
        if(Arrays.asList(DisplayLevel.details, DisplayLevel.full).contains(level)) {
            setMessage(t.getMessage());
        } else {
            setMessage("Details hidden: You can configure the output via security.logging.displayLevel");
        }

        setLogLevel(level.toString());
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        t.printStackTrace(printWriter);
        printWriter.flush();
        if (level == DisplayLevel.full) {
            if (Context.getCurrentInstance() != null) {
                setStacktrace(Context.getCurrentInstance().getB3().toString() + "\n" + writer);
            } else {
                setStacktrace(writer.toString());
            }
        }
    }

    public String[] getStacktraceArray() {
        return stacktrace != null ? stacktrace.replace("\r\n","\n").replace("\t","").split("\n") : null;
    }
}
