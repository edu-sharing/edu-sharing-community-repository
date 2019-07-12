package org.edu_sharing.service.tracking.model;

import org.edu_sharing.service.tracking.TrackingService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StatisticEntry implements Comparable<StatisticEntry>{
    protected String date;
    protected Map<String,String> data=new HashMap<>();

    protected Map<TrackingService.EventType,Integer> counts=new HashMap<>();
    private String authority;

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

    public Map<String, String> getData() {
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

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }

    @Override
    public int compareTo(StatisticEntry o) {
        return date==null ? 0 : date.compareTo(o.date);
    }
}
