package org.edu_sharing.metadataset.v2;

public class MetadataQueryParameter {
	private String name;
	private String statement;
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

	public String getStatement() {
		if(statement!=null)
			return statement;
		return "@"+name.replace(":", "\\:")+":\"*${value}*\"";
	}

	public void setStatement(String statement) {
		this.statement = statement;
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
