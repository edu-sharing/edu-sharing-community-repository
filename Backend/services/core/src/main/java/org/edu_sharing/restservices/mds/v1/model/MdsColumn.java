package org.edu_sharing.restservices.mds.v1.model;

import org.edu_sharing.metadataset.v2.MetadataColumn;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "")
public class MdsColumn {
		private String id;
		private String format;
		private boolean showDefault;
	
		public MdsColumn(MetadataColumn column) {
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

