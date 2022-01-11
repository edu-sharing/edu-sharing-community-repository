package org.edu_sharing.service.mediacenter;

import java.util.Date;
import java.util.List;

public interface MediacenterLicenseProvider {

    /**
     * get all nodes currently licensed for this mediacenter
     * @param mediacenter
     * @return
     */
    public List<String> getNodes(String mediacenter);

    /**
     * get all nodes new licensed for this mediacenter in period {@param from, @param until}
     * @param mediacenter
     * @param from
     * @param until
     * @return
     */
    public List<String> getNodes(String mediacenter, Date from, Date until);

    /**
     * get all nodes not longer licensed in period {@param from, @param until}
     * @param mediacenter
     * @param from
     * @param until
     * @return
     */
    public List<String> getNodesLicenseRemoved(String mediacenter, Date from, Date until);

    default public String getSet(){
        return "commercial";
    }

    default public String getCatalogId(){
        return "default";
    }
}
