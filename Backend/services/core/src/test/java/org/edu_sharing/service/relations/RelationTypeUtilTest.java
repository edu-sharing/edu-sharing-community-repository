package org.edu_sharing.service.relations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class RelationTypeUtilTest {


    @ParameterizedTest
    @EnumSource(OutputRelationType.class)
    void invertRelationTypeTest(OutputRelationType type) {
        // This test will fail if we've added a new OutputRelationType without assigning an inverse relation
        Assertions.assertNotNull(RelationTypeUtil.invert(type));
    }

    @ParameterizedTest
    @EnumSource(InputRelationType.class)
    void toOutputRelationTypeTest(InputRelationType type) {
        // This test will fail if we've added a new InputRelationType without assigning an OutputRelationType relation
        Assertions.assertNotNull(RelationTypeUtil.toOutputType(type));
    }
}