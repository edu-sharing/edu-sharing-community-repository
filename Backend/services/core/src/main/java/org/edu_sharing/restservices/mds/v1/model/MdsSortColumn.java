package org.edu_sharing.restservices.mds.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.metadataset.v2.MetadataColumn;
import org.edu_sharing.metadataset.v2.MetadataSortColumn;

@Schema(description = "")
public class MdsSortColumn {
		private String id;
		private String mode;

		public MdsSortColumn(MetadataSortColumn column) {
			this.id=column.getId();		
			this.mode=column.getMode();
		}
		
		
		@JsonProperty(required = true)
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}

	@JsonProperty
	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}
}

