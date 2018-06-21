package org.edu_sharing.service.permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.Authority;
import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.Result;
import org.edu_sharing.repository.client.rpc.User;

public class PermissionServiceAdapter implements PermissionService {
	
	protected List<String> ALLOWED_PERMISSIONS=new ArrayList<>();
	
	@Override
	public void addPermissions(String _nodeId, HashMap<String, String[]> _authPerm, Boolean _inheritPermissions,
			String _mailText, Boolean _sendMail, Boolean _sendCopy, Boolean createHandle) throws Throwable {
	}

	@Override
	public void setPermissions(String nodeId, ACE[] aces, Boolean inheritPermissions, String mailText, Boolean sendMail,
			Boolean sendCopy, Boolean createHandle) throws Throwable {
	}

	@Override
	public List<Notify> getNotifyList(String nodeId) throws Throwable {
		return null;
	}

	@Override
	public void setPermissions(String nodeId, ACE[] aces, Boolean inheritPermission) throws Exception {
	}

	@Override
	public void setPermissions(String nodeId, ACE[] aces) throws Exception {	
	}

	@Override
	public void setPermissions(String nodeId, String authority, String[] permissions, Boolean inheritPermission)
			throws Exception {
	}

	@Override
	public void addPermissions(String nodeId, ACE[] aces) throws Exception {
	}

	@Override
	public void removePermissions(String nodeId, ACE[] aces) throws Exception {	
	}

	@Override
	public void removePermissions(String nodeId, String authority, String[] _permissions) throws Exception {
	}

	@Override
	public Result<List<User>> findUsers(HashMap<String, String> propVals, boolean globalContext, int from,
			int nrOfResults) {
		return null;
	}

	@Override
	public Result<List<Authority>> findAuthorities(String searchWord, boolean globalContext, int from,
			int nrOfResults) {
		return null;
	}

	@Override
	public Result<List<Group>> findGroups(String searchWord, boolean globalContext, int from, int nrOfResults) {
		return null;
	}

	@Override
	public void createNotifyObject(String nodeId, String user, String event, String action) {
	}

	@Override
	public boolean hasPermission(String storeProtocol, String storeId, String nodeId, String permission) {
		return 	ALLOWED_PERMISSIONS.contains(permission);
	}

	@Override
	public HashMap<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId,
			String[] permissions) {
		HashMap<String, Boolean> map = new HashMap<String, Boolean>();
		for(String permission : permissions){
			if(ALLOWED_PERMISSIONS.contains(permission))
				map.put(permission, true);
			else
				map.put(permission, false);
		}
		return map;
	}
	
	@Override
	public ACL getPermissions(String nodeId) throws Exception {
		return null;
	}

	@Override
	public List<String> getPermissionsForAuthority(String nodeId, String authorityId) throws Exception {
		return null;
	}
	
	@Override
	public void setPermission(String nodeId, String authority, String permission) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<String> getExplicitPermissionsForAuthority(String nodeId, String authorityId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
