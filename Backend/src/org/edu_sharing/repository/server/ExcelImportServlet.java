package org.edu_sharing.repository.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.SingleThreadModel;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.server.importer.ExcelLOMImporter;

public class ExcelImportServlet extends HttpServlet implements SingleThreadModel{

	private static Log logger = LogFactory.getLog(ExcelImportServlet.class);
	
	protected void doPost(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
		
		ServletOutputStream out = resp.getOutputStream();
		if (!ServletFileUpload.isMultipartContent(request)){
			out.print("No MultipartContent");
			return;
		}
		
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		
		
		HashMap<String, String> authInfo = new AuthenticationToolAPI().validateAuthentication(request.getSession());
		
		if(authInfo == null){
			logger.error("not authenticated");
			return;
		}
		try{
			if(!new MCAlfrescoAPIClient().isAdmin()){
				logger.error("not an admin");
				return;
			}
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			return;
		}
		
		try {
			List items = upload.parseRequest(request);
			
			
			
			String parentId = null;
			
			DiskFileItem dfi = null;
			
			
			for (Object item : items) {
				FileItem fileItem = (FileItem) item;
				//logger.info(fileItem.getFieldName()+" "+fileItem.getName()+" "+fileItem.getContentType()+" "+fileItem.getString()+" ");
				if(!fileItem.isFormField()){
					dfi = (DiskFileItem)fileItem;
				}
				
				if(fileItem.getFieldName().equals("parentId")){
					parentId = fileItem.getString();
				}
				
			}
			
			
			if(dfi == null){
				logger.error("no file provided");
				return;
			}
			
			if(parentId == null){
				logger.error("no file provided");
				return;
			}
			
			try{
				new ExcelLOMImporter(parentId, dfi.getInputStream(), false);
			}catch(Exception e){
				e.printStackTrace();
			}

		} catch (FileUploadException e) {
			logger.error(e.getMessage(), e);
			out.print(e.getMessage());
			return;
		}
	}
	
}
