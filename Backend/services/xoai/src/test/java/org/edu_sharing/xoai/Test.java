//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.edu_sharing.xoai;

import com.lyncode.xml.exceptions.XmlWriteException;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import org.dspace.xoai.dataprovider.model.ItemIdentifier;
import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Granularity;
import org.dspace.xoai.model.oaipmh.OAIPMH;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import java.util.*;

public class Test {
    public Test() {
    }

    public static void main(String[] args) throws OAIException, XMLStreamException, XmlWriteException, TransformerConfigurationException {
        RepositoryConfiguration configuration = new RepositoryConfiguration().
                withMaxListIdentifiers(100).withMaxListSets(100).withMaxListRecords(100).withMaxListSets(100).
                withAdminEmail("foo@bar.de").withBaseUrl("http://test.de").withDeleteMethod(DeletedRecord.NO).
                withGranularity(Granularity.Second).
                withEarliestDate(new Date(0)).withRepositoryName("test");

        EduOai oai = new EduOai(configuration, "lom", new EduDataHandler() {
            public ListItemIdentifiersResult getIdentifiers(int offset, int length, String set) {
                ArrayList<ItemIdentifier> list = new ArrayList<>();

                for(int i = 0; i < 100; ++i) {
                    list.add(new EduItemIdentifier("node" + i, new Date()));
                }

                return new ListItemIdentifiersResult(true, list);
            }

            public EduItem getItem(String identifier) {
                return new EduItem(identifier, "<lom></lom>");
            }

            @Override
            public ListItemIdentifiersResult getIdentifiersFrom(int var1, int var2, Date var3, String set) {
                return null;
            }

            @Override
            public ListItemIdentifiersResult getIdentifiersUntil(int var1, int var2, Date var3, String set) {
                return null;
            }

            @Override
            public ListItemIdentifiersResult getIdentifiersFromUntil(int var1, int var2, Date var3, Date var4, String set) {
                return null;
            }

        });
        OAIPMH identify = oai.handleRequest(identifyRequest());
        System.out.println(EduOai.responseToXML(identify));
        OAIPMH result = oai.handleRequest(listRequest());
        System.out.println(EduOai.responseToXML(result));
        oai.handleRequest(listResumptionRequest("MToxMDB8MjpkZWZhdWx0fDM6fDQ6fDU6bG9t"));
    }

    private static Map<String, List<String>> identifyRequest() {
        Map<String, List<String>> requests = new HashMap<>();
        requests.put("verb", Collections.singletonList("Identify"));
        return requests;
    }

    private static Map<String, List<String>> listRequest() {
        Map<String, List<String>> requests = new HashMap<>();
        requests.put("verb", Collections.singletonList("ListIdentifiers"));
        requests.put("metadataPrefix", Collections.singletonList("lom"));
        requests.put("set", Collections.singletonList("default"));
        return requests;
    }

    private static Map<String, List<String>> listResumptionRequest(String resumptionToken) {
        Map<String, List<String>> requests = new HashMap<>();
        requests.put("verb", Collections.singletonList("ListIdentifiers"));
        requests.put("resumptionToken", Collections.singletonList(resumptionToken));
        return requests;
    }

    private static Map<String, List<String>> listRecordRequest() {
        Map<String, List<String>> requests = new HashMap<>();
        requests.put("verb", Collections.singletonList("ListRecords"));
        requests.put("metadataPrefix", Collections.singletonList("lom"));
        return requests;
    }

    private static Map<String, List<String>> getRecordRequest() {
        Map<String, List<String>> requests = new HashMap<>();
        requests.put("verb", Collections.singletonList("GetRecord"));
        requests.put("metadataPrefix", Collections.singletonList("lom"));
        requests.put("identifier", Collections.singletonList("my-node-id"));
        return requests;
    }
}
