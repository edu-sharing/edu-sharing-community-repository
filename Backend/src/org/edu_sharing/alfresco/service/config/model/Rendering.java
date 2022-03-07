package org.edu_sharing.alfresco.service.config.model;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Rendering implements Serializable {
	@XmlElement public Boolean showPreview;
	@XmlElement public Boolean showDownloadButton;
	@XmlElement public Boolean prerender;
}
