package org.edu_sharing.spring.conditions;

/**
 * Some named search strategies for beans in the bean factory hierarchy.
 */
public enum SearchStrategy {

    /**
     * Search only the current context.
     */
    CURRENT,

    /**
     * Search all ancestors, but not the current context.
     */
    ANCESTORS,

    /**
     * Search the entire hierarchy.
     */
    ALL

}
