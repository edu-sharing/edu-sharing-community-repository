package org.edu_sharing.repository.server.jobs.quartz;

import com.google.common.collect.Lists;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.axis.utils.StringUtils;
import org.apache.commons.csv.writer.CSVConfig;
import org.apache.commons.csv.writer.CSVField;
import org.apache.commons.csv.writer.CSVWriter;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.NodeTool;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.service.mediacenter.MediacenterService;
import org.edu_sharing.service.mediacenter.MediacenterServiceFactory;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceDefault;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@JobDescription(description = "Creates reports for all mediacenters on the 1st of each month for the last month")
public class MediacenterMonthlyReportsJob extends AbstractJobMapAnnotationParams {

	protected Logger logger = Logger.getLogger(MediacenterMonthlyReportsJob.class);
	private org.alfresco.service.cmr.repository.NodeService nodeService;
	private NodeService nodeServiceEdu;

	@JobFieldDescription(description = "List of properties to include in the export file", sampleValue = "cm:name")
	private List<String> columns = new ArrayList<String>() {{
		add("cclom:title");
		add("ccm:replicationsourceid");
	}};

	static List<TrackingService.EventType> STAT_FIELDS = Arrays.asList(
			TrackingService.EventType.VIEW_MATERIAL,
			TrackingService.EventType.VIEW_MATERIAL_EMBEDDED,
			TrackingService.EventType.VIEW_MATERIAL_PLAY_MEDIA,
			TrackingService.EventType.DOWNLOAD_MATERIAL
	);
	@JobFieldDescription(description = "Optional, set a list of mediacenters to apply, otherwise it will run for all")
	private List<String> mediacenters;
	@JobFieldDescription(description = "force run, even if the date is currently not the 1st")
	private boolean force;
	private ContentService contentService;


	@Override
	protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		contentService = serviceRegistry.getContentService();
		nodeService = serviceRegistry.getNodeService();
		nodeServiceEdu = NodeServiceFactory.getLocalService();

		Date date = new Date();
		LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		if (!force && localDate.getDayOfMonth() != 1) {
			logger.info("Job not running because of date: " + localDate.getDayOfMonth());
			return;
		}
		AuthenticationUtil.runAsSystem(this::createStats);
	}

	private Void createStats() {
		try {
			Date date = new Date();
			LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			LocalDate from = LocalDate.of(localDate.getYear(), localDate.getMonthValue() - 1, 1);
			YearMonth month = YearMonth.from(from);
			LocalDate to = month.atEndOfMonth();
			TrackingService trackingService = TrackingServiceFactory.getTrackingService();
			MediacenterService mediacenterService = MediacenterServiceFactory.getLocalService();
			String baseFolder = new UserEnvironmentTool().getEdu_SharingMediacenterFolder();
			for (String mediacenter : mediacenters == null ? SearchServiceFactory.getLocalService().getAllMediacenters() : mediacenters) {
				if(isInterrupted()) {
					return null;
				}
				logger.info("Building stats for mediacenter " + mediacenter);
				List<NodeRef> nodes = mediacenterService.getAllLicensedNodes(mediacenter);
				Map<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> data = trackingService.getListNodeData(
						nodes.stream().map(
								ref -> new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, ref.getNodeId())
						).collect(Collectors.toList()),
						Date.from(from.atStartOfDay().toInstant(ZoneOffset.UTC)),
						Date.from(to.atTime(23, 59).toInstant(ZoneOffset.UTC)));
				String filename = from.format(DateTimeFormatter.ISO_DATE) + " - " + to.format(DateTimeFormatter.ISO_DATE) + ".csv";
				String parent = new NodeTool().createOrGetNodeByName(new MCAlfrescoAPIClient(), baseFolder, new String[]{mediacenter});
				PermissionServiceFactory.getLocalService().setPermission(parent, mediacenterService.getMediacenterAdminGroup(mediacenter), CCConstants.PERMISSION_CONSUMER);
				String nodeId = NodeServiceFactory.getLocalService().createNode(parent, CCConstants.CCM_TYPE_IO, NodeServiceFactory.getLocalService().getNameProperty(filename));
				writeCSVFile(nodes, nodeId);
			}
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
		return null;
	}

	private void writeCSVFile(List<NodeRef> nodes, String nodeId) throws Exception {
		ContentWriter contentWriter = contentService.getWriter(
				new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId),
				ContentModel.PROP_CONTENT,
				true);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(bos);
		CSVWriter writer = new CSVWriter();
		writer.setWriter(osw);
		CSVConfig config = new CSVConfig();
		List<CSVField> fields = columns.stream().map(CSVField::new).collect(Collectors.toList());
		fields.addAll(STAT_FIELDS.stream().map(Enum::toString).map(CSVField::new).collect(Collectors.toList()));
		config.setFields(
				fields
		);
		writer.setConfig(config);
		Map<String, String> header = columns.stream().collect(Collectors.toMap(
				e -> e,
				e -> I18nAngular.getTranslationAngular("common", "NODE." + e)
		));
		header.putAll(STAT_FIELDS.stream().collect(Collectors.toMap(
				Enum::toString,
				e -> e.toString()
		)));
		writer.writeRecord(header);
		for (NodeRef node : nodes) {
			if(isInterrupted()) {
				return;
			}
			Map<String, String> csvMap = columns.stream().collect(Collectors.toMap(
							e -> e,
							e -> {
								String prop = NodeServiceHelper.getProperty(
										new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node.getNodeId()),
										CCConstants.getValidGlobalName(e));
								if(StringUtils.isEmpty(prop)) {
									return "";
								}
							return prop;
							}
					)
			);
			writer.writeRecord(csvMap);
		}
		new MCAlfrescoAPIClient().writeContent(nodeId, bos.toByteArray(), "text/csv", String.valueOf(StandardCharsets.UTF_8), CCConstants.CM_PROP_CONTENT);
	}
}
