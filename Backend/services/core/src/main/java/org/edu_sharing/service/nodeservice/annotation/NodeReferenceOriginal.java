package org.edu_sharing.service.nodeservice.annotation;

import java.lang.annotation.*;

/**
 * Maps the incoming node id to the original node id only for collection references
 * (ccm:collection_io_reference aspect to ccm:original).
 *
 * Unlike {@link NodeOriginal}, this annotation does NOT follow the published-copy chain
 * (ccm:io_published_original). Use this when a published copy must retain its own identity,
 * for example when extracting fulltext or other content-bound data that may differ between
 * the copy and the original.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface NodeReferenceOriginal {
}
