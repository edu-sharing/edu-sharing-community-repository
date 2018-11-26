package org.edu_sharing.restservices.mds.v1.model;

import java.util.ArrayList;
import java.util.List;

import org.edu_sharing.metadataset.v2.MetadataColumn;
import org.edu_sharing.metadataset.v2.MetadataList;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
@ApiModel(description = "")
public class ListV2 {
		private String id;
		private List<ColumnV2> columns;
	
		public ListV2(){}
		public ListV2(MetadataList list) {
			this.id=list.getId();
			if(list.getColumns()!=null){
				columns=new ArrayList<>();
				for(MetadataColumn column : list.getColumns()){
					columns.add(new ColumnV2(column));
				}
			}
		}

	@JsonProperty("id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
		@JsonProperty("columns")
		public List<ColumnV2> getColumns() {
			return columns;
		}
		public void setColumns(List<ColumnV2> columns) {
			this.columns = columns;
		}
		
		
	}

