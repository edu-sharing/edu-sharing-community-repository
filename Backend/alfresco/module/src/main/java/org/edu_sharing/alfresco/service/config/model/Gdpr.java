package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;

import java.io.Serializable;

public class Gdpr implements Serializable {
    @XmlElement public boolean enabled;
    @XmlElement public GdprEntry[] entry;
}
