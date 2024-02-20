package org.edu_sharing.alfresco.transformer.executors;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.executors.AbstractCommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.tika.Tika;
import org.edu_sharing.alfresco.transformer.executors.tools.Commands;
import org.edu_sharing.alfresco.transformer.executors.tools.ZipTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.edu_sharing.repository.server.tools.ImageTool;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class EduSharingZipThumbnailExecutor extends AbstractCommandExecutor implements CustomTransformerFileAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(EduSharingZipThumbnailExecutor.class);

    public static String ID = "EduSharingZipThumbnailExecutor";

    @Override
    protected RuntimeExec createTransformCommand() {

        /**
         * @TODO dummy command
         */
        return Commands.getFFMPegRuntimeExec();
    }

    @Override
    protected RuntimeExec createCheckCommand() {
        return createTransformCommand();
    }


    @Override
    public String getTransformerName() {
        return ID;
    }

    @Override
    public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions, File sourceFile, File targetFile, TransformManager transformManager) throws Exception {
        logger.info("sourceMimetype:"+sourceMimetype+" targetMimetype:"+targetMimetype+" sourceFile:"+sourceFile +" targetFile:"+targetFile);
        if(transformOptions != null)
            transformOptions.entrySet().stream().forEach(e -> System.out.println("o:"+ e.getKey() + " "+e.getValue()));

        //Files.copy(sourceFile.toPath(), Path.of("/tmp/source.h5p"), StandardCopyOption.REPLACE_EXISTING);

        try {
            ArchiveInputStream zip = ZipTool.getZipInputStream(sourceFile);
            while (true) {
                ArchiveEntry entry = zip.getNextEntry();
                if(entry==null) {
                    logger.info("entry is null");
                    break;
                }
                String name=entry.getName().toLowerCase();
                //h5p
                if(name.startsWith("content/images") && (name.endsWith(".jpg") || name.endsWith(".png"))){

                    logger.info("found preview in zip");

                    OutputStream os = new FileOutputStream(targetFile);
                    InputStream is = ImageTool.autoRotateImage(zip, ImageTool.MAX_THUMB_SIZE);
                    StreamUtils.copy(is,os);
                    os.close();
                    return;
                }
                //geogebra
                if(name.endsWith("geogebra_thumbnail.png")){
                    OutputStream os = new FileOutputStream(targetFile);
                    InputStream is = ImageTool.autoRotateImage(zip, ImageTool.MAX_THUMB_SIZE);
                    StreamUtils.copy(is,os);
                    os.close();
                    return;
                }
            }
            logger.info("no thumbnail found in zip file");
        }
        catch(Throwable t){
            logger.error(t.getMessage(),t);
        }
    }
}
