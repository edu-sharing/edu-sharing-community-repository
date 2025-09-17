package org.edu_sharing.restservices.mds.v1.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Data;
import org.edu_sharing.metadataset.v2.MetadataColumn;
import org.edu_sharing.metadataset.v2.MetadataList;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class MdsList {
    private String id;
    private Map<MetadataList.ColumnType, List<MdsColumn>> columns;

    public MdsList(MetadataList list) {
        this.id = list.getId();
        if (list.getColumns() != null) {
            columns = new HashMap<>();
            for (Map.Entry<MetadataList.ColumnType, List<MetadataColumn>> column : list.getColumns().entrySet()) {
                columns.put(column.getKey(), column.getValue().stream().map(MdsColumn::new).collect(Collectors.toList()));
            }
        }
    }
}

