package org.edu_sharing.restservices.tracking.v1.model;

import org.edu_sharing.restservices.shared.Group;
import org.edu_sharing.restservices.shared.Organization;
import org.edu_sharing.service.tracking.TrackingService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Tracking {
    private final Map<TrackingService.EventType, Integer> counts;
    private final String date;
    private final TrackingAuthority trackingAuthority;
    private final Map<String, Serializable> fields;
    private final Map<TrackingService.EventType, Map<String, Map<String, Long>>> groups;

    public Tracking(String date, TrackingAuthority trackingAuthority, Map<TrackingService.EventType,Integer> counts, Map<String,Serializable> fields, Map<TrackingService.EventType, Map<String, Map<String, Long>>> groups){
        this.date=date;
        this.trackingAuthority = trackingAuthority;
        this.counts=counts;
        this.fields=fields;
        this.groups=groups;

    }

    public Map<TrackingService.EventType, Integer> getCounts() {
        return counts;
    }

    public String getDate() {
        return date;
    }

    public Map<String, Serializable> getFields() {
        return fields;
    }

    public Map<TrackingService.EventType, Map<String, Map<String, Long>>> getGroups() {
        return groups;
    }

    public TrackingAuthority getAuthority() {
        return trackingAuthority;
    }

    public static class TrackingAuthority {
        private String hash;
        private List<Organization> organization;
        private List<Group> mediacenter;

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public List<Organization> getOrganization() {
            return organization;
        }

        public void setOrganization(List<Organization> organization) {
            this.organization = organization;
        }

        public List<Group> getMediacenter() {
            return mediacenter;
        }

        public void setMediacenter(List<Group> mediacenter) {
            this.mediacenter = mediacenter;
        }
    }
}
