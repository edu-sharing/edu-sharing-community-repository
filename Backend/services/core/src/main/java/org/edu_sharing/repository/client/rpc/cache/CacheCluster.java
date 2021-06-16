package org.edu_sharing.repository.client.rpc.cache;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class CacheCluster implements Serializable{
	
	List<CacheMember> instances;
	
	List<CacheInfo> cacheInfos;
	
	String localMember;
	
	long freeMemory;
	
	long totalMemory;
	
	long maxMemory;
	
	int availableProcessors;
	
	Date timeStamp;
	
	String groupName;
	

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

	public long getFreeMemory() {
		return freeMemory;
	}

	public void setFreeMemory(long freeMemory) {
		this.freeMemory = freeMemory;
	}

	public long getTotalMemory() {
		return totalMemory;
	}

	public void setTotalMemory(long totalMemory) {
		this.totalMemory = totalMemory;
	}

	public long getMaxMemory() {
		return maxMemory;
	}

	public void setMaxMemory(long maxMemory) {
		this.maxMemory = maxMemory;
	}

	public int getAvailableProcessors() {
		return availableProcessors;
	}

	public void setAvailableProcessors(int availableProcessors) {
		this.availableProcessors = availableProcessors;
	}
	
	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public Date getTimeStamp() {
		return timeStamp;
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	
	
	
}
