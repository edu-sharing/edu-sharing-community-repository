package org.edu_sharing.repository.server.oembed;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "oembed")
public class oEmbedVideo extends oEmbedBase{
    private String html;
    private int width;
    private int height;

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
