package org.edu_sharing.repository.server.jobs.quartz;

import java.util.List;

public class JobDescription {
    String name;
    String description;
    List<JobFieldDescription> params;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<JobFieldDescription> getParams() {
        return this.params;
    }

    public void setParams(List<JobFieldDescription> params) {
        this.params = params;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static class JobFieldDescription{
        String name;
        Class<?> type;
        String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Class<?> getType() {
            return type;
        }

        public void setType(Class<?> type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
