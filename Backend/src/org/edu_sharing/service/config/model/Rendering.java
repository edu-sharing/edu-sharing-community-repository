package org.edu_sharing.service.config.model;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Rendering implements Serializable {
	@XmlElement Boolean showPreview;
	@XmlElement Boolean showDownloadButton;
}
