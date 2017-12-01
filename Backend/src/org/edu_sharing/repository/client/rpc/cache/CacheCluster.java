package org.edu_sharing.repository.client.rpc.cache;

import java.io.Serializable;
import java.util.List;

public class CacheCluster implements Serializable{
	
	List<CacheMember> instances;
	
	List<CacheInfo> cacheInfos;
	
	String localMember;

	public List<CacheInfo> getCacheInfos() {
		return cacheInfos;
	}

	public void setCacheInfos(List<CacheInfo> cacheInfos) {
		this.cacheInfos = cacheInfos;
	}

	
	public List<CacheMember> getInstances() {
		return instances;
	}
	
	public void setInstances(List<CacheMember> instances) {
		this.instances = instances;
	}
	
	public void setLocalMember(String localMember) {
		this.localMember = localMember;
	}
	
	public String getLocalMember() {
		return localMember;
	}
	
	
}
