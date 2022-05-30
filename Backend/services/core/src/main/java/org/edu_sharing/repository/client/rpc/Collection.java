package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;

public class Collection implements Serializable{

	boolean level0;
	
	String nodeId; 
	String title; 
	String description;
	String type;
	String viewtype;
	
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

}

