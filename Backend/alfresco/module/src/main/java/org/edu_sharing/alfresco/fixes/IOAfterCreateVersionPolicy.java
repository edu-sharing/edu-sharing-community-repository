package org.edu_sharing.alfresco.fixes;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.version.VersionServicePolicies;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;

public class IOAfterCreateVersionPolicy implements VersionServicePolicies.AfterCreateVersionPolicy {

	
	private static Log logger = LogFactory.getLog(IOAfterCreateVersionPolicy.class);

	public static final QName CCM_TYPE_IO = QName.createQName(CCConstants.CCM_TYPE_IO);
	
	public static final QName QNAME = QName.createQName(NamespaceService.ALFRESCO_URI, "afterCreateVersion");
	
	
	/**
	 * The common node service
	 */
	protected NodeService nodeService;

	/**
	 * Policy component
	 */
	protected PolicyComponent policyComponent;
	private BehaviourFilter policyBehaviourFilter;

	/**
	 * Spring bean init method
	 */
	public void init() {
		logger.info("called!");
		policyComponent.bindClassBehaviour(QNAME, CCM_TYPE_IO, new JavaBehaviour(this, "afterCreateVersion"));
	}
	
	@Override
	public void afterCreateVersion(NodeRef versionableNode, Version version) {
		/*if(!policyBehaviourFilter.isEnabled(versionableNode)){
			logger.info("policy filter is currently disabled, do not alter version for node " + versionableNode);
			return;
		}*/
		/**
		 * usage looks on LOM_PROP_LIFECYCLE_VERSION to find out what has to be rendered. 
		 * cause this version property is also set by an revert to the reverted Label of the reverted Version.
		 * 
		 * see org.edu_sharing.alfresco.fixes.Version2ServiceImpl.revert
		 */
		this.nodeService.setProperty(versionableNode, QName.createQName(CCConstants.LOM_PROP_LIFECYCLE_VERSION), version.getVersionLabel());
		
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public void setPolicyBehaviourFilter(BehaviourFilter policyBehaviourFilter) {
		this.policyBehaviourFilter = policyBehaviourFilter;
	}
}
