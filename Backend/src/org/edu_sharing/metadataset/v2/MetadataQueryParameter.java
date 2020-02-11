package org.edu_sharing.metadataset.v2;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class MetadataQueryParameter implements Serializable {
	private String name;
	private Map<String,String> statements;
	private boolean multiple;
	private boolean exactMatching = true;
	private String multiplejoin;
	private int ignorable;
	private List<String> facets;
	private String preprocessor;
	private boolean mandatory = true;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, String> getStatements() {
		return statements;
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
		return QueryUtils.replaceCommonQueryParams(statement, QueryUtils.luceneReplacer);
	}
	private String getDefaultStatement() {
		return "@"+name.replace(":", "\\:")+":\"*${value}*\"";
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MetadataQueryParameter){
			MetadataQueryParameter other=(MetadataQueryParameter)obj;
			return (other.name.equals(name));
		}
		return super.equals(obj);
	}

	public void setStatements(Map<String, String> statements) {
		this.statements = statements;
	}

	public boolean isMultiple() {
		return multiple;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}

	public String getMultiplejoin() {
		return multiplejoin;
	}

	public void setMultiplejoin(String multiplejoin) {
		this.multiplejoin = multiplejoin;
	}

	public int getIgnorable() {
		return ignorable;
	}

	public void setIgnorable(int ignorable) {
		this.ignorable = ignorable;
	}

	public boolean isExactMatching() {
		return exactMatching;
	}

	public void setExactMatching(boolean exactMatching) {
		this.exactMatching = exactMatching;
	}


    public void setFacets(List<String> facets) {
        this.facets = facets;
    }

    public List<String> getFacets() {
        return facets;
    }

    public void setPreprocessor(String preprocessor) {
        this.preprocessor = preprocessor;
    }

    public String getPreprocessor() {
        return preprocessor;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isMandatory() {
        return mandatory;
    }
}
