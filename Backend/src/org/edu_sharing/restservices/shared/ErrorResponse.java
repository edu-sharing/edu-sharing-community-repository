package org.edu_sharing.restservices.shared;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.Response;

import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.edu_sharing.restservices.DAODuplicateNodeNameException;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.DAOMissingException;
import org.edu_sharing.restservices.DAOSecurityException;
import org.edu_sharing.restservices.DAOToolPermissionException;
import org.edu_sharing.restservices.DAOValidationException;
import org.edu_sharing.restservices.node.v1.NodeApi;

import com.fasterxml.jackson.annotation.JsonProperty;

@ApiModel(description = "")
public class ErrorResponse {

	private String error = null;
	private String message = null;
	private String stacktrace = null;
	private String logLevel;

	public ErrorResponse() {	
		
	}
	private static Logger logger = Logger.getLogger(ErrorResponse.class);

	public static Response createResponse(Throwable t){
		if(t instanceof DAOValidationException) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();
		}
    	if(t instanceof DAOSecurityException) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();
    	}
        if(t instanceof DAOMissingException) {
    		
    		logger.warn(t.getMessage(), t);
    		return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();
    		
    	}
        if(t instanceof DAODuplicateNodeNameException){
        	logger.info(t.getMessage(), t);
        	return Response.status(Response.Status.CONFLICT).entity(new ErrorResponse(t)).build();
        }
    		
    		logger.error(t.getMessage(), t);
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
    	
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
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class Error {\n");

		sb.append("  error: ").append(error).append("\n");
		sb.append("  message: ").append(message).append("\n");
		sb.append("}\n");
		return sb.toString();
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
