package org.edu_sharing.alfresco.fixes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import lombok.Setter;
import org.alfresco.repo.domain.qname.QNameDAO;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationEvent;
import org.springframework.extensions.surf.util.AbstractLifecycleBean;

/**
 * Ensures that all types and aspects of the edu-sharing models are present in alf_qname.
 *
 * Alfresco only creates a qname row when it is first used by a node. On an empty repository the
 * solr tracking api (/api/solr/nodes) then silently drops unknown qnames from includeNodeTypes /
 * excludeAspects filters and renders an empty "in ()" clause, which fails with a SQL syntax error.
 */
@Setter
public class ModelQNameBootstrap extends AbstractLifecycleBean {

	private static final Set<String> NAMESPACES = Set.of(CCConstants.NAMESPACE_CCM, CCConstants.NAMESPACE_LOM);

	private QNameDAO qnameDAO;
	private DictionaryService dictionaryService;
	private TransactionService transactionService;

	@Override
	protected void onBootstrap(ApplicationEvent event) {
		if (transactionService.isReadOnly()) {
			log.warn("Repository is read-only, skipping registration of edu-sharing model qnames");
			return;
		}
		List<QName> qnames = Stream.concat(dictionaryService.getAllTypes().stream(), dictionaryService.getAllAspects().stream())
				.filter(qname -> NAMESPACES.contains(qname.getNamespaceURI()))
				.toList();
		AuthenticationUtil.runAsSystem(() ->
				transactionService.getRetryingTransactionHelper().doInTransaction(() -> {
					List<QName> created = new ArrayList<>();
					for (QName qname : qnames) {
						if (qnameDAO.getQName(qname) == null) {
							qnameDAO.getOrCreateQName(qname);
							created.add(qname);
						}
					}
					if (!created.isEmpty()) {
						log.info("Registered " + created.size() + " missing edu-sharing model qnames: " + created);
					}
					return null;
				}, false, true)
		);
	}

	@Override
	protected void onShutdown(ApplicationEvent event) {
	}

}
