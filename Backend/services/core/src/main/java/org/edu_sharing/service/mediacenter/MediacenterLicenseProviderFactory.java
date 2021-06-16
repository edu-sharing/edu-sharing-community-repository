package org.edu_sharing.service.mediacenter;

import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.spring.ApplicationContextFactory;

public class MediacenterLicenseProviderFactory {
    public static MediacenterLicenseProvider getMediacenterLicenseProvider(){
        return (MediacenterLicenseProvider) ApplicationContextFactory.getApplicationContext().getBean("mediacenterLicenseProvider");
    }
}
