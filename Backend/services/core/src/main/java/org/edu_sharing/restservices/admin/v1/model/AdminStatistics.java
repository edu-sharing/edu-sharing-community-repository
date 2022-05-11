package org.edu_sharing.restservices.admin.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.ArrayList;
import java.util.List;

import org.edu_sharing.restservices.shared.Node;

import com.fasterxml.jackson.annotation.JsonProperty;


@Schema(description = "")
public class AdminStatistics  {
  
  private int activeSessions,numberOfPreviews;
  private long maxMemory,allocatedMemory,previewCacheSize;
  private List<Node> activeLocks;
	public int getActiveSessions() {
		return activeSessions;
	}
	public void setActiveSessions(int activeSessions) {
		this.activeSessions = activeSessions;
	}
	public List<Node> getActiveLocks() {
		return activeLocks;
	}
	public void setActiveLocks(List<Node> activeLocks) {
		this.activeLocks = activeLocks;
	}
	public long getMaxMemory() {
		return maxMemory;
	}
	public void setMaxMemory(long maxMemory) {
		this.maxMemory = maxMemory;
	}
	public long getAllocatedMemory() {
		return allocatedMemory;
	}
	public void setAllocatedMemory(long allocatedMemory) {
		this.allocatedMemory = allocatedMemory;
	}
	public int getNumberOfPreviews() {
		return numberOfPreviews;
	}
	public void setNumberOfPreviews(int numberOfPreviews) {
		this.numberOfPreviews = numberOfPreviews;
	}
	public long getPreviewCacheSize() {
		return previewCacheSize;
	}
	public void setPreviewCacheSize(long previewCacheSize) {
		this.previewCacheSize = previewCacheSize;
	}
	
  
}
