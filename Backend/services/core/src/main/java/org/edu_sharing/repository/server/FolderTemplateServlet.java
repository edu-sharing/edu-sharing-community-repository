package org.edu_sharing.repository.server;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.foldertemplates.FolderTemplatesImpl;

public class FolderTemplateServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet( req,  resp);	
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
	 	String templatename = req.getParameter("template");
		String edugroup = req.getParameter("group");
		String folder = req.getParameter("folderid");

		resp.getOutputStream().println("Start");

		
		try{
			
			String homeAppId = ApplicationInfoList.getHomeRepository().getAppId();
			AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeAppId);
			
			// HashMap<String,String> authInfo = authTool.createNewSession(userName, password);

			MCAlfrescoBaseClient baseclient = (MCAlfrescoBaseClient)RepoFactory.getInstance(homeAppId, req.getSession());
			
		 	FolderTemplatesImpl ft = new FolderTemplatesImpl(baseclient);
		 	ft.setTemplate(templatename,edugroup,folder);			
		 	List<String> slist = ft.getMessage();
		 	resp.getOutputStream().println(slist.toString());
		}catch(Throwable e){
			e.printStackTrace();
			resp.getOutputStream().println(e.getMessage());
		}
	}
}
