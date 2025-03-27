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
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.springframework.context.ApplicationContext;


public class CCConfirmActivation extends HttpServlet {

	static Logger log = Logger.getLogger(CCConfirmActivation.class);

	String repositoryFehler = "Repository Fehler. Bitte kontaktieren Sie den Administrator!";

	String accessDenied = "Der Zugang wurde verweigert. Der Zugang konnte nicht freigeschaltet werden.";

	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {

		PrintWriter out = resp.getWriter();
		String appId = request.getParameter("appId");
		String appUserId = request.getParameter("appUserId");
		String tmpMail = request.getParameter("mail");
		String key = request.getParameter("key");

		log.info("appId:" + appId + " appUserid" + appUserId + " mail:" + tmpMail + " key:" + key);

		Map<String, ApplicationInfo> appInfos = ApplicationInfoList.getApplicationInfos();
		ApplicationInfo homeRepository = ApplicationInfoList.getHomeRepository();

		if (homeRepository == null) {
			out.print(repositoryFehler);
			log.error("Home Repository File not Found!!!");
			return;
		}

		ApplicationInfo remoteAppInfo = appInfos.get(appId);
		if (remoteAppInfo == null || remoteAppInfo.getTrustedclient() == null || !remoteAppInfo.getTrustedclient().equals("true")) {
			out.print(accessDenied);
			log.info("access denied!!!");
			return;
		}

		if (tmpMail != null) {
			tmpMail = tmpMail.trim().toLowerCase();
		} else {
			out.print(accessDenied);
			log.info("access denied cause mail:" + tmpMail);
			return;
		}

		String mail = tmpMail;
		
		//determine the repository username in dependence of auth_by_app_username_prop in Application file 
		String tmpUsername = appUserId;
		if(remoteAppInfo.getAuthByAppUsernameProp() != null && remoteAppInfo.getAuthByAppUsernameProp().equals(ApplicationInfo.AUTHBYAPP_USERNAME_PROP_MAIL)){
			tmpUsername = mail;
		}
		String repositoryUsername = tmpUsername;



		AuthenticationUtil.runAsSystem(() -> {
			try {


				NodeRef person = serviceRegistry.getPersonService().getPerson(repositoryUsername);

				if(person != null){

					MCAlfrescoBaseClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();
					String nodeId = person.getId();
					Map<String, Map<String, Object>> childMap = mcAlfrescoBaseClient.getChildrenByType(nodeId,
							CCConstants.CM_TYPE_PERSONACCESSELEMENT);
					for (Map.Entry<String, Map<String, Object>> entry : childMap.entrySet()) {
						Map<String, Object> childProps = entry.getValue();
						String tmpAppId = (String) childProps.get(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCAPPID);
						String tmpappUserId = (String) childProps.get(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCUSERID);
						String tmpActivateKey = (String) childProps.get(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCACTIVATEKEY);

						log.info("activation request for user" + repositoryUsername + "found:" + " appId:" + tmpAppId + " appUserId:" + tmpappUserId
								+ " key:" + tmpActivateKey);

						if (tmpAppId.equals(appId) && tmpappUserId.equals(appUserId) && tmpActivateKey.equals(key)) {

							// create a new PropsToSafe Map. dont take the
							// one retrieved by getChildrenByType
							// cause there are properties that are not part
							// of the type definition
							Map<String, Object> propsToSafe = new HashMap<>();
							propsToSafe.put(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCAPPID, tmpAppId);
							propsToSafe.put(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCUSERID, tmpappUserId);
							propsToSafe.put(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCACTIVATEKEY, tmpActivateKey);
							propsToSafe.put(CCConstants.CM_PROP_PERSONACCESSELEMENT_CCACCESS, new Boolean(true).toString());

							mcAlfrescoBaseClient.updateNode(entry.getKey(), propsToSafe);
							log.info("access for user:" + repositoryUsername + " appId:" + appId + " appUSerId:" + appUserId + " was activated!");
							out.print("Der Zugang für User " + appUserId + " und Applikation: " + remoteAppInfo.getAppCaption()
									+ " auf das Repository: " + homeRepository.getAppCaption() + " wurde freigeschaltet!");
							return null;
						}
					}
				}else{
					log.error("no user found with repository username: "+repositoryUsername);
				}

			} catch (Throwable e) {
				e.printStackTrace();
				out.print(e.getMessage());
			}

			log.info("access for user:" + mail + " appId:" + appId + " appUSerId:" + appUserId + " was denied!");
			out.print(this.accessDenied);
			return null;
		});
	}
}
