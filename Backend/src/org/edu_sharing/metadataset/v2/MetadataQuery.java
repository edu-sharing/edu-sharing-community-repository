package org.edu_sharing.metadataset.v2;

import java.util.List;

public class MetadataQuery {
	private String id,join,basequery;
	private List<MetadataQueryParameter> parameters;
	private MetadataQueries parent;
	private Boolean applyBasequery;
	public MetadataQuery(MetadataQueries parent){
		this.parent = parent;
	}
	
	public MetadataQueries getParent() {
		return parent;
	}

	public boolean isApplyBasequery() {
		if(applyBasequery==null)
			return true;
		
		return applyBasequery;
	}

	public void setApplyBasequery(Boolean applyBasequery) {
		this.applyBasequery = applyBasequery;
	}

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
	
	public String getBasequery() {
		return basequery;
	}

	public void setBasequery(String basequery) {
		this.basequery = basequery;
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

	public void setParent(MetadataQueries parent) {
		this.parent = parent;	
	}

	public void overrideWith(MetadataQuery query) {
		this.parent=query.parent;
		if(query.applyBasequery!=null)
			this.applyBasequery=query.applyBasequery;
		if(query.basequery!=null)
			this.basequery=query.basequery;
		if(query.join!=null)
			this.join=query.join;
		for(MetadataQueryParameter param : query.parameters) {
			if(parameters.contains(param)) {
				parameters.remove(param);
				parameters.add(param);
			}
			else {
				parameters.add(param);
			}
		}
	}
	
}
