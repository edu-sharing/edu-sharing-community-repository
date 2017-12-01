package org.edu_sharing.service.toolpermission;

import com.sun.star.uno.RuntimeException;

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
