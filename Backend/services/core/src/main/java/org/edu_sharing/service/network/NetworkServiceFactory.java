package org.edu_sharing.service.network;


public class NetworkServiceFactory {
	public static NetworkService getNetworkService(){
		return new NetworkServiceImpl();
	}
}
