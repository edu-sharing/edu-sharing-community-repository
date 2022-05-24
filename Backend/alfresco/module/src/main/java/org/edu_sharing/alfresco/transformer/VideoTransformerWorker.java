package org.edu_sharing.alfresco.transformer;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.transform.exceptions.TransformException;
import org.alfresco.transformer.AbstractTransformerController;
import org.alfresco.transformer.probes.ProbeTestTransform;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public class VideoTransformerWorker  extends AbstractTransformerController {

	
	String ffmpegPath = "ffmpeg"; 
	
	static Logger logger = Logger.getLogger(VideoTransformerWorker.class);
	
	NodeService nodeService = null;
	TransactionService transactionService = null;

	MimetypeService mimetypeService;


	
	/**@Override
	 * TODO: mimetype prüfung in json config
	public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options) {

		//JCodec supports only AVC, H.264 in MP4, ISO BMF, Quicktime container for getting a singe frame
		logger.debug("isTransformable sourceMimetype:"+sourceMimetype+ " targetMimetype:"+targetMimetype);
		if((MimetypeMap.MIMETYPE_VIDEO_MP4.equals(sourceMimetype)
				|| MimetypeMap.MIMETYPE_VIDEO_QUICKTIME.equals(sourceMimetype)
				|| "video/x-matroska".equals(sourceMimetype))
				&& (MimetypeMap.MIMETYPE_IMAGE_PNG.equals(targetMimetype) || MimetypeMap.MIMETYPE_IMAGE_JPEG.equals(targetMimetype) )){
			return true;
		}
		
		return false;
	}**/
	public static void convertFFMPEG(File sourceFile,File targetFile,Format format) throws InterruptedException, IOException {

        
	//        long videoLength = getViedeoLength(sourceFile.getAbsolutePath());
	//        if(videoLength > 1){
	        	
	        	//ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-an", "-ss", ""+videoLength, "-t", "00:00:01", "-vframes","1", "-i",sourceFile.getAbsolutePath(),"-f", "image2", targetFile.getAbsolutePath());
	        
        		// create jpg at the start
	        	//ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i",sourceFile.getCanonicalPath(),"-vf","thumbnail,scale=640:360","-frames:v","1","-ss","1", "-y", targetFile.getCanonicalPath());

				ProcessBuilder pb;
        		// create gif animation
				if(format.equals(Format.gif)) {
					String FRAME_COUNT = "5";
					String GIF_FRAMERATE = "1";
					pb = new ProcessBuilder("ffmpeg", "-i", sourceFile.getCanonicalPath(), "-t", FRAME_COUNT, "-preset", "ultrafast", "-lavfi", "setpts=0.075*PTS,scale=400:-1", "-r", GIF_FRAMERATE, "-f", "gif", "-y", targetFile.getCanonicalPath());
					//ffmpeg -i <video> -vf  "thumbnail,scale=640:360" -frames:v 1 -ss 1 <image>
				}
				else{
					// create webp animation
					String TIME_COUNT="15";
					String WEBP_FRAMERATE="1";
					String WEBP_QUALITY="20";
					pb = new ProcessBuilder("ffmpeg", "-i",sourceFile.getCanonicalPath(),"-t",TIME_COUNT,"-loop","0","-q",WEBP_QUALITY,"-filter","setpts=0.15*PTS,scale=400:-1","-r",WEBP_FRAMERATE,"-f","webp","-y",targetFile.getCanonicalPath());
				}


	        	pb.environment().remove("LD_LIBRARY_PATH");
				Process p = pb.start();
				
				// required for unknown reason, clear the input stream from ffmpeg
				new Thread(new Runnable(){
		          public void run(){
		                Scanner stdin = new Scanner(p.getErrorStream());
		                while(stdin.hasNextLine()){
							logger.warn("ffmpeg: "+stdin.nextLine());
						}
		                stdin.close();
		                }

			        }).start();

				p.waitFor();
				/*
				while(p.isAlive()) {
					logger.info(convertStreamToString(p.getInputStream()));
					logger.info(convertStreamToString(p.getErrorStream()));
					Thread.sleep(1000);
				}
				*/
				

	        	logger.info("ffmpeg: decoded size: "+targetFile.length());
	//        }else{
	//        	logger.error("determined videoLength is to small");
	//        }
	}

	@Override
	public void transformImpl(String transformName,
							  String sourceMimetype,
							  String targetMimetype, Map<String, String> transformOptions, File sourceFile, File targetFile) {
		// get the extensions to use
		MimetypeService mimetypeService = getMimetypeService();
		String sourceExtension = mimetypeService.getExtension(sourceMimetype);
		String targetExtension = mimetypeService.getExtension(targetMimetype);
		if (sourceExtension == null || targetExtension == null)
		{
			throw new AlfrescoRuntimeException("Unknown extensions for mimetypes: \n" +
					"   source mimetype: " + sourceMimetype + "\n" +
					"   source extension: " + sourceExtension + "\n" +
					"   target mimetype: " + targetMimetype + "\n" +
					"   target extension: " + targetExtension);
		}

		try {
			// create required temp files

			// pull reader file into source temp file
			convertFFMPEG(sourceFile, targetFile,Format.webp);

			if(targetFile.length() < 1){
				logger.warn("ffmpeg failed to convert webp. Check version is greater or equal to (March 24, 2014, FFmpeg 2.2). Will fall back to gif.");
				convertFFMPEG(sourceFile, targetFile,Format.gif);
				if(targetFile.length() < 1){
					throw new AlfrescoRuntimeException("ffmpeg: generated preview file has no content");
				}
			}

		}catch(Throwable t) {
			throw new TransformException(INTERNAL_SERVER_ERROR.value(),"There was a problem during transformation: " + t.getMessage());
		}
	}

	@Override
	public String getTransformerName() {
		return "edu-sharing video transformer";
	}

	@Override
	public ProbeTestTransform getProbeTestTransform() {
		//@TODO
		return null;
	}

	@Override
	public String version() {
		return "1.0";
	}

	long getMilliseconds(String timeString) throws ParseException{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

       // String inputString = "00:01:30.500";

        Date date = sdf.parse("1970-01-01 " + timeString);
        System.out.println("in milliseconds: " + date.getTime()); 
        
        return date.getTime();
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

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setTransactionService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}


	private enum Format {
		webp,
		gif
	}

	public MimetypeService getMimetypeService() {
		return mimetypeService;
	}

	public void setMimetypeService(MimetypeService mimetypeService) {
		this.mimetypeService = mimetypeService;
	}
}
