package org.edu_sharing.repository.tomcat;

import java.io.IOException;
import java.io.Writer;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;


/**
 * 
 * @author rudi
 * siehe http://saschadoemer.blogspot.de/2013/11/anwendungsubergreifende-fehlerseiten-im.html
 */
public class CustomErrorReportValve extends ErrorReportValve {


	
	
	@Override
	protected void report(Request req, Response resp, Throwable caught) {
		
		int statusCode = resp.getStatus();
		
		// Do nothing on a 1xx, 2xx and 3xx status
		// Do nothing if anything has been written already 
		if ((statusCode < 400) || (resp.getContentLength() > 0)){
			return;
		}else{
			try{
				
				resp.setContentType("text/html");
				resp.setCharacterEncoding("utf-8");
				
				Writer writer = resp.getReporter();
				if (writer != null) {
				   // If writer is null, it's an indication that the response has
				   // been hard committed already, which should never happen
				
					String msg = (caught != null) ? caught.getLocalizedMessage() : "";
					
				   writer.write("<html><head></head><body>Whoops something went wrong..."+ msg +"</body></html>");
				
				}
				
				resp.finishResponse();
				resp.flushBuffer();
				
			}catch(IOException e){
				e.printStackTrace();
			}catch(Throwable e){
				e.printStackTrace();
			}
		}
	}
	
	
	///String getErrorFileContent
}
