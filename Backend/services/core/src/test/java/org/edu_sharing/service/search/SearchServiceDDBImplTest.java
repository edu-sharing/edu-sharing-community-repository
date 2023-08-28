package org.edu_sharing.service.search;

import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.NodeServiceDDBImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@Disabled
class SearchServiceDDBImplTest {
    private SearchServiceDDBImpl underTestSearch;
    private MockedStatic<ApplicationInfoList> applicationInfoListMockedStatic;
    private MockedStatic<AlfAppContextGate> alfAppContextGateMockedStatic;
    private String apiKey = "";
    private NodeServiceDDBImpl underTestNode;
    private String nodeId1 = "2HJYZQEWG4DK3GK5LGPEGJJYRTOPYM7T";
    private String nodeId2 = "P5HVKHUWVSTR7QXJ3TF5RP47GJU34WOP";

    @BeforeEach
    void setUp() {
        alfAppContextGateMockedStatic = Mockito.mockStatic(AlfAppContextGate.class);
        ApplicationContext applicationContextMock = Mockito.mock(ApplicationContext.class);
        Mockito.when(applicationContextMock.getBean(ArgumentMatchers.anyString())).thenReturn(null);
        alfAppContextGateMockedStatic.when(() -> AlfAppContextGate.getApplicationContext()).thenReturn(applicationContextMock);

        applicationInfoListMockedStatic = Mockito.mockStatic(ApplicationInfoList.class);
        ApplicationInfo dummyInfo = Mockito.mock(ApplicationInfo.class);
        Mockito.when(dummyInfo.getApiKey()).thenReturn(this.apiKey);
        applicationInfoListMockedStatic.when(() -> ApplicationInfoList.getRepositoryInfoById(null)).thenReturn(dummyInfo);
        underTestSearch = new SearchServiceDDBImpl(null);
        underTestNode = new NodeServiceDDBImpl(null);
    }
    @AfterEach
    void teardown() {
        alfAppContextGateMockedStatic.close();
        applicationInfoListMockedStatic.close();
    }
    @Test
    void searchDDB() {
        try {
            SearchResultNodeRef result = underTestSearch.searchDDB(null, null, "/search?query=Mathe&oauth_consumer_key=" + this.apiKey);
            /*result.getData().forEach(r -> {
                try {
                    underTestNode.getProperties(null, null, r.getNodeId());
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });*/
            assertTrue(result.getData().size() > 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    void fetchNode() {
        try {
            HashMap<String, Object> result1 = underTestNode.getProperties(null, null, this.nodeId1);
            assertEquals("Mathes, Alban", result1.get(CCConstants.LOM_PROP_GENERAL_TITLE));
            assertEquals("Hierarchie: Indigenatsgesuche\n" +
                    "Geburtsdatum: 19.07.1860 - Antragsdatum: 1899 - Religion: ev - Indexbegriff Person: Gottschämmer, Barbara - Indexbegriff Person: Mathes, Barbara - Indexbegriff Person: Mathes, Elsa Martha - Indexbegriff Person: Mathes, Rudolf - Indexbegriff Person: Mathes, Wilhelmina - Indexbegriff Ort: Geburtsort: Schönheide - Indexbegriff Ort: Geburtsort: Sachsen - Indexbegriff Sache: Klassifikation C-Bestände: Gesuche 1899", result1.get(CCConstants.LOM_PROP_GENERAL_DESCRIPTION));
            // enable after DESP-738
            // assertEquals(4, ((List)result1.get(CCConstants.LOM_PROP_GENERAL_KEYWORD)).size());


            HashMap<String, Object> result2 = underTestNode.getProperties(null, null, this.nodeId2);
            assertEquals("Mathes, Philipp", result2.get(CCConstants.LOM_PROP_GENERAL_TITLE));
            assertEquals(VCardTool.nameToVCard("Hessisches Hauptstaatsarchiv"), result2.get(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER));
            assertEquals("Hierarchie: Herzogtum Nassau: Amt Wiesbaden >> 30 Freiwillige Gerichtsbarkeit >> 30.16 Wiesbaden\n" +
                    "Identifikation (Fallakte): Aktenart: Nachlassakte - Angaben zur Person: Personenname: Mathes, Philipp - Angaben zur Person: Wohnort: Wiesbaden", result2.get(CCConstants.LOM_PROP_GENERAL_DESCRIPTION));
            // enable after DESP-738
            // assertEquals(1, ((List)result2.get(CCConstants.LOM_PROP_GENERAL_KEYWORD)).size());
            assertEquals("Wed Mar 01 14:00:16 CET 2023", result2.get(CCConstants.CM_PROP_C_MODIFIED).toString());
            assertEquals(CCConstants.COMMON_LICENSE_CC_ZERO, result2.get(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}