package org.edu_sharing.repository.server.tools;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

class VCardConverterTest {

    @Test
    void getNameForVCardTest() {
        HashMap<String, Object> data = new HashMap<>() {{
            put(CCConstants.VCARD_TITLE, "");
            put(CCConstants.VCARD_GIVENNAME, "A");
            put(CCConstants.VCARD_SURNAME, "B");
        }};
        Assertions.assertEquals("A B", VCardConverter.getNameForVCard("", data));
        data = new HashMap<>() {{
            put(CCConstants.VCARD_TITLE, "Mr.");
            put(CCConstants.VCARD_GIVENNAME, "A");
            put(CCConstants.VCARD_SURNAME, "B");
        }};
        Assertions.assertEquals("Mr. A B", VCardConverter.getNameForVCard("", data));
        data = new HashMap<>() {{
            put(CCConstants.VCARD_ORG, "Org");
            put(CCConstants.VCARD_TITLE, "Mr.");
            put(CCConstants.VCARD_GIVENNAME, "A");
            put(CCConstants.VCARD_SURNAME, "B");
        }};
        Assertions.assertEquals("Mr. A B", VCardConverter.getNameForVCard("", data));
        data = new HashMap<>() {{
            put(CCConstants.VCARD_ORG, "Org");
        }};
        Assertions.assertEquals("Org", VCardConverter.getNameForVCard("", data));
        data = new HashMap<>() {{
            put(CCConstants.VCARD_T_FN, "FN Name");
            put(CCConstants.VCARD_ORG, "Org");
        }};
        Assertions.assertEquals("FN Name", VCardConverter.getNameForVCard("", data));
    }

}