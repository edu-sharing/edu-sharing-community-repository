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
package org.edu_sharing.repository.server.tools;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

public class MemSizeCalculator {
	
	public static byte[] sizeOf(Object obj) throws java.io.IOException
	{
		ByteArrayOutputStream byteObject = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteObject);
		objectOutputStream.writeObject(obj);
		objectOutputStream.flush();
		objectOutputStream.close();
		byteObject.close();
	
		return byteObject.toByteArray();
	}
	
	public static int sizeInBytes(Object obj) throws java.io.IOException{
		return sizeOf(obj).length;
	}
	
	public static int sizeInMB(Object obj) throws java.io.IOException{
		return sizeInBytes(obj) / 1000;
	}
	
}
