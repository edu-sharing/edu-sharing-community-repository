package org.edu_sharing.service.nodeservice;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceOCartImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@RequiredArgsConstructor
public class NodeServiceOCAImpl extends NodeServiceAdapter{

    private final SearchServiceOCartImpl searchService;

    @Override
	public Map<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {
		return searchService.getProperties(nodeId);

   }
	
}
