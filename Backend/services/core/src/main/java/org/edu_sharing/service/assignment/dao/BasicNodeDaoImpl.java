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

import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Getter
abstract class BasicNodeDaoImpl implements BasicNodeDao {
    protected String nodeId;
    protected final LazyProvider<PropertyMapper> propertyMapper;
    protected final LazyProvider<RepositoryDao> repositoryDao = new LazyProvider<>(RepositoryDao::getHomeRepository);

    @Setter(onMethod_ = @Autowired)
    protected NodeService nodeService;

    public BasicNodeDaoImpl(String nodeId) {
        this(nodeId, Optional.empty());
    }

    public BasicNodeDaoImpl(String nodeId, Optional<org.edu_sharing.service.model.NodeRef> nodeRef) {
        this.nodeId = nodeId;
        propertyMapper = new LazyProvider<>(CheckedSupplier.wrap(() -> {
            validateExists();
            Map<String, Object> properties = AuthenticationUtil.runAsSystem(CheckedRunAsWork.wrap(() ->
                    nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), getNodeId())));
            return new PropertyMapper(properties);
        }), nodeRef.map(x -> new PropertyMapper(x.getProperties())).orElse(null));
    }

    public boolean exists() {
        return StringUtils.isNotBlank(nodeId) && nodeService.exists(nodeId);
    }

    protected void doDelete() {
        log.debug("Deleting {} {}", this.getClass().getSimpleName(), nodeId);
        nodeService.removeNode(nodeId, null, true);
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
