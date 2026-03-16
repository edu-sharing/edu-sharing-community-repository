package org.edu_sharing.service.license;

import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.Map;

public class H5PLicenseMapper {

    public final static Map<String, String> ccMap = Map.of("CC BY", CCConstants.COMMON_LICENSE_CC_BY,
            "CC BY-SA", CCConstants.COMMON_LICENSE_CC_BY_SA,
            "CC BY-NC", CCConstants.COMMON_LICENSE_CC_BY_NC,
            "CC BY-NC-SA", CCConstants.COMMON_LICENSE_CC_BY_NC_SA,
            "CC BY-ND", CCConstants.COMMON_LICENSE_CC_BY_ND,
            "CC0", CCConstants.COMMON_LICENSE_CC_ZERO,
            "PD", CCConstants.COMMON_LICENSE_PDM
    );

    public static String get(String h5pLicenseKey){
       return ccMap.get(h5pLicenseKey);
    }


}
