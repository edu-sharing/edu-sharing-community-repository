package org.edu_sharing.alfresco.service.connector;

import com.typesafe.config.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * a simple connector uses a simple http call to a given api and stores the result as a link element
 */
@Data
public class SimpleConnector implements Serializable {
	@Data
	@AllArgsConstructor
	public static class ConnectorRequest {
		Map<String, String[]> requestParameters;
		SimpleConnector simpleConnector;
		NodeRef nodeRefOriginal;
	}
	public interface PostRequestHandler {
		/**
		 * handle the request and return the properties that should be added to the node
		 * @param request
		 * @param result
		 */
		Map<String, Serializable> handleRequest(ConnectorRequest request, JSONObject result);
	}
	public interface BodyHandler {
		/**
		 * handle and additional mapping of params
		 */
		List<BasicNameValuePair> handle(List<BasicNameValuePair> pairs, Map<String, String[]> requestParameters, SimpleConnector simpleConnector);
	}
	private String id;

	private String icon;

	@Optional private boolean showNew=true;

	@Optional private boolean onlyDesktop=false;

	@Optional private SimpleConnectorApi api;


	@Optional private String mdsGroup;

	private List<ConnectorFileType> filetypes;
	@Data
	public static class SimpleConnectorApi {
		private Method method;
		private String url;
		/**
		 * Java class implementing PostRequestHandler
		 */
		@Optional private String postRequestHandler;
		/**
		 * Java class implementing BodyHandler
		 */
		@Optional private String bodyHandler;
		@Optional private SimpleConnectorAuthentication authentication;
		@Optional private BodyType bodyType;
		private Map<String, Object> body;
		public enum Method {
			Post
		}
		public enum BodyType {
			Form,
		}
	}
	@Data
	public static class SimpleConnectorAuthentication {
		AuthenticationType type;
		@Optional private String url;
		private SimpleConnectorApi.Method method;
		@Optional private SimpleConnectorApi.BodyType bodyType;
		@Optional private Map<String, Object> body;

		public enum AuthenticationType {
			OAuth
		}
	}
}
