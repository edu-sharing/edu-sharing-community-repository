package org.edu_sharing.service.oai;

import javax.xml.parsers.ParserConfigurationException;

import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.exporter.OAILOMExporter;

public class OAIExporterService {
	
	
	String outputDir;

	public OAIExporterService() {
		outputDir = OAIProperties.instance.getOaiPath();
	}
	
	
	public void export(String nodeId) {
		OAIExporterFactory.getOAILOMExporter().export(outputDir,nodeId);
	}
	
	public boolean available(){
		if(OAIProperties.instance.getOaiPath() != null && OAIProperties.instance.getOaiPath().trim().length() > 0) {
			return true;
		}else {
			return false;
		}
	}

	
}
