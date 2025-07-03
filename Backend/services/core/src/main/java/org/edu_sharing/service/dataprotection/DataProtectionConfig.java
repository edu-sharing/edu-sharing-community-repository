package org.edu_sharing.service.dataprotection;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.alfresco.repo.content.ContentStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class DataProtectionConfig {

    @Autowired
    ContentStore fileContentStore;

    @Getter
    String mainPath;

    @PostConstruct
    public void init(){
        mainPath = new File(fileContentStore.getRootLocation()).getParentFile().getPath().concat("/dataprotection");
        File dir = new File(mainPath);
        if (!dir.exists()) {
            boolean mkdirs = dir.mkdirs();// creates parent folders as needed
            if (!mkdirs) {
                throw new RuntimeException("Unable to create directory " + dir.getAbsolutePath());
            }
        }
    }

}
