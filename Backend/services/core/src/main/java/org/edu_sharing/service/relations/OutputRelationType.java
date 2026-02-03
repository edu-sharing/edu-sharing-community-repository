package org.edu_sharing.service.relations;

public enum OutputRelationType {
    isPartOf, hasPart,
    isBasedOn, isBasisFor,

    references,

    //new
    isDuplicateOf,
    requires, isRequiredBy,
    replaces, isReplacedBy,
    hasFormat, isFormatOf,
}
