package org.edu_sharing.service.tracking.model;

import org.edu_sharing.service.tracking.TrackingService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StatisticEntry implements Comparable<StatisticEntry>{
    protected String date;
    protected Map<String,Serializable> data=new HashMap<>();

    protected Map<TrackingService.EventType,Integer> counts=new HashMap<>();
    private final AuthorityInfo authorityInfo = new AuthorityInfo();

    public Map<TrackingService.EventType, Integer> getCounts() {
        return counts;
    }

    public StatisticEntry() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Map<String, Serializable> getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatisticEntry that = (StatisticEntry) o;
        return Objects.equals(date, that.date) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {

        return Objects.hash(date);
    }

    public AuthorityInfo getAuthorityInfo() {
        return authorityInfo;
    }

    @Override
    public int compareTo(StatisticEntry o) {
        return date==null ? 0 : date.compareTo(o.date);
    }

    public static class AuthorityInfo {
        private String authority;
        private String[] organizations;
        private String[] mediacenters;

        public String getAuthority() {
            return authority;
        }

        public void setAuthority(String authority) {
            this.authority = authority;
        }

        public String[] getOrganizations() {
            return organizations;
        }

        public void setOrganizations(String[] organizations) {
            this.organizations = organizations;
        }

        public String[] getMediacenters() {
            return mediacenters;
        }

        public void setMediacenters(String[] mediacenters) {
            this.mediacenters = mediacenters;
        }
    }
}
