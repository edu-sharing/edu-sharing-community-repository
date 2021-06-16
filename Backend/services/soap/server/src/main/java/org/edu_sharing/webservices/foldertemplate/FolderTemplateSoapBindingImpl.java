package org.edu_sharing.webservices.foldertemplate;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.service.foldertemplates.FolderTemplatesImpl;

public class FolderTemplateSoapBindingImpl implements FolderTemplate {

	Logger logger = Logger.getLogger(FolderTemplateSoapBindingImpl.class);
	
	@Override
	public String process(String template, String group, String folderid) throws RemoteException {
		
		try{
		
		 	FolderTemplatesImpl ft = new FolderTemplatesImpl(new MCAlfrescoAPIClient());
		 	ft.setTemplate(template,group, folderid);			
		 	List<String> slist = ft.getMessage();
		 	return slist.toString();

		}catch(Throwable e){
			logger.error(e.getMessage(), e);
			throw new RemoteException(e.getMessage());
		}
		
	}
}
