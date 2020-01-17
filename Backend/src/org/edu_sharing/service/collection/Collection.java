package org.edu_sharing.service.collection;

import org.edu_sharing.repository.client.rpc.User;

public class Collection {

	boolean level0 = false;
	boolean pinned = false;

	String nodeId; 
	String title; 
	String description;
	String type;
	String viewtype;
	String scope;
	String orderMode;
	
	int childCollectionsCount;
	
	int childReferencesCount;
	
	User owner;
	private String authorFreetext;


	public boolean isLevel0() {
		return level0;
	}
	public void setLevel0(boolean level0) {
		this.level0 = level0;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getZ() {
		return z;
	}
	public void setZ(int z) {
		this.z = z;
	}
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}
	int x;
	int y;
	int z;
	
	String color;

	private boolean fromUser;

	
	
	//float order;
	/**
	 * performance?
	 * Anzahl der Kinder (Ref-Objekte)?
		Anzahl der Kinder (Untersammungen)?
	 * 
	 * 
	 * Datentyp anzeige info, mimetype (WerteListe kommt noch)???
	 */
	
	public String getNodeId() {
		return nodeId;
	}
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	
	
	public String getViewtype() {
		return viewtype;
	}
	public void setViewtype(String viewtype) {
		this.viewtype = viewtype;
	}
	
	public int getChildCollectionsCount() {
		return childCollectionsCount;
	}
	
	public void setChildCollectionsCount(int childCollectionsCount) {
		this.childCollectionsCount = childCollectionsCount;
	}
	
	public int getChildReferencesCount() {
		return childReferencesCount;
	}
	
	public void setChildReferencesCount(int childReferencesCount) {
		this.childReferencesCount = childReferencesCount;
	}
	
	public User getOwner() {
		return owner;
	}
	
	public void setOwner(User owner) {
		this.owner = owner;
	}
	public boolean isFromUser() {
		return fromUser;
	}
	public void setFromUser(boolean fromUser) {
		this.fromUser=fromUser;
	}
	public String getScope() {
		return scope;
	}
	public void setScope(String scope) {
		this.scope=scope;
	}
	public boolean isPinned() {
		return pinned;
	}
	public void setPinned(boolean pinned) {
		this.pinned = pinned;
	}
	public String getOrderMode() {
		return orderMode;
	}
	public void setOrderMode(String orderMode) {
		this.orderMode = orderMode;
	}


    public String getAuthorFreetext() {
        return authorFreetext;
    }

    public void setAuthorFreetext(String authorFreetext) {
        this.authorFreetext = authorFreetext;
    }
}
