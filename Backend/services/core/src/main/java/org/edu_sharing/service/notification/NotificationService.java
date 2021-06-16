package org.edu_sharing.service.notification;

import java.util.HashMap;
import java.util.List;

import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.Authority;
import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.Result;
import org.edu_sharing.repository.client.rpc.User;

public interface NotificationService {
	public void notifyNodeIssue(String nodeId,String reason,String userEmail,String userComment) throws Throwable;
}
