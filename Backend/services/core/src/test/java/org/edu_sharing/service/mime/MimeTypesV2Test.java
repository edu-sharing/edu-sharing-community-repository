package org.edu_sharing.service.mime;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MimeTypesV2Test {
    private static String mediaType(String nodeType) {
        return MimeTypesV2.getNodeType(
                new MimeTypesV2.NodeInfo(nodeType, new HashMap<>(), Collections.emptyList()),
                MimeTypesV2.Type.Mediatype);
    }

    @Test
    void testGetNodeType_savedSearch() {
        assertEquals("saved_search", mediaType(CCConstants.CCM_TYPE_SAVED_SEARCH));
        assertEquals("saved_search", mediaType(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_SAVED_SEARCH)));
    }

    @Test
    void testGetNodeType_folder() {
        assertEquals("folder", mediaType(CCConstants.CCM_TYPE_MAP));
        assertEquals("folder", mediaType(CCConstants.getValidLocalName(CCConstants.CCM_TYPE_MAP)));
    }
}
