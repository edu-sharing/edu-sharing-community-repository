package org.edu_sharing.metadataset.v2;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetadataQueryBaseTest {
    @Test
    void findBasequery() {
        MetadataQuery underTest = new MetadataQuery();
        underTest.setSyntax("dsl");
        Map<String, String> basequery = new HashMap<>();
        basequery.put(null, "NULL");
        basequery.put("A", "A");
        underTest.setBasequery(basequery);
        assertEquals("NULL", underTest.findBasequery(null));
        assertEquals("NULL", underTest.findBasequery(Collections.singleton("A")));
        assertEquals("A", underTest.findBasequery(Collections.singleton("B")));

        basequery = new HashMap<>();
        basequery.put(null, "NULL");
        // when both property a and b are null
        basequery.put("A+B", "A");
        underTest.setBasequery(basequery);
        assertEquals("NULL", underTest.findBasequery(null));
        assertEquals("NULL", underTest.findBasequery(Collections.singleton("A")));
        assertEquals("NULL", underTest.findBasequery(Collections.singleton("B")));
        assertEquals("NULL", underTest.findBasequery(new HashSet<>(Arrays.asList("A", "B"))));
        assertEquals("A", underTest.findBasequery(new HashSet<>(Arrays.asList("C", "D"))));
    }
}