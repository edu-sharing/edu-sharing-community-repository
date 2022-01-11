package org.edu_sharing.rest;

import org.edu_sharing.rest.usage.UsageResource;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.routing.Router;

public class RestService extends Application {

	@Override
	public Restlet createInboundRoot() {
		Router router = new Router(getContext());
		
		//info how to use
		router.attach("/", new Restlet(getContext()) {
		    @Override
		    public void handle(Request request, Response response) {
		        // Print the user name of the requested orders
		        
		        response.setEntity("<b>Usages:</b><br>"+UsageResource.howToString, MediaType.TEXT_HTML);
		    }
		});
		
		//getUsageByParentId
		router.attach(UsageResource.RESTPATH_USAGES_IO, UsageResource.class);
		
		//getUsage
		//setUsage
		//deleteUsage
		router.attach(UsageResource.RESTPATH_USAGE, UsageResource.class);
		router.attach(UsageResource.RESTPATH_USAGE_RESOURCE, UsageResource.class);
		
		
		//deleteUsages lässt sich so nicht mappen wird momentan nicht benötigt
		
		return router;
	}
	
}
