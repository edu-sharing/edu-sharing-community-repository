package org.edu_sharing.service.quota;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.appcontext.AppContextServiceLocator;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceElastic;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Helper to resolve the per person count quotas (max. number of owned materials / collections / folders)
 */
public class NodeCountQuotaHelper {

    private NodeCountQuotaHelper() {
    }

    private static ServiceRegistry getServiceRegistry() {
        return (ServiceRegistry) AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
    }

    /**
     * @return the limit of the given type from the given person properties, or null if no (valid) limit is set
     */
    public static Long getLimit(Map<String, ? extends Serializable> personProperties, NodeCountQuotaType type) {
        return toLimit(personProperties.get(type.getPersonProperty()));
    }

    /**
     * @return all limits which are set for the given user (empty if the user has no count quota at all)
     */
    public static Map<NodeCountQuotaType, Long> getLimits(String userName) {
        return AuthenticationUtil.runAsSystem(() -> {
            Map<NodeCountQuotaType, Long> result = new EnumMap<>(NodeCountQuotaType.class);
            ServiceRegistry serviceRegistry = getServiceRegistry();
            NodeRef person = serviceRegistry.getPersonService().getPersonOrNull(userName);
            if (person == null) {
                return result;
            }
            for (NodeCountQuotaType type : NodeCountQuotaType.values()) {
                Long limit = toLimit(serviceRegistry.getNodeService().getProperty(person, QName.createQName(type.getPersonProperty())));
                if (limit != null) {
                    result.put(type, limit);
                }
            }
            return result;
        });
    }

    /**
     * @return the number of (indexed) nodes of the given type owned by the user, or null if no elastic search is available
     */
    public static Long countOwnedNodes(String userName, NodeCountQuotaType type) {
        SearchService searchService = AppContextServiceLocator.getInstance().get(SearchService.class);
        if (!(searchService instanceof SearchServiceElastic searchServiceElastic)) {
            return null;
        }
        return AuthenticationUtil.runAsSystem(() -> searchServiceElastic.countOwnedNodes(userName, type.getContentType(), type.getAdditionalFilters()));
    }

    private static Long toLimit(Object value) {
        try {
            Long limit = DefaultTypeConverter.INSTANCE.convert(Long.class, value);
            return limit != null && limit > 0 ? limit : null;
        } catch (TypeConversionException e) {
            return null;
        }
    }
}
