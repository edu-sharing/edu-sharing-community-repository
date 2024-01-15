package org.edu_sharing.alfresco.transformer.executors;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.executors.AbstractCommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class SerloIndexExecutor extends AbstractCommandExecutor implements CustomTransformerFileAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(SerloIndexExecutor.class);


    public static String ID = "EduSharingSerloIndexExecutor";

    @Override
    protected RuntimeExec createTransformCommand() {
        /**
         * @TODO dummy command
         */
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*", new String[]{"ffmpeg", "-version"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);
        return runtimeExec;
    }

    @Override
    protected RuntimeExec createCheckCommand() {
        return createTransformCommand();
    }




    private void traverse(StringBuffer resultString, String key, Object o) {
        logger.debug("traversing:" + key);
        if (o instanceof JSONObject) {
            for (Object e : ((JSONObject) o).entrySet()) {
                Map.Entry entry = (Map.Entry) e;
                traverse(resultString, (String) entry.getKey(), entry.getValue());
            }
        } else if (o instanceof JSONArray) {
            JSONArray jarr = (JSONArray) o;
            for (int i = 0; i < jarr.size(); i++) {
                traverse(resultString, null, jarr.get(i));
            }
        } else if (o instanceof String) {
            if ("text".equals(key)) {
                if (resultString.length() == 0) {
                    resultString.append((String) o);
                } else {
                    resultString.append(" " + (String) o);
                }
            }
        } else if (o instanceof Long) {
            //can be the version
        } else {
            logger.warn("unknown class " + o.getClass() + " key:" + key);
        }

    }

    @Override
    public String getTransformerName() {
        return ID;
    }

    @Override
    public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions, File sourceFile, File targetFile, TransformManager transformManager) throws Exception {
        logger.info("sourceMimetype:" + sourceMimetype + " targetMimetype:" + targetMimetype + " sourceFile:" + sourceFile + " targetFile:" + targetFile);
        if (transformOptions != null)
            transformOptions.entrySet().stream().forEach(e -> System.out.println("o:" + e.getKey() + " " + e.getValue()));

        try {
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(new FileReader(sourceFile));
            String type = (String) jsonObject.get("type");
            if (type == null || !"https://github.com/serlo/ece-as-a-service".equals(type)) {
                throw new Exception("Unsupported json document with type:" + type);
            }

            StringBuffer resultString = new StringBuffer();
            traverse(resultString, null, jsonObject);

            OutputStream os = new FileOutputStream(targetFile);
            StreamUtils.copy(IOUtils.toInputStream(resultString), os);
            os.close();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }
}