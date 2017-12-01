package org.edu_sharing.restservices.collection.v1.model;

import java.util.List;

import org.edu_sharing.restservices.shared.NodeAccess;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.Preview;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "")
public class CollectionBase {

	private NodeRef ref = null;
	
	private List<String> access = null;
	private Preview preview;

	private String scope;
	
	/**
	   **/
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("ref")
		public NodeRef getRef() {
			return ref;
		}

		public void setRef(NodeRef ref) {
			this.ref = ref;
		}
		@ApiModelProperty(required = true, value = "")
		@JsonProperty("scope")
		public String getScope() {
			return scope;
		}

		public void setScope(String scope) {
			this.scope = scope;
		}
		
		
		/**
		   **/
			@ApiModelProperty(required = true, value = "")
			@JsonProperty("access")
			public List<String> getAccess() {
				return access;
			}

			public void setAccess(List<String> access) {
				this.access = access;
			}
			/**
			   **/
				@ApiModelProperty(required = true, value = "")
				@JsonProperty("preview")
				public Preview getPreview() {
					return preview;
				}

				public void setPreview(Preview preview) {
					this.preview = preview;
				}
}
