package org.edu_sharing.restservices;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.node.v1.model.NodeEntry;
import org.edu_sharing.restservices.shared.ErrorResponse;

import io.swagger.annotations.ApiResponse;

public class RestConstants {
	public static final String MESSAGE_REPOSITORY_ID="ID of repository (or \"-home-\" for home repository)";
	public static final String MESSAGE_PARENT_NODE="ID of parent node";
	public static final String MESSAGE_SOURCE_NODE="ID of source node";
	public static final String MESSAGE_NODE_ID="ID of node";
	public static final String MESSAGE_MAX_ITEMS=	"maximum items per page";
	public static final String MESSAGE_SKIP_COUNT=	"skip a number of items";
	public static final String MESSAGE_FILTER=		"filter by type files,folders";
	public static final String MESSAGE_SORT_PROPERTIES="sort properties";	
	public static final String MESSAGE_SORT_ASCENDING="sort ascending, true if not set. Use multiple values to change the direction according to the given property at the same index";
	public static final ApiResponse[] RETURN_TYPES = null;
	
	public static final String HTTP_200="OK.";
	public static final String HTTP_400="Preconditions are not present.";
	public static final String HTTP_401="Authorization failed.";
	public static final String HTTP_403="Session user has insufficient rights to perform this operation.";
	public static final String HTTP_404="Ressources are not found.";
	public static final String HTTP_409="Duplicate Entity/Node conflict (Node with same name exists)";
	public static final String HTTP_500="Fatal error occured.";
	public static final String VALUES_FILTER = "files,folders";
	public static final String VALUES_SORT_PROPERTIES = "cm:name,cm:modifiedDate";
	public static final int DEFAULT_MAX_ITEMS = 10;
	public static final String MESSAGE_PROPERTY_FILTER = "property filter for result nodes (or \"-all-\" for all properties)";
	
}
