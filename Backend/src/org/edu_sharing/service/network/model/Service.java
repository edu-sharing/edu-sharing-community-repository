package org.edu_sharing.service.network.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.Date;

public class Service {
    private String name;
    private String url,icon,logo,country,primaryLanguage,type;
    private Collection<Description> description;
    private Collection<Audience> audience;
    private Collection<String> scope;
    private boolean isPublic;
    private Coverage coverage;
    private Provider provider;
    private long creationdate;
    private Collection<Interface> interfaces;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Coverage getCoverage() {
        return coverage;
    }

    public void setCoverage(Coverage coverage) {
        this.coverage = coverage;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPrimaryLanguage() {
        return primaryLanguage;
    }

    public void setPrimaryLanguage(String primaryLanguage) {
        this.primaryLanguage = primaryLanguage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Collection<Description> getDescription() {
        return description;
    }

    public void setDescription(Collection<Description> description) {
        this.description = description;
    }

    public Collection<Audience> getAudience() {
        return audience;
    }

    public void setAudience(Collection<Audience> audience) {
        this.audience = audience;
    }

    public Collection<String> getScope() {
        return scope;
    }

    public void setScope(Collection<String> scope) {
        this.scope = scope;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public long getCreationdate() {
        return creationdate;
    }

    public void setCreationdate(long creationdate) {
        this.creationdate = creationdate;
    }

    public Collection<Interface> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(Collection<Interface> interfaces) {
        this.interfaces = interfaces;
    }

    public static class Description {
        private String value,language;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }

    public static class Provider {
        private String name,country,url,email,admin;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getAdmin() {
            return admin;
        }

        public void setAdmin(String admin) {
            this.admin = admin;
        }
    }
    public static class Audience {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class Interface {
        private String url,set,metadataPrefix;
        private Format format;
        private Type type;

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSet() {
            return set;
        }

        public void setSet(String set) {
            this.set = set;
        }

        public String getMetadataPrefix() {
            return metadataPrefix;
        }

        public void setMetadataPrefix(String metadataPrefix) {
            this.metadataPrefix = metadataPrefix;
        }

        public Format getFormat() {
            return format;
        }

        public void setFormat(Format format) {
            this.format = format;
        }

        enum Format {
            Json,
            XML,
            Text
        }
        enum Type {
            Search,
            Sitemap,
            Statistics,
            OAI,
            Generic_Api,
        }
    }
    enum Coverage{
        Organization,
        City,
        State,
        Country,
        Continent,
        World

    }
}
