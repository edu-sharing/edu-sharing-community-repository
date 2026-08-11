package org.edu_sharing.service.assignment.dao;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.service.assignment.BasicNodeDao;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.util.PropertyMapper;
import org.edu_sharing.util.CheckedRunAsWork;
import org.edu_sharing.util.CheckedSupplier;
import org.edu_sharing.util.LazyProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Slf4j
@Getter
abstract class BasicNodeDaoImpl implements BasicNodeDao {
    private final Set<LazyProvider<?>> lazyProviders = new HashSet<>();

    protected String nodeId;
    protected final LazyProvider<PropertyMapper> propertyMapper;
    protected final LazyProvider<RepositoryDao> repositoryDao = registerLazyProvider(new LazyProvider<>(RepositoryDao::getHomeRepository));

    @Setter(onMethod_ = @Autowired)
    protected NodeService nodeService;


    public BasicNodeDaoImpl(String nodeId) {
        this.nodeId = nodeId;
        propertyMapper = registerLazyProvider(new LazyProvider<>(CheckedSupplier.wrap(() -> {
            validateExists();
            Map<String, Object> properties = AuthenticationUtil.runAsSystem(CheckedRunAsWork.wrap(() ->
                    nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), getNodeId())));
            return new PropertyMapper(properties);
        })));
    }

    public BasicNodeDaoImpl(org.edu_sharing.service.model.NodeRef nodeRef) {
        this.nodeId = nodeRef.getNodeId();
        propertyMapper = registerLazyProvider(new LazyProvider<>(CheckedSupplier.wrap(() -> {
            validateExists();
            return new PropertyMapper(nodeRef.getProperties());
        })));
    }

    public boolean exists() {
        return StringUtils.isNotBlank(nodeId) && nodeService.exists(nodeId);
    }

    protected void doDelete() {
        log.debug("Deleting {} {}", this.getClass().getSimpleName(), nodeId);
        nodeService.removeNode(nodeId, null, true);
    }

    /**
     * Permanently removes the node, bypassing the recycle bin as well as the ACL checks that
     * {@link org.edu_sharing.service.nodeservice.NodeService#removeNode(String, String, boolean)}
     * enforces recursively on descendant nodes.
     */
    protected void doDeletePermanently() {
        log.debug("Permanently deleting {} {}", this.getClass().getSimpleName(), nodeId);
        nodeService.removeNodeForce(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, false);
    }

    @NotNull
    protected <T> LazyProvider<T> registerLazyProvider(@NotNull LazyProvider<T> lazyProvider) {
        Objects.requireNonNull(lazyProvider, "lazyProviders cannot be null");
        lazyProviders.add(lazyProvider);
        return lazyProvider;
    }

    @Override
    public void refresh() {
        log.debug("Refreshing {} {}", this.getClass().getSimpleName(),  nodeId);
        invalidateLazyProviders();
    }

    protected void invalidateLazyProviders() {
        for (LazyProvider<?> lazyProvider : lazyProviders) {
            lazyProvider.invalidate();
        }
    }

    @Override
    public String getCreator() {
        return propertyMapper.get().getString(CCConstants.CM_PROP_C_CREATOR);
    }

    @Override
    public Date getModifiedDate() {
        return propertyMapper.get().getDate(CCConstants.CM_PROP_C_MODIFIED);
    }

    @Override
    public Date getCreateDate() {
        return propertyMapper.get().getDate(CCConstants.CM_PROP_C_CREATED);
    }

    protected void validateExists() {
        if (!exists()) {
            throw new IllegalArgumentException("Node with id " + nodeId + " does not exist.");
        }
    }

    @NotNull
    @Override
    public NodeRef getNodeRef() {
        return new NodeRef(repositoryDao.get(), nodeId);
    }

    @Override
    public org.alfresco.service.cmr.repository.NodeRef getAlfrescoNodeRef() {
        return new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
    }
}
