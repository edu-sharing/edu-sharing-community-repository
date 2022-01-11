package org.edu_sharing.service.network.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class Service {
    private String name;
    private String url,icon,logo,inLanguage,type;
    private String description;
    private Collection<Audience> audience;
    private boolean isAccessibleForFree;
    private Provider provider;
    private String startDate;
    private Collection<Interface> interfaces=new ArrayList<>();
    private Collection<String> about=new ArrayList<>();

    public Collection<String> getAbout() {
        return about;
    }

    public void setAbout(Collection<String> about) {
        this.about = about;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getInLanguage() {
        return inLanguage;
    }

    public void setInLanguage(String inLanguage) {
        this.inLanguage = inLanguage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Collection<Audience> getAudience() {
        return audience;
    }

    public void setAudience(Collection<Audience> audience) {
        this.audience = audience;
    }

    @JsonProperty("isAccessibleForFree")
    public boolean isAccessibleForFree() {
        return isAccessibleForFree;
    }

    public void setAccessibleForFree(boolean accessibleForFree) {
        isAccessibleForFree = accessibleForFree;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
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

        private String legalName,url,email;
        private AreaServed areaServed;
        private Location location;

        public String getLegalName() {
            return legalName;
        }

        public void setLegalName(String legalName) {
            this.legalName = legalName;
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

        public AreaServed getAreaServed() {
            return areaServed;
        }

        public void setAreaServed(AreaServed areaServed) {
            this.areaServed = areaServed;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public enum AreaServed{
            Organization,
            City,
            State,
            Country,
            Continent,
            World

        }
        private static class Location {
            private Geo geo;

            public Geo getGeo() {
                return geo;
            }

            public void setGeo(Geo geo) {
                this.geo = geo;
            }

            private static class Geo {
                private Double longitude, latitude;
                private String addressCountry;

                public Double getLongitude() {
                    return longitude;
                }

                public void setLongitude(Double longitude) {
                    this.longitude = longitude;
                }

                public Double getLatitude() {
                    return latitude;
                }

                public void setLatitude(Double latitude) {
                    this.latitude = latitude;
                }

                public String getAddressCountry() {
                    return addressCountry;
                }

                public void setAddressCountry(String addressCountry) {
                    this.addressCountry = addressCountry;
                }
            }
        }
    }
    public static class Audience {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Interface {
        private String url,set,metadataPrefix,documentation;
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

        public String getDocumentation() {
            return documentation;
        }

        public void setDocumentation(String documentation) {
            this.documentation = documentation;
        }

        public enum Format {
            Json,
            XML,
            Text
        }
        public enum Type {
            Search,
            Sitemap,
            Statistics,
            OAI,
            Generic_Api,
        }
    }
}
