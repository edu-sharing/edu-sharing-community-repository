package org.edu_sharing.repository.server.oembed;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class oEmbedBase {
    // fallback for the size when the content has no obtainable size (e.g. a pdf or document)
    public static final int DEFAULT_SIZE = 700;
    // default size for sending thumbnails
    public static final int DEFAULT_THUMBNAIL_SIZE = 500;
    private String type;
    private String version="1.0";
    private String title;
    private String author_name;
    private String author_url;
    private String provider_name;
    private String provider_url;
    private String cache_age;
    private String thumbnail_url;
    private int thumbnail_width;
    private int thumbnail_height;
    private String url;
    private String html;
    private int width;
    private int height;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor_name() {
        return author_name;
    }

    public void setAuthor_name(String author_name) {
        this.author_name = author_name;
    }

    public String getAuthor_url() {
        return author_url;
    }

    public void setAuthor_url(String author_url) {
        this.author_url = author_url;
    }

    public String getProvider_name() {
        return provider_name;
    }

    public void setProvider_name(String provider_name) {
        this.provider_name = provider_name;
    }

    public String getProvider_url() {
        return provider_url;
    }

    public void setProvider_url(String provider_url) {
        this.provider_url = provider_url;
    }

    public String getCache_age() {
        return cache_age;
    }

    public void setCache_age(String cache_age) {
        this.cache_age = cache_age;
    }

    public String getThumbnail_url() {
        return thumbnail_url;
    }

    public void setThumbnail_url(String thumbnail_url) {
        this.thumbnail_url = thumbnail_url;
    }

    public int getThumbnail_width() {
        return thumbnail_width;
    }

    public void setThumbnail_width(int thumbnail_width) {
        this.thumbnail_width = thumbnail_width;
    }

    public int getThumbnail_height() {
        return thumbnail_height;
    }

    public void setThumbnail_height(int thumbnail_height) {
        this.thumbnail_height = thumbnail_height;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

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
