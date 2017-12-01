package org.edu_sharing.service.rendering;

public class RenderingServiceFactory {
	
	public static RenderingService getRenderingService(String appId){
		return new RenderingServiceImpl(appId);
	}
}
