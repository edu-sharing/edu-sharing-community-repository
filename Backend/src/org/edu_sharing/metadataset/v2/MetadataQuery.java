package org.edu_sharing.metadataset.v2;

import java.util.List;

public class MetadataQuery {
	private String id,join;
	private List<MetadataQueryParameter> parameters;
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getJoin() {
		return join;
	}

	public void setJoin(String join) {
		this.join = join;
	}

	public List<MetadataQueryParameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<MetadataQueryParameter> parameters) {
		this.parameters = parameters;
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MetadataQuery){
			MetadataQuery other=(MetadataQuery)obj;
			return (other.id.equals(id));
		}
		return super.equals(obj);
	}

	public MetadataQueryParameter findParameterByName(String name) {
		for(MetadataQueryParameter parameter : getParameters()){
			if(parameter.getName().equals(name))
				return parameter;
		}
		return null;
	}
	
}
