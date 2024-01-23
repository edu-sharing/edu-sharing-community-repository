package org.edu_sharing.alfresco.transformer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeogebraTransformerWorkerTest {
//    private GeogebraTransformerWorker underTest;
//
//
//    @BeforeEach
//    void setUp() {
//        underTest = new GeogebraTransformerWorker();
//    }
//
//    @Test
//    public void extractTextContentTest() throws Exception {
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        underTest.extractTextContent(new ByteArrayInputStream((
//                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
//                        "<geogebra>" +
//                        "<element type=\"inlinetext\" label=\"a\">\n" +
//                        "<content val=\"[{&quot;text&quot;:&quot;TEST\\nNEW Line&quot;}]\"/>\n" +
//                        "</element>" +
//                        "<element type=\"inlinetext\" label=\"a\">\n" +
//                        "<content val=\"[{&quot;text&quot;:&quot;Second Content&quot;}]\"/>\n" +
//                        "</element>" +
//                        "</geogebra>"
//        ).getBytes()), bos);
//        assertEquals("TEST\nNEW Line Second Content", bos.toString());
//
//        bos = new ByteArrayOutputStream();
//        underTest.extractTextContent(new ByteArrayInputStream((
//                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
//                        "<geogebra>" +
//                        "<element type=\"inlinetext\" label=\"a\">\n" +
//                        "</element>" +
//                        "</geogebra>"
//        ).getBytes()), bos);
//        assertEquals("", bos.toString());
//    }
}