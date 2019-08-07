/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.update.*;
import org.springframework.context.ApplicationContext;

public class ServerUpdate extends HttpServlet implements SingleThreadModel {

	private String ticket = null;

	private static Log logger = LogFactory.getLog(ServerUpdate.class);

	PrintWriter out = null;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		out = resp.getWriter();

		Update[] avaiableUpdates = getAvailableUpdates(out);

		ticket = (String)req.getSession().getAttribute(CCConstants.AUTH_TICKET);
		
		String updateId = req.getParameter("ID");
		
		if(updateId == null || updateId.trim().equals("")){
			logger.info("No updateId (ID)");
			out.println("No updateId (ID)");
			return;
		}

		String doItReally = req.getParameter("doit");

		boolean doIt = false;

		if (doItReally != null && doItReally.trim().equals("1")) {
			doIt = true;
			logger.info("execution mode");
			out.println("execution mode");
		} else {
			logger.info("test mode");
			out.println("test mode");
		}

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		
		logger.info("authenticate");
		try{
			serviceRegistry.getAuthenticationService().validate(ticket);
			for(Update update:avaiableUpdates){
				if(update.getId().equals(updateId)){
					if(doIt){
						update.execute();
					}else{
						update.test();
					}
				}
			}
		}catch(AuthenticationException e){
			e.printStackTrace();
			out.print("Authentication failed!!!");
		}

	}

	public static Update[] getAvailableUpdates(PrintWriter out) {
		return new Update[]{
					new Licenses1(out),
					new Licenses2(out),
					new ClassificationKWToGeneralKW(out),
					new SystemFolderNameToDisplayName(out),
					new Release_1_6_SystemFolderNameRename(out),
					new Release_1_7_UnmountGroupFolders(out),
					new Edu_SharingAuthoritiesUpdate(out),
					new Release_1_7_SubObjectsToFlatObjects(out),
					new RefreshMimetypPreview(out),
					new KeyGenerator(out),
					new FixMissingUserstoreNode(out),
					new FolderToMap(out),
					new Edu_SharingPersonEsuidUpdate(out),
					new Release_3_2_FillOriginalId(out),
					new Release_3_2_DefaultScope(out),
					new Release_4_1_FixClassificationKeywordPrefix(out),
					new Release_4_2_PersonStatusUpdater(out),
					new Release_5_0_NotifyRefactoring(out),
					new Release_5_0_Educontext_Default(out)
			};
	}
}
