package org.edu_sharing.metadataset.v2;

import java.io.Serializable;
import java.util.List;

public class MetadataQuery extends MetadataQueryBase implements Serializable {
	protected String id;
	protected String join;
	protected Boolean applyBasequery;
	private List<MetadataQueryParameter> parameters;

	public MetadataQuery(){}

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

	public void overrideWith(MetadataQuery query) {
		if(query.applyBasequery!=null)
			this.applyBasequery=query.applyBasequery;
		if(query.basequery!=null)
			this.basequery=query.basequery;
		if(query.conditions!=null)
			this.conditions=query.conditions;
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

	public boolean isApplyBasequery() {
		if(applyBasequery==null)
			return true;

		return applyBasequery;
	}
}
