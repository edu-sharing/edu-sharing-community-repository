package org.edu_sharing.metadataset.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataList implements Serializable {
	public enum ColumnType {
		Default,
		Table,
	}
	private String id;
	private final Map<ColumnType,List<MetadataColumn>> columns = new HashMap<>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<ColumnType,List<MetadataColumn>> getColumns() {
		return columns;
	}
	public void setColumns(ColumnType columnType, List<MetadataColumn> columns) {
		this.columns.put(columnType, columns);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MetadataList){

			return ((MetadataList)obj).id.equals(id);
		}
		return super.equals(obj);
	}
}
