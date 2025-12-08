package org.edu_sharing.service.permission;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.rpc.*;
import org.edu_sharing.service.InsufficientPermissionException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.*;

@Lazy
@Service
public class PermissionServiceAdapter implements PermissionService {
	
	protected List<String> ALLOWED_PERMISSIONS=new ArrayList<>();

	@Override
	public void updateTimedPermissions() {

	}

	@Override
	public void addPermissions(String _nodeId, Map<String, String[]> _authPerm, Boolean _inheritPermissions,
							   String _mailText, Boolean _sendMail, Boolean _sendCopy) {
	}

	@Override
	public List<Notify> getNotifyList(String nodeId) throws Throwable {
		return null;
	}

	@Override
	public void setPermissions(String nodeId, List<ACE> aces, Boolean inheritPermissions, String mailText,
							   Boolean sendMail, Boolean sendCopy) throws Throwable {

	}

	@Override
	public void setPermissions(String nodeId, List<ACE> aces, Boolean inheritPermission) throws Exception {

	}

	@Override
	public void setPermissions(String nodeId, List<ACE> aces) throws Exception {

	}

	@Override
	public void setPermissions(String nodeId, String authority, String[] permissions, Boolean inheritPermission)
			throws Exception {
	}

	@Override
	public void setPermissionInherit(String nodeId, boolean inheritPermission) {

	}

	@Override
	public void addPermissions(String nodeId, ACE[] aces) {
	}

	@Override
	public void removePermissions(String nodeId, List<ACE> aces) {
	}

	@Override
	public void removeAllPermissions(String nodeId) {

	}

    @Override
	public void createNotifyObject(String nodeId, String user, String action) {
	}

	@Override
	public void addToRecentProperty(String property, NodeRef elementAdd) {

	}

	@Override
	public boolean hasPermission(String storeProtocol, String storeId, String nodeId, String permission) {
		return 	ALLOWED_PERMISSIONS.contains(permission);
	}

	@Override
	public Map<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId,
			String[] permissions) {
		Map<String, Boolean> map = new HashMap<>();
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
	public List<String> getPermissionsForAuthority(String nodeId, String authorityId, Collection<String> permissions) throws InsufficientPermissionException {
		return null;
	}

	@Override
	public void setPermission(String nodeId, String authority, String permission) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public void removePermission(String nodeId, String authority, String permission) {

    }

    @Override
	public boolean hasPermission(String storeProtocol, String storeId, String nodeId, String authority, String permission) {
		return false;
	}

	@Override
	public Map<String, Boolean> hasAllPermissions(String storeProtocol, String storeId, String nodeId, String authority, String[] permissions) {
		return null;
	}
	@Override
	public List<String> getExplicitPermissionsForAuthority(String nodeId, String authorityId) throws InsufficientPermissionException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<String> getRecentlyInvited() {
		return null;
	}

	@Override
	public ArrayList<NodeRef> getRecentProperty(String property) {
		return null;
	}

	@Override
	public boolean isAdminOrSystem() {
		return false;
	}

	@Override
	public List<String> getOrganizationsOfUser() {
		return List.of();
	}
}
