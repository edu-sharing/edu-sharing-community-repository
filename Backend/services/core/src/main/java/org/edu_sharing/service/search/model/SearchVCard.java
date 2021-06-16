package org.edu_sharing.service.search.model;

import java.io.Serializable;

public class SearchVCard implements Serializable {
    private String vcard;

    public SearchVCard(String vcard) {
        this.vcard = vcard;
    }

    public String getVcard() {
        return vcard;
    }

    public void setVcard(String vcard) {
        this.vcard = vcard;
    }
}
