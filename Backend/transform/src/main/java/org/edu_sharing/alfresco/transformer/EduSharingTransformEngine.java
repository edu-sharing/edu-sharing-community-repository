package org.edu_sharing.alfresco.transformer;

import org.alfresco.transform.base.TransformEngine;
import org.alfresco.transform.base.probes.ProbeTransform;
import org.alfresco.transform.config.TransformConfig;
import org.alfresco.transform.config.reader.TransformConfigResourceReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class EduSharingTransformEngine implements TransformEngine {

    @Autowired
    private TransformConfigResourceReader transformConfigResourceReader;
    @Override
    public String getTransformEngineName() {
        return "edu-sharing transformations";
    }

    @Override
    public String getStartupMessage() {
        return "Startup "+getTransformEngineName();
    }

    @Override
    public TransformConfig getTransformConfig() {
        return transformConfigResourceReader.read("classpath:engine_config.json");
    }

    @Override
    public ProbeTransform getProbeTransform() {


        return new ProbeTransform("spark.mp4", "video/mp4", "image/jpeg",Collections.emptyMap(),20,150,1024,1,1,1);

    }
}
