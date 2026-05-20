package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;

public class ContextMenuEntry extends AbstractEntry{
	@Schema(description = "When to show: 'nodes' (selected nodes), 'noNodes' (nothing selected), 'noNodesNotEmpty' (nothing selected but items available), 'always'")
	@XmlElement public String mode;
	@Schema(description = "Scopes where option appears (e.g. 'Render', 'Search', 'WorkspaceList'). Empty = all scopes")
	@XmlElement public ContextMenuEntryScope[] scopes;
	@Schema(description = "If true, call URL via AJAX; if false, open in current window")
	@XmlElement public Boolean ajax;
	@Schema(description = "Option grouping (e.g. 'Create', 'View', 'Edit')")
	@XmlElement public String group;
	@Schema(description = "Only show if node has this permission (e.g. 'Write', 'CCPublish')")
	@XmlElement public String permission;
	@Schema(description = "Only show if user has this tool permission")
	@XmlElement public String toolpermission;
	@Schema(description = "true = only for folders, false = only for files, null = both")
	@XmlElement public Boolean isDirectory;
	@Schema(description = "If true, show as action in toolbar")
	@XmlElement public Boolean showAsAction;
	@Schema(description = "If true, action works on multiple selected nodes")
	@XmlElement public Boolean multiple;
	@Schema(description = "For modifications: 'update' to modify existing option, 'remove' to delete")
	@XmlElement public ContextMenuEntryChangeStrategy changeStrategy;

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
