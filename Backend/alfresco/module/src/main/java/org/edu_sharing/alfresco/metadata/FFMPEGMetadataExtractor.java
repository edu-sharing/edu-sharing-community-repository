package org.edu_sharing.alfresco.metadata;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.metadata.AbstractMappingMetadataExtracter;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;
import org.apache.tika.io.TikaInputStream;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFMPEGMetadataExtractor extends AbstractMappingMetadataExtracter {

    static List<String> supportedMimeTypes = Arrays.asList(new String[]{
            MimetypeMap.MIMETYPE_VIDEO_3GP, MimetypeMap.MIMETYPE_VIDEO_3GP2,
            MimetypeMap.MIMETYPE_VIDEO_AVI, MimetypeMap.MIMETYPE_VIDEO_FLV,
            MimetypeMap.MIMETYPE_VIDEO_MP4,MimetypeMap.MIMETYPE_VIDEO_MPG,
            MimetypeMap.MIMETYPE_VIDEO_QUICKTIME,MimetypeMap.MIMETYPE_VIDEO_WMV,
            "video/mp4v-es", "video/x-flv"});

    public FFMPEGMetadataExtractor(){
        super(new HashSet<>(supportedMimeTypes));
    }

    static String KEY_WIDTH = "WIDTH";
    static String KEY_HEIGHT = "HEIGHT";
    static String KEY_LENGTH = "LENGTH";

    @Override
    protected Map<String, Serializable> extractRaw(ContentReader contentReader) throws Throwable {

        Map<String,Serializable> result = new HashMap<>();

        //use tika framework to write the file
        TikaInputStream tis = TikaInputStream.get(contentReader.getContentInputStream());//TempFileProvider.createTempFile(contentReader.getContentInputStream(),"edu",".bin");
        File tmpFile = tis.getFile();

        try {
            String resolution = getResolutionString(tmpFile.getCanonicalPath());

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

            long videoLength = getVideoLength(tmpFile.getCanonicalPath());
            if (videoLength > 0) {
                result.put(KEY_LENGTH, "PT" + videoLength + "S");
            }

            tis.close();
        }finally {
            tmpFile.delete();
        }
        return result;
    }

    @Override
    protected Map<String, Set<QName>> getDefaultMapping() {
        Map<String,Set<QName>> map = new HashMap<>();
        map.put(KEY_LENGTH, new HashSet<>(Arrays.asList(new QName[]{QName.createQName(CCConstants.LOM_PROP_TECHNICAL_DURATION)})));
        map.put(KEY_WIDTH, new HashSet<>(Arrays.asList(new QName[]{QName.createQName(CCConstants.CCM_PROP_IO_WIDTH)})));
        map.put(KEY_HEIGHT, new HashSet<>(Arrays.asList(new QName[]{QName.createQName(CCConstants.CCM_PROP_IO_HEIGHT)})));
        return map;
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
