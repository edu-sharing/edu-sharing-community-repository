package org.edu_sharing.alfresco.transformer.executors;

import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.executors.AbstractCommandExecutor;
import org.alfresco.transformer.executors.RuntimeExec;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;

import java.io.*;
import java.nio.file.CopyOption;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.edu_sharing.repository.server.tools.ImageTool;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class H5pThumbnailExecutor extends AbstractCommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(H5pThumbnailExecutor.class);

    public static String ID = "EduSharingH5pThumbnailExecutor";

    @Override
    protected RuntimeExec createTransformCommand() {

        /**
         * @TODO dummy command
         */
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*", new String[] { "ffmpeg", "-version" });
        runtimeExec.setCommandsAndArguments(commandsAndArguments);
        return runtimeExec;
    }

    @Override
    protected RuntimeExec createCheckCommand() {
        return createTransformCommand();
    }

    @Override
    public String getTransformerId() {
        return ID;
    }

    @Override
    public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions, File sourceFile, File targetFile) throws TransformException {
        this.transform(null, targetMimetype, transformOptions, sourceFile, targetFile);
    }

    @Override
    public void transform(String transformName, String sourceMimetype, String targetMimetype, Map<String, String> transformOptions, File sourceFile, File targetFile) throws Exception {

        logger.info("sourceMimetype:"+sourceMimetype+" targetMimetype:"+targetMimetype+" sourceFile:"+sourceFile +" targetFile:"+targetFile);
        if(transformOptions != null)
            transformOptions.entrySet().stream().forEach(e -> System.out.println("o:"+ e.getKey() + " "+e.getValue()));

        Files.copy(sourceFile.toPath(), Path.of("/tmp/source.h5p"), StandardCopyOption.REPLACE_EXISTING);

        try {
            ArchiveInputStream zip = getZipInputStream(sourceFile);
            while (true) {
                ArchiveEntry entry = zip.getNextEntry();
                if(entry==null) {
                    logger.info("entry is null");
                    break;
                }
                String name=entry.getName().toLowerCase();
                if(name.startsWith("content/images") && (name.endsWith(".jpg") || name.endsWith(".png"))){

                    logger.info("found preview in zip");

                    OutputStream os = new FileOutputStream(targetFile);
                    InputStream is = ImageTool.autoRotateImage(zip, ImageTool.MAX_THUMB_SIZE);
                    StreamUtils.copy(is,os);
                    os.close();
                    return;
                }
            }
        }
        catch(Throwable t){
            logger.error(t.getMessage(),t);
        }

    }

    public static ArchiveInputStream getZipInputStream(File sourceFile) throws IOException {

        InputStream is = new FileInputStream(sourceFile);

        /*
        Attention this kills the inputstream and leads to

        java.util.zip.ZipException: Unexpected record signature: 0X302D3831

        when calling


        Tika tika = new Tika();
        String type = tika.detect(is);
        logger.info("type:" + type);*/

        Tika tika = new Tika();
        String type = tika.detect(sourceFile);
        logger.info("type:" + type);

        if(type == null) {

        }else if(type.equals("application/gzip")) {

            try {
                final InputStream bis = new BufferedInputStream(is);
                CompressorInputStream cis = null;
                cis = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP,
                        bis);
                return new TarArchiveInputStream(cis);

            }catch(CompressorException e) {
                logger.error(e.getMessage());
            }
        }else if(type.equals("application/zip")) {

            String encoding = System.getProperty("file.encoding");
            logger.info("encoding:"+encoding);
            // allowStoredEntriesWithDataDescriptor = true because some h5p might have this
            return new ZipArchiveInputStream(is, encoding, true, true);
        }else {
            logger.info("unknown format:" +  type);
        }
        is.close();
        return null;
    }
}
