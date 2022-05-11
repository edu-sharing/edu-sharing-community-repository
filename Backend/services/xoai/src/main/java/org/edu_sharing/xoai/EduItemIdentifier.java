package org.edu_sharing.xoai;

import org.dspace.xoai.dataprovider.model.ItemIdentifier;
import org.dspace.xoai.dataprovider.model.Set;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EduItemIdentifier implements ItemIdentifier {
    private final String identifier;
    private final Date modifiedDate;

    public EduItemIdentifier(String identifier,Date modifiedDate) {
        this.identifier=identifier;
        this.modifiedDate=modifiedDate;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public Date getDatestamp() {
        return modifiedDate;
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
