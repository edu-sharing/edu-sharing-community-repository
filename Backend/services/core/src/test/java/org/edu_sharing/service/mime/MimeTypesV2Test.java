package org.edu_sharing.service.mime;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MimeTypesV2Test {
    @Test
    void testGetNodeType_savedSearch() {
        assertEquals("saved_search", MimeTypesV2.getNodeType(CCConstants.CCM_TYPE_SAVED_SEARCH, new HashMap<>(), Collections.emptyList()));
        assertEquals("saved_search", MimeTypesV2.getNodeType(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_SAVED_SEARCH), new HashMap<>(), Collections.emptyList()));
    }

    @Test
    void testGetNodeType_folder() {
        assertEquals("folder", MimeTypesV2.getNodeType(CCConstants.CCM_TYPE_MAP, new HashMap<>(), Collections.emptyList()));
        assertEquals("folder", MimeTypesV2.getNodeType(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_MAP), new HashMap<>(), Collections.emptyList()));
    }
}
