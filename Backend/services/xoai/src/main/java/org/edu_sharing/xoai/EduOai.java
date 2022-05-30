package org.edu_sharing.xoai;

import com.lyncode.xml.exceptions.XmlWriteException;
import org.dspace.xoai.dataprovider.DataProvider;
import org.dspace.xoai.dataprovider.exceptions.OAIException;
import org.dspace.xoai.dataprovider.handlers.results.ListSetsResult;
import org.dspace.xoai.dataprovider.model.Context;
import org.dspace.xoai.dataprovider.model.Set;
import org.dspace.xoai.dataprovider.parameters.OAIRequest;
import org.dspace.xoai.dataprovider.repository.Repository;
import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.dataprovider.repository.SetRepository;
import org.dspace.xoai.model.oaipmh.OAIPMH;
import org.dspace.xoai.services.impl.SimpleResumptionTokenFormat;
import org.dspace.xoai.xml.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EduOai {
    private String metadataPrefix;
    private RepositoryConfiguration configuration;
    private EduItemRepository itemRepository;
    private DataProvider provider;
    public EduOai(RepositoryConfiguration configuration, String metadataPrefix,EduDataHandler handler) throws TransformerConfigurationException {
        this.metadataPrefix = metadataPrefix;
        itemRepository=new EduItemRepository(handler);
        SetRepository setRepository=new SetRepository() {
            @Override
            public boolean supportSets() {
                return true;
            }

            @Override
            public ListSetsResult retrieveSets(int offset, int length) {
                List<Set> results = getSets();
                return new ListSetsResult(false,results);
            }

            private List<Set> getSets() {
                try {
                    return handler.getSets().stream().map(Set::new).collect(Collectors.toList());
                } catch (OAIException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean exists(String setSpec) {
                return getSets().stream().anyMatch((set) -> set.getSpec().equals(setSpec));
            }
        };
        provider=new DataProvider(Context.context().withMetadataFormat(metadataPrefix,TransformerFactory.newInstance().newTransformer()),
                Repository.repository().withSetRepository(setRepository).withItemRepository(itemRepository).withConfiguration(configuration).withResumptionTokenFormatter(new SimpleResumptionTokenFormat()));

    }
    public static String responseToXML(OAIPMH response) throws XMLStreamException, XmlWriteException {
        ByteArrayOutputStream os=new ByteArrayOutputStream();
        XmlWriter writer = new XmlWriter(os);
        response.write(writer);
        writer.close();
        return os.toString();
    }
    public OAIPMH handleRequest(Map<String, List<String>> requests) throws OAIException, XMLStreamException, XmlWriteException {
        return provider.handle(new OAIRequest(requests));
    }
}
