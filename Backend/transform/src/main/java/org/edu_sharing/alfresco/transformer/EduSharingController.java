package org.edu_sharing.alfresco.transformer;

import org.alfresco.transformer.AbstractTransformerController;
import org.alfresco.transformer.probes.ProbeTestTransform;
import org.edu_sharing.alfresco.transformer.executors.H5pThumbnailExecutor;
import org.edu_sharing.alfresco.transformer.executors.SerloIndexExecutor;
import org.edu_sharing.alfresco.transformer.executors.VideoThumbnailExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.util.Collections;
import java.util.Map;

@Controller
public class EduSharingController extends AbstractTransformerController {
    private static final Logger logger = LoggerFactory.getLogger(EduSharingController.class);
    @Autowired
    VideoThumbnailExecutor videoThumbnailExecutor;

    @Autowired
    H5pThumbnailExecutor h5pThumbnailExecutor;

    @Autowired
    SerloIndexExecutor serloIndexExecutor;


    @Override
    public void transformImpl(String transformName, String sourceMimetype, String targetMimetype, Map<String, String> transformOptions, File sourceFile, File targetFile) {

        logger.info("sourceMimetype:"+sourceMimetype+" targetMimetype:"+targetMimetype+" sourceFile:"+sourceFile +" targetFile:"+targetFile);
        if(transformOptions != null)
            transformOptions.entrySet().stream().forEach(e -> logger.info("o:"+ e.getKey() + " "+e.getValue()));

        if(transformName.equals(VideoThumbnailExecutor.ID)){
            try {
                this.videoThumbnailExecutor.transform(transformName,sourceMimetype,targetMimetype,transformOptions,sourceFile,targetFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else if(transformName.equals(H5pThumbnailExecutor.ID)){
            try {
                this.h5pThumbnailExecutor.transform(transformName,sourceMimetype,targetMimetype,transformOptions,sourceFile,targetFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else if(transformName.equals("EduSharingVideoMetadataExecutor")){
            try{
                this.videoThumbnailExecutor.extractMetadata(transformName,sourceMimetype,targetMimetype,transformOptions,
                        sourceFile,targetFile);
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }else if(transformName.equals(SerloIndexExecutor.ID)){
            try {
                this.serloIndexExecutor.transform(transformName,sourceMimetype,targetMimetype,transformOptions,sourceFile,targetFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }else{
            logger.error("unsupported transformer:" + transformName);
        }
    }


    @Override
    public String getTransformerName() {
        return "edu-sharing transformations";
    }


    @Override
    public ProbeTestTransform getProbeTestTransform() {
        return new ProbeTestTransform(this,"spark.mp4","spark.jpeg",5526, 20, 150, 1024,
                1,1) {
            @Override
            protected void executeTransformCommand(File sourceFile, File targetFile) {
                Map<String, String> transformOptions = Collections.singletonMap("language", "Spanish");
                transformImpl(VideoThumbnailExecutor.ID, "video/mp4", "image/jpeg", transformOptions, sourceFile, targetFile);
            }
        };
    }

    @Override
    public String version() {
        return getTransformerName() + " available";
    }
}
