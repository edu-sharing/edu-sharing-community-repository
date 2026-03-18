package org.edu_sharing.restservices.mds.v1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.edu_sharing.metadataset.v2.IndexConfig;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MdsIndex {
    private IndexConfig.DataType dataType;
}
