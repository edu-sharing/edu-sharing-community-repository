package org.edu_sharing.alfresco.transformer.executors.tools;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.tika.Tika;
import org.edu_sharing.alfresco.transformer.executors.EduSharingZipThumbnailExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class ZipTool {

    private static final Logger logger = LoggerFactory.getLogger(ZipTool.class);

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
