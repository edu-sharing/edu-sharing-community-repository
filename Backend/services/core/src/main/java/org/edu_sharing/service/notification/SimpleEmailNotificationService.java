package org.edu_sharing.service.notification;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.notification.events.*;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchResult;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "repository.mail.simpleMailservice", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SimpleEmailNotificationService {

    private final Optional<NotificationService.NodeIssueMapping> customNodeIssueMapping;
    private final AuthenticationToolAPI authTool;


    @PostConstruct
    public void init() {
        log.info("SimpleEmailNotificationService initialized");
    }

    @EventListener
    public void onNodeIssueEvent(NodeIssueEvent event) throws Exception {
        log.info("send notifyNodeIssue: nodeId: {}, reason: {}, userComment: {}", event.nodeId(), event.reason(), event.userComment());
        String currentLocale = authTool.getCurrentLocale();
        NotificationService.NodeContext nodeContext = new NotificationService.NodeContext(event.nodeId(), event.aspects(), event.nodeProperties());
        String defaultTemplate = "nodeIssue";
        if(NotificationService.NotifyMode.Feedback.equals(event.mode())) {
            defaultTemplate = "nodeIssueFeedback";
        }
        String templateId = (customNodeIssueMapping.isPresent()) ? customNodeIssueMapping.get().getTemplateId(nodeContext) : defaultTemplate;
        String subject= MailTemplate.getSubject(templateId, currentLocale);
        String content=MailTemplate.getContent(templateId, currentLocale, true);
        Map<String, String> replace = new HashMap<>();
        replace.put("reporterEmail", event.userEmail().trim());
        replace.put("userComment", event.userComment());
        replace.put("reason", event.reason());
        replace.put("name", (String) event.nodeProperties().get(CCConstants.CM_NAME));
        replace.put("id", event.nodeId());
        replace.put("link", URLHelper.getNgRenderNodeUrl(event.nodeId(), null, true));
        replace.put("link.static", URLHelper.getNgRenderNodeUrl(event.nodeId(), null, false));
        MailTemplate.applyNodePropertiesToMap("node.", event.nodeProperties(), replace);

        if(Context.getCurrentInstance() != null && Context.getCurrentInstance().getRequest() != null) {
            // add request headers to evaluate in template (i.e. user-agent)
            replace.putAll(
                    Collections.list(Context.getCurrentInstance().getRequest().getHeaderNames()).stream().collect(
                            HashMap::new,
                            (m, entry) -> m.put("request." + entry, Context.getCurrentInstance().getRequest().getHeader(entry)),
                            HashMap::putAll
                    )
            );
        }
        try {
            AuthorityService authorityService = AuthorityServiceFactory.getInstance().getLocalService();
            Map<String, Object> userProps = NodeServiceHelper.getProperties(authorityService.getAuthorityNodeRef(AuthenticationUtil.getFullyAuthenticatedUser()));
            MailTemplate.applyNodePropertiesToMap("user.", userProps, replace);
            SearchService searchService = SearchServiceFactory.getInstance().getLocalService();
            SearchResult<EduGroup> orgList = searchService.getAllOrganizations(true);
            if(!orgList.getData().isEmpty()) {
                Map<String, Object> orgProps = NodeServiceHelper.getProperties(authorityService.getAuthorityNodeRef(orgList.getData().get(0).getGroupname()));
                MailTemplate.applyNodePropertiesToMap("user.organization.", orgProps, replace);
            }
            List<String> mzList = searchService.getAllMediacenters();
            if(!mzList.isEmpty()) {
                Map<String, Object> mzProps = NodeServiceHelper.getProperties(authorityService.getAuthorityNodeRef(mzList.get(0)));
                MailTemplate.applyNodePropertiesToMap("user.mediacenter.", mzProps, replace);
            }
        } catch (Throwable ignored) {

        }
        Mail mail=new Mail();
        List<String> receivers = null;
        if(customNodeIssueMapping.isPresent()) {
            receivers = customNodeIssueMapping.get().getReceivers(nodeContext);
        }
        if(receivers != null && !receivers.isEmpty()) {
            // receivers were provided by custom mapping
        } else if (mail.getConfig().hasPath("report.receivers")) {
            receivers = mail.getConfig().getStringList("report.receivers");
        } else if (mail.getConfig().getString("report.receiver") != null){
            receivers = Collections.singletonList(mail.getConfig().getString("report.receiver"));
            log.info("report.receiver is deprecated. Prefer using the report.receivers field instead");
        }
        if(receivers==null || receivers.isEmpty()) {
            throw new IllegalArgumentException("no mail.report.receivers registered in ccmail.properties");
        }
        ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
        for (String receiver : receivers) {
            mail.sendMailHtml(
                    context,
                    null,
                    event.userEmail(),
                    receiver,
                    subject, content, replace);
        }
    }

    @EventListener
    public void onWorkflowChanged(WorkflowChangedEvent event) {
        log.info("send notifyWorkflowChanged: nodeId: {}, nodePropertiesList: {}, comment: {}, status: {}", event.nodeId(), event.nodeProperties(), event.comment(), event.status());

        MailTemplate.UserMail receiverMail = MailTemplate.getUserMailData(event.receiver());
        EmailValidator mailValidator = EmailValidator.getInstance(true, true);
        if (mailValidator.isValid(receiverMail.getEmail())) {
            try {
                MailTemplate.UserMail sender = MailTemplate.getUserMailData(authTool.getCurrentUser());
                Map<String, String> replace = new HashMap<>();
                sender.applyToMap("assigner.", replace);
                replace.put("comment", event.comment());
                MailTemplate.addContentLinks(ApplicationInfoList.getHomeRepository(), event.nodeId(), replace, "link");
                replace.put("status", I18nAngular.getTranslationAngular("common", "WORKFLOW." + event.status()));
                receiverMail.applyToMap("", replace);
                MailTemplate.applyNodePropertiesToMap("node.", event.nodeProperties(), replace);

                String template = "invited_workflow";
                MailTemplate.sendMail(sender.getFullName(), sender.getEmail(), receiverMail.getEmail(), template, replace);
            } catch (Throwable t) {
                log.warn("Mail send failed", t);
            }
        }
    }

    @EventListener
    public void onAddedToInbox(AddedToInboxEvent event){
        log.info("send notifyAddedToInbox: nodeId: {}, nodePropertiesList: {}, comment: {}", event.nodeId(), event.properties(), event.comment());

        MailTemplate.UserMail receiverMail = MailTemplate.getUserMailData(event.receiverAuthority());
        EmailValidator mailValidator = EmailValidator.getInstance(true, true);
        if (mailValidator.isValid(receiverMail.getEmail())) {
            try {
                MailTemplate.UserMail sender = MailTemplate.getUserMailData(event.senderAuthority());
                Map<String, String> replace = new HashMap<>();
                sender.applyToMap("assigner.", replace);
                replace.put("comment", event.comment());
                MailTemplate.addContentLinks(ApplicationInfoList.getHomeRepository(), event.nodeId(), replace, "link");
                receiverMail.applyToMap("", replace);
                MailTemplate.applyNodePropertiesToMap("node.", event.properties(), replace);

                String template = "added_inbox";
                MailTemplate.sendMail(sender.getFullName(), sender.getEmail(), receiverMail.getEmail(), template, replace);
            } catch (Throwable t) {
                log.warn("Mail send failed", t);
            }
        }
    }

    @EventListener
    public void onPersonStatusChanged(PersonStatusChangedEvent event) {
        Map<String, String> replace = new HashMap<>();
        replace.put("firstName", event.firstname());
        replace.put("lastName", event.lastName());
        replace.put("oldStatus", I18nAngular.getTranslationAngular("permissions", "PERMISSIONS.USER_STATUS." + event.oldStatus()));
        replace.put("newStatus", I18nAngular.getTranslationAngular("permissions", "PERMISSIONS.USER_STATUS." + event.newStatus()));
        try {
            String template = "userStatusChanged";
            MailTemplate.sendMail(event.receiver(), template, replace);
        } catch (Exception e) {
            log.warn("Can not send status notify mail to user: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void onMetadataSetSuggestion(MetadataSetSuggestionEvent event) throws Throwable {
        String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
        Map<String, String> replace = new HashMap<>();
        if (currentUser != null) {
            NodeRef userRef = AuthorityServiceHelper.getAuthorityNodeRef(currentUser);
            if (userRef != null) {
                replace.put("firstName", NodeServiceHelper.getProperty(userRef, CCConstants.CM_PROP_PERSON_FIRSTNAME));
                replace.put("lastName", NodeServiceHelper.getProperty(userRef, CCConstants.CM_PROP_PERSON_LASTNAME));
            }
        }
        replace.put("widgetId", event.widgetDefinition().getId());
        replace.put("widgetCaption", event.widgetDefinition().getCaption());
        replace.put("caption", event.mdsValue().getCaption());
        replace.put("id", event.mdsValue().getId());
        replace.put("parentId", event.mdsValue().getParent());
        replace.put("parentCaption", event.mdsValue().getParent() == null ? null : event.widgetDefinition().getValuesAsMap().get(event.mdsValue().getParent()).getCaption());
        if(!event.nodeIds().isEmpty()) {
            MailTemplate.applyNodePropertiesToMap("node.", event.nodePropertiesList().get(0), replace);
            MailTemplate.addContentLinks(ApplicationInfoList.getHomeRepository(), event.nodeIds().get(0), replace, "link");
        }
        String[] receivers = event.widgetDefinition().getSuggestionReceiver().split(",");
        for (String receiver : receivers) {
            MailTemplate.sendMail(receiver, "mdsValuespaceSuggestion", replace);
        }
    }

    @EventListener
    public void onPermissionChanged(PermissionChangedEvent event) throws Throwable {
        MailTemplate.UserMail sender = MailTemplate.getUserMailData(event.senderAuthority());
        MailTemplate.UserMail receiver = MailTemplate.getUserMailData(event.receiverAuthority());
        EmailValidator mailValidator = EmailValidator.getInstance(true, true);

        if (!mailValidator.isValid(receiver.getEmail())) {
            log.info("username/receiver: {} has no valid emailaddress:{}", event.receiverAuthority(), receiver.getEmail());
            return;
        }

        // if the receiver is the creator itself, skip it (because it is automatically added)
        String nodeCreator = (String) event.nodeProperties().get(CCConstants.CM_PROP_C_CREATOR);
        if (event.receiverAuthority().equals(nodeCreator)) {
            return;
        }

        // used for sending copy to user
        String internalNodeType = (String) event.nodeProperties().get(CCConstants.NODETYPE);

        String name = internalNodeType.equals(CCConstants.CCM_TYPE_IO)
                ? (String) event.nodeProperties().get(CCConstants.LOM_PROP_GENERAL_TITLE)
                : (String) event.nodeProperties().get(CCConstants.CM_PROP_C_TITLE);

        if (StringUtils.isBlank(name)) {
            name = (String) event.nodeProperties().get(CCConstants.CM_NAME);
        }

        String permText = Arrays.stream(event.permissions())
                .filter(perm -> !(CCConstants.CCM_VALUE_SCOPE_SAFE.equals(NodeServiceInterceptor.getEduSharingScope()) && Objects.equals(CCConstants.PERMISSION_CC_PUBLISH, perm)))
                .map(I18nAngular::getPermissionDescription)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("\n"));

        Map<String, String> replace = new HashMap<>();
        receiver.applyToMap("", replace);
        sender.applyToMap("inviter.", replace);
        MailTemplate.applyNodePropertiesToMap("node.", event.nodeProperties(), replace);
        replace.put("name", name.trim());
        replace.put("message", event.mailText().replace("\n", "<br />").trim());
        replace.put("permissions", permText.trim());
        MailTemplate.addContentLinks(ApplicationInfoList.getHomeRepository(), event.nodeId(), replace, "link");

        String template = "invited";
        if (CCConstants.CCM_VALUE_SCOPE_SAFE.equals(NodeServiceInterceptor.getEduSharingScope())) {
            template = "invited_safe";
        } else if (internalNodeType.equals(CCConstants.CCM_TYPE_MAP) && event.aspects().contains(CCConstants.CCM_ASPECT_COLLECTION)) {
            template = "invited_collection";
        }

        MailTemplate.sendMail(sender.getFullName(), sender.getEmail(), receiver.getEmail(), template, replace);
    }
}
