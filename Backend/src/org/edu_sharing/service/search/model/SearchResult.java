package org.edu_sharing.service.search.model;

import java.util.List;

import org.alfresco.query.PagingResults;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.util.Pair;

public class SearchResult<E> {
	public List<E> getData() {
		return data;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public int getSkipCount() {
		return skipCount;
	}

	private List<E> data;
	private int totalCount;
	private int skipCount;

	public SearchResult(List<E> data,int skipCount,Pair<Integer,Integer> pair) {
		this.data=data;
		
		this.skipCount=skipCount;
		this.totalCount=pair==null ? 0 : pair.getFirst()!=null ? pair.getFirst() : pair.getSecond();
	}
	public SearchResult(List<E> data,int skipCount,int total) {
		this.data=data;
		
		this.skipCount=skipCount;
		this.totalCount=total;
	}
	public int getCount() {
		return data==null ? 0 : data.size();
	}

}
