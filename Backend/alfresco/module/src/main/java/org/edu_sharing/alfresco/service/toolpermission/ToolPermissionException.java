package org.edu_sharing.alfresco.service.toolpermission;


public class ToolPermissionException extends RuntimeException{
	private String toolpermission;

	public ToolPermissionException(String toolpermission) {
		this.toolpermission=toolpermission;
	}
	@Override
	public String toString() {
		return toolpermission+" is missing for current user";
	}
	@Override
	public String getMessage() {
		return toString();
	}
}
