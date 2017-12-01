package org.edu_sharing.repository.server.tools;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;


/**
 * remembers the Actions that are called on nodes
 * when the action List for a node is requested all actions that are ready will be removed
 * 
 * is a singelton
 * 
 * Attention! only use with actions that got TrackStatus=true
 * 
 * @author rudi
 *
 */
public class ActionObserver {

	
	private static ActionObserver instance = null;
	
	Logger logger = Logger.getLogger(ActionObserver.class);
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	NodeService nodeservice  = serviceRegistry.getNodeService();
	
	
	public static String ACTION_OBSERVER_ADD_DATE = "action-observer-add-date";
	
	public static int ACTION_OBSERVER_TIMEOUT_HOURS = 5;
	
	private ActionObserver(){
		
	}
	
	public HashMap<NodeRef,List<Action>> nodeActionsMap = new HashMap<NodeRef,List<Action>>();
	
	/**
	 * calls removeInactiveActions
	 * 
	 * only actions that got TrackStatus=true a observed here
	 * 
	 * @param nodeRef
	 * @param action
	 */
	public void addAction(NodeRef nodeRef, Action action){
		removeInactiveActions();
		
		if(action.getTrackStatus() == false){
			logger.error("action.getTrackStatus() = false, will return");
			return;
		}
		
		List<Action> actions = nodeActionsMap.get(nodeRef);
		if(actions == null){
			synchronized(this){
				actions = new ArrayList<Action>();
				nodeActionsMap.put(nodeRef, actions);
			}
		}
		actions.add(action);		
	}
	
	/**
	 * returns the first action it founds for the nodeRef with ActionDefinitionName @param actionName
	 * 
	 * @param nodeRef
	 * @param actionName
	 * @return
	 */
	public Action getAction(NodeRef nodeRef, String actionName){
		List<Action> actions =  nodeActionsMap.get(nodeRef);
		if(actions == null){
			return null;
		}
		
		for(Action action : actions){
			if(action.getActionDefinitionName().equals(actionName)){
				return action;
			}
		}
		return null;
	}
	
	public synchronized void removeInactiveActions(){
		
		RunAsWork<Void> runAs = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				ArrayList<NodeRef> toRemove = new ArrayList<NodeRef>();
				
				
				for(Map.Entry<NodeRef, List<Action>> entry : nodeActionsMap.entrySet()){
					
					
					
					if(!nodeservice.exists(entry.getKey())) {
						logger.info(entry.getKey() + " was deleted will remove entry");
						toRemove.add(entry.getKey());
					}
					
					if(entry.getValue() != null && entry.getValue().size() > 0){
						
						List<Action> actions = entry.getValue();
						List<Action> toRemoveActions = new ArrayList<Action>();
						
						
						
						for(Action action : actions){
							Date addDate = (Date)action.getParameterValue(ACTION_OBSERVER_ADD_DATE);
							boolean actionTimedOut = false;
							if(addDate != null) {
								long hours = TimeUnit.HOURS.convert(new Date().getTime() - addDate.getTime(), TimeUnit.MILLISECONDS);
								if(hours > ACTION_OBSERVER_TIMEOUT_HOURS) {
									actionTimedOut = true;
									logger.info("action timed out");
								}
							}
							if(action != null && 
								   (action.getExecutionStatus().equals(ActionStatus.Cancelled) ||
									action.getExecutionStatus().equals(ActionStatus.Completed) ||
									action.getExecutionStatus().equals(ActionStatus.Failed)) ||
								    actionTimedOut ){
								
									logger.info("will remove inactive action "+action.getActionDefinitionName()+" with status" + action.getExecutionStatus() + " for "+entry.getKey());
									toRemoveActions.add(action);
							}
						}
						for(Action action:toRemoveActions){
							actions.remove(action);
						}
					}else{
						toRemove.add(entry.getKey());
					}
				}
				for(NodeRef nodeRef:toRemove){
					nodeActionsMap.remove(nodeRef);
				}
				return null;
			}
			
		};
		
		AuthenticationUtil.runAsSystem(runAs);
		
	}
	
	public static synchronized ActionObserver getInstance(){
		if(instance == null){
			instance = new  ActionObserver();
		}
		return instance;
	}
	
	public HashMap<NodeRef, List<Action>> getNodeActionsMap() {
		return nodeActionsMap;
	}
}
