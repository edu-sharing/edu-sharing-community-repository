package org.edu_sharing.metadataset.v2;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MetadataQueryBase implements Serializable{
    static Logger logger = Logger.getLogger(MetadataQueryBase.class);
    protected String basequery;
    private List<MetadataQueryCondition> conditions=new ArrayList<>();

    public void addCondition(MetadataQueryCondition condition) {
        conditions.add(condition);
    }

    public Iterable<MetadataQueryCondition> getConditions() {
        return conditions;
    }

    public String getBasequery() {
        return QueryUtils.replaceCommonQueryParams(basequery, QueryUtils.luceneReplacer);
    }

    public void setBasequery(String basequery) {
        this.basequery = basequery;
    }
}
