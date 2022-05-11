package org.edu_sharing.metadataset.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MetadataSort implements Serializable {
	public class MetadataSortDefault implements Serializable {
		private String sortBy;
		private boolean sortAscending;

		public String getSortBy() {
			return sortBy;
		}

		public void setSortBy(String sortBy) {
			this.sortBy = sortBy;
		}

		public boolean isSortAscending() {
			return sortAscending;
		}

		public void setSortAscending(boolean sortAscending) {
			this.sortAscending = sortAscending;
		}
	}
	private String id;
	private String mode;
	private MetadataSortDefault defaultValue=new MetadataSortDefault();
	private List<MetadataSortColumn> columns;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public List<MetadataSortColumn> getColumns() {
		return columns;
	}
	public void setColumns(List<MetadataSortColumn> columns) {
		this.columns = columns;
	}

	public MetadataSortDefault getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(MetadataSortDefault defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MetadataSort){
			return ((MetadataSort)obj).id.equals(id);
		}
		return super.equals(obj);
	}
}
