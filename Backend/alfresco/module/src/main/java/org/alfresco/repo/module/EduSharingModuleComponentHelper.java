package org.alfresco.repo.module;

import org.alfresco.repo.module.tool.ModuleManagementToolException;
import org.alfresco.util.VersionNumber;

import java.io.Serializable;

public class EduSharingModuleComponentHelper extends ModuleComponentHelper {

    @Override
    ModuleVersionNumber getVersion(String moduleId) {
        return super.getVersion(moduleId);
    }

    @Override
    protected ModuleVersionNumber getModuleVersionNumber(Serializable moduleVersion)
    {

        //return new EduSharingModuleVersionNumber();
        if (moduleVersion instanceof ModuleVersionNumber) return new EduModuleVersionNumber((ModuleVersionNumber)moduleVersion);
        if (moduleVersion instanceof VersionNumber) return new EduModuleVersionNumber((VersionNumber)moduleVersion);
        if (moduleVersion instanceof String) return new EduModuleVersionNumber((String)moduleVersion);
        throw new ModuleManagementToolException("Invalid moduleVersion");
    }


}
