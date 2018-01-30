package org.edu_sharing.metadataset.v2;

import java.util.Map;

public class MetadataQueryParameter {
	private String name;
	private Map<String,String> statements;
	private boolean multiple;
	private boolean exactMatching;
	private String multiplejoin;
	private int ignorable;
	
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
		if(statements!=null) {
			if(statements.containsKey(value))
				return statements.get(value);
			if(statements.get(null)!=null)
				return statements.get(null);
		}
		return getDefaultStatement();
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

	
	
}
