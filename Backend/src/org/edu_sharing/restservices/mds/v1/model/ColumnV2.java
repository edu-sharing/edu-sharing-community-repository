package org.edu_sharing.restservices.mds.v1.model;

import java.util.List;

import org.edu_sharing.metadataset.v2.MetadataColumn;
import org.edu_sharing.metadataset.v2.MetadataGroup;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataTemplate;
import org.edu_sharing.metadataset.v2.MetadataWidget;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
@ApiModel(description = "")
public class ColumnV2 {
		private String id;
		private String format;
		private boolean showDefault;
	
		public ColumnV2(MetadataColumn column) {
			this.id=column.getId();		
			this.showDefault=column.isShowDefault();
			this.format=column.getFormat();
		}
		
		
		@JsonProperty("id")
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}

		@JsonProperty("showDefault")
		public boolean isShowDefault() {
			return showDefault;
		}

		public void setShowDefault(boolean showDefault) {
			this.showDefault = showDefault;
		}

		@JsonProperty("format")
		public String getFormat() {
			return format;
		}

		public void setFormat(String format) {
			this.format = format;
		}
		
		
	}

