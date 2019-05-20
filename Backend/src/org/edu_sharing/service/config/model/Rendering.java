package org.edu_sharing.service.config.model;
import javax.xml.bind.annotation.XmlElement;

public class Rendering {
	@XmlElement Boolean showPreview;
	@XmlElement Boolean showDownloadButton;
	@XmlElement Boolean prerender;
}
