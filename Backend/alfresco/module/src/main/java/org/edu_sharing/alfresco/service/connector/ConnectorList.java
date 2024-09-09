package org.edu_sharing.alfresco.service.connector;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ConnectorList implements Serializable {

	List<Connector> connectors;
	List<SimpleConnector> simpleConnectors;
}
