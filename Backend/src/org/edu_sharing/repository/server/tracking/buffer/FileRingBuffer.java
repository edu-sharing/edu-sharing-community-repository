/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.tracking.buffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.xml.bind.JAXB;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tracking.TrackingEvent;

/**
 * This class is a simple implementation for a tracking buffer.
 * 
 * All entries will be stored in an filesystem directory,
 * so persistent will be provided!   
 * 
 * @author thomschke
 *
 */
public class FileRingBuffer extends TrackingRingBuffer {
	
	private static final String SUFFIX_TRACK  = ".track";
	private static final String SUFFIX_FIRST  = ".first";
	private static final String SUFFIX_LAST   = ".last";

	private final Log logger = LogFactory.getLog(TrackingRingBuffer.class);
		
	private final NumberFormat formatter;
	private final File directory;	

	public FileRingBuffer(File directory, int maxSize) {

		super(maxSize);
		
		this.directory = directory;
		
		StringBuffer pattern = new StringBuffer();		
		for ( int i = 0, c = Integer.toString(maxSize-1).length()
			; i < c
			; ++i ) {
			
			pattern.append("0");			
		}			
		this.formatter = new DecimalFormat(pattern.toString());
		
		int first = 0;
		int last = 0;
		
		File[] firstFiles = directory.listFiles(new FilenameFilter() {		
			public boolean accept(File dir, String name) {
				
				return name.endsWith(SUFFIX_FIRST);
			}
		});
		
		if (firstFiles != null && firstFiles.length == 1) {
				
			try {
				String fileName = firstFiles[0].getName();
				first = this.formatter.parse(fileName.substring(0,  fileName.indexOf("."))).intValue();
				
			} catch (ParseException e) {
				logger.warn("can not parse the first index of FileRingBuffer");
			}
			
		}		
				
		File[] lastFiles = directory.listFiles(new FilenameFilter() {		
			public boolean accept(File dir, String name) {
				
				return name.endsWith(SUFFIX_LAST);
			}
		});
		
		if (lastFiles != null && lastFiles.length  == 1) {
			
			try {
				String fileName = lastFiles[0].getName();
				last = this.formatter.parse(fileName.substring(0,  fileName.indexOf("."))).intValue();
				
			} catch (ParseException e) {
				logger.warn("can not parse the last index of FileRingBuffer");
			}			
				
		}

		init(first, last);
				
	}
	
	protected TrackingEvent get(int index) {
		
		File file = new File(this.directory, this.formatter.format(index) + SUFFIX_TRACK);				
		return (file.exists() ? JAXB.unmarshal(file, TrackingEvent.class) : null);
	}
	
	protected void set(int index, TrackingEvent event) {
		
		File file = new File(this.directory, this.formatter.format(index) + SUFFIX_TRACK);
		
		if (file.exists()) {
			file.delete();
		}
		
		if (event != null) {
			JAXB.marshal(event, new File(this.directory, this.formatter.format(index) + SUFFIX_TRACK));
		}
	}

	protected void setFirst(int first) {
				
		if (! new File(this.directory, this.formatter.format(getFirst()) + SUFFIX_FIRST).delete()) {
			logger.warn("can not clean the first index of FileRingBuffer");
		}
		
		super.setFirst(first);
		
		try {
			
			FileOutputStream out = 
					new FileOutputStream(
							new File(this.directory, this.formatter.format(getFirst()) + SUFFIX_FIRST));
			
			out.write(getFirst());
			out.close();
			
		} catch (IOException e) {
			logger.warn("can not set the first index of FileRingBuffer");			
		}
		
	}
	
	protected void setLast(int last) {
		
		if (! new File(this.directory, this.formatter.format(getLast()) + SUFFIX_LAST).delete()) {
			logger.warn("can not clean the last index of FileRingBuffer");
		}
		
		super.setLast(last);
		
		try {
			
			FileOutputStream out = 
					new FileOutputStream(
							new File(this.directory, this.formatter.format(getLast()) + SUFFIX_LAST));
			
			out.write(getLast());
			out.close();
			
		} catch (IOException e) {
			logger.warn("can not set the last index of FileRingBuffer");			
		}

	}
}
