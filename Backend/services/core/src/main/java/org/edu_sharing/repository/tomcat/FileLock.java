package org.edu_sharing.repository.tomcat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class FileLock {

	public static void main(String[] args) {
		new FileLock().lock();
	}

	public void lock()
    {
		String pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		
		FileChannel channel = null;
		java.nio.channels.FileLock lock = null;
		RandomAccessFile raf = null;
        try
        {
        		File f = new File("process.lock");
    			raf = new RandomAccessFile(f, "rw");
    			
            // Check if the lock exist
            
            // Try to get the lock
            channel = raf.getChannel();
            lock = channel.tryLock();
            if(lock == null)
            {
            	
            	System.out.println(	pid + " File is lock by other application");
                // File is lock by other application
                channel.close();
                throw new RuntimeException("Two instance cant run at a time.");
            }else {
            
            		System.out.println(	pid + " aquired lock slepping");
            		try {
            		Thread.sleep(10000);
            		}catch(Exception e) {
            			e.printStackTrace();
            		}
            		
            }
       
        } catch(IOException e) {
        	
               throw new RuntimeException(pid +"Could not start process.", e);
               
        } finally {
        	
        		try{
	        		if(lock != null) {
	        			lock.release();
	        			System.out.println(	pid +" lock released");
	        		}
	        		
	        		if(channel != null) {
	        			channel.close();
	        			System.out.println(	pid +" channel closed");
	        		}
	        		
	        		if(raf != null) {
	        			raf.close();
	        			System.out.println(	pid +" raf closed");
	        		}
	        		System.out.println(	pid + "finished");
        		} catch (Exception e) {
        		}
        }
    }
}
