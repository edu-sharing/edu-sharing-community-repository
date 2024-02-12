package org.edu_sharing.alfresco.transformer.executors;

import com.google.gson.Gson;
import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.executors.AbstractCommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.edu_sharing.alfresco.transformer.executors.tools.Commands;
import org.edu_sharing.alfresco.transformer.executors.tools.ZipTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.List;
import java.util.Map;

@Component
public class EduSharingZipTextExecuter extends AbstractCommandExecutor implements CustomTransformerFileAdaptor {

    public static String ID = "EduSharingZipTextExecuter";

    private static final Logger logger = LoggerFactory.getLogger(EduSharingZipTextExecuter.class);

    @Override
    public String getTransformerName() {
        return ID;
    }

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
    public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions, File sourceFile, File targetFile, TransformManager transformManager) throws Exception {
        try {
            ArchiveInputStream zip = ZipTool.getZipInputStream(sourceFile);
            while (true) {
                ArchiveEntry entry = zip.getNextEntry();
                if(entry==null) {
                    logger.info("entry is null");
                    break;
                }
                String name=entry.getName().toLowerCase();
                if(targetMimetype.equals("text/plain")){
                    if(name.endsWith("geogebra.xml")) {
                        OutputStream os = new FileOutputStream(targetFile);
                        extractTextContent(zip, os);
                        os.close();
                        return;
                    }
                }else logger.info("can not handle targetMimetype:" + targetMimetype);
            }
            logger.info("no file to process found in zip");
        }
        catch(Throwable t){
            logger.error(t.getMessage(),t);
        }
    }


    void extractTextContent(InputStream is, OutputStream os) throws Exception {
        PrintWriter pw = new PrintWriter(os);
        Document doc = loadFromStream(is);
        XPathFactory pfactory = XPathFactory.newInstance();
        XPath xpath = pfactory.newXPath();
        String path = "/geogebra//element[@type='inlinetext']/content/@val";
        NodeList text = (NodeList) xpath.evaluate(path, doc, XPathConstants.NODESET);
        boolean hasContent = false;
        for(int i = 0; i < text.getLength(); i++) {
            Node node = text.item(i);
            List<Object> jsonData = new Gson().fromJson(node.getTextContent(), List.class);
            if(!jsonData.isEmpty()) {
                Map<Object, Object> map = (Map<Object, Object>) jsonData.get(0);
                if (map.containsKey("text")) {
                    if (hasContent) {
                        pw.write(" ");
                    }
                    pw.write((String) map.get("text"));
                    hasContent = true;
                }
            }
        }
        pw.close();
    }

    public Document loadFromStream(InputStream is) {
        Document result = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            result = builder.parse(is);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
