package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;

public class ContextMenuEntry extends AbstractEntry{
	@XmlElement public String mode;
	@XmlElement public ContextMenuEntryScope[] scopes;
	@XmlElement public Boolean ajax;
	@XmlElement	public String group;
	@XmlElement	public String permission;
	@XmlElement	public String toolpermission;
	@XmlElement	public Boolean isDirectory;
	@XmlElement	public Boolean showAsAction;
	@XmlElement	public Boolean multiple;
	@XmlElement	public ContextMenuEntryChangeStrategy changeStrategy;

	enum ContextMenuEntryScope{
		Render,
		Search,
		CollectionsReferences,
		CollectionsCollection,
		WorkspaceList,
		WorkspaceTree,
		Oer,
		CreateMenu,
	}
	enum ContextMenuEntryChangeStrategy{
		update,
		remove
	}
}
