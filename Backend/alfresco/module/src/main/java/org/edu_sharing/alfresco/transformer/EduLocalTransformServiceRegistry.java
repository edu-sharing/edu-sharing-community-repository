package org.edu_sharing.alfresco.transformer;

import java.util.Map;

public class EduLocalTransformServiceRegistry extends org.alfresco.repo.content.transform.LocalTransformServiceRegistry{

    public static final String TRANSFORM_OPTION_RESOURCETYPE = "eduResourceType";

    @Override
    public long findMaxSize(String sourceMimetype, String targetMimetype, Map<String, String> options, String renditionName) {

        //disable cache to prevent override default rendition
        if(options.containsKey(TRANSFORM_OPTION_RESOURCETYPE)){
            renditionName = null;
        }

        return super.findMaxSize(sourceMimetype,targetMimetype,options,renditionName);
    }

    @Override
    public String findTransformerName(String sourceMimetype, long sourceSizeInBytes, String targetMimetype, Map<String, String> actualOptions, String renditionName) {

        //disable cache to prevent override default rendition
        if(actualOptions.containsKey(TRANSFORM_OPTION_RESOURCETYPE)){
            renditionName = null;
        }
        return super.findTransformerName(sourceMimetype, sourceSizeInBytes, targetMimetype, actualOptions, renditionName);
    }
}
