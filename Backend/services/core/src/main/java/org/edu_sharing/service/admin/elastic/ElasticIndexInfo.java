package org.edu_sharing.service.admin.elastic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElasticIndexInfo {
    private String name;
    private List<String> aliases;
    private String health;
    private String status;
    private Long docsCount;
    private String storeSize;
    /**
     * true if this is the index used by the repository for nodes (workspace)
     */
    private boolean workspace;
    /**
     * true if this is the index used by the repository for users and groups
     */
    private boolean authorities;
}
