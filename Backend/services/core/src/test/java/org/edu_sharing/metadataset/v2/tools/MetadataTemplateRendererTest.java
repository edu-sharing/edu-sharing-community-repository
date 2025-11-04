package org.edu_sharing.metadataset.v2.tools;

import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetadataTemplateRendererTest {

    @Test
    void cleanupText() {
        assertEquals("<a target=\"_BLANK\" href=\"https://example.tld\" rel=\"nofollow noopener noreferrer\"></a>",
                MetadataTemplateRenderer.cleanupText(MetadataWidget.TextEscapingPolicy.htmlBasic, "<a target=\"_BLANK\" href=\"https://example.tld\">"));
        assertEquals("<a target=\"_BLANK\" href=\"https://example.tld\" referrerpolicy=\"no-referrer-when-downgrade\" rel=\"nofollow\"></a>",
                MetadataTemplateRenderer.cleanupText(MetadataWidget.TextEscapingPolicy.htmlBasicWithReferrer, "<a target=\"_BLANK\" href=\"https://example.tld\">"));
    }
}