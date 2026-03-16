package org.edu_sharing.alfresco.transformer.extractors;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.metadata.AbstractMetadataExtractorEmbedder;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.tika.utils.StringUtils;
import org.edu_sharing.alfresco.transformer.executors.tools.ZipTool;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.service.license.H5PLicenseMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.alfresco.transform.base.metadata.AbstractMetadataExtractorEmbedder.Type.EXTRACTOR;

@Component
public class ZipMetadataExtractor extends AbstractMetadataExtractorEmbedder {

    public static String ID = "EduSharingZipMetadataExtractor";

    public static String TITLE = "title";
    public static String LICENSE = "license";
    public static String LICENSEVERSION = "licenseVersion";
    public static String AUTHOR = "author";
    public static String AUTHOREDITOR = "authorEditor";

    private static final Logger logger = LoggerFactory.getLogger(ZipMetadataExtractor.class);

    public ZipMetadataExtractor() {
        super(EXTRACTOR, logger);
    }

    @Override
    public void embedMetadata(String sourceMimetype, InputStream inputStream, String targetMimetype, OutputStream outputStream, Map<String, String> transformOptions, TransformManager transformManager) throws Exception {
        logger.info("called");
    }

    @Override
    public Map<String, Serializable> extractMetadata(String sourceMimetype, InputStream inputStream, String targetMimetype, OutputStream outputStream, Map<String, String> transformOptions, TransformManager transformManager) throws Exception {

        logger.info("called");


        Map<String, Serializable> result = new HashMap<>();

        File sourceFile = transformManager.createSourceFile();
        try {
            ArchiveInputStream zip = ZipTool.getZipInputStream(sourceFile);
            while (true) {
                ArchiveEntry entry = zip.getNextEntry();
                if(entry==null) {
                    logger.info("entry is null");
                    break;
                }
                String name=entry.getName().toLowerCase();
                if(name.endsWith("h5p.json")) {

                    Reader reader = new InputStreamReader(zip);
                    JSONObject jo = (JSONObject)new JSONParser().parse(reader);
                    String title = (String)jo.get("title");
                    if(!StringUtils.isBlank(title)) {
                        result.put(TITLE, title);
                    }

                    String licenseKey =  (String)jo.get("license");
                    if(licenseKey!=null) {
                        String eduLicenseKey = H5PLicenseMapper.get(licenseKey);
                        if(eduLicenseKey != null) {
                            result.put(LICENSE,eduLicenseKey);
                        }
                    }
                    String licenseVersion = (String)jo.get("licenseVersion");
                    if(licenseVersion!=null) {
                        result.put(LICENSEVERSION,licenseVersion);
                    }


                    JSONArray authors = (JSONArray)jo.get("authors");
                    ArrayList<String> authorList = new ArrayList<>();
                    ArrayList<String> authorEditorList = new ArrayList<>();
                    if(authors !=null){
                        for(int i=0;i<authors.size();i++) {
                            JSONObject author = (JSONObject)authors.get(i);
                            String authorName = (String)author.get("name");
                            String authorRole = (String)author.get("role");
                            if(!StringUtils.isBlank(authorName)) {
                                String vcard = VCardTool.nameToVCard(authorName);
                                if(AUTHOREDITOR.equals(authorRole)) authorEditorList.add(vcard);
                                else authorList.add(vcard);
                            }
                        }
                    }
                    if(!authorList.isEmpty())result.put(AUTHOR, authorList);
                    if(!authorEditorList.isEmpty())result.put(AUTHOREDITOR, authorEditorList);

                }

            }
        }catch(Throwable t){
            logger.error(t.getMessage(),t);
        }
        return result;
    }

    @Override
    public String getTransformerName() {
        return ID;
    }
}
