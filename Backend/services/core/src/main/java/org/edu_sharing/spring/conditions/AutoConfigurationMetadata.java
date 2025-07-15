package org.edu_sharing.spring.conditions;

import java.util.Set;

/**
 * Provides access to meta-data written by the auto-configure annotation processor.
 */
public interface AutoConfigurationMetadata {

    /**
     * Return {@code true} if the specified class name was processed by the annotation
     * processor.
     * @param className the source class
     * @return if the class was processed
     */
    boolean wasProcessed(String className);

    /**
     * Get an {@link Integer} value from the meta-data.
     * @param className the source class
     * @param key the meta-data key
     * @return the meta-data value or {@code null}
     */
    Integer getInteger(String className, String key);

    /**
     * Get an {@link Integer} value from the meta-data.
     * @param className the source class
     * @param key the meta-data key
     * @param defaultValue the default value
     * @return the meta-data value or {@code defaultValue}
     */
    Integer getInteger(String className, String key, Integer defaultValue);

    /**
     * Get a {@link Set} value from the meta-data.
     * @param className the source class
     * @param key the meta-data key
     * @return the meta-data value or {@code null}
     */
    Set<String> getSet(String className, String key);

    /**
     * Get a {@link Set} value from the meta-data.
     * @param className the source class
     * @param key the meta-data key
     * @param defaultValue the default value
     * @return the meta-data value or {@code defaultValue}
     */
    Set<String> getSet(String className, String key, Set<String> defaultValue);

    /**
     * Get an {@link String} value from the meta-data.
     * @param className the source class
     * @param key the meta-data key
     * @return the meta-data value or {@code null}
     */
    String get(String className, String key);

    /**
     * Get an {@link String} value from the meta-data.
     * @param className the source class
     * @param key the meta-data key
     * @param defaultValue the default value
     * @return the meta-data value or {@code defaultValue}
     */
    String get(String className, String key, String defaultValue);

}
