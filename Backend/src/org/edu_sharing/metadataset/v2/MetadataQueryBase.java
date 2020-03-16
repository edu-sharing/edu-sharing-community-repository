package org.edu_sharing.metadataset.v2;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MetadataQueryBase implements Serializable{
    static Logger logger = Logger.getLogger(MetadataQueryBase.class);
    protected Map<String, String> basequery;
    private List<MetadataQueryCondition> conditions=new ArrayList<>();

    public void addCondition(MetadataQueryCondition condition) {
        conditions.add(condition);
    }

    public Iterable<MetadataQueryCondition> getConditions() {
        return conditions;
    }

    public Map<String, String> getBasequery() {
        return this.basequery;
    }

    public void setBasequery(Map<String, String> basequery) {
        this.basequery = basequery;
    }
    public String findBasequery(Set<String> existingParameters) {
        List<Map.Entry<String, String>> filter = basequery.entrySet().stream().filter((e) -> {
            if (e.getKey() == null) {
                return false;
            }
            return existingParameters!=null && !existingParameters.contains(e.getKey());
        }).collect(Collectors.toList());
        if(filter.size() == 0) {
            return QueryUtils.replaceCommonQueryParams(basequery.get(null), QueryUtils.luceneReplacer);
        }
        return QueryUtils.replaceCommonQueryParams(filter.get(0).getValue(), QueryUtils.luceneReplacer);
    }
}
