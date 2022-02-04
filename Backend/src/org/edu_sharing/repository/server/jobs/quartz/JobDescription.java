package org.edu_sharing.repository.server.jobs.quartz;

import java.util.List;

public class JobDescription {
    String name;
    String description;
    List<JobFieldDescription> params;
    private org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription.JobTag[] tags;

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

    public void setTags(org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription.JobTag[] tags) {
        this.tags = tags;
    }

    public org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription.JobTag[] getTags() {
        return tags;
    }

    public static class JobFieldDescription{
        String name;
        Class<?> type;
        String description;
        boolean file;
        List<JobFieldDescription> values;
        String sampleValue;
        private boolean isArray;

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

        public boolean isFile() {
            return file;
        }

        public void setFile(boolean file) {
            this.file = file;
        }

        public List<JobFieldDescription> getValues() {
            return values;
        }

        public void setValues(List<JobFieldDescription> values) {
            this.values = values;
        }

        public void setSampleValue(String sampleValue) {
            this.sampleValue = sampleValue;
        }

        public String getSampleValue() {
            return sampleValue;
        }

        public void setIsArray(boolean array) {
            this.isArray = array;
        }

        public boolean isArray() {
            return isArray;
        }
    }
}
