package org.edu_sharing.service.relations;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class RelationTypeUtil {
    static {
        invertRelationTypeSet = Map.ofEntries(
                // normalized to inverted
                Map.entry(OutputRelationType.isPartOf, OutputRelationType.hasPart),
                Map.entry(OutputRelationType.isBasedOn, OutputRelationType.isBasisFor),
                Map.entry(OutputRelationType.requires, OutputRelationType.isRequiredBy),
                Map.entry(OutputRelationType.replaces, OutputRelationType.isReplacedBy),
                Map.entry(OutputRelationType.hasFormat, OutputRelationType.isFormatOf),

                // inverted to normalized
                Map.entry(OutputRelationType.hasPart, OutputRelationType.isPartOf),
                Map.entry(OutputRelationType.isBasisFor, OutputRelationType.isBasedOn),
                Map.entry(OutputRelationType.isRequiredBy, OutputRelationType.requires),
                Map.entry(OutputRelationType.isReplacedBy, OutputRelationType.replaces),
                Map.entry(OutputRelationType.isFormatOf, OutputRelationType.hasFormat),

                // no convertions
                Map.entry(OutputRelationType.references, OutputRelationType.references),
                Map.entry(OutputRelationType.isDuplicateOf, OutputRelationType.isDuplicateOf)
        );

        reverseInputRelationTypeSet = Arrays.stream(InputRelationType.values())
                .collect(Collectors.toMap(Function.identity(), RelationTypeUtil::reverse));
    }

    public final static Map<InputRelationType, OutputRelationType> reverseInputRelationTypeSet;

    private final static Map<OutputRelationType, OutputRelationType> invertRelationTypeSet;


    public static OutputRelationType reverse(InputRelationType type) {
        return reverse(toOutputType(type));
    }

    public static OutputRelationType reverse(OutputRelationType type) {
        return invertRelationTypeSet.get(type);
    }

    public static OutputRelationType toOutputType(InputRelationType type) {
        return OutputRelationType.valueOf(type.name());
    }
}
