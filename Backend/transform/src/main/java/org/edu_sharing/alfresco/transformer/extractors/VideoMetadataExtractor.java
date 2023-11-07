package org.edu_sharing.alfresco.transformer.extractors;

import org.alfresco.transformer.metadataExtractors.AbstractMetadataExtractor;
import org.apache.tika.io.TikaInputStream;
import org.slf4j.Logger;


import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @TODO att to engine_config.json
 *
 * {"sourceMediaType": "video/x-m4v",     "targetMediaType": "alfresco-metadata-extract"},
 *         {"sourceMediaType": "video/3gpp",  "targetMediaType": "alfresco-metadata-extract"},
 *         {"sourceMediaType": "video/3gpp2",  "targetMediaType": "alfresco-metadata-extract"},
 *         {"sourceMediaType": "video/x-msvideo",  "targetMediaType": "alfresco-metadata-extract"},
 *         {"sourceMediaType": "video/x-flv",  "targetMediaType": "alfresco-metadata-extract"},
 *         {"sourceMediaType": "video/mpeg",  "targetMediaType": "alfresco-metadata-extract"},
 *         {"sourceMediaType": "video/quicktime",  "targetMediaType": "alfresco-metadata-extract"},
 *         {"sourceMediaType": "video/x-ms-wmv",  "targetMediaType": "alfresco-metadata-extract"},
 *         {"sourceMediaType": "video/mp4v-es",  "targetMediaType": "alfresco-metadata-extract"},
 *         {"sourceMediaType": "video/x-flv",  "targetMediaType": "alfresco-metadata-extract"}
 *
 *
 *
 * VideoMetadataExtractor_metadata_extract.properties
 *         #
 * # OfficeMetadataExtracter - default mapping
 * #
 * # author: Derek Hulley
 *
 * # Namespaces
 * namespace.prefix.cclom=http://www.campuscontent.de/model/lom/1.0
 * namespace.prefix.ccm=http://www.campuscontent.de/model/1.0
 *
 * # Mappings
 * LENGTH=cclom:duration
 * HEIGHT=ccm:width
 * WIDTH=ccm:height
 */
public class VideoMetadataExtractor extends AbstractMetadataExtractor {

    static String KEY_WIDTH = "WIDTH";
    static String KEY_HEIGHT = "HEIGHT";
    static String KEY_LENGTH = "LENGTH";

    public VideoMetadataExtractor(Logger logger) {
        super(logger);
    }

    @Override
    public Map<String, Serializable> extractMetadata(String sourceMimetype, Map<String, String> transformOptions, File sourceFile) throws Exception {
        return extractRaw(sourceFile);
    }

    protected Map<String, Serializable> extractRaw(File sourceFile) throws IOException,InterruptedException {

        Map<String,Serializable> result = new HashMap<>();
        String resolution = getResolutionString(sourceFile.getCanonicalPath());

        Integer resolutionX = null;
        Integer resolutionY = null;
        if (resolution != null && resolution.contains("x")) {
            resolutionX = Integer.parseInt(resolution.split("x")[0]);
            resolutionY = Integer.parseInt(resolution.split("x")[1]);
        }
        if (resolutionX != null && resolutionY != null) {
            result.put(KEY_WIDTH, resolutionX);
            result.put(KEY_HEIGHT, resolutionY);
        }

        long videoLength = getVideoLength(sourceFile.getCanonicalPath());
        if (videoLength > 0) {
            result.put(KEY_LENGTH, "PT" + videoLength + "S");
        }

        return result;
    }

    String getResolutionString(String videoFilePath) throws IOException,InterruptedException{
        ProcessBuilder pb = new ProcessBuilder("ffmpeg","-i",videoFilePath);
        pb.environment().remove("LD_LIBRARY_PATH");

        InputStream pResult = null;

        Process p = pb.start();
        p.waitFor();

        pResult = p.getInputStream();
        String output = convertStreamToString(pResult);

        //ffmpeg error "At least one output file must be specified" though gives info we need
        if(output == null || output.trim().equals("")){
            pResult = p.getErrorStream();
            output = convertStreamToString(pResult);
            //logger.error(output);
        }

        BufferedReader bufReader = new BufferedReader(new StringReader(output));

        String line = null;
        while( (line = bufReader.readLine()) != null )
        {
            if(line != null && line.contains("Video:")){
                System.out.println("line:" + line);
                Pattern pattern = Pattern.compile(", [0-9]*x[0-9]*");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find())
                {
                    System.out.println(matcher.group());
                    return matcher.group().replace(",","").trim();
                }
            }
        }
        return null;
    }

    long getVideoLength(String videoFilePath){
        ProcessBuilder pb = new ProcessBuilder("ffmpeg","-i",videoFilePath);
        pb.environment().remove("LD_LIBRARY_PATH");

        InputStream pResult = null;
        try{
            Process p = pb.start();
            p.waitFor();
            pResult = p.getInputStream();
            String output = convertStreamToString(pResult);

            if(output == null || output.trim().equals("")){
                pResult = p.getErrorStream();
                output = convertStreamToString(pResult);
            }

            if(output == null || output.trim().equals("")){
                return -1;
            }

            String[] result=output.split("Duration: ");

            if(result.length > 1) {
                String[] duration = result[1].split(",")[0].split(":");
                long seconds = Math.round(Double.parseDouble(duration[0]) * 60 * 60 + Double.parseDouble(duration[1]) * 60 + Double.parseDouble(duration[2]));

                return seconds;
            }else{
                return -1;
            }

        }catch(IOException e){
            e.printStackTrace();
        }
        catch(Throwable t){
            t.printStackTrace();
        }finally{
            if(pResult != null){
                try{
                    pResult.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }


        return -1;

    }

    public static String convertStreamToString(InputStream is) throws IOException{
        int k;
        StringBuffer sb = new StringBuffer();
        while((k=is.read())!=-1)
        {
            sb.append((char)k);
        }
        return sb.toString();
    }
}
