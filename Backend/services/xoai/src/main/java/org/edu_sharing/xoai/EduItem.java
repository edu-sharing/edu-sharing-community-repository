package org.edu_sharing.xoai;

import com.lyncode.xml.XmlWritable;
import com.lyncode.xml.exceptions.XmlWriteException;
import org.dspace.xoai.dataprovider.model.Item;
import org.dspace.xoai.dataprovider.model.Set;
import org.dspace.xoai.model.oaipmh.About;
import org.dspace.xoai.model.oaipmh.Metadata;
import org.dspace.xoai.xml.EchoElement;
import org.dspace.xoai.xml.XmlWriter;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EduItem implements Item {
    private final String identifier;
    private Metadata metadata;

    public EduItem(String identifier,String lomMetadata) {
        this.identifier = identifier;
        this.metadata=new Metadata(lomMetadata);
    }

    @Override
    public List<About> getAbout() {
        return null;
    }

    @Override
    public Metadata getMetadata() {
        return this.metadata;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public Date getDatestamp() {
        return new Date();
    }

    @Override
    public List<Set> getSets() {
        return new ArrayList<>();
    }

    @Override
    public boolean isDeleted() {
        return false;
    }
}
