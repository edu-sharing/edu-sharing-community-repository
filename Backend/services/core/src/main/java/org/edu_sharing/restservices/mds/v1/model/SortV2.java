package org.edu_sharing.restservices.mds.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import org.edu_sharing.metadataset.v2.MetadataColumn;
import org.edu_sharing.metadataset.v2.MetadataList;
import org.edu_sharing.metadataset.v2.MetadataSort;
import org.edu_sharing.metadataset.v2.MetadataSortColumn;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "")
public class SortV2 {
	public class SortV2Default{
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
	private SortV2Default defaultValue;
	private List<SortColumnV2> columns;

	public SortV2(){}
	public SortV2(MetadataSort sort) {
		this.id=sort.getId();
		if(sort.getDefaultValue()!=null){
			this.defaultValue=new SortV2Default();
			this.defaultValue.setSortBy(sort.getDefaultValue().getSortBy());
			this.defaultValue.setSortAscending(sort.getDefaultValue().isSortAscending());
		}
		if(sort.getColumns()!=null){
			columns=new ArrayList<>();
			for(MetadataSortColumn column : sort.getColumns()){
				columns.add(new SortColumnV2(column));
			}
		}
	}

	@JsonProperty
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	@JsonProperty
	public List<SortColumnV2> getColumns() {
		return columns;
	}
	public void setColumns(List<SortColumnV2> columns) {
		this.columns = columns;
	}

	@JsonProperty("default")
	public SortV2Default getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(SortV2Default defaultValue) {
		this.defaultValue = defaultValue;
	}
}

