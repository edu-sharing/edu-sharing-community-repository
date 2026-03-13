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

        assertEquals("<b>Empty</b>",
                MetadataTemplateRenderer.cleanupText(MetadataWidget.TextEscapingPolicy.htmlRestricted, "<script>alert('BAD'); </script><b onclick=\"alert('BAD')\">Empty</b>"));
        assertEquals("http://a.strange.url/but%20%==noJS.mp3",
                MetadataTemplateRenderer.cleanupText(MetadataWidget.TextEscapingPolicy.all, "http://a.strange.url/but%20%==noJS.mp3"));
    }
}