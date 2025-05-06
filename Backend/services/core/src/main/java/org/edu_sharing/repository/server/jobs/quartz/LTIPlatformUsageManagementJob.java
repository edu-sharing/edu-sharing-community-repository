package org.edu_sharing.repository.server.jobs.quartz;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.alfresco.action.RessourceInfoExecuter;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.usage.Usage;
import org.edu_sharing.service.usage.Usage2Service;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@JobDescription(description = "cleanup/re-add usages of embedding nodes")
public class LTIPlatformUsageManagementJob extends AbstractJobMapAnnotationParams{

    List<String> validApplicationSubtypes = List.of(RessourceInfoExecuter.CCM_RESSOURCETYPE_GEOGEBRA, RessourceInfoExecuter.CCM_RESSOURCETYPE_SERLO);

    Usage2Service usage2Service = new Usage2Service();

    @JobFieldDescription(description = "ignore usages/embedding nodes younger than minimumAge (in hours)",sampleValue = "1")
    Integer minimumAge = 1;

    @JobFieldDescription(description = "weather to make persistent changes or to run in protocol mode")
    Boolean execute;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        AuthenticationUtil.runAsSystem(() -> {

            ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
            ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
            NodeService nodeService = serviceRegistry.getNodeService();

            List<ApplicationInfo> appInfos = ApplicationInfoList.getApplicationInfos().entrySet()
                    .stream()
                    .filter(e -> e.getValue().isLtiPlatform() && validApplicationSubtypes.contains(e.getValue().getSubtype()))
                    .map(e -> e.getValue())
                    .collect(Collectors.toList());

            for (ApplicationInfo appInfo : appInfos) {
                ZonedDateTime minimumAgeTime = ZonedDateTime.now(ZoneId.systemDefault()).minusHours(minimumAge);
                List<Usage> usages = usage2Service.getUsages(appInfo.getAppId(), null, Long.valueOf(minimumAgeTime.toInstant().toEpochMilli()));
                usages.forEach(usage -> {
                    NodeRef usageNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,usage.getNodeId());
                    if(!nodeService.exists(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,usage.getCourseId()))){
                        logger.info("remove usage "+usage.getNodeId()+" for application:" + appInfo.getAppId() +" ("+ appInfo.getSubtype()+") cause: embedding node " + usage.getCourseId() + " does not longer exist.");
                        if(execute)
                            nodeService.deleteNode(usageNodeRef);
                        return;
                    }

                    NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,usage.getCourseId());

                    Date modified = (Date)nodeService.getProperty(nodeRef,ContentModel.PROP_MODIFIED);
                    if(modified.getTime() > Date.from(minimumAgeTime.toInstant()).getTime()){
                        logger.info("embedding node " + usage.getCourseId() + " was recently modified. ignoring.");
                        return;
                    }

                    ContentReader reader = serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
                    if(reader == null || reader.getContentData().getSize() == 0){
                        logger.info("embedding node " + usage.getCourseId() + " has no content.");
                        return;
                    }

                    if(RessourceInfoExecuter.CCM_RESSOURCETYPE_GEOGEBRA.equals(appInfo.getSubtype())){
                        try(InputStream is = reader.getContentInputStream()){
                            List<String> embeddedNodes = getEmbeddedNodesGeogebra(is);
                            if(!embeddedNodes.contains(usage.getParentNodeId())){
                                logger.info("will remove usage " + usage.getNodeId() + " for application:" + appInfo.getAppId() + " (" + appInfo.getSubtype() + ") embedded node " + usage.getParentNodeId() + " is not longer in content of embedding node:"+nodeRef.getId());
                                if(execute){
                                    nodeService.deleteNode(usageNodeRef);
                                }
                            }
                        }catch (IOException e){
                            logger.error(e.getMessage(), e);
                        }
                    }
                });
            }
            return null;
        });
    }

    private List<String> getEmbeddedNodesGeogebra(InputStream is){
        String targetFileName = "geogebra.xml";

        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(targetFileName)) {
                    return parseGeogebraXML(zis);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Could not parse geogebra XML file");
    }

    private List<String> parseGeogebraXML(ZipInputStream zis) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(zis);
        XPath xpath = XPathFactory.newInstance().newXPath();
        String expFormat = "/geogebra/@format";
        String format = (String)xpath.evaluate(expFormat, doc, XPathConstants.STRING);
        if(!"5.0".equals(format)){
            throw new RuntimeException("can not parse geogebra format:"+format);
        }
        String expression = "//embed[starts-with(@url, 'edu-sharing:')]/@url";
        NodeList urlAttributes = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
        List<String> nodeIds = new ArrayList<>();

        for (int i = 0; i < urlAttributes.getLength(); i++) {
            Attr urlAttr = (Attr) urlAttributes.item(i);
            nodeIds.add(urlAttr.getValue().replace("edu-sharing:", ""));
        }
        return nodeIds;
    }
}
