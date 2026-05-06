package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class SessionExpiredDialog implements Serializable {
	@Schema(description = "If true (default), show dialog when session expires (client-side notification)")
	@XmlElement Boolean show;
}
