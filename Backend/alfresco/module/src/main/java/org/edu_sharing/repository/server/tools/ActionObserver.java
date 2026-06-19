package org.edu_sharing.repository.server.tools;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.edu_sharing.repository.server.tools.cache.Queue;

/**
 * manage queue of thumbnail actions processed by PreviewJob
 *
 */
public class ActionObserver {

    @Getter
    private static final ActionObserver instance = new ActionObserver();

	public static String ACTION_OBSERVER_ADD_DATE = "action-observer-add-date";



	private ActionObserver() {

	}

    @Data
    @Builder
    public static class ActionData{
        NodeRef nodeRef;
        Action action;
    }


    final Queue<NodeRef> queue = (Queue<NodeRef>)AlfAppContextGate.getApplicationContext().getBean("eduSharingPreviewQueue");


	@Getter
    public Map<NodeRef, List<Action>> nodeActionsMap = new ConcurrentHashMap<>();

	/**
	 *
	 * @param nodeRef
	 *
	 */
	public void addAction(NodeRef nodeRef) {
        queue.offer(nodeRef);
	}


    public NodeRef pollNewAction(){
        NodeRef data = queue.poll();
        return data;
    }

    public int queueSize(){
        return queue.size();
    }
}
