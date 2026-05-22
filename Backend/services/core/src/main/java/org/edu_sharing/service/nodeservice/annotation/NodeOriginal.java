package org.edu_sharing.service.nodeservice.annotation;

import java.lang.annotation.*;

/**
 * Maps the incoming node id automatically to the original node id.
 *
 * Resolves both:
 * - collection references (ccm:collection_io_reference aspect to ccm:original)
 * - published copies (ccm:io_published_original property)
 *
 * Use {@link NodeReferenceOriginal} when only collection-reference resolution is desired
 * (i.e. a published copy should keep its own identity).
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface NodeOriginal {
}