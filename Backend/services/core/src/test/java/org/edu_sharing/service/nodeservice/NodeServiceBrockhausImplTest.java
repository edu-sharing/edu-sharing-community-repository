package org.edu_sharing.service.nodeservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeServiceBrockhausImplTest {

    private NodeServiceBrockhausImpl underTest;

    @BeforeEach
    void setUp() {
        underTest = new NodeServiceBrockhausImpl(null);
    }

    @Test
    void encodeDecodeTest(){
        String testId= "%2fTestKilex2313";
        assertEquals("ABC1", underTest.encodeId("ABC1"));
        assertEquals(testId, underTest.decodeId(testId));
        assertEquals("JTJmVGVzdEtpbGV4MjMxMw==", underTest.encodeId(testId));
        assertEquals(testId, underTest.decodeId(underTest.encodeId(testId)));
    }
}