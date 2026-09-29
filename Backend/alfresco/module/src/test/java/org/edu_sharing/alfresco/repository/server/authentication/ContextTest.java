package org.edu_sharing.alfresco.repository.server.authentication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContextTest {

    private static final String GIVEN = "3f2b8c1e-5d4a-4e6f-9a7b-1c2d3e4f5a6b";

    @Test
    void keepsAnIncomingUuid() {
        assertEquals(GIVEN, Context.resolveRequestId(GIVEN));
        assertEquals(GIVEN.toUpperCase(), Context.resolveRequestId(GIVEN.toUpperCase()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    // the last two: forms UUID.fromString alone would accept
    @ValueSource(strings = {"req-1", "3f2b8c1e5d4a4e6f9a7b1c2d3e4f5a6b", GIVEN + "x", " " + GIVEN, "1-2-3-4-5", "+f2b8c1e-5d4a-4e6f-9a7b-1c2d3e4f5a6b"})
    void generatesAUuidOtherwise(String incoming) {
        String id = Context.resolveRequestId(incoming);
        assertNotEquals(incoming, id);
        assertDoesNotThrow(() -> UUID.fromString(id));
        assertNotEquals(id, Context.resolveRequestId(incoming));
    }
}
