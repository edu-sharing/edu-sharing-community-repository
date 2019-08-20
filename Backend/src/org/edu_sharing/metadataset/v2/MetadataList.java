package org.edu_sharing.metadataset.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MetadataList implements Serializable {
	private String id;
	private List<MetadataColumn> columns;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<MetadataColumn> getColumns() {
		return columns;
	}
	public void setColumns(List<MetadataColumn> columns) {
		this.columns = columns;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MetadataList){

			return ((MetadataList)obj).id.equals(id);
		}
		return super.equals(obj);
	}
}
