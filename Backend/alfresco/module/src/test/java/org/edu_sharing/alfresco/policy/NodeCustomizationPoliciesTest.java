package org.edu_sharing.alfresco.policy;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.FileContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.tika.mime.MediaType;
import org.edu_sharing.alfresco.action.RessourceInfoExecuter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;

class NodeCustomizationPoliciesTest {

    private static final String PPTX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    /**
     * Builds a minimal but real OOXML package that Tika's OPCPackageDetector (POI based) recognizes as
     * a pptx: a Content_Types override for the presentation main part plus a core document relationship
     * pointing at it - Tika/POI do not need the part's actual XML content to be schema-valid for detection.
     * When paddingBytes > 0 an additional entry with (barely compressible) pseudo random bytes is added so
     * the resulting file exceeds Tika's DefaultZipContainerDetector#markLimit (16 MB) on disk - this is
     * what forces Tika to spool zip based detection to a temp file when only a stream is available.
     */
    private static Path createFakePptx(Path dir, String name, int paddingBytes) throws IOException {
        Path zipPath = dir.resolve(name);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zos.write((
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/ppt/presentation.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml\"/>" +
                    "</Types>"
            ).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("_rels/.rels"));
            zos.write((
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"ppt/presentation.xml\"/>" +
                    "</Relationships>"
            ).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("ppt/presentation.xml"));
            zos.write("<p:presentation xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\"/>".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            if (paddingBytes > 0) {
                zos.putNextEntry(new ZipEntry("ppt/media/padding.bin"));
                Random random = new Random(42);
                byte[] buffer = new byte[64 * 1024];
                int written = 0;
                while (written < paddingBytes) {
                    random.nextBytes(buffer);
                    int chunk = Math.min(buffer.length, paddingBytes - written);
                    zos.write(buffer, 0, chunk);
                    written += chunk;
                }
                zos.closeEntry();
            }
        }
        return zipPath;
    }

    private static long countTikaTempFiles() {
        File[] files = new File(System.getProperty("java.io.tmpdir")).listFiles((dir, name) -> name.startsWith("apache-tika"));
        return files == null ? 0 : files.length;
    }

    @Test
    void verifyMimetypeByMagicBytes() throws UnsupportedEncodingException {
        Map<String, byte[]> LIST = new HashMap<>() {{
            put("image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
            put("text/plain", "TEST".getBytes(StandardCharsets.UTF_8));
            put("application/xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a></a>".getBytes(StandardCharsets.UTF_8));
            //put("text/xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a></a>".getBytes(StandardCharsets.UTF_8));
            put("application/zip", new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x05, (byte) 0x06});
        }};

        LIST.forEach((mimetype, magic) -> {
            ContentReader contentReader =
                    Mockito.mock(ContentReader.class);
            Mockito.when(contentReader.exists()).thenReturn(true);
            Mockito.when(contentReader.getContentInputStream()).thenReturn(
                    new ByteArrayInputStream(magic)
            );
            String filename = UUID.randomUUID() + "." + mimetype.split("/")[1];
            Mockito.when(contentReader.getMimetype()).thenReturn(mimetype);
            Map<String, List<String>> allowList = new HashMap<>() {{
                put(mimetype, Collections.singletonList(mimetype.split("/")[1]));
            }};
            NodeCustomizationPolicies.verifyMimetype(
                    contentReader,
                    filename,
                    allowList,
                    false);
            NodeCustomizationPolicies.verifyMimetype(
                    contentReader,
                    null,
                    allowList,
                    false);
            Map<String, List<String>> allowListWrongMimetype = new HashMap<String, List<String>>() {{
                put("test/sample", Collections.singletonList(mimetype.split("/")[1]));
            }};
            Mockito.when(contentReader.getContentInputStream()).thenReturn(
                    new ByteArrayInputStream(magic)
            );
            assertThrowsExactly(NodeMimetypeValidationException.class, () -> NodeCustomizationPolicies.verifyMimetype(
                    contentReader,
                    filename,
                    allowListWrongMimetype,
                    false));
            Map<String, List<String>> allowListWrongFileExtension = new HashMap<>() {{
                put(mimetype, Collections.singletonList("wrong"));
            }};
            Mockito.when(contentReader.getContentInputStream()).thenReturn(
                    new ByteArrayInputStream(magic)
            );
            assertThrows(NodeFileExtensionValidationException.class, () -> NodeCustomizationPolicies.verifyMimetype(
                    contentReader,
                    filename,
                    allowListWrongFileExtension,
                    false));
        });
    }
    @Test
    void verifyMimetypeUnknown() {
        ContentReader contentReader =
                Mockito.mock(ContentReader.class);
        Mockito.when(contentReader.exists()).thenReturn(true);
        Mockito.when(contentReader.getMimetype()).thenReturn("image/jpeg");
        Mockito.when(contentReader.getContentInputStream()).thenReturn(
                new ByteArrayInputStream(new byte[]{})
        );
        assertThrows(NodeMimetypeUnknownValidationException.class,() -> NodeCustomizationPolicies.verifyMimetype(
                contentReader,
                "test.dummy",
                new HashMap<>(),
                false));

        assertDoesNotThrow(() -> NodeCustomizationPolicies.verifyMimetype(
                contentReader,
                "test.dummy",
                new HashMap<>() {{
                    put(MediaType.OCTET_STREAM.toString(), Collections.singletonList("dummy"));
                }},
                true));
    }

    @Test
    void getMediaTypeDetectsJupyterNotebookByExtension() throws Exception {
        // A Jupyter notebook is valid JSON; the .ipynb extension must yield the dedicated mimetype.
        byte[] notebook = "{\"cells\":[],\"nbformat\":4,\"nbformat_minor\":5}".getBytes(StandardCharsets.UTF_8);
        MediaType ipynb = NodeCustomizationPolicies.getMediaType("notebook.ipynb", new ByteArrayInputStream(notebook));
        assertEquals("application/x-ipynb+json", ipynb.toString());

        // Case-insensitive extension match.
        MediaType upper = NodeCustomizationPolicies.getMediaType("NOTEBOOK.IPYNB", new ByteArrayInputStream(notebook));
        assertEquals("application/x-ipynb+json", upper.toString());

        // Plain JSON without the extension must still be detected as application/json by Tika.
        MediaType json = NodeCustomizationPolicies.getMediaType("data.json", new ByteArrayInputStream(notebook));
        assertEquals("application/json", json.toString());
    }

    @Test
    void getMediaTypeFromPathDetectsJupyterNotebookByExtension(@TempDir Path tempDir) throws Exception {
        // the filename based .ipynb override must win in the Path overload too, not just for streams.
        Path notebook = tempDir.resolve("notebook.ipynb");
        Files.write(notebook, "{\"cells\":[],\"nbformat\":4,\"nbformat_minor\":5}".getBytes(StandardCharsets.UTF_8));
        MediaType ipynb = NodeCustomizationPolicies.getMediaType("notebook.ipynb", notebook);
        assertEquals("application/x-ipynb+json", ipynb.toString());
    }

    @Test
    void getMediaTypeFromPathDetectsOoxml(@TempDir Path tempDir) throws Exception {
        // File based detection (used for FileContentReader backed content, i.e. the default file content
        // store) must precisely recognize office formats via Tika's real zip/OPCPackage based detector.
        Path pptx = createFakePptx(tempDir, "slides.pptx", 0);
        MediaType mediaType = NodeCustomizationPolicies.getMediaType("slides.pptx", pptx);
        assertEquals(PPTX_MEDIA_TYPE, mediaType.toString());
    }

    @Test
    void getMediaTypeDoesNotLeakTempFilesForLargeZipStreams(@TempDir Path tempDir) throws Exception {
        // Regression test for the leak this fix addresses: for content > Tika's markLimit (16 MB), the
        // DefaultZipContainerDetector spools the stream to its own "apache-tika-*" temp file to run real
        // zip detection on it. That temp file must be deleted again once we close the TikaInputStream.
        Path pptx = createFakePptx(tempDir, "large.pptx", 17 * 1024 * 1024);
        assertTrue(Files.size(pptx) > 16 * 1024 * 1024, "fixture must exceed Tika's markLimit on disk");

        long before = countTikaTempFiles();
        MediaType mediaType;
        try (InputStream in = Files.newInputStream(pptx)) {
            mediaType = NodeCustomizationPolicies.getMediaType("large.pptx", in);
        }
        assertEquals(PPTX_MEDIA_TYPE, mediaType.toString());
        assertEquals(before, countTikaTempFiles(), "no apache-tika-* temp file must be left behind");
    }

    @Test
    void getMediaTypeDetectsFromByteArray() throws Exception {
        // used by ImageTool, which loads the whole (small) image into memory first
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        MediaType mediaType = NodeCustomizationPolicies.getMediaType("photo.jpg", jpeg);
        assertEquals("image/jpeg", mediaType.toString());
    }

    @Test
    void getMediaTypeClosesInputStream() throws Exception {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        InputStream spy = Mockito.spy(new ByteArrayInputStream(jpeg));
        NodeCustomizationPolicies.getMediaType("test.jpg", spy);
        Mockito.verify(spy, Mockito.atLeastOnce()).close();
    }

    @Test
    void getMediaTypeUsesStoreFileForFileContentReader(@TempDir Path tempDir) throws Exception {
        // The fast path: a FileContentReader (the default file content store) must be detected directly
        // off its store file - no copy, and getContentInputStream() must never be consulted.
        Path pptx = createFakePptx(tempDir, "slides.pptx", 0);
        FileContentReader reader = Mockito.mock(FileContentReader.class);
        Mockito.when(reader.exists()).thenReturn(true);
        Mockito.when(reader.getFile()).thenReturn(pptx.toFile());

        MediaType mediaType = NodeCustomizationPolicies.resolveMediaType("slides.pptx", reader);

        assertEquals(PPTX_MEDIA_TYPE, mediaType.toString());
        Mockito.verify(reader, Mockito.never()).getContentInputStream();
    }

    @Test
    void getMediaTypeSpoolsLargeNonFileReaderOnce(@TempDir Path tempDir) throws Exception {
        // A non file based reader (e.g. a remote content store) above the spool threshold must be copied
        // to a temp file exactly once, detected there, and the temp file must be cleaned up afterwards.
        Path pptx = createFakePptx(tempDir, "slides.pptx", 0);
        ContentReader reader = Mockito.mock(ContentReader.class);
        Mockito.when(reader.exists()).thenReturn(true);
        Mockito.when(reader.getSize()).thenReturn(20L * 1024 * 1024);
        Mockito.when(reader.getContentInputStream()).thenReturn(Files.newInputStream(pptx));

        long before = countTikaTempFiles();
        MediaType mediaType = NodeCustomizationPolicies.resolveMediaType("slides.pptx", reader);

        assertEquals(PPTX_MEDIA_TYPE, mediaType.toString());
        Mockito.verify(reader, Mockito.times(1)).getContentInputStream();
        assertEquals(before, countTikaTempFiles(), "no apache-tika-* temp file must be left behind");
        File[] leftoverSpoolFiles = new File(System.getProperty("java.io.tmpdir"))
                .listFiles((dir, name) -> name.startsWith("edu_mimedetect_"));
        assertTrue(leftoverSpoolFiles == null || leftoverSpoolFiles.length == 0, "our own spool temp file must be deleted again");
    }

    @Test
    void checkGithubDataTest() {
        checkGithubUri("https://github.com/edu-sharing/edu-sharing-community-repository", RessourceInfoExecuter.CCM_RESSOURCETYPE_GIT_DEFAULT);
        checkGithubUri("https://github.com/edu-sharing/edu-sharing-community-repository/tree/release/6.0", RessourceInfoExecuter.CCM_RESSOURCETYPE_GIT_DEFAULT);
        checkGithubUri("https://github.com/edu-sharing/edu-sharing-community-repository/tree/a27f86e5e923779a17c31a838f4a992d6e05188b", RessourceInfoExecuter.CCM_RESSOURCETYPE_GIT_DEFAULT);
        checkGithubUri("https://github.com/KI-Campus/AMALEA", RessourceInfoExecuter.CCM_RESSOURCETYPE_GIT_JUPYTER_BINDER);
        checkGithubUri("https://github.com/KI-Campus/AMALEA/blob/master/Woche%201/1%20Erste%20Schritte.ipynb", RessourceInfoExecuter.CCM_RESSOURCETYPE_GIT_JUPYTER_BINDER);
        checkGithubUri("https://github.com/KI-Campus/AMALEA/blob/data/Woche%201/1%20Erste%20Schritte.ipynb", RessourceInfoExecuter.CCM_RESSOURCETYPE_GIT_JUPYTER_BINDER);
    }

    void checkGithubUri(String uri, String resourceType) {
        NodeCustomizationPolicies underTest = new NodeCustomizationPolicies();

        NodeService mockedNodeService = Mockito.mock(NodeService.class);
        underTest.setNodeService(mockedNodeService);
        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, UUID.randomUUID().toString());
        underTest.checkGithubData(nodeRef, uri);
        Mockito.verify(mockedNodeService, times(1)).setProperty(nodeRef, QName.createQName(RessourceInfoExecuter.CCM_PROP_IO_RESSOURCETYPE), resourceType);
    }
}
