package org.edu_sharing.service.editlock;

public class EditLockServiceFactory {
	public static EditLockService getEditLockService(){
		return new EditLockServiceImpl();
	}
}
