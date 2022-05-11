package org.edu_sharing.metadataset.v2;

import java.io.Serializable;

public class MetadataQueryCondition implements Serializable {
    MetadataCondition condition;
    String queryTrue,queryFalse;

    public MetadataCondition getCondition() {
        return condition;
    }

    public void setCondition(MetadataCondition condition) {
        this.condition = condition;
    }

    public String getQueryTrue() {
        return queryTrue;
    }

    public void setQueryTrue(String queryTrue) {
        this.queryTrue = queryTrue;
    }

    public String getQueryFalse() {
        return queryFalse;
    }

    public void setQueryFalse(String queryFalse) {
        this.queryFalse = queryFalse;
    }
}
