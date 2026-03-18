package org.edu_sharing.metadataset.v2;


import lombok.Data;

@Data
public class IndexConfig {

    public enum DataType {
        Dynamic, // for native type use as fallback if nothing is specified.
        JsonData // for Text fields containing Json
    }

    /**
     * Specifies the type of data configuration for the index.
     * This variable indicates how data will be handled and interpreted in elastic search.
     */
    private DataType dataType = DataType.Dynamic;
}
