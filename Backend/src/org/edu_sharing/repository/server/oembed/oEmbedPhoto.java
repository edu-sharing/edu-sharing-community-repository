package org.edu_sharing.repository.server.oembed;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "oembed")
public class oEmbedPhoto extends oEmbedBase{
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
