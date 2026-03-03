package org.edu_sharing.spring.scheduler;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ActionObserver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class PreviewJob {

    @Value("${repository.transformer.preview.maxRunning}")
    int pollSize = 3;

    @Getter
    @Value("${repository.transformer.preview.delay}")
    int delay;


    ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext()
            .getBean(ServiceRegistry.SERVICE_REGISTRY);
    ActionService actionService = serviceRegistry.getActionService();
    NodeService nodeService = (NodeService) AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");


    @Scheduled(fixedDelayString = "#{@previewJob.delay}")
    public void run(){
        AuthenticationUtil.runAsSystem(() -> {executeActions();return null;});
    }

    public void executeActions(){
        ActionObserver actionObserver = ActionObserver.getInstance();
        for(int i = 0; i < pollSize; i++){
            NodeRef nodeRef = actionObserver.pollNewAction();
            if(nodeRef == null){
                return;
            }

            try {
                String creator = (String) nodeService
                        .getProperty(nodeRef, ContentModel.PROP_CREATOR);

                log.info("executing preview task: {}",nodeRef);
                AuthenticationUtil.runAs(() -> serviceRegistry.getRetryingTransactionHelper().doInTransaction(()->{
                    serviceRegistry.getRenditionService2().render(nodeRef, "imgpreview");
                    return null;
                },false)
                        , creator);
            } catch(InvalidNodeRefException e){
                log.info("preview task for {} failed: invalid nodeRef.",nodeRef);
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }
        }
    }
}
