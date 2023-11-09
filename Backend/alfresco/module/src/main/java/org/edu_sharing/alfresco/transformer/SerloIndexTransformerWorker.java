package org.edu_sharing.alfresco.transformer;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.Map;
import java.util.Set;

public class SerloIndexTransformerWorker {};
/*extends ContentTransformerHelper implements ContentTransformerWorker {

    Logger logger = Logger.getLogger(SerloIndexTransformerWorker.class);

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getVersionString() {
        return "1.0";
    }

    @Override
    public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options) {
        logger.debug("called:" +sourceMimetype +" "+targetMimetype + " use:" + options.getUse());

        if(sourceMimetype.equals("application/json") && targetMimetype.equals("text/plain")){
            return true;
        }
        return false;
    }

    @Override
    public void transform(ContentReader reader, ContentWriter writer, TransformationOptions options) throws Exception {
        logger.debug("called");


        JSONObject jsonObject = (JSONObject) new JSONParser().parse(reader.getContentString());
        String type = (String) jsonObject.get("type");
        if(type == null || !"https://github.com/serlo/ece-as-a-service".equals(type)){
            throw new Exception("Unsupported json document with type:"+type);
        }

        StringBuffer resultString = new StringBuffer();
        traverse(resultString,null, jsonObject);

        writer.putContent(resultString.toString());
    }

    private void traverse(StringBuffer resultString, String key, Object o){
        logger.debug("traversing:"+key);
        if(o instanceof JSONObject){
            for(Object e : ((JSONObject)o).entrySet()){
                Map.Entry entry = (Map.Entry)e;
                traverse(resultString, (String)entry.getKey(), entry.getValue());
            }
        }else if(o instanceof JSONArray){
            JSONArray jarr = (JSONArray)o;
            for(int i = 0; i < jarr.size(); i++){
                traverse(resultString, null, jarr.get(i));
            }
        }else if(o instanceof String){
            if("text".equals(key)){
                if(resultString.length() == 0){
                    resultString.append((String)o);
                }else{
                    resultString.append(" " +(String)o);
                }
            }
        }else if(o instanceof Long){
            //can be the version
        }else {
            logger.warn("unknown class "+o.getClass() +" key:"+key);
        }

    }
}*/
