package org.edu_sharing.alfresco.service.connector;

import java.io.Serializable;
import java.util.List;

public class ConnectorList implements Serializable {

	List<Connector> connectors;
		
	public List<Connector> getConnectors() {
		return connectors;
	}
	
	public void setConnectors(List<Connector> connectors) {
		this.connectors = connectors;
	}
}
