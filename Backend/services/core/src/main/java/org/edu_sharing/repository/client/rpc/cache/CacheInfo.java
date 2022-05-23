package org.edu_sharing.repository.client.rpc.cache;

import java.io.Serializable;

public class CacheInfo implements Serializable{
	
	int size;
	
	

	
	long statisticHits;
	
	
	String name;
	
	int backupCount;
	
	long backupEntryCount;
	
	long backupEntryMemoryCost;
	
	long heapCost;
	
	long ownedEntryCount;
	
	long getOwnedEntryMemoryCost;
	
	/**
	 * calculated by Instrumentation class
	 */
	long sizeInMemory;
	
	String member;
	
	String groupName;
	
	
	/**
	 * config values
	 * @return
	 */
	int maxSize;
	
	public String getMember() {
		return member;
	}

	public void setMember(String member) {
		this.member = member;
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}


	public long getStatisticHits() {
		return statisticHits;
	}

	public void setStatisticHits(long statisticCacheHitCount) {
		this.statisticHits = statisticCacheHitCount;
	}


	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public int getBackupCount() {
		return backupCount;
	}

	public void setBackupCount(int backupCount) {
		this.backupCount = backupCount;
	}

	public long getBackupEntryCount() {
		return backupEntryCount;
	}

	public void setBackupEntryCount(long backupEntryCount) {
		this.backupEntryCount = backupEntryCount;
	}

	public long getBackupEntryMemoryCost() {
		return backupEntryMemoryCost;
	}

	public void setBackupEntryMemoryCost(long backupEntryMemoryCost) {
		this.backupEntryMemoryCost = backupEntryMemoryCost;
	}

	public long getHeapCost() {
		return heapCost;
	}

	public void setHeapCost(long heapCost) {
		this.heapCost = heapCost;
	}
	
	public void setOwnedEntryCount(long ownedEntryCount) {
		this.ownedEntryCount = ownedEntryCount;
	}
	
	public long getOwnedEntryCount() {
		return ownedEntryCount;
	}
	
	public void setSizeInMemory(long sizeInMemory) {
		this.sizeInMemory = sizeInMemory;
	}
	
	public long getSizeInMemory() {
		return sizeInMemory;
	}
	
	public void setGetOwnedEntryMemoryCost(long getOwnedEntryMemoryCost) {
		this.getOwnedEntryMemoryCost = getOwnedEntryMemoryCost;
	}
	
	public long getGetOwnedEntryMemoryCost() {
		return getOwnedEntryMemoryCost;
	}
	
	public int getMaxSize() {
		return maxSize;
	}
	
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

}
