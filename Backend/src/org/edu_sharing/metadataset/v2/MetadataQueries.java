package org.edu_sharing.metadataset.v2;

import java.util.List;

public class MetadataQueries {
	private String basequery;
	private boolean allowSearchWithoutCriteria;
	private List<MetadataQuery> queries;
	public String getBasequery() {
		return basequery;
	}
	public void setBasequery(String basequery) {
		this.basequery = basequery;
	}
	public boolean isAllowSearchWithoutCriteria() {
		return allowSearchWithoutCriteria;
	}
	public void setAllowSearchWithoutCriteria(boolean allowSearchWithoutCriteria) {
		this.allowSearchWithoutCriteria = allowSearchWithoutCriteria;
	}
	public List<MetadataQuery> getQueries() {
		return queries;
	}
	public void setQueries(List<MetadataQuery> queries) {
		this.queries = queries;
	}
	public void overrideWith(MetadataQueries queries2) {
		if(queries2==null)
			return;
		if(queries2.getBasequery()!=null)
			setBasequery(queries2.getBasequery());
		for(MetadataQuery query: queries2.getQueries()){
			if(queries.contains(query)){
				queries.remove(query);
				queries.add(query);
			}
			else{
				queries.add(query);
			}
		}
	}
}
