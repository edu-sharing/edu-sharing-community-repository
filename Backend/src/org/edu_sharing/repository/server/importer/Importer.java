package org.edu_sharing.repository.server.importer;

public interface Importer {
	
	public void setBaseUrl(String baseUrl);
	
	public void setRecordHandler(RecordHandlerInterface recordHandler);
	
	public void setPersistentHandler(PersistentHandlerInterface persistentHandler);
	
	public void setBinaryHandler(BinaryHandler binaryHandler);
	
	public void setNrOfResumptions(int nrOfResumptions);
	
	public void setNrOfRecords(int nrOfRecords);
	
	public void setMetadataPrefix(String metadataPrefix);
	
	public void setSet(String set);
	
	public void startImport() throws Throwable;
	
	public void startImport(String[] oaiIDs, String set);
	
}
