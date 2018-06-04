package org.edu_sharing.alfresco.service.handleservice;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Properties;



import net.handle.api.HSAdapter;
import net.handle.api.HSAdapterFactory;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.AuthenticationInfo;
import net.handle.hdllib.Common;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.Resolver;
import net.handle.hdllib.Util;

public class HandleService {

	String privkeyPath = null;
	
	
	static String handleServerPrefix = null;
	
	String id = null;
	Integer idIndex = 300;
	
	
	Resolver resolver = new Resolver();
	
	
	public HandleService() throws HandleServiceNotConfiguredException{
		if(this.handleServerAvailable()) {
			handleServerPrefix = HandleServiceProperties.instance.getHandleServerPrefix();
			privkeyPath = HandleServiceProperties.instance.getHandleServerPrivKey();
			id = "0.NA/"+handleServerPrefix;
		}else {
			throw new HandleServiceNotConfiguredException();
		}
	}
	
	
	public HandleValue[] getHandleValues(String handle, String[] values) {
		try {
			HSAdapter adapter = HSAdapterFactory.newInstance(id,idIndex,getPrivateKeyBytes(),null);
			return adapter.resolveHandle(handle, values, null);
			
		} catch (HandleException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return null;
	}
	
	public String createHandle(String nodeId, String version, final HandleValue[] values) throws Exception {
		String handle = generateHandle(nodeId,version);
		createHandle(handle,values);
		
		return handle;
	}
	
	public String createHandle(String handle, final HandleValue[] values) throws Exception {
		HSAdapter adapter = HSAdapterFactory.newInstance(id,idIndex,getPrivateKeyBytes(),null);
		adapter.createHandle(handle, values);
		return handle;
	}
	
	
	public void deleteHandle(String handle) throws Exception {
		HSAdapter adapter = HSAdapterFactory.newInstance(id,idIndex,getPrivateKeyBytes(),null);
		adapter.deleteHandle(handle);
	}
	

	public AuthenticationInfo getAuthenticationInfo(String id, Integer idIndex) {
		PrivateKey privKey = loadPrivateKeyFromFile();
		if (privKey == null) {
			return null;
		}
		return new PublicKeyAuthenticationInfo(Util.encodeString(id), idIndex, privKey);
	}

	private PrivateKey loadPrivateKeyFromFile() {

		try {

			return Util.getPrivateKeyFromBytes(getPrivateKeyBytes(), 0);

		} catch (Exception e) {
			e.printStackTrace();

			return null;
		}
	}
	
	byte[] getPrivateKeyBytes() throws IOException {
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		byte buf[] = new byte[4096];
		FileInputStream fin = new FileInputStream(privkeyPath);
		try {
			int r = 0;
			while ((r = fin.read(buf)) >= 0)
				bout.write(buf, 0, r);
		} finally {
			fin.close();
		}
		buf = bout.toByteArray();
		return buf;
	}
	
	
	public static void main(String[] args) {
		
		
		HandleService main;
		try {
			main = new HandleService();
		
		HandleValue[] hvs =  main.getHandleValues(handleServerPrefix + "/901-LEO-ORG",
				new String[] {new String(Common.STD_TYPE_EMAIL), new String(Common.STD_TYPE_URL)});
		
		for(HandleValue hv : hvs) {
			System.out.println(hv.getDataAsString() + " " +  hv.getIndex());
		}
		
		HandleValue hv = new HandleValue(1,Common.STD_TYPE_URL,"https://google.de".getBytes());

		main.createHandle(handleServerPrefix +"/905-google",new HandleValue[] {hv});
				
		} catch (HandleServiceNotConfiguredException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public boolean handleServerAvailable() {
		if(HandleServiceProperties.instance.getHandleServerPrefix() != null && !HandleServiceProperties.instance.getHandleServerPrefix().trim().equals("") 
				&& HandleServiceProperties.instance.getHandleServerPrivKey() != null && !HandleServiceProperties.instance.getHandleServerPrivKey().trim().equals("")) {
			return true;
		}else {
			return false;
		}
	}
	
	public String generateHandle(String nodeId, String version) {
		if(version != null && version.trim().length() > 0)
			return handleServerPrefix +"/" + nodeId + "-" +version;
		else return handleServerPrefix +"/" + nodeId;
	}
	
	public String getId() {
		return id;
	}
	
	
	public HandleValue[] getDefautValues(String url) {
		HandleValue hvUrl = new HandleValue(1,Common.STD_TYPE_URL, url.getBytes());
		
		String mail = HandleServiceProperties.instance.getHandleServerEMail(); 
		mail = (mail == null) ? "info@edu-sharing.com" : mail;
		
		HandleValue hvMail = new HandleValue(2,Common.STD_TYPE_EMAIL, mail.getBytes());
		HandleValue hvAdmin = new HandleValue();
		hvAdmin.setIndex(100);
		hvAdmin.setType(Common.ADMIN_TYPE);
		hvAdmin.setData(Encoder.encodeAdminRecord(new AdminRecord(Util.encodeString(getId()), 200, 
                true, // addHandle
                true, // deleteHandle
                false, // addNA
                false, // deleteNA
                true, // readValue
                true, // modifyValue
                true, // removeValue
                true, // addValue
                true, // modifyAdmin
                true, // removeAdmin
                true, // addAdmin
                false  // listHandles
                )));
		
		return new HandleValue[] {hvUrl,hvMail,hvAdmin};
	}
	


	
}



