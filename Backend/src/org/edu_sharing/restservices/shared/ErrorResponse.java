package org.edu_sharing.restservices.shared;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.Response;

import org.alfresco.error.AlfrescoRuntimeException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;

@ApiModel(description = "")
public class ErrorResponse {
	public enum ErrorResponseLogging{
		strict, // default, log every error, including stacktrace
		relaxed, // stacktrace only for unknown errors, otherwise just one-line logging (recommended for get endpoints)
	}
	private String error = null;
	private String message = null;
	private String stacktrace = null;
	private String logLevel;

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

	private static Logger logger = Logger.getLogger(ErrorResponse.class);
	public static Response createResponse(Throwable t) {
		return createResponse(t,ErrorResponseLogging.strict);
	}
	public static Response createResponse(Throwable t,ErrorResponseLogging logging){
		handleLog(t,logging);
		// in case alfresco transaction exception, map to causing exception which is the DAO exception
		if(t instanceof AlfrescoRuntimeException && t.getCause() != null) {
			t = t.getCause();
		}
		if(t instanceof RuntimeException && t.getCause() != null) {
			t = t.getCause();
		}
		if(t instanceof DAOValidationException) {
    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
		}
    	if(t instanceof DAOSecurityException) {
    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
    	}
        if(t instanceof DAOMissingException) {
    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
    	}
		if(t instanceof DAOMimetypeVerificationException || t instanceof DAOFileExtensionVerificationException) {
			return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).entity(new ErrorResponse(t)).build();
		}
		if(t instanceof DAOVirusDetectedException || t instanceof DAOVirusScanFailedException) {
			return Response.status(HttpStatus.UNPROCESSABLE_ENTITY.value()).entity(new ErrorResponse(t)).build();
		}
    	if(t instanceof DAOQuotaException){

			logger.info(t.getMessage(), t);
			return Response.status(CCConstants.HTTP_INSUFFICIENT_STORAGE).entity(new ErrorResponse(t)).build();
		}
        if(t instanceof DAODuplicateNodeNameException){
        	return Response.status(Response.Status.CONFLICT).entity(new ErrorResponse(t)).build();
        }



		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
	}

	private static void handleLog(Throwable t, ErrorResponseLogging logging) {
		if(t instanceof DAOValidationException ||
				t instanceof DAOSecurityException ||
				t instanceof DAOMissingException ||
				t instanceof DAODuplicateNodeNameException){
			if(logging.equals(ErrorResponseLogging.strict)) {
				logger.warn(t.getMessage(), t);
			}else if(logging.equals(ErrorResponseLogging.relaxed)){
				logger.info(t.getMessage());
			}
		}
		else{
			// unknown error, log at highest level with stacktrace
			logger.error(t.getMessage(),t);
		}
	}

	public ErrorResponse(Throwable t) {
		Level level=logger.getEffectiveLevel();
		
		if(level!=null){
			setError(t.getClass().getName());
			if(level.toInt()<=Level.INFO_INT)
				setMessage(t.getMessage());
			else
				setMessage("InvalidLogLevel: Log Level must be at least INFO for showing error messages");

			setLogLevel(level.toString());
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter( writer );
			t.printStackTrace( printWriter );
			printWriter.flush();
			if(level.toInt()<=Level.DEBUG_INT)
				setStacktrace(writer.toString());
			else
				setStacktrace("InvalidLogLevel: Log Level must be at least DEBUG for showing stacktraces");
		}
	}
	
	/**
	 **/
	@ApiModelProperty(required = true, value = "")
	@JsonProperty("error")
	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("message")
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@JsonProperty("logLevel")
	public String getLogLevel() {
		return logLevel;
	}
	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	@ApiModelProperty(required = true, value = "")
	@JsonProperty("stacktraceArray")
	public String[] getStacktraceArray() {
		return stacktrace.replace("\r\n","\n").replace("\t","").split("\n");
	}
	public void setStacktrace(String stacktrace) {
		this.stacktrace = stacktrace;
	}
	
}
