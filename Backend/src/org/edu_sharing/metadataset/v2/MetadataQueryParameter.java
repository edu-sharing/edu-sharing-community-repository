package org.edu_sharing.metadataset.v2;

public class MetadataQueryParameter {
	private String name;
	private String statement;
	private boolean multiple;
	private String multiplejoin;

	
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
	
}
