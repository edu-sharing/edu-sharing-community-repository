package org.edu_sharing.repository.server.jobs.quartz;

import org.apache.http.client.ClientProtocolException;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CheckLinkJobTest {
    private CheckLinkJob underTest;

    @BeforeEach
    void setUp() {
        underTest = new CheckLinkJob();
    }
    @Test
    public void testInvalidLinks() {
        CheckLinkJob.StatusResult actual = underTest.getStatus("invalid_link");
        assertEquals(CheckLinkJob.STATUS_CODE_INVALID_LINK, actual.getStatus());
        assertEquals(ClientProtocolException.class, actual.getException().getClass());

        actual = underTest.getStatus("htp:/invalid.link");
        assertEquals(CheckLinkJob.STATUS_CODE_INVALID_LINK, actual.getStatus());
        assertEquals(ClientProtocolException.class, actual.getException().getClass());
    }
    @Test
    @Ignore
    public void testStatusCodes() {
        Arrays.asList(200, 201, 400, 401, 403, 404, 500).forEach(expected -> {
            CheckLinkJob.StatusResult actual = underTest.getStatus("https://httpstat.us/" + expected);
            assertEquals(expected, actual.getStatus());
            assertNull(actual.getException());
        });
    }
}