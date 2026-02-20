package org.edu_sharing.repository.server.tools;

import lombok.Getter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.alfresco.model.ContentModel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * remembers the Actions that are called on nodes when the action List for a
 * node is requested all actions that are ready will be removed
 * 
 * is a singelton
 * 
 * Attention! only use with actions that got TrackStatus=true
 * 
 * @author rudi
 *
 */
public class ActionObserver {

    @Getter
    private static final ActionObserver instance = new ActionObserver();

	Logger logger = Logger.getLogger(ActionObserver.class);

	NodeService nodeservice = (NodeService) AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");
    ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
    ContentService contentService = serviceRegistry.getContentService();

	public static String ACTION_OBSERVER_ADD_DATE = "action-observer-add-date";

    long shouldBeRemovedCheck = -1;


	private ActionObserver() {

	}

	@Getter
    public Map<NodeRef, List<Action>> nodeActionsMap = new ConcurrentHashMap<>();

	/**
	 * calls removeInactiveActions
	 * 
	 * only actions that got TrackStatus=true a observed here
	 * 
	 * @param nodeRef
	 * @param action
	 */
	public void addAction(NodeRef nodeRef, Action action) {
		//removeInactiveActions();

		if (action.getTrackStatus() == false) {
			logger.error("action.getTrackStatus() = false, will return");
			return;
		}

        nodeActionsMap.compute(nodeRef, (k, actions) -> {
            if(actions == null) actions = new CopyOnWriteArrayList<>();

            boolean alreadyThere = actions.stream()
                    .anyMatch(a -> a.getActionDefinitionName().equals(action.getActionDefinitionName()));

            if(!alreadyThere) actions.add(action);
            return actions;
        });
	}

	/**
	 * returns the first action it founds for the nodeRef with
	 * ActionDefinitionName @param actionName
	 * 
	 * @param nodeRef
	 * @param actionName
	 * @return
	 */
	public Action getAction(NodeRef nodeRef, String actionName) {
		List<Action> actions = nodeActionsMap.get(nodeRef);
		if (actions == null) {
			return null;
		}

		for (Action action : actions) {
			if (action.getActionDefinitionName().equals(actionName)) {
				return action;
			}
		}
		return null;
	}

	public void removeInactiveActions() {

        String timeout = LightbendConfigLoader.get().getString("repository.transformer.preview.actionTimeout");
        long timeoutInMs = Duration.parse(timeout).toMillis();

		RunAsWork<Void> runAs = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				ArrayList<NodeRef> toRemove = new ArrayList<>();

                for (Map.Entry<NodeRef, List<Action>> entry : nodeActionsMap.entrySet()) {

                    if(entry.getValue() == null || entry.getValue().size() == 0){
                        logger.info(entry.getKey() +" has no actions. will remove entry");
                        toRemove.add(entry.getKey());
                        continue;
                    }

                    //observer removes action when node exists check fails. this can happen when transaction is not commited already.
                    boolean shouldBeRemoved = shouldBeRemoved(entry);
                    if (shouldBeRemoved) {
                        logger.info(entry.getKey() + " was deleted. will remove entry");
                        toRemove.add(entry.getKey());
                        continue;
                    }

                    if (entry.getValue() != null && entry.getValue().size() > 0) {

                        List<Action> actions = entry.getValue();
                        List<Action> toRemoveActions = new ArrayList<>();

                        for (Action action : actions) {
                            Date addDate = (Date) action.getParameterValue(ACTION_OBSERVER_ADD_DATE);
                            boolean actionTimedOut = false;
                            if (addDate != null) {
                                long msSinceCreation = new Date().getTime() - addDate.getTime();
                                if (msSinceCreation > timeoutInMs) {
                                    actionTimedOut = true;
                                    logger.info("action timed out");
                                }
                            }
                            if (action != null
                                    && (action.getExecutionStatus().equals(ActionStatus.Cancelled)
                                            || action.getExecutionStatus().equals(ActionStatus.Completed)
                                            || action.getExecutionStatus().equals(ActionStatus.Failed))
                                    || actionTimedOut) {

                                logger.info("will remove inactive action " + action.getActionDefinitionName()
                                        + " with status" + action.getExecutionStatus() + " for " + entry.getKey());
                                toRemoveActions.add(action);
                            }
                        }
                        for (Action action : toRemoveActions) {
                            actions.remove(action);
                        }
                    }
                }

				for (NodeRef nodeRef : toRemove) {
					nodeActionsMap.remove(nodeRef);
				}
				return null;
			}

		};

		AuthenticationUtil.runAsSystem(runAs);

	}

    private boolean shouldBeRemoved(Map.Entry<NodeRef, List<Action>> entry) {


        Duration checkFirst = Duration.parse(LightbendConfigLoader.get().getString("repository.transformer.preview.checkFirst"));
        Duration checkInterval = Duration.parse(LightbendConfigLoader.get().getString("repository.transformer.preview.checkInterval"));

        boolean check = true;
        if(entry.getValue().stream().anyMatch(a -> (a.getParameterValue(ACTION_OBSERVER_ADD_DATE) != null
                && (new Date().getTime() - ((Date)a.getParameterValue(ACTION_OBSERVER_ADD_DATE)).getTime()) < checkFirst.toMillis()))){
            check = false;
        }


        if(check && (shouldBeRemovedCheck > -1 && (System.currentTimeMillis() - shouldBeRemovedCheck) < checkInterval.toMillis())){
            check  = false;
        }

        if (check){
            logger.info("check if actions for node should be removed: " + entry.getKey());
            boolean mustBeRemoved = false;
            if (!nodeservice.exists(entry.getKey())) {
                logger.info(entry.getKey() + " was deleted. must be removed.");
                mustBeRemoved = true;
            }
            ContentReader reader = contentService.getReader(entry.getKey(), ContentModel.PROP_CONTENT);
            if(reader == null || reader.getSize() < 1){
                logger.info(entry.getKey() + " has no content. must be removed.");
                mustBeRemoved = true;
            }
            shouldBeRemovedCheck = System.currentTimeMillis();
            return mustBeRemoved;
        }


        return false;
    }

}
