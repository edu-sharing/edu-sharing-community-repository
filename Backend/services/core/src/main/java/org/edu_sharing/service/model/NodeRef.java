package org.edu_sharing.service.model;

import org.edu_sharing.restservices.shared.Contributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface NodeRef {

    String getOwner();

	void setOwner(String owner);

	Map<NodeRefImpl.Relation, NodeRef> getRelations();

	void setRelations(Map<NodeRefImpl.Relation, NodeRef> relations);

	void setContributors(List<Contributor> contributors);

	List<Contributor> getContributors();

	interface Preview{
		String getMimetype();
		byte[] getData();

		String getType();

		Boolean getIcon();
	}

	public String getRepositoryId();

	public void setRepositoryId(String repositoryId);

	public String getStoreProtocol();

	public void setStoreProtocol(String storeProtocol);

	public String getStoreId();

	public void setStoreId(String storeId);

	public String getNodeId();

	public void setNodeId(String nodeId);

	public HashMap<String,Object> getProperties();

	public void setProperties(HashMap<String,Object> properties);

	public void setPreview(Preview preview);

	public Preview getPreview();

	public Map<String,Boolean> getPermissions();

	public void setPermissions(Map<String,Boolean> permissions);

	public Boolean getPublic();

	public void setPublic(Boolean aPublic);

	void setAspects(List<String> aspects);

	public List<String> getAspects();

	public void setUsedInCollections(List<CollectionRef> usedInCollections);

	public List<CollectionRef> getUsedInCollections();

}
