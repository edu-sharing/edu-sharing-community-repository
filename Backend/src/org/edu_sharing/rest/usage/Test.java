package org.edu_sharing.rest.usage;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String ticket = "TICKET_14f3e5d98c8b2436587612733b4cb92547cd4799";
		
		HttpClient client = new HttpClient();
		client.getParams().setParameter("http.useragent", "Test Client");
		PutMethod putMethod = new PutMethod("http://127.0.0.1:8080/edu-sharing/restservice/usages/aec10771-bd75-4bd7-85fa-f44624d75c8b/local/1234");
				
		putMethod.setQueryString("?ticket="+ticket+"&appUser=admin");
		
		try{
			int status = client.executeMethod(putMethod);
			System.out.println("Status PUT:"+status);
			String result = putMethod.getResponseBodyAsString();
			System.out.println(result);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		GetMethod getMethod = new GetMethod("http://127.0.0.1:8080/edu-sharing/restservice/usages/aec10771-bd75-4bd7-85fa-f44624d75c8b/local/1234");
		getMethod.setQueryString("?ticket="+ticket+"&appUser=admin");
		try{
			int status = client.executeMethod(getMethod);
			System.out.println("Status GET:"+status);
			String result = getMethod.getResponseBodyAsString();
			System.out.println(result);
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

}
