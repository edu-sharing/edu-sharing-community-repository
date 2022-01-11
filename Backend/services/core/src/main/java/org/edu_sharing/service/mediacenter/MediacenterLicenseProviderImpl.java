package org.edu_sharing.service.mediacenter;



import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MediacenterLicenseProviderImpl implements MediacenterLicenseProvider {

    Logger logger = Logger.getLogger(MediacenterLicenseProviderImpl.class);

    @Override
    public List<String> getNodes(String mediacenter) {
        logger.error("not yet implemented");
        return new ArrayList<>();
    }

    @Override
    public List<String> getNodes(String mediacenter, Date from, Date until) {
        logger.error("not yet implemented");
        return new ArrayList<>();
    }

    @Override
    public List<String> getNodesLicenseRemoved(String mediacenter, Date from, Date until) {
        logger.error("not yet implemented");
        return new ArrayList<>();
    }
}
