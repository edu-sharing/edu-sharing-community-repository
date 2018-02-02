package org.edu_sharing.service.stream;

public class StreamServiceFactory {
	public static StreamService getStreamService() {
		try {
			return (StreamService) Class.forName(StreamService.class.getName()+"ElasticsearchImpl").newInstance();
		}catch(Throwable t) {
			throw new RuntimeException(t);
		}
	}
}
