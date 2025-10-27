package org.edu_sharing.service.tracking;

import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.I18nAngular;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

@Lazy
@Service
@RequiredArgsConstructor
public class StatisticsFileService {
    private final NodeService nodeService;

    private final List<ActivityOnNodeEventType> statFields = Arrays.asList(
            ActivityOnNodeEventType.VIEW_MATERIAL,
            ActivityOnNodeEventType.VIEW_MATERIAL_EMBEDDED,
            ActivityOnNodeEventType.VIEW_MATERIAL_PLAY_MEDIA,
            ActivityOnNodeEventType.OPEN_EXTERNAL_LINK,
            ActivityOnNodeEventType.DOWNLOAD_MATERIAL
    );


    public String writeCSV(String targetFolder, String filename, Map<NodeRef, StatisticEntry> trackingNodes, List<List<String>> columns) throws Throwable {
        if (!filename.endsWith(".csv")) {
            filename += ".csv";
        }

        List<String> header = Stream.concat(
                        columns.stream()
                                .filter(field -> !field.isEmpty())
                                .map(x -> x.size() == 1 ? "NODE." + x.get(0) : "VCARD." + x.get(1))
                                .map(x -> I18nAngular.getTranslationAngular("common", x)),
                        statFields.stream()
                                .map(x -> "ADMIN.STATISTICS.ACTIONS." + x)
                                .map(x -> I18nAngular.getTranslationAngular("admin", x)))
                .toList();


        List<String[]> data = trackingNodes
                .entrySet()
                .stream()
                .map(entry -> Stream.concat(
                                        columns.stream()
                                                .filter(field -> !field.isEmpty())
                                                .map(field -> {
                                                    NodeRef nodeRef = entry.getKey();

                                                    String propName = CCConstants.getValidGlobalName(field.get(0));
                                                    String prop = nodeRef.getProperties()
                                                            .getOrDefault(propName, "")
                                                            .toString();

                                                    if (StringUtils.isBlank(prop)) {
                                                        return "";
                                                    }

                                                    if (VCardConverter.isVCardProp(propName)) {
                                                        if (field.size() == 1) {
                                                            return VCardConverter.getNameForVCardString(prop);
                                                        } else {
                                                            ArrayList<Map<String, Object>> vcard = VCardConverter.vcardToMap(null, prop);
                                                            if (vcard.isEmpty()) {
                                                                return "";
                                                            }

                                                            return vcard.get(0)
                                                                    .getOrDefault(field.get(1), "")
                                                                    .toString();
                                                        }
                                                    }

                                                    return prop;
                                                }),
                                        statFields.stream()
                                                .map(event -> entry.getValue().getCounts().getOrDefault(event.toString(), 0))
                                                .map(String::valueOf)
                                )
                                .toArray(String[]::new)
                )
                .toList();

        String existingNodeId = nodeService.findNodeByName(targetFolder, filename);
        if (existingNodeId != null) {
            nodeService.removeNode(existingNodeId, targetFolder, false);
        }

        String targetNodeId = nodeService.createNode(targetFolder, CCConstants.CCM_TYPE_IO, nodeService.getNameProperty(filename));
        try {
            writeToFile(targetNodeId, header, data);
            return existingNodeId;
        } catch (
                IOException e) {
            nodeService.removeNode(targetNodeId, targetFolder, false);
            throw new RuntimeException(e);
        }
    }

    private void writeToFile(String nodeId, List<String> header, List<String[]> data) throws Exception {
        try (
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(bos);
                CSVWriter writer = new CSVWriter(osw)
        ) {
            writer.writeNext(header.toArray(new String[0]));
            writer.writeAll(data);
            writer.flush();
            new MCAlfrescoAPIClient().writeContent(nodeId, bos.toByteArray(), "text/csv", String.valueOf(StandardCharsets.UTF_8), CCConstants.CM_PROP_CONTENT);
        }
    }

}
