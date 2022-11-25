package org.alfresco.repo.module;

import org.alfresco.util.VersionNumber;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.Serializable;

public class EduModuleVersionNumber extends ModuleVersionNumber {
    public EduModuleVersionNumber(ModuleVersionNumber versionNumber){
        delegate = versionNumber.delegate;
    }

    public EduModuleVersionNumber(String versionString)
    {
        delegate = new ComparableVersion(versionString);
    }

    public EduModuleVersionNumber(VersionNumber versionCurrent)
    {
        this(versionCurrent.toString());
    }

    @Override
    public int compareTo(ModuleVersionNumber installingVersion) {
        int value = super.compareTo(installingVersion);
        // we want to support downgrading here!
        return Math.min(value, 0);
    }
}
