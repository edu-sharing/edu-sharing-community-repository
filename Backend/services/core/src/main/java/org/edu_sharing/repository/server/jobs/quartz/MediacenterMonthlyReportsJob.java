package org.edu_sharing.repository.server.jobs.quartz;

import com.opencsv.CSVWriter;
import lombok.*;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.NodeTool;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.mediacenter.MediacenterService;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.tracking.ActivityOnNodeEventType;
import org.edu_sharing.service.tracking.ActivityStatisticService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.edu_sharing.alfresco.service.AuthorityService.ORG_GROUP_PREFIX;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "Creates reports for all mediacenters on the 1st of each month for the last month")
public class MediacenterMonthlyReportsJob extends AbstractJobMapAnnotationParams {

    public enum ReportMode {
        @JobFieldDescription(description = "Use the tracked mediacenter user data. Elements accessed from users assigned to more than one mediacenter are ignored")
        TrackingMediacenterData,
        @JobFieldDescription(description = "Use the current licensed node data. Elements not licensed anymore will not be visible. Elements accessed from users assigned to more than one MZ are counted as well (legacy)")
        AlfrescoPermissionData
    }

    private enum ReportType {
        Monthly,
        Yearly,
        Quarterly,
    }

    @JobFieldDescription(description = "Mode to use for processing")
    private ReportMode mode;

    protected Logger logger = Logger.getLogger(MediacenterMonthlyReportsJob.class);
    @JobFieldDescription(description = "List of properties to include in the export file, first value is the property, second value (only for vcard) is the vcard field id", sampleValue = "[\"cm:name\"]")
    private List<List<String>> columns = new ArrayList<List<String>>() {{
        add(Collections.singletonList("cclom:title"));
        add(Collections.singletonList("ccm:replicationsourceid"));
        add(Collections.singletonList("ccm:lifecyclecontributer_publisher"));
        add(Arrays.asList("ccm:lifecyclecontributer_publisher", "X-ES-LOM-CONTRIBUTE-DATE"));
        add(Collections.singletonList("sys:node-uuid"));
    }};
    @JobFieldDescription(description = "List of additional (custom) fields to be fetched from the tracking data", sampleValue = "field1")
    private List<String> additionalFields = Collections.emptyList();

    static List<ActivityOnNodeEventType> STAT_FIELDS = Arrays.asList(
            ActivityOnNodeEventType.VIEW_MATERIAL,
            ActivityOnNodeEventType.VIEW_MATERIAL_EMBEDDED,
            ActivityOnNodeEventType.VIEW_MATERIAL_PLAY_MEDIA,
            ActivityOnNodeEventType.VIEW_MATERIAL_GDPR_CONFIRMED,
            ActivityOnNodeEventType.OPEN_EXTERNAL_LINK,
            ActivityOnNodeEventType.DOWNLOAD_MATERIAL
    );
    @JobFieldDescription(description = "Optional, set a list of mediacenters to apply, otherwise it will run for all", sampleValue = "GROUP_MEDIA_CENTER_1")
    private List<String> mediacenters;
    @JobFieldDescription(description = "force run, even if the date is currently not the 1st")
    private boolean force = false;

    @JobFieldDescription(description = "When set to true, the job will generate the monthly report")
    private boolean generateMonthly = true;

    @JobFieldDescription(description = "When set to true, the job will generate a yearly report as well (only on 1st January)")
    private boolean generateYearly = false;

    @JobFieldDescription(description = "When set to true, the job will generate a quarterly report as well (only on 1st of January, April, July and October)")
    private boolean generateQuarterly = false;

    @JobFieldDescription(description = "use a custom date (month) to run the job for. Note: The job will run the month BEFORE the given date!", sampleValue = "YYYY-MM-DD")
    private Date customDate = null;


    @JobFieldDescription(description = "Delete stats of the month if they're already existing")
    private boolean delete = false;


    @Autowired
    private NodeService nodeServiceEdu;
    @Autowired
    private SearchService searchService;
    @Autowired
    private AuthorityService authorityService;
    @Autowired
    private MediacenterService mediacenterService;
    @Autowired
    private org.edu_sharing.service.permission.PermissionService permissionService;
    ActivityStatisticService activityStatisticService = TrackingServiceFactory.getTrackingService();

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {


        Date date = new Date();
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (!force && localDate.getDayOfMonth() != 1) {
            logger.info("Job not running because of date: " + localDate.getDayOfMonth());
            return;
        }
        if (mode == null) {
            mode = ReportMode.TrackingMediacenterData;
        }
        AuthenticationUtil.runAsSystem(this::createStats);
    }

    private Void createStats() {
        try {
            Date date = new Date();
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (customDate != null) {
                localDate = customDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            for (String mediacenter : mediacenters == null ? searchService.getAllMediacenters() : mediacenters) {

                LocalDate lastMonth = localDate.minusMonths(1);
                LocalDate from = lastMonth.withDayOfMonth(1);

                YearMonth month = YearMonth.from(from);
                LocalDate to = month.atEndOfMonth();

                if (isInterrupted()) {
                    return null;
                }
                logger.info("Building stats for mediacenter " + mediacenter);
                Date startDate = Date.from(from.atStartOfDay().toInstant(ZoneOffset.UTC));
                Date endDate = Date.from(to.atTime(23, 59).toInstant(ZoneOffset.UTC));
                if (generateMonthly) {
                    generateReportByTimeRange(mediacenter, startDate, endDate, ReportType.Monthly);
                    generateSchoolReportByTimeRange(mediacenter, startDate, endDate, ReportType.Monthly);
                }
                if (generateYearly && localDate.getMonthValue() == 1) {
                    from = localDate.minusYears(1).withDayOfMonth(1);
                    startDate = Date.from(from.atStartOfDay().toInstant(ZoneOffset.UTC));
                    generateReportByTimeRange(mediacenter, startDate, endDate, ReportType.Yearly);
                }

                if (generateQuarterly && List.of(1, 4, 7, 10).contains(localDate.getMonthValue())) {
                    from = lastMonth.withMonth(lastMonth.getMonth().firstMonthOfQuarter().getValue())
                            .withDayOfMonth(1);

                    startDate = Date.from(from.atStartOfDay().toInstant(ZoneOffset.UTC));
                    generateReportByTimeRange(mediacenter, startDate, endDate, ReportType.Quarterly);
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return null;
    }

    private void generateSchoolReportByTimeRange(String mediacenter, Date startDate, Date endDate, ReportType reportType) throws Throwable {
        if (mode.equals(ReportMode.TrackingMediacenterData)) {
            Map<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> dataNodes = activityStatisticService.getListNodeDataByMediacenter(
                    mediacenter,
                    startDate,
                    endDate,
                    Collections.singletonList("authority_organization")
            );
            dataNodes = filterNonMediacenterMedia(dataNodes);

            // Holds for each event (VIEW, DOWNLOAD...) a list of Org ids + counts
            Map<String, Map<String, Long>> result = new HashMap<>();
            Set<String> organizations = new HashSet<>(authorityService.getMembershipsOfGroup(mediacenter));
            for (StatisticEntry v : dataNodes.values()) {
                v.getGroups().forEach((eventType, stats) -> {
                    result.computeIfAbsent(eventType, k -> new HashMap<>());
                    Map<String, Long> orgList = stats.get("authority_organization");
                    organizations.addAll(orgList.keySet());
                    orgList.forEach((org, count) -> result.get(eventType).put(org, result.get(eventType).getOrDefault(org, 0L) + count));
                });
            }

            List<String> eventList = new ArrayList<>(result.keySet());
            List<String> header = new ArrayList<>();
            header.add(I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.HEADERS.authority_organization_id"));
            header.add(I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.HEADERS.authority_organization"));
            eventList.forEach(eventType -> header.add(I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.ACTIONS." + eventType)));


            List<String[]> csvList = organizations.stream().filter((org) -> {
                // Serializable groupType = NodeServiceHelper.getPropertyNative(AuthorityServiceHelper.getAuthorityNodeRef(org), CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE);
                return
                        org.startsWith(PermissionService.GROUP_PREFIX + ORG_GROUP_PREFIX);
                // && (groupType == null || !groupType.equals(MEDIACENTER_ADMINISTRATORS_GROUP));
            }).sorted((a, b) -> {
                try {
                    String numA = a.substring((PermissionService.GROUP_PREFIX + ORG_GROUP_PREFIX).length());
                    String numB = b.substring((PermissionService.GROUP_PREFIX + ORG_GROUP_PREFIX).length());
                    return Long.compare(Long.parseLong(numA), Long.parseLong(numB));
                } catch (Throwable t) {
                    return a.compareToIgnoreCase(b);
                }

            }).map(org -> {
                List<String> resultArray = new ArrayList<>();
                org.alfresco.service.cmr.repository.NodeRef ref = AuthorityServiceHelper.getAuthorityNodeRef(org);
                resultArray.add(org.substring((PermissionService.GROUP_PREFIX + ORG_GROUP_PREFIX).length()));
                resultArray.add((String) NodeServiceHelper.getPropertyNative(ref, ContentModel.PROP_AUTHORITY_DISPLAY_NAME.toString()));
                for (String eventType : eventList) {
                    resultArray.add(result.get(eventType).getOrDefault(org, 0L).toString());
                }
                return resultArray.toArray(new String[0]);
            }).collect(Collectors.toList());
            String nodeId = generateCSVNode(mediacenter, "nach-Schulen", startDate, endDate, reportType);
            try {
                writeCSVFileInternal(nodeId, header, csvList);
            } catch (Throwable t) {
                logger.warn("Error writing csv school data for mediacenter " + mediacenter, t);
                NodeServiceHelper.removeNode(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), false);
            }
        } else {
            logger.info("generateSchoolReportByTimeRange is only supported in mode " + ReportMode.TrackingMediacenterData);
        }
    }


    private void generateReportByTimeRange(String mediacenter, Date startDate, Date endDate, ReportType reportType) throws Throwable {
        Map<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> data = null;
        if (mode.equals(ReportMode.AlfrescoPermissionData)) {
            List<NodeRef> nodes = mediacenterService.getAllLicensedNodes(mediacenter, Collections.emptyMap(), null);
            data = activityStatisticService.getListNodeData(
                    nodes.stream().map(
                            ref -> new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, ref.getNodeId())
                    ).collect(Collectors.toList()),
                    startDate,
                    endDate,
                    additionalFields,
                    mediacenter
            );
        } else if (mode.equals(ReportMode.TrackingMediacenterData)) {
            data = activityStatisticService.getListNodeDataByMediacenter(
                    mediacenter,
                    startDate,
                    endDate,
                    additionalFields
            );
            logger.info("Tracking db done for " + mediacenter + " (" + data.size() + " elements), fetching all licensed nodes...");
            List<NodeRef> nodes = mediacenterService.getAllLicensedNodes(mediacenter, Collections.emptyMap(), null);
            logger.info(mediacenter + " has currently " + nodes.size() + " licensed nodes");
            // The licensed nodes are the source of truth for the report. Filter them on the
            // ccm:io_editorial_state that Elasticsearch already delivered in memory (calling
            // NodeServiceHelper.getPropertyNative per node instead would load the full property map of
            // every node into the Alfresco L2 caches and blow up the heap) and merge in the tracking
            // counts collected above.
            data = new HashMap<>();
            for (NodeRef n : nodes) {
                if (n.getProperties() != null && !"restricted_mz".equals(n.getProperties().get(CCConstants.CCM_PROP_IO_EDITORIAL_STATE))) {
                    continue;
                }
                org.alfresco.service.cmr.repository.NodeRef mappedRef = new org.alfresco.service.cmr.repository.NodeRef(new StoreRef(n.getStoreProtocol(), n.getStoreId()), n.getNodeId());
                StatisticEntry entry = data.get(mappedRef);
                data.put(mappedRef, entry != null ? entry : new StatisticEntry());
            }
            logger.info(mediacenter + " remaining " + data.size() + " elements after filtering for non-mediacenter elements");
        }
        String nodeId = generateCSVNode(mediacenter, "nach-Medien", startDate, endDate, reportType);
        try {
            writeCSVFile(data, nodeId);
        } catch (Throwable t) {
            logger.warn("Error writing csv data for mediacenter " + mediacenter, t);
            NodeServiceHelper.removeNode(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), false);
        }
    }

    private String generateCSVNode(String mediacenter, String csvPostfix, Date startDate, Date endDate, ReportType reportType) throws Throwable {
        String baseFolder = new UserEnvironmentTool().getEdu_SharingMediacenterFolder();
        String filename = getFilename(mediacenter, csvPostfix, startDate, endDate, reportType);
        String parent = NodeTool.createOrGetNodeByName(baseFolder, new String[]{mediacenter});
        permissionService.setPermission(parent, mediacenterService.getMediacenterAdminGroup(mediacenter), CCConstants.PERMISSION_CONSUMER);
        if (delete) {
            String node = nodeServiceEdu.findNodeByName(parent, filename);
            if (node != null) {
                nodeServiceEdu.removeNode(node, null, false);
            }
        }
        String nodeId = nodeServiceEdu.createNode(parent, CCConstants.CCM_TYPE_IO, nodeServiceEdu.getNameProperty(filename));
        NodeServiceHelper.addAspect(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), CCConstants.CCM_MEDIACENTER_STATISTICS);
        NodeServiceHelper.setProperty(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), CCConstants.CCM_PROP_MEDIACENTER_ID, mediacenter, false);
        return nodeId;
    }

    @NotNull
    private static String getFilename(String mediacenter, String postfix, Date startDate, Date endDate, ReportType reportType) {
        StringBuilder sb = new StringBuilder();
        switch (reportType) {

            case Yearly:
                sb.append(startDate.toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(("yyyy"))));
                break;

            case Quarterly:
                sb.append(startDate.toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(("yyyy"))));
                sb.append("-Q");
                sb.append((endDate.getMonth() + 1) / 3);
                break;

            default:
                sb.append(startDate.toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(("yyyy-MM"))));
                break;
        }
        sb.append("_");
        sb.append(NodeServiceHelper.getPropertyNative(AuthorityServiceHelper.getAuthorityNodeRef(mediacenter), CCConstants.CCM_PROP_MEDIACENTER_ID));
        sb.append("_");
        sb.append(postfix);
        sb.append(".csv");
        return sb.toString();
    }

    private Map<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> filterNonMediacenterMedia(Map<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> data) {
        return data.entrySet().stream().filter(
                e -> {
                    try {
                        return "restricted_mz".equals(NodeServiceHelper.getPropertyNative(e.getKey(), CCConstants.CCM_PROP_IO_EDITORIAL_STATE));
                    } catch (InvalidNodeRefException exception) {
                        // node is deleted
                        logger.info("restricted_mz was not verifiable: " + e.getKey() + ": " + exception.getMessage());
                        return false;
                    }
                }
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void writeCSVFileInternal(String nodeId, List<String> header, List<String[]> data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(bos);
        CSVWriter writer = new CSVWriter(osw);
        writer.writeNext(header.toArray(new String[0]));
        writer.writeAll(data);
        osw.close();
        new MCAlfrescoAPIClient().writeContent(nodeId, bos.toByteArray(), "text/csv", String.valueOf(StandardCharsets.UTF_8), CCConstants.CM_PROP_CONTENT);
    }

    private void writeCSVFile(Map<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> data, String nodeId) throws Exception {
        List<String> header = columns.stream().map(c ->
                I18nAngular.getTranslationAngular("common", (
                        c.size() == 1 ? "NODE." + c.get(0) : "VCARD." + c.get(1))
                )
        ).collect(Collectors.toList());
        //header.add(I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.HEADERS.count"));
        header.addAll(STAT_FIELDS.stream().map(e -> I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.ACTIONS." + e)).collect(Collectors.toList()));
        Map<String, Set<String>> additionalFieldValues = new HashMap<>();
        for (String field : additionalFields) {
            Set<String> values = data.values().stream()
                    .map(d -> STAT_FIELDS.stream()
                            .map(event -> d.getGroups().getOrDefault(event, Collections.emptyMap()))
                            .map(x -> x.getOrDefault(field, Collections.emptyMap()))
                            .map(Map::keySet)
                            .collect(Collectors.toSet()))
                    .flatMap(Set::stream)
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());

            additionalFieldValues.put(field, values);
        }

        // concat field name with possible field value
        // i.e. role: teacher
        additionalFieldValues.forEach((key, values) -> header.addAll(values
                .stream()
                .map(subField -> I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.HEADERS." + key) + ": "
                        + (StringUtils.isBlank(subField)
                        ? I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.UNKNOWN_VALUE")
                        : I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.CUSTOM." + key + "." + subField)))
                .map(subField -> I18nAngular.getTranslationAngular("admin", "ADMIN.STATISTICS.HEADERS.count") + " (" + subField + ")")
                .collect(Collectors.toList())
        ));

        ArrayList<ReportEntry> entries = new ArrayList<>();
        for (Map.Entry<org.alfresco.service.cmr.repository.NodeRef, StatisticEntry> entry : data.entrySet()) {
            if (isInterrupted()) {
                return;
            }
            List<String> csvEntry = columns.stream().map(
                    e -> {
                        try {
                            String prop = NodeServiceHelper.getProperty(
                                    new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, entry.getKey().getId()),
                                    CCConstants.getValidGlobalName(e.get(0))
                            );
                            if (VCardConverter.isVCardProp(CCConstants.getValidGlobalName(e.get(0)))) {
                                if (e.size() == 1) {
                                    prop = VCardConverter.getNameForVCardString(prop);
                                } else {
                                    ArrayList<Map<String, Object>> vcard = VCardConverter.vcardToMap(null, prop);
                                    if (vcard.isEmpty()) {
                                        return "";
                                    }
                                    return (String) vcard.get(0).getOrDefault(e.get(1), "");
                                }
                            }
                            if (StringUtils.isEmpty(prop)) {
                                return "";
                            }
                            return prop;
                        } catch (Throwable t) {
                            logger.debug(t.getMessage(), t);
                            return entry.getKey().getId();
                        }
                    }
            ).collect(Collectors.toList());
            // add total sum count
            int totalSum =
                    STAT_FIELDS.stream()
                            .map(event -> entry.getValue().getCounts().getOrDefault(event, 0))
                            .reduce(Integer::sum)
                            .orElse(0);
            // csvEntry.add(String.valueOf(totalSum));

            // add counts per stat field
            csvEntry.addAll(STAT_FIELDS.stream()
                    .map(event -> entry.getValue().getCounts().getOrDefault(event, 0))
                    .map(String::valueOf)
                    .toList());

            // add additional, custom mapped field accounts (i.e. for role)
            additionalFieldValues.forEach((key, value) ->
                    csvEntry.addAll(value.stream()
                            .map(field -> STAT_FIELDS.stream()
                                    .map(event -> entry.getValue()
                                            .getGroups()
                                            .getOrDefault(event, Collections.emptyMap())
                                            .getOrDefault(key, Collections.emptyMap())
                                            .getOrDefault(field, 0L))
                                    .reduce(Long::sum)
                                    .orElse(0L))
                            .map(String::valueOf)
                            .toList()
                    ));
            entries.add(new ReportEntry(csvEntry, totalSum));
        }
        Collections.sort(entries);
        List<String[]> csvContent = entries.stream().map(ReportEntry::entry).map(l -> l.toArray(new String[0])).collect(Collectors.toList());
        writeCSVFileInternal(nodeId, header, csvContent);
    }

        private record ReportEntry(List<String> entry, int totalCount) implements Comparable<ReportEntry> {
            @Override
            public int compareTo(@NotNull ReportEntry reportEntry) {
                return Long.compare(reportEntry.totalCount, this.totalCount);
            }
        }
}
