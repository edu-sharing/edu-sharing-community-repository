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
package org.edu_sharing.webservices.usage;

public class Test {

	public static void main(String[] args) {
		
		UsageServiceLocator locator = new UsageServiceLocator();

		locator.setusageEndpointAddress("http://localhost:8080/edu-sharing/services/usage");
		
		try{
			
			Usage usage = locator.getusage();
			UsageResult result =usage.getUsage("TICKET_163d3ee4c82544b09982fa6126291b41cf0c8ad3", "Baumschule-Lehrer", "local", "1", "22e26499-f619-4a35-9332-2534a11031c6", "admin", "1");

		}catch(Exception e){
			e.printStackTrace();
		}
		
		
	}

}
