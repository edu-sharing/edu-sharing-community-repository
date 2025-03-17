package org.edu_sharing.metadataset.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MetadataQueryParameter implements Serializable {
	// the used syntax, inherited by the group of queries
	private final String syntax;
	private final transient MetadataSet mds;
	private String name;
	private Map<String,String> statements;
	private boolean multiple;
	private boolean exactMatching = true;
	private String multiplejoin;
	private int ignorable;


	private MetadataQueryFacet facet;
	private String preprocessor;
	private boolean mandatory = true;
	//only DSL
	private boolean asFilter = true;

	public MetadataQueryParameter(String syntax, MetadataSet mds){
		this.syntax = syntax;
		this.mds = mds;
	}

	public String getStatement(String value) {
		String statement=null;
		if(statements!=null) {
			if(statements.containsKey(value))
				statement=statements.get(value);
			else if(statements.get(null)!=null)
				statement=statements.get(null);
		}
		if(statement==null) {
			statement = getDefaultStatement();
		}
		return QueryUtils.replaceCommonQueryParams(statement, QueryUtils.replacerFromSyntax(syntax, true));
	}
	private String getDefaultStatement() {
		if(syntax.equals(MetadataReader.QUERY_SYNTAX_DSL)){
			//return "{\"wildcard\":{\"properties." + name  +"\":{\"value\":\"${value}\"}}}";
			try {

				JSONObject jsonObject = new JSONObject();
				JSONObject termObject = new JSONObject();
				JSONObject detailObject = new JSONObject();
				jsonObject.put("wildcard", termObject);
				termObject.put("properties." + name + ".keyword", detailObject);
				detailObject.put("case_insensitive", true);
				detailObject.put("value", "*${value}*");
				return jsonObject.toString();
			}catch(JSONException e){
				throw new RuntimeException(e);
			}
		} else if(syntax.equals(MetadataReader.QUERY_SYNTAX_LUCENE)) {
			return "@" + name.replace(":", "\\:") + ":\"*${value}*\"";
		}
		throw new RuntimeException("Unsupported syntax for query language: " + syntax);
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MetadataQueryParameter){
			MetadataQueryParameter other=(MetadataQueryParameter)obj;
			return (other.name.equals(name));
		}
		return super.equals(obj);
	}


	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MetadataQueryFacet implements Serializable {

		private SortBy sortBy = SortBy.count;
		private SortOrder sortOrder = SortOrder.desc;
		/**
		 * Limits the number of buckets returned out of the overall terms list.
		 * This value overrides the request specification with a fixed limit.
		 */
		private Integer maxBucketSize = null;
		private List<MetadataQueryFacetItem> items = new ArrayList<>();


		public enum SortBy {
			count,
			caption,
		}
		public enum SortOrder {
			asc,
			desc,
		}
	}


	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MetadataQueryFacetItem implements Serializable {
		private String value;
		private String nested;
	}
}
