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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;


/**
 * @author rudolph
 *
 */
public class RemoveUsersNodes extends HttpServlet{
	
	private static Log logger = LogFactory.getLog(RemoveUsersNodes.class);
	static StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	/* (non-Javadoc)
	 * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		PrintWriter out = resp.getWriter();
		
		
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		NodeService nodeService = serviceRegistry.getNodeService();
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

			GuestService guestService = applicationContext.getBean(GuestService.class);
			guestService.getAllGuestAuthorities().forEach(guestUserName -> {
				if (StringUtils.isBlank(guestUserName)) {
					return;
				}

				org.edu_sharing.service.search.SearchService localService = SearchServiceFactory.getLocalService();
				SearchToken token = new SearchToken();
				token.setFrom(0);
				//force searchAll
				token.setMaxResult(Integer.MAX_VALUE);
                try {
					SearchResultNodeRef searchResultNodeRef = localService.searchByProperty(token,
							org.edu_sharing.service.search.SearchService.CombineMode.AND,
							List.of(CCConstants.CM_PROP_C_CREATOR),
							List.of(guestUserName),
							null);
					searchResultNodeRef.getData().forEach(n -> {
						NodeRef nodeRef = n.asAlfrescoNodeRef();
						if (nodeService.exists(nodeRef)) {
							String nodeType = nodeService.getType(nodeRef).toString();
							logger.info("removing Object " + nodeRef.getId() + " Type:" + nodeType);
							nodeService.deleteNode(nodeRef);
						}
					});
				} catch (IOException e) {
                    throw new RuntimeException(e);
                }
			});
			
		}catch(AuthenticationException e){
			e.printStackTrace();
			out.print("Authentication failed!!!");
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
			out.println(e.getMessage());
		}
	}
}
