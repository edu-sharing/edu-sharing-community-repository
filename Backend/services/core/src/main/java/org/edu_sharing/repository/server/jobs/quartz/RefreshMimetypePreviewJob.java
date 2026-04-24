/**
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.edu_sharing.repository.server.jobs.quartz;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import jakarta.transaction.UserTransaction;
import org.alfresco.model.ContentModel;
import org.alfresco.model.RenditionModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.ActionObserver;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Base64;
import java.util.List;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "refresh the mimetype and preview for a given solr search filter")
public class RefreshMimetypePreviewJob extends AbstractJobMapAnnotationParams {
    protected Logger logger = Logger.getLogger(RefreshMimetypePreviewJob.class);
    @JobFieldDescription(description = "define a filter for files that will be refreshed for mimetype and preview", sampleValue = "{\"term\": {\"properties.cclom:format\":\"text/xml\"}}")
    private String filter;

    @JobFieldDescription(description = "if true only thumbail generation will be don's. mimetype fix will be skipped. default is false.")
    Boolean skipMimeTypeFix;

    @JobFieldDescription(description = "thumbnailservice checks if thumbnail already exists and skips process when exists. when cleanUpExistingThumbnail is true the thumbnail node with name imagepreview will be deleted. default is false.")
    Boolean cleanUpExistingThumbnail;


    @Autowired
    private BehaviourFilter policyBehaviourFilter;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private ContentService contentService;
    @Autowired
    private MimetypeService mimetypeService;
    @Autowired
    private LockService lockService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private ActionService actionService;
    @Autowired
    private org.edu_sharing.service.search.SearchService searchService;

    @Override
    public void executeInternal(JobExecutionContext context) throws JobExecutionException {
        AuthenticationUtil.runAsSystem(() -> {
            doWork(context);
            return null;
        });
    }

    private void doWork(JobExecutionContext context) {


        try {
            logger.info("using filter:" + filter);

            if (StringUtils.isBlank(filter)) {
                logger.error("no filter defined. will stop processing!");
                return;
            }

            if (skipMimeTypeFix == null) skipMimeTypeFix = false;
            logger.info("using skipMimeTypeFix:" + skipMimeTypeFix);

            if (cleanUpExistingThumbnail == null) cleanUpExistingThumbnail = false;
            logger.info("using cleanUpExistingThumbnail:" + cleanUpExistingThumbnail);


            SearchToken searchToken = new SearchToken();
            searchToken.setElasticQuery(QueryBuilders.wrapper().query(new String(Base64.getEncoder().encode(filter.getBytes()))).build());
            searchToken.setFrom(0);
            searchToken.setMaxResult(Integer.MAX_VALUE);
            SearchResultNodeRef search = searchService.search(searchToken);
            logger.info("found:" + search.getNodeCount());
            search.getData().forEach(n -> {
                NodeRef nodeRef = new NodeRef(new StoreRef(n.getStoreProtocol(), n.getStoreId()), n.getNodeId());
                QName typeQName = nodeService.getType(nodeRef);
                String type = typeQName.getLocalName();
                String name = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);

                logger.info("name:" + name + " type:" + type);

                if (typeQName.equals(QName.createQName(CCConstants.CCM_TYPE_IO))) {

                    if (nodeService.getAspects(nodeRef).contains(QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))) {
                        logger.warn("ignoring collection_io_reference:" + nodeRef);
                        return;
                    }

                    UserTransaction nonPropagatingUserTransaction = transactionService.getNonPropagatingUserTransaction();
                    try {
                        nonPropagatingUserTransaction.begin();
                        try {
                            policyBehaviourFilter.disableBehaviour(nodeRef);
                            if (skipMimeTypeFix) triggerThumbnailAction(nodeRef);
                            else refreshMimetype(nodeRef, name);
                        } finally {
                            policyBehaviourFilter.enableBehaviour(nodeRef);
                        }
                        nonPropagatingUserTransaction.commit();
                    } catch (Throwable e) {
                        logger.error(e.getMessage(), e);
                        try {
                            nonPropagatingUserTransaction.rollback();
                        } catch (Exception e1) {
                            logger.error("error rolling back transaction", e1);
                        }
                    }

                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void refreshMimetype(NodeRef nodeRef, String name) {
        ContentReader contentReader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        if (contentReader == null || !contentReader.exists()) {
            logger.warn("no content found:" + nodeRef);
            return;
        }

        String oldMimeType = contentReader.getMimetype();

        //set to null so that guessMimetype() will not use old one
        contentReader.setEncoding(null);
        contentReader.setMimetype(null);

        String newMimetype = mimetypeService.guessMimetype(name, contentReader);
        logger.info("oldMimeType:" + oldMimeType + " newMimeType:" + newMimetype);
        try {

            lockService.lock(nodeRef, LockType.WRITE_LOCK);

            ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);

            //call preview action cause call in policy only works for new content
            writer.addListener(() -> {
                logger.info("finished setting new mimetype");
                triggerThumbnailAction(nodeRef);
                logger.info("finished setting new preview");
            });
            writer.setEncoding("UTF-8");
            writer.setMimetype(newMimetype);
            writer.putContent(contentService.getReader(nodeRef, ContentModel.PROP_CONTENT).getContentInputStream());
            nodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_FORMAT), newMimetype);

        } finally {
            lockService.unlock(nodeRef);
        }
    }

    private void triggerThumbnailAction(NodeRef nodeRef) {

        if (this.cleanUpExistingThumbnail) {
            List<ChildAssociationRef> imgpreview = this.nodeService
                    .getChildAssocs(nodeRef, RenditionModel.ASSOC_RENDITION, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "imgpreview"));
            if (imgpreview != null) {
                if (imgpreview.isEmpty()) {
                    logger.error("no preview found to delete.");
                } else if (imgpreview.size() > 1) {
                    logger.error("number of previews > 1. don't know which one to delete");
                } else {
                    NodeRef thumbnail = imgpreview.get(0).getChildRef();
                    logger.info("remove generated thumbnail: " + thumbnail + " from io:" + nodeRef);
                    new MCAlfrescoAPIClient().removeNode(thumbnail.getId(), null, false);
                }
            }
        }

        Action thumbnailAction = actionService.createAction(CCConstants.ACTION_NAME_CREATE_THUMBNAIL);
        thumbnailAction.setTrackStatus(true);

        thumbnailAction.setParameterValue("thumbnail-name", CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png);

        //cause its already async set executeAsynchronously to false
        actionService.executeAction(thumbnailAction, nodeRef, true, false);

        if (ActionStatus.Completed.equals(thumbnailAction.getExecutionStatus())) {
            logger.info("action was successfull. trigger io update for elastic tracker");
            String name = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
            nodeService.setProperty(nodeRef, ContentModel.PROP_NAME, name);
        } else {
            logger.error("action status." + thumbnailAction.getExecutionStatus() + " will not trigger update on io. tracker will not retrack preview");
        }
    }
}
