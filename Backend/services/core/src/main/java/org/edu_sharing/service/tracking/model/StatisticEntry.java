package org.edu_sharing.service.tracking.model;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
public class StatisticEntry implements Comparable<StatisticEntry> {
    protected String date;
    protected Map<String, Serializable> fields = new HashMap<>();
    protected Map<String, Map<String, Map<String, Long>>> groups = new HashMap<>();

    protected Map<String, Integer> counts = new HashMap<>();
    private final AuthorityInfo authorityInfo = new AuthorityInfo();


    public StatisticEntry() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatisticEntry that = (StatisticEntry) o;
        return Objects.equals(date, that.date) && Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, fields);
    }

    @Override
    public int compareTo(StatisticEntry o) {
        return date == null ? 0 : date.compareTo(o.date);
    }

    @Data
    public static class AuthorityInfo {
        private String authority;
        private String[] organizations;
        private String[] mediacenters;
    }
}
