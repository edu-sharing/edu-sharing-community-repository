package org.edu_sharing.alfresco.fixes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;

public class UsagePolicy implements NodeServicePolicies.BeforeDeleteNodePolicy {

	private static final Log logger = LogFactory.getLog(UsagePolicy.class);

	private static final Set<QName> type = new HashSet<QName>(
			Arrays.asList(new QName[] { QName
					.createQName(CCConstants.CCM_TYPE_USAGE) }));

	/**
	 * The common node service
	 */
	protected NodeService nodeService;

	/**
	 * Policy component
	 */
	protected PolicyComponent policyComponent;

	protected boolean protectContentWithUsage;

	/**
	 * Spring bean init method
	 */
	public void init() {

		logger.info("called!");

		policyComponent.bindClassBehaviour(BeforeDeleteNodePolicy.QNAME, QName
				.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this,
				"beforeDeleteNode"));

	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public void setProtectContentWithUsage(boolean protectContentWithUsage) {
		this.protectContentWithUsage = protectContentWithUsage;
	}

	@Override
	public void beforeDeleteNode(NodeRef ref) {

		if (protectContentWithUsage
				&& nodeService.getChildAssocs(ref, type).size() > 0) {

			throw new NodeLockedException(ref);

		}

	}

}
