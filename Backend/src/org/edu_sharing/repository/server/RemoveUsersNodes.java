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
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.springframework.context.ApplicationContext;


/**
 * @author rudolph
 *
 */
public class RemoveUsersNodes extends HttpServlet implements SingleThreadModel{
	
	private static Log logger = LogFactory.getLog(RemoveUsersNodes.class);
	static StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		PrintWriter out = resp.getWriter();
		
		
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		logger.info("authenticate");
		try{
			
			//authenticate
			MCAlfrescoBaseClient baseClient = (MCAlfrescoBaseClient)RepoFactory.getInstance(null, req.getSession());
			String username =  RepoFactory.getAuthenticationToolInstance(null).getAuthentication( req.getSession()).get(CCConstants.AUTH_USERNAME);
			if(!baseClient.isAdmin(username)){
				logger.info(username+" is not an admin");
				out.println(username+" is not an admin");
				return;
			}
			
			String guestUserName = ApplicationInfoList.getHomeRepository().getGuest_username();
			if(guestUserName == null || guestUserName.trim().equals("")){
				logger.error("No Username for guest found in homeapplication");
				out.println("No Username found in guest.properties.xml");
				return;
			}
			String searchString = "@cm\\:creator:"+guestUserName;
			SearchService searchService = serviceRegistry.getSearchService();
			NodeService nodeService = serviceRegistry.getNodeService();
			ResultSet resultSet = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, searchString);
			for(NodeRef nodeRef : resultSet.getNodeRefs()){
				if(nodeService.exists(nodeRef)){
					String nodeType = nodeService.getType(nodeRef).toString();
					logger.info("removing Object "+nodeRef.getId()+" Type:"+nodeType);
					nodeService.deleteNode(nodeRef);
				}
			}
			
		}catch(AuthenticationException e){
			e.printStackTrace();
			out.print("Authentication failed!!!");
		}catch(Throwable e){
			logger.error("Ein Fehler",e);
			out.println(e.getMessage());
		}
	}
}
