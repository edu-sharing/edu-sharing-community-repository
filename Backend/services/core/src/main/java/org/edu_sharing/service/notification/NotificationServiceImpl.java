package org.edu_sharing.service.notification;

import java.util.*;
import java.util.stream.Collectors;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.*;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.restservices.mds.v1.model.MdsValue;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.rating.RatingDetails;

public class NotificationServiceImpl implements NotificationService {

    private final Logger logger = Logger.getLogger(NotificationServiceImpl.class);

    private final ApplicationInfo appInfo;


    public NotificationServiceImpl(String appId) {
        appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);
    }

    @Override
    public void notifyNodeIssue(String nodeId, String reason, Map<String, Object> properties, String userEmail, String userComment) throws Throwable {
        logger.info(String.format("send notifyNodeIssue: nodeId: %s, reason: %s, userComment: %s", nodeId, reason, userComment));

        Map<String, String> replace = new HashMap<>();
        replace.put("reporterEmail", userEmail.trim());
        replace.put("userComment", userComment);
        replace.put("reason", reason);
        replace.put("name", (String) properties.get(CCConstants.CM_NAME));
        replace.put("id", nodeId);
        replace.put("link", URLTool.getNgRenderNodeUrl(nodeId, null, true));
        replace.put("link.static", URLTool.getNgRenderNodeUrl(nodeId, null, false));
        MailTemplate.applyNodePropertiesToMap("nodeId.", properties, replace);
        MailTemplate.sendMail("nodeIssue", replace);
    }

    @Override
    public void notifyWorkflowChanged(String nodeId, Map<String, Object> nodeProperties, String receiver, String comment, String status) {
        logger.info(String.format("send notifyWorkflowChanged: nodeId: %s, nodePropertiesList: %s, comment: %s, status: %s", nodeId, nodeProperties, comment, status));

        MailTemplate.UserMail receiverMail = MailTemplate.getUserMailData(receiver);
        EmailValidator mailValidator = EmailValidator.getInstance(true, true);
        if (mailValidator.isValid(receiverMail.getEmail())) {
            try {
                MailTemplate.UserMail sender = MailTemplate.getUserMailData(new AuthenticationToolAPI().getCurrentUser());
                Map<String, String> replace = new HashMap<>();
                sender.applyToMap("assigner.", replace);
                replace.put("comment", comment);
                MailTemplate.addContentLinks(ApplicationInfoList.getHomeRepository(), nodeId, replace, "link");
                replace.put("status", I18nAngular.getTranslationAngular("common", "WORKFLOW." + status));
                receiverMail.applyToMap("", replace);
                MailTemplate.applyNodePropertiesToMap("nodeId.", nodeProperties, replace);

                String template = "invited_workflow";
                MailTemplate.sendMail(sender.getFullName(), sender.getEmail(), receiver, template, replace);
            } catch (Throwable t) {
                logger.warn("Mail send failed", t);
            }
        }
    }

    @Override
    public void notifyPersonStatusChanged(String receiver, String firstname, String lastName, String oldStatus, String newStatus) {
        Map<String, String> replace = new HashMap<>();
        replace.put("firstName", firstname);
        replace.put("lastName", lastName);
        replace.put("oldStatus", I18nAngular.getTranslationAngular("permissions", "PERMISSIONS.USER_STATUS." + oldStatus));
        replace.put("newStatus", I18nAngular.getTranslationAngular("permissions", "PERMISSIONS.USER_STATUS." + newStatus));
        try {
            String template = "userStatusChanged";
            MailTemplate.sendMail(receiver, template, replace);
        } catch (Exception e) {
            logger.warn("Can not send status notify mail to user: " + e.getMessage(), e);
        }
    }

    @Override
    public void notifyPermissionChanged(String senderAuthority, String receiverAuthority, String nodeId, Map<String, Object> props, String[] aspects, String[] permissions, String mailText) throws Throwable {
        MailTemplate.UserMail sender = MailTemplate.getUserMailData(senderAuthority);
        MailTemplate.UserMail receiver = MailTemplate.getUserMailData(receiverAuthority);
        EmailValidator mailValidator = EmailValidator.getInstance(true, true);

        if (!mailValidator.isValid(receiver.getEmail())) {
            logger.info("username/receiverAuthority: " + receiverAuthority + " has no valid emailaddress:" + receiver.getEmail());
            return;
        }

        // if the receiver is the creator itself, skip it (because it is automatically added)
        String nodeCreator = (String) props.get(CCConstants.CM_PROP_C_CREATOR);
        if (receiverAuthority.equals(nodeCreator)) {
            return;
        }

        String currentLocale = new AuthenticationToolAPI().getCurrentLocale();

        // used for sending copy to user
        String nodeType = (String) props.get(CCConstants.NODETYPE);

        String name = nodeType.equals(CCConstants.CCM_TYPE_IO)
                ? (String) props.get(CCConstants.LOM_PROP_GENERAL_TITLE)
                : (String) props.get(CCConstants.CM_PROP_C_TITLE);

        if (StringUtils.isBlank(name)) {
            name = (String) props.get(CCConstants.CM_NAME);
        }

        String permText = Arrays.stream(permissions)
                .filter(perm -> !(CCConstants.CCM_VALUE_SCOPE_SAFE.equals(NodeServiceInterceptor.getEduSharingScope()) && Objects.equals(CCConstants.PERMISSION_CC_PUBLISH, perm)))
                .map(perm -> I18nServer.getTranslationDefaultResourcebundle(I18nServer.getPermissionDescription(perm), currentLocale))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("\n"));

        Map<String, String> replace = new HashMap<>();
        receiver.applyToMap("", replace);
        sender.applyToMap("inviter.", replace);
        MailTemplate.applyNodePropertiesToMap("nodeId.", props, replace);
        replace.put("name", name.trim());
        replace.put("message", mailText.replace("\n", "<br />").trim());
        replace.put("permissions", permText.trim());
        MailTemplate.addContentLinks(appInfo, nodeId, replace, "link");

        String template = "invited";
        if (CCConstants.CCM_VALUE_SCOPE_SAFE.equals(NodeServiceInterceptor.getEduSharingScope())) {
            template = "invited_safe";
        } else if (nodeType.equals(CCConstants.CCM_TYPE_MAP) && Arrays.asList(aspects).contains(CCConstants.CCM_ASPECT_COLLECTION)) {
            template = "invited_collection";
        }

        MailTemplate.sendMail(sender.getFullName(), sender.getEmail(), receiver.getEmail(), template, replace);
    }

    public void notifyMetadataSetSuggestion(MdsValue mdsValue, MetadataWidget widgetDefinition, List<String> nodeId, List<Map<String, Object>> nodePropertiesList) throws Throwable {
        String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
        Map<String, String> replace = new HashMap<>();
        if (currentUser != null) {
            NodeRef userRef = AuthorityServiceHelper.getAuthorityNodeRef(currentUser);
            if (userRef != null) {
                replace.put("firstName", NodeServiceHelper.getProperty(userRef, CCConstants.CM_PROP_PERSON_FIRSTNAME));
                replace.put("lastName", NodeServiceHelper.getProperty(userRef, CCConstants.CM_PROP_PERSON_LASTNAME));
            }
        }
        replace.put("widgetId", widgetDefinition.getId());
        replace.put("widgetCaption", widgetDefinition.getCaption());
        replace.put("caption", mdsValue.getCaption());
        replace.put("id", mdsValue.getId());
        replace.put("parentId", mdsValue.getParent());
        replace.put("parentCaption", mdsValue.getParent() == null ? null : widgetDefinition.getValuesAsMap().get(mdsValue.getParent()).getCaption());
        if(!nodeId.isEmpty()) {
            MailTemplate.applyNodePropertiesToMap("nodeId.", nodePropertiesList.get(0), replace);
            MailTemplate.addContentLinks(ApplicationInfoList.getHomeRepository(), nodeId.get(0), replace, "link");
        }
        String[] receivers = widgetDefinition.getSuggestionReceiver().split(",");
        for (String receiver : receivers) {
            MailTemplate.sendMail(receiver, "mdsValuespaceSuggestion", replace);
        }
    }

    @Override
    public void notifyComment(String node, String comment, String commentReference, Map<String, Object> nodeProperties, Status status) {

    }

    @Override
    public void notifyCollection(String collectionId, String refNodeId, Map<String, Object> collectionProperties, Map<String, Object> nodeProperties, Status status) {

    }

    @Override
    public void notifyRatingChanged(String nodeId, Map<String, Object> nodeProps, Double rating, RatingDetails accumulatedRatings, Status removed) {

    }

}
