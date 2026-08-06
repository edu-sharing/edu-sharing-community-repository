package org.edu_sharing.restservices.shared;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.action.RessourceInfoExecuter;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.connector.ConnectorFileType;
import org.edu_sharing.alfresco.service.connector.SimpleConnector;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.connector.ConnectorServiceFactory;
import org.edu_sharing.service.connector.SimpleConnectorAttributes;
import org.edu_sharing.service.lti13.LTIJWTUtil;
import org.edu_sharing.service.mime.MimeTypesV2;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
public class NodeUrls {

    private String repositoryBaseUrl;
    private String generateLtiResourceLink;
    private String connectorRenderUrl;

    public NodeUrls(String nodeId, List<String> aspects, String requestedVersion) {
        this(nodeId, aspects, requestedVersion, null, null);
    }

    public NodeUrls(String nodeId, List<String> aspects, String requestedVersion, Map<String, Object> properties, String nodeType) {
        repositoryBaseUrl = URLHelper.getBaseUrl(true);
        if(aspects.contains(CCConstants.CCM_ASPECT_LTITOOL_NODE)){
            generateLtiResourceLink = repositoryBaseUrl + "/rest/ltiplatform/v13/generateLoginInitiationFormResourceLink?nodeId=" + nodeId;
            if(Context.getCurrentInstance() != null){
                if(Context.getCurrentInstance().isSingleUseNodeId(nodeId)){
                    //generate short living jwt
                    // @TODO will this be supported with Ticket access of RS2?
                    try {
                        String jwt = LTIJWTUtil.getShortAccessJwt(nodeId,60);
                        generateLtiResourceLink +="&jwt="+jwt;
                    } catch (GeneralSecurityException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
            if(requestedVersion != null && !requestedVersion.equals("-1")){
                generateLtiResourceLink +="&version="+requestedVersion;
            }
        }
        connectorRenderUrl = getConnectorRenderUrl(nodeId, properties, nodeType);
    }

    /**
     * the "renderUrl" of the simple connector the given element was created with or which supports its mimetype
     */
    private static String getConnectorRenderUrl(String nodeId, Map<String, Object> properties, String nodeType) {
        if(properties == null) {
            return null;
        }
        List<SimpleConnector> simpleConnectors = ConnectorServiceFactory.getConnectorList().getSimpleConnectors();
        if(simpleConnectors == null) {
            return null;
        }
        return simpleConnectors.stream()
                .filter(connector -> StringUtils.isNotEmpty(connector.getRenderUrl()))
                .filter(connector -> supportsNode(connector, properties, nodeType))
                .findAny()
                .map(connector -> SimpleConnectorAttributes.replaceForUrl(
                        Map.of("nodeId", new String[]{nodeId}), connector.getRenderUrl()))
                .orElse(null);
    }

    /**
     * whether the element was created with the given connector (ccm:ccressourcesubtype)
     * or one of the connectors filetypes matches the mimetype of the element
     * (same logic as RestConnectorsService.connectorSupportsEdit / getFiletype in the frontend)
     */
    private static boolean supportsNode(SimpleConnector connector, Map<String, Object> properties, String nodeType) {
        if(RessourceInfoExecuter.CCM_RESSOURCETYPE_CONNECTOR.equals(properties.get(CCConstants.CCM_PROP_CCRESSOURCETYPE))
                && connector.getId().equals(properties.get(CCConstants.CCM_PROP_CCRESSOURCESUBTYPE))) {
            return true;
        }
        return getMatchingFiletype(connector, properties, nodeType) != null;
    }

    private static ConnectorFileType getMatchingFiletype(SimpleConnector connector, Map<String, Object> properties, String nodeType) {
        if(connector.getFiletypes() == null) {
            return null;
        }
        String mimetype = MimeTypesV2.getMimeType(properties, nodeType);
        if(mimetype == null) {
            return null;
        }
        return connector.getFiletypes().stream()
                .filter(filetype -> mimetype.equals(filetype.getMimetype()))
                .filter(filetype -> matchesEditorType(filetype, properties))
                .filter(filetype -> matchesCompressedRessourceType(filetype, properties, mimetype))
                .findAny()
                .orElse(null);
    }

    private static boolean matchesEditorType(ConnectorFileType filetype, Map<String, Object> properties) {
        return StringUtils.isEmpty(filetype.getEditorType())
                || filetype.getEditorType().equals(properties.get(CCConstants.CCM_PROP_EDITOR_TYPE));
    }

    /**
     * zip based elements (like h5p or scorm) are only supported when their ressourcetype matches as well
     */
    private static boolean matchesCompressedRessourceType(ConnectorFileType filetype, Map<String, Object> properties, String mimetype) {
        if(!"application/zip".equals(mimetype)) {
            return true;
        }
        return Objects.equals(filetype.getCcressourcetype(), properties.get(CCConstants.CCM_PROP_CCRESSOURCETYPE))
                && (StringUtils.isEmpty(filetype.getCcressourceversion())
                    || filetype.getCcressourceversion().equals(properties.get(CCConstants.CCM_PROP_CCRESSOURCEVERSION)))
                && (StringUtils.isEmpty(filetype.getCcresourcesubtype())
                    || filetype.getCcresourcesubtype().equals(properties.get(CCConstants.CCM_PROP_CCRESSOURCESUBTYPE)));
    }

}
