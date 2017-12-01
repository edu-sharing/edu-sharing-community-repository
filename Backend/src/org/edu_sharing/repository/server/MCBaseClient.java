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

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.rpc.SearchResult;
import org.edu_sharing.repository.client.rpc.SearchToken;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.BlowFish;
import org.edu_sharing.repository.server.tools.URLTool;


public abstract class MCBaseClient {
	
	
	private static Log logger = LogFactory.getLog(MCBaseClient.class);
	
	/**
	 * username, ticket for the current repository 
	 */
	protected HashMap<String, String> authenticationInfo = null;
	
	/**
	 * @return the authenticationInfo
	 */
	public HashMap<String, String> getAuthenticationInfo() {
		return authenticationInfo;
	}
	
	protected String getRedirectServletLink(String repId, String nodeId){
		return URLTool.getRedirectServletLink(repId, nodeId);
	}
	
	/**
	 * sets encrypted username in authInfo when it's not already there and returns encrypted username
	 * @param authInfo
	 * @param appInfo where blowfishkey is set
	 */
	public String getUserNameEncrypted(HashMap authInfo, ApplicationInfo appInfo){
		String user_encrypted = (String)authInfo.get(CCConstants.AUTH_USERNAME_ENCRYPTED);
		if(user_encrypted == null){
			logger.info("encrypt will be called");
			
			String username =  (String)authInfo.get(CCConstants.AUTH_USERNAME);
			user_encrypted = getBlowFishEncrypted(username,appInfo);
			authInfo.put(CCConstants.AUTH_USERNAME_ENCRYPTED, user_encrypted);
		}
		return user_encrypted;
	}
	
	/**
	 * encrypts a String with the blowfish algorithm
	 * also an Base64encoding and URLEncoding is done
	 * 
	 * @param toencrypt the String to encrypt
	 * @param appInfo the ApplicationInfo where the iv for encryption is found
	 * @return
	 */
	public static String getBlowFishEncrypted(String toEncrypt, ApplicationInfo appInfo){
		return  BlowFish.encryptForUrl(toEncrypt, appInfo.getBlowfishiv(),  appInfo.getBlowfishkey());
	}
	/**
	 * encrypts a String with the blowfish algorithm using iv and key from the homeApplication
	 * also an Base64encoding and URLEncoding is done
	 * 
	 * 
	 * @param toEncrypt
	 * @return
	 */
	public static String getBlowFishEncrypted(String toEncrypt){
		String encrypted = null;
		String iv = ApplicationInfoList.getHomeRepository().getBlowfishiv();
		String key = ApplicationInfoList.getHomeRepository().getBlowfishkey();
		if (iv != null && key != null) {
			BlowFish.setIv(iv);
			encrypted =  BlowFish.encryptForUrl(toEncrypt, iv, key);
		}
		return encrypted;
	}
	
	
	/**
	 * decrypts a blofish encrypted file
	 * 
	 * @param toDecrypt the String to decrypt
	 * @param appInfo the ApplicationInfo where the iv for decryption is found
	 * @return
	 */
	public static String getBlowFishDecrypted(String toDecrypt,ApplicationInfo appInfo){
		byte[] base64decoded = BlowFish.getBase64Decoded(toDecrypt);
		String result = BlowFish.decrypt(base64decoded, appInfo.getBlowfishkey());
		return result;
	}
	
	
	/**
	 * Creates a node. 
	 * @param parentID the Id of the parent node
	 * @param nodeTypeString the type of node (folder, content, map,  io...)
	 * @param childAssociation the kind of childAssociation
	 * @param _props the properties (metadata) of the node 
	 * @return
	 * @throws Exception
	 */
	public abstract String createNode(String parentID, String nodeTypeString, String childAssociation, HashMap<String,Object> _props) throws Exception;
	
	/**
	 * Creates a node with the default ChildAssociation
	 * @param parentID the Id of the parent node
	 * @param nodeTypeString the type of node (folder, content, map,  io...)
	 * @param _props the properties (metadata) of the node 
	 * @return
	 * @throws Exception
	 */
	public abstract String createNode( String parentID, String nodeTypeString, HashMap<String,Object> _props) throws Exception;
	
	/**
	 * Adds a aspect to a node
	 * 
	 * @param nodeId the id of the node
	 * @param aspect the aspect type
	 * @throws Exception
	 */
	public abstract void addAspect(String nodeId,String aspect) throws Exception;
	
	/**
	 * Updates a repository node in the default Store.
	 * Only the properties that are contained in _props will be overwritten
	 *  
	 * @param nodeId the id of the node
	 * @param _props the properties of the node
	 * @throws Exception
	 */
	public abstract void updateNode(String nodeId, HashMap<String,Object> _props) throws Exception;
	
	/**
	 * Creates an association between 2 nodes
	 * 
	 * @param fromID id of the from node
	 * @param toID id of the to node
	 * @param association the type of association
	 */
	public abstract void createAssociation(String fromID, String toID, String association);
	
	/**
	 * set binary content for a node. use only for small files cause the byte array is in memory
	 * 
	 * @param nodeID the id of the node
	 * @param content the binary content
	 * @param mimetype the mimetype of the content
	 * @param encoding the encoding of the content
	 * @param property the binary content property name of the node
	 * @throws Exception
	 */
	public abstract void writeContent(String nodeID, byte[] content, String mimetype, String encoding, String property) throws Exception;
	
	/**
	 * get all properties of a node
	 * @param nodeId
	 * @return
	 * @throws Throwable
	 */
	public abstract HashMap<String, Object> getProperties(String nodeId) throws Throwable;
	
	/**
	 * get the first child that matches the params
	 * 
	 * @param parentId the parentId of the node that has the child
	 * @param type the type of the child
	 * @param property the property name that is used for matching
	 * @param value the value of the property name that is used for matching
	 * @return
	 * @throws Throwable
	 */
	public abstract HashMap<String,Object> getChild(String parentId, String type, String property, String value) throws Throwable;
	
	
	/**
	 * get the node id form the user home of the given user
	 * @param username the username
	 * @return
	 * @throws Exception
	 */
	public abstract String getHomeFolderID(String username) throws Exception;
	
	/**
	 * create a new version for an node with the given metadata (_properties)
	 * @param nodeId the nodeId of the node
	 * @param _properties the metadata for the node's version
	 * @throws Exception
	 */
	public abstract void createVersion(String nodeId, HashMap _properties) throws Exception;
	
	/**
	 * get the nodeIds that are referenced by the specified association of the given node
	 * 
	 * @param nodeID node id from the given node
	 * @param association the association type 
	 * @return target Assocs NodeIds
	 * 
	 * @throws Exception
	 */
	public abstract List<String> getAssociationNodeIds(String nodeID, String association) throws Exception;
	
	/**
	 * remove all associations between 2 nodes that got the given association type
	 * @param fromID the id of the from node
	 * @param toID the id of the to node
	 * @param association the association type
	 * 
	 * @throws Exception
	 */
	public abstract void removeAssociation( String fromID, String toID, String association) throws Exception;
	
	/**
	 * get all children that got the specified type
	 * 
	 * @param nodeId the node id
	 * @param type the type of children
	 * @return a nested HashMap of nodeIds and the corresponding properties 
	 * @throws Exception
	 */
	public abstract HashMap<String, HashMap<String, Object>> getChildrenByType( String nodeId, String type) throws Exception;
	
	/**
	 * get all properties of a person
	 * 
	 * @param userName the username of the person
	 * @return property/value HashMap
	 * @throws Exception
	 */
	public abstract HashMap<String, String> getUserInfo(String userName) throws Exception;
	
	/**
	 * remove a node from a given parent. if the node is the primary child it will be deleted. when its a secondary parent 
	 * only the association will be removed.
	 * 
	 * @param nodeID the nodeId of the node that should be removed
	 * @param fromID the nodeId of the parent node
	 * 
	 * @throws Exception
	 */
	public abstract void removeNode(String nodeID, String fromID) throws Exception;
	
	/**
	 * 
	 * @param storeProtocol
	 * @param storeId
	 * @param nodeId
	 * @param recycle if true, do move the item to recycle (default)
	 */
	public abstract void removeNode(String nodeID, String fromID,boolean recycle) throws Exception;
	/**
	 * 
	 * find out if a node has binary content for a given content property
	 * 
	 * @param nodeId the nodeId of node
	 * @param contentProp the content property
	 * 
	 * @return true when there is content and false when there is no
	 * @throws Exception
	 */
	public abstract boolean hasContent(String nodeId, String contentProp) throws Exception;
	
	
	
	/**
	 * the default edu-sharing search method 
	 * 
	 * @param searchToken an object that contains information like the search criterias, the repository, from-to range for browsing ...
	 * @return SearchResult an object that contains all the nodes that where found with all their properties
	 * @throws Throwable
	 */
	public abstract SearchResult search(SearchToken searchToken) throws Throwable;
	
	
	public abstract SearchResult searchRecommend(int startIdx, int nrOfResults) throws Throwable;
	
	
	/**
	 * returns a html snippet for the details panel. useful for no edu-sharing repositories 
	 * 
	 * @param nodeId
	 * @return a String withe the details snippet
	 * @throws Exception
	 */
	public abstract String getDetailsHtmlSnippet(String nodeId) throws Exception;
	
	
	/**
	 * check if the current authenticated user has all given permissions on a node
	 * @param nodeId the id of the node the permissions sholud be checked
	 * @param permissions the permissions to check
	 * @return true when there are all permissions else false
	 * @throws Exception
	 */
	public abstract boolean hasPermissions(String nodeId, String[] permissions) throws Exception;
	/**
	 * Classes that extend MCBaseClient are instantiated dynamicaly by Reflections.
	 * Reflections constructor calls are much slower than direct constructor calls
	 * 
	 * look at:
	 * http://stackoverflow.com/questions/435553/java-reflection-performance
	 * 
	 * so we will do reflection constructor call only the first time for every Application. 
	 * For every ApplicationFile we remember a Object of the configured MCBaseClient Subclass in HashMap in the 
	 * Class org.edu_sharing.repository.server.RepoFactory. When we need a new Instance the getInstance Method of the Object will be called,
	 * which calls the constructor directly. 
	 * @param _authenticationInfo
	 * @return
	 */
	public abstract MCBaseClient getInstance(HashMap<String, String> _authenticationInfo);
	/**
	 * Classes that extend MCBaseClient are instantiated dynamicaly by Reflections.
	 * Reflections constructor calls are much slower than direct constructor calls
	 * 
	 * look at:
	 * http://stackoverflow.com/questions/435553/java-reflection-performance
	 * 
	 * so we will do reflection constructor call only the first time for every Application. 
	 * For every ApplicationFile we remember a Object of the configured MCBaseClient Subclass in HashMap in the 
	 * Class org.edu_sharing.repository.server.RepoFactory. When we need a new Instance the getInstance Method of the Object will be called,
	 * which calls the constructor directly. 
	 * @param _authenticationInfo
	 * @return
	 */
	public abstract MCBaseClient getInstance(String _repositoryFile, HashMap<String, String> _authenticationInfo);
}
