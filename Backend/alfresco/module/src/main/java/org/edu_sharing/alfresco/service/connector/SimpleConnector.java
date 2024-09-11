package org.edu_sharing.alfresco.service.connector;

import com.typesafe.config.Optional;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * a simple connector uses a simple http call to a given api and stores the result as a link element
 */
@Data
public class SimpleConnector implements Serializable {
	
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
