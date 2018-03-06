package org.edu_sharing.service.admin.model;

public class ToolPermission {
	public static enum Status {
		ALLOWED,
		DENIED,
		UNDEFINED
	}
	private Status explicit;
	private Status effective;
	
	public ToolPermission() {
		explicit=Status.UNDEFINED;
		effective=Status.UNDEFINED;				
	}
	public Status getExplicit() {
		return explicit;
	}
	public void setExplicit(Status explicit) {
		this.explicit = explicit;
	}
	public Status getEffective() {
		return effective;
	}
	public void setEffective(Status effective) {
		this.effective = effective;
	}

}
