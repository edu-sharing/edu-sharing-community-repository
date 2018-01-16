package org.edu_sharing.alfresco.transformer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformerHelper;
import org.alfresco.repo.content.transform.ContentTransformerWorker;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.TempFileProvider;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

public class VideoTransformerWorker extends ContentTransformerHelper implements ContentTransformerWorker  {

	
	String ffmpegPath = "ffmpeg"; 
	
	Logger logger = Logger.getLogger(VideoTransformerWorker.class);
	
	NodeService nodeService = null;
	TransactionService transactionService = null;

	@Override
	public String getComments(boolean available) {
		StringBuilder sb = new StringBuilder();
        sb.append("# Supports transformations between mimetypes starting with \"video/\", but not\n");
        sb.append("# tiff to pdf.\n");
        
        logger.debug("getComments");
        
        return sb.toString();
	}
	
	public String getVersionString() {
		logger.debug("getVersionString");
		return "1.0";
	};
	
	public boolean isAvailable() {
		logger.debug("isAvailable");
		return true;
	};
	
	@Override
	public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options) {
		
		//JCodec supports only AVC, H.264 in MP4, ISO BMF, Quicktime container for getting a singe frame
		logger.debug("isTransformable sourceMimetype:"+sourceMimetype+ " targetMimetype:"+targetMimetype);
		if((MimetypeMap.MIMETYPE_VIDEO_MP4.equals(sourceMimetype) || MimetypeMap.MIMETYPE_VIDEO_QUICKTIME.equals(sourceMimetype) || "video/mp4v-es".equals(sourceMimetype)) 
				&& (MimetypeMap.MIMETYPE_IMAGE_PNG.equals(targetMimetype) || MimetypeMap.MIMETYPE_IMAGE_JPEG.equals(targetMimetype) )){
			return true;
		}
		
		return false;
	}
	
	@Override
	public void transform(ContentReader reader, ContentWriter writer, TransformationOptions options) throws Exception {
		
		  // get mimetypes
        String sourceMimetype = getMimetype(reader);
        String targetMimetype = getMimetype(writer);
        
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
        
        // create required temp files
        File sourceFile = TempFileProvider.createTempFile(
                getClass().getSimpleName() + "_source_",
                "." + sourceExtension);
        File targetFile = TempFileProvider.createTempFile(
                getClass().getSimpleName() + "_target_",
                "." + targetExtension);
        
        // pull reader file into source temp file
        reader.getContent(sourceFile);
        
        
        try {
	//        long videoLength = getViedeoLength(sourceFile.getAbsolutePath());
	//        if(videoLength > 1){
	        	
	        	//ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-an", "-ss", ""+videoLength, "-t", "00:00:01", "-vframes","1", "-i",sourceFile.getAbsolutePath(),"-f", "image2", targetFile.getAbsolutePath());
	        
	        	ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i",sourceFile.getCanonicalPath(),"-vf","thumbnail,scale=640:360","-frames:v","1","-ss","1", "-y", targetFile.getCanonicalPath());
	        	//ffmpeg -i <video> -vf  "thumbnail,scale=640:360" -frames:v 1 -ss 1 <image>
	        	
	        	pb.environment().remove("LD_LIBRARY_PATH");
				Process p = pb.start();
				p.waitFor();
	//        }else{
	//        	logger.error("determined videoLength is to small");
	//        }
	        if(targetFile.length() > 0){
	        	writer.putContent(targetFile);
	        }else{
	        	logger.warn("ffpmpeg: generated preview file has no content");
	        }
	        writeLength(sourceFile,options);
			
        }catch(Throwable t) {
        	logger.error("Error initializing ffmpeg. Generating preview+reading metadata failed ("+t.getMessage()+")");
        }
        		
	}
	
	
	private void writeLength(File sourceFile, TransformationOptions options) throws IOException {
		long length=getVideoLength(sourceFile.getCanonicalPath());
		if(length<0)
			return;
		RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
        txnHelper.setForceWritable(true);
        boolean requiresNew = false;
        if (AlfrescoTransactionSupport.getTransactionReadState() != TxnReadState.TXN_READ_WRITE){
            requiresNew = true;
        }
        txnHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable{
            	nodeService.setProperty(options.getSourceNodeRef(), 
            			QName.createQName(CCConstants.LOM_PROP_TECHNICAL_DURATION), 
            			"PT"+length+"S");
            	return null;
            }
    
        }, false, requiresNew);
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
			String[] duration=result[1].split(",")[0].split(":");
			long seconds=Math.round(Double.parseDouble(duration[0])*60*60 + Double.parseDouble(duration[1])*60+Double.parseDouble(duration[2]));
			
			return seconds;
			
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
	

	
	long getMilliseconds(String timeString) throws ParseException{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

       // String inputString = "00:01:30.500";

        Date date = sdf.parse("1970-01-01 " + timeString);
        System.out.println("in milliseconds: " + date.getTime()); 
        
        return date.getTime();
	}
	
	
	
	public String convertStreamToString(InputStream is) throws IOException{
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
	
	
}
