package org.edu_sharing.service.admin.model;

import org.edu_sharing.restservices.shared.Group;

import java.util.List;

public class ToolPermission {

    public static enum Status {
		ALLOWED,
		DENIED,
		UNDEFINED
	}
	private Status explicit;
	private Status effective;
	private List<Group> effectiveSource;

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

	public void setEffectiveSource(List<Group> effectiveSource) {
		this.effectiveSource = effectiveSource;
	}

	public List<Group> getEffectiveSource() {
		return effectiveSource;
	}

}
