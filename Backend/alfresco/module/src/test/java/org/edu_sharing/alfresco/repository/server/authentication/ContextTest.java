package org.edu_sharing.alfresco.repository.server.authentication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.edu_sharing.alfresco.repository.server.authentication.Context.SingleB3;

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

    /*
     * The `b3` single header. rendering2 runs Spring Boot with
     * management.tracing.propagation.type=b3, which sends this form instead of the X-B3-* headers -
     * without parsing it the repository sees no trace at all.
     */

    @Test
    void parsesTheSingleB3Header() {
        SingleB3 parsed = SingleB3.parse("e9efe61242111cc3c2af563d44e148ec-6be94aed91234d75-1");
        assertEquals("e9efe61242111cc3c2af563d44e148ec", parsed.traceId());
        assertEquals("6be94aed91234d75", parsed.spanId());
        assertEquals("1", parsed.sampled());
    }

    @Test
    void acceptsTheOptionalFieldsOfTheSingleHeader() {
        // no sampling state
        assertEquals("6be94aed91234d75", SingleB3.parse("e9efe61242111cc3c2af563d44e148ec-6be94aed91234d75").spanId());
        // a 64 bit trace id
        assertEquals("e9efe61242111cc3", SingleB3.parse("e9efe61242111cc3-6be94aed91234d75-1").traceId());
        // debug implies sampled
        assertEquals("d", SingleB3.parse("e9efe61242111cc3c2af563d44e148ec-6be94aed91234d75-d").sampled());
        // trailing parent span id
        assertEquals("1", SingleB3.parse("e9efe61242111cc3c2af563d44e148ec-6be94aed91234d75-1-aaaaaaaaaaaaaaaa").sampled());
        // "b3: 0" is the sampling decision alone
        SingleB3 deny = SingleB3.parse("0");
        assertNull(deny.traceId());
        assertEquals("0", deny.sampled());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "not-a-trace",
            "e9efe61242111cc3c2af563d44e148ec",                        // span id missing
            "e9efe61242111cc-6be94aed91234d75-1",                      // trace id too short
            "e9efe61242111cc3c2af563d44e148ec-6be94aed9123-1",         // span id too short
            "e9efe61242111cc3c2af563d44e148ec-6be94aed91234d75-2",     // no such sampling state
            "e9efe61242111cc3c2af563d44e148ec-6be94aed91234d75-1-xx",  // malformed parent
    })
    void ignoresAMalformedSingleHeader(String header) {
        // a broken trace header costs the correlation, it must not fail the request
        SingleB3 parsed = assertDoesNotThrow(() -> SingleB3.parse(header));
        assertNull(parsed.traceId());
        assertNull(parsed.spanId());
    }
}
