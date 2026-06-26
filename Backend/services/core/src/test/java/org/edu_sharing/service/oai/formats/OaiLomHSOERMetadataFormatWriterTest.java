package org.edu_sharing.service.oai.formats;

import org.alfresco.repo.version.NodeServiceImpl;
import org.edu_sharing.alfresco.service.guest.GuestServiceImpl;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.search.Suggestion;
import org.edu_sharing.service.util.PropertyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OaiLomHSOERMetadataFormatWriterTest {
    private OaiLomHSOERMetadataFormatWriter underTest;
    @Mock
    NodeServiceImpl nodeService;
    @Mock
    GuestServiceImpl guestService;
    @Mock
    PropertyMapper mapper;
    @BeforeEach
    void setUp() {
        underTest = new OaiLomHSOERMetadataFormatWriter(nodeService, guestService, null);
    }
    private Element testElement(Context context) throws ParserConfigurationException {
        return context.createElement("test");
    }
    private String elementToString(Element element) throws Exception {
        StringWriter writer = new StringWriter();
        Transformer t = TransformerFactory.newInstance().newTransformer();

        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.transform(new DOMSource(element), new StreamResult(writer));
        return writer.toString().trim();
    }
    @Test
    void createKeyword() throws Exception {
        // getMds() and the keyword suggestion lookup hit the repository/search DB,
        // so we stub the static calls instead of letting them run.
        try (MockedStatic<MetadataHelper> metadataHelper = mockStatic(MetadataHelper.class);
             MockedStatic<MetadataSearchHelper> searchHelper = mockStatic(MetadataSearchHelper.class)) {

            MetadataSet defaultMds = mock(MetadataSet.class);
            metadataHelper.when(MetadataHelper::getLocalDefaultMetadataset).thenReturn(defaultMds);

            // return one suggestion per requested keyword value, with a translation + GND url
            searchHelper.when(() -> MetadataSearchHelper.getSuggestions(
                            any(), any(), any(), any(), any(), any(), any()))
                    .thenAnswer(invocation -> {
                        String value = invocation.getArgument(4);
                        Suggestion suggestion = new Suggestion();
                        suggestion.setTranslation("label " + value);
                        suggestion.setUrl("https://gnd/" + value);
                        return List.of(suggestion);
                    });

            Context context = getContext();
            Element element = testElement(context);
            underTest.createKeyword(context, element);
            assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><test/>", elementToString(element));


            List<String> generalKeywords = Arrays.asList("general 1", "general 2");
            when(mapper.getStringList(CCConstants.LOM_PROP_GENERAL_KEYWORD))
                    .thenReturn(generalKeywords);

            List<String> classificationKeyword = Arrays.asList("classification_id1", "classification_id2");
            when(mapper.getStringList(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD))
                    .thenReturn(classificationKeyword);

            context = getContext();
            element = testElement(context);
            underTest.createKeyword(context, element);
            assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><test>\n" +
                    "    <keyword>\n" +
                    "        <langstring xml:lang=\"en\">general 1</langstring>\n" +
                    "    </keyword>\n" +
                    "    <keyword>\n" +
                    "        <langstring xml:lang=\"en\">general 2</langstring>\n" +
                    "    </keyword>\n" +
                    "    <keyword>\n" +
                    "        <langstring xml:lang=\"de\">label classification_id1</langstring>\n" +
                    "        <langstring xml:lang=\"en\">label classification_id1</langstring>\n" +
                    "        <langstring xml:lang=\"x-none\">https://gnd/classification_id1</langstring>\n" +
                    "        <id>classification_id1</id>\n" +
                    "    </keyword>\n" +
                    "    <keyword>\n" +
                    "        <langstring xml:lang=\"de\">label classification_id2</langstring>\n" +
                    "        <langstring xml:lang=\"en\">label classification_id2</langstring>\n" +
                    "        <langstring xml:lang=\"x-none\">https://gnd/classification_id2</langstring>\n" +
                    "        <id>classification_id2</id>\n" +
                    "    </keyword>\n" +
                    "</test>", elementToString(element));
        }
    }

    private Context getContext() throws ParserConfigurationException {
        when(mapper.getString(any())).thenReturn("en-US");
        return new Context(this.mapper, mock(MetadataSet.class), "node-uuid", false);

    }
}