package org.edu_sharing.alfresco.policy;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.tika.mime.MediaType;
import org.edu_sharing.alfresco.action.RessourceInfoExecuter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;

class NodeCustomizationPoliciesTest {

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
