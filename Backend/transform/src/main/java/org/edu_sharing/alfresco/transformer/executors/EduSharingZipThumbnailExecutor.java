package org.edu_sharing.alfresco.transformer.executors;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.executors.AbstractCommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.edu_sharing.alfresco.transformer.executors.tools.Commands;
import org.edu_sharing.alfresco.transformer.executors.tools.ZipTool;
import org.edu_sharing.repository.server.tools.ImageTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.Map;

@Component
public class EduSharingZipThumbnailExecutor extends AbstractCommandExecutor implements CustomTransformerFileAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(EduSharingZipThumbnailExecutor.class);

    public static String ID = "EduSharingZipThumbnailExecutor";

    private static final int MAX_BUFFERED_IMAGE_SIZE = 2 * (1024 * 1000);

    /**
     * false: no preview image is extracted from h5p files
     */
    @Value("${edu-sharing.transformer.zip-thumbnail.h5p.enabled:true}")
    private boolean h5pExtractionEnabled;

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
            InputStream fallback = null;
            InputStream contentImage = null;
            ArchiveInputStream zip = ZipTool.getZipInputStream(sourceFile);
            while (true) {
                ArchiveEntry entry = zip.getNextEntry();
                if(entry==null) {
                    logger.info("entry is null");
                    break;
                }
                String name=entry.getName().toLowerCase();


                //fallback image found in root
                if(!name.contains("/") && (name.endsWith(".jpg") || name.endsWith(".png")) && entry.getSize() > -1 && entry.getSize() < MAX_BUFFERED_IMAGE_SIZE ) {
                    InputStream image = readEntry(zip);
                    if(image != null) {
                        fallback = image;
                        logger.info("found potential fallback:" + name);
                    }
                }

                //h5p extraction disabled: no thumbnail, repository uses its default preview
                if(!h5pExtractionEnabled && name.equals("h5p.json")){
                    logger.info("h5p preview extraction disabled");
                    return;
                }

                //h5p
                if(name.startsWith("content/images") && (name.endsWith(".jpg") || name.endsWith(".png"))){
                    if(h5pExtractionEnabled) {
                        logger.info("found preview in zip");
                        processImage(targetFile, zip);
                        return;
                    }
                    // might not be a h5p, decide when zip is fully read
                    if(contentImage == null) {
                        contentImage = readEntry(zip);
                        logger.info("found potential content image:" + name);
                    }
                }
                //geogebra
                if(name.endsWith("geogebra_thumbnail.png")){
                    processImage(targetFile,zip);
                    return;
                }
            }
            if(contentImage != null){
                logger.info("processing content image");
                processImage(targetFile,contentImage);
                return;
            }
            if(fallback != null){
                logger.info("processing fallback");
                processImage(targetFile,fallback);
                return;
            }
            logger.info("no thumbnail found in zip file");
        }catch(Throwable t){
            logger.error(t.getMessage(),t);
        }
    }

    /**
     * buffers the current zip entry, returns null if it exceeds MAX_BUFFERED_IMAGE_SIZE
     */
    private InputStream readEntry(ArchiveInputStream zip) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zip.read(buffer)) > 0) {
            outputStream.write(buffer, 0, len);
            if(outputStream.size() > MAX_BUFFERED_IMAGE_SIZE) {
                logger.info("zip entry too large to buffer, skipping");
                return null;
            }
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public void processImage(File targetFile, InputStream in) throws IOException {
        OutputStream os = new FileOutputStream(targetFile);
        InputStream is = ImageTool.autoRotateImage(in, ImageTool.MAX_THUMB_SIZE);
        StreamUtils.copy(is,os);
        os.close();
    }
}
