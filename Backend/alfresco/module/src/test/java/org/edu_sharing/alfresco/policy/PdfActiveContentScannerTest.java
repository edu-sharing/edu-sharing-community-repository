package org.edu_sharing.alfresco.policy;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDJavascriptNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionLaunch;
import org.apache.pdfbox.pdmodel.interactive.action.PDPageAdditionalActions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class PdfActiveContentScannerTest {

    @TempDir
    File tempDir;

    private File createPdf(Consumer<PDDocument> customizer) throws IOException {
        File file = new File(tempDir, System.nanoTime() + ".pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            customizer.accept(document);
            document.save(file);
        }
        return file;
    }

    @Test
    void cleanPdfHasNoFindings() throws IOException {
        assertEquals(Set.of(), PdfActiveContentScanner.scan(createPdf(document -> {
        })));
    }

    @Test
    void openActionJavaScript() throws IOException {
        File file = createPdf(document -> document.getDocumentCatalog().setOpenAction(new PDActionJavaScript("app.alert('Test')")));
        assertEquals(Set.of(PdfActiveContentScanner.Finding.JAVASCRIPT), PdfActiveContentScanner.scan(file));
    }

    @Test
    void namesTreeJavaScript() throws IOException {
        File file = createPdf(document -> {
            PDJavascriptNameTreeNode javaScript = new PDJavascriptNameTreeNode();
            javaScript.setNames(Map.of("test", new PDActionJavaScript("app.alert('Test')")));
            PDDocumentNameDictionary names = new PDDocumentNameDictionary(document.getDocumentCatalog());
            names.setJavascript(javaScript);
            document.getDocumentCatalog().setNames(names);
        });
        assertEquals(Set.of(PdfActiveContentScanner.Finding.JAVASCRIPT), PdfActiveContentScanner.scan(file));
    }

    @Test
    void pageAdditionalActionJavaScript() throws IOException {
        File file = createPdf(document -> {
            PDPageAdditionalActions actions = new PDPageAdditionalActions();
            actions.setO(new PDActionJavaScript("app.alert('Test')"));
            document.getPage(0).setActions(actions);
        });
        assertEquals(Set.of(PdfActiveContentScanner.Finding.JAVASCRIPT), PdfActiveContentScanner.scan(file));
    }

    @Test
    void launchAction() throws IOException {
        File file = createPdf(document -> document.getDocumentCatalog().setOpenAction(new PDActionLaunch()));
        assertEquals(Set.of(PdfActiveContentScanner.Finding.LAUNCH_ACTION), PdfActiveContentScanner.scan(file));
    }

    @Test
    void looksLikePdf() {
        byte[] pdf = "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
        assertTrue(PdfActiveContentScanner.looksLikePdf(pdf, pdf.length));
        byte[] prefixed = "garbage before header %PDF-1.4".getBytes(StandardCharsets.US_ASCII);
        assertTrue(PdfActiveContentScanner.looksLikePdf(prefixed, prefixed.length));
        byte[] text = "just some text".getBytes(StandardCharsets.US_ASCII);
        assertFalse(PdfActiveContentScanner.looksLikePdf(text, text.length));
    }
}
