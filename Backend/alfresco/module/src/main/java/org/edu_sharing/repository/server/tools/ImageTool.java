package org.edu_sharing.repository.server.tools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.catalina.util.IOTools;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.sun.star.uno.Exception;

/** 
 * 
 * @author Torsten
 * Tool for common image tasks like rotating by exif orientation
 */
public class ImageTool {
	public static final int MAX_THUMB_SIZE = 900;

	private static int readImageOrientation(InputStream imageFile)  throws IOException, MetadataException, ImageProcessingException {
	    Metadata metadata = ImageMetadataReader.readMetadata(new BufferedInputStream(imageFile));
	    Directory directory = metadata.getDirectory(ExifIFD0Directory.class);
	    int orientation = 1;
	    try {
	        orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
	    } catch (MetadataException me) {
	    }
	    return orientation;
	}
	// Look at http://chunter.tistory.com/143 for information
	private static AffineTransform getExifTransformation(int orientation,int width,int height) {

	    AffineTransform t = new AffineTransform();

	    switch (orientation) {
	    case 1:
	        break;
	    case 2: // Flip X
	        t.scale(-1.0, 1.0);
	        t.translate(-width, 0);
	        break;
	    case 3: // PI rotation 
	        t.translate(width, height);
	        t.rotate(Math.PI);
	        break;
	    case 4: // Flip Y
	        t.scale(1.0, -1.0);
	        t.translate(0, -height);
	        break;
	    case 5: // - PI/2 and Flip X
	        t.rotate(-Math.PI / 2);
	        t.scale(-1.0, 1.0);
	        break;
	    case 6: // -PI/2 and -width
	        t.translate(height, 0);
	        t.rotate(Math.PI / 2);
	        break;
	    case 7: // PI/2 and Flip
	        t.scale(-1.0, 1.0);
	        t.translate(-height, 0);
	        t.translate(0, width);
	        t.rotate(  3 * Math.PI / 2);
	        break;
	    case 8: // PI / 2
	        t.translate(0, width);
	        t.rotate(  3 * Math.PI / 2);
	        break;
	    }

	    return t;
	}
	private static BufferedImage transformImage(BufferedImage image,int orientation) throws Exception {
		int width=image.getWidth();
		int height=image.getHeight();
	    AffineTransformOp op = new AffineTransformOp(getExifTransformation(orientation, width, height), AffineTransformOp.TYPE_BICUBIC);

	    BufferedImage destinationImage = op.createCompatibleDestImage(image, (image.getType() == BufferedImage.TYPE_BYTE_GRAY) ? image.getColorModel() : null );
	    Graphics2D g = destinationImage.createGraphics();
	    g.setBackground(Color.WHITE);
	    g.clearRect(0, 0, destinationImage.getWidth(), destinationImage.getHeight());
	    destinationImage = op.filter(image, destinationImage);
	    return destinationImage;
	}
	/**
	 * 
	 * @param is Input stream containing image data
	 * @param maxSize max size of longest side, or 0 for original size
	 * @return
	 * @throws IOException 
	 */
	public static InputStream autoRotateImage(InputStream is,int maxSize) throws IOException{
		byte[] data=org.apache.poi.util.IOUtils.toByteArray(is);
		try{
			BufferedImage image=ImageIO.read(new ByteArrayInputStream(data));
			ByteArrayOutputStream os=new ByteArrayOutputStream();
			if(maxSize>0)
				image=scaleImage(image,maxSize);
			try{
				int orientation=readImageOrientation(new ByteArrayInputStream(data));
				image=transformImage(image, orientation);
			}catch (Throwable t) {
				// no exif information, no rotation
			}
			
			ImageIO.write(image, "PNG", os);
			return new ByteArrayInputStream(os.toByteArray());
		}
		catch(Throwable t){
			return new ByteArrayInputStream(data);
		}
		
	}
	private static BufferedImage scaleImage(BufferedImage image, int maxSize) {
		double aspect=(double)image.getWidth()/image.getHeight();
		int width=(int) (maxSize*aspect);
		int height=maxSize;
		
		if(aspect>1){
			width=maxSize;
			height=(int) (maxSize/aspect);
			if(image.getWidth()<width)
				return image;
		}
		else{
			if(image.getHeight()<height)
				return image;
		}
		
		Image scaled = image.getScaledInstance(width, height, BufferedImage.SCALE_AREA_AVERAGING);
		BufferedImage buffered = new BufferedImage(width,height, image.getType());
		buffered.getGraphics().drawImage(scaled, 0, 0 , null);
		return buffered;
	}
}