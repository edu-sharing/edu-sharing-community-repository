package org.edu_sharing.service.admin.elastic;

public enum ElasticPropertySource {
    /**
     * read values from the indexed _source document (fast)
     */
    elastic,
    /**
     * read values for every hit from the alfresco repository (exact, slow)
     */
    alfresco
}
