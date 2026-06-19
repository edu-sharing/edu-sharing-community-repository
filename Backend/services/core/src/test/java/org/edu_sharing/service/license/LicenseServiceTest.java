package org.edu_sharing.service.license;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LicenseServiceTest {

    private LicenseService underTest;

    @BeforeEach
    void setUp() {
        underTest = new LicenseService();
    }

    @Test
    void getLicenseUrl() {
        assertEquals(
                "https://creativecommons.org/licenses/by/4.0/deed.de",
                underTest.getLicenseUrl("CC_BY", "", "4.0", "de")
        );
        assertEquals(
                "https://creativecommons.org/licenses/by-sa/4.0/deed.de",
                underTest.getLicenseUrl("CC_BY_SA", "de", "4.0", "de")
        );
        assertEquals(
                "https://creativecommons.org/licenses/by-sa/3.0/deed.de",
                underTest.getLicenseUrl("CC_BY_SA", "", "3.0", "de")
        );
        assertEquals(
                "https://creativecommons.org/licenses/by-sa/3.0/fr/deed.de",
                underTest.getLicenseUrl("CC_BY_SA", "fr", "3.0", "de")
        );
        assertEquals(
                "https://creativecommons.org/licenses/by-sa/3.0/de/deed.de",
                underTest.getLicenseUrl("CC_BY_SA", "de", "3.0", "de")
        );
        assertEquals(
                "https://creativecommons.org/licenses/by-sa/3.0/de/deed.en",
                underTest.getLicenseUrl("CC_BY_SA", "de", "3.0", "en")
        );
    }

    @Test
    void parseLicenseUrl() {
        assertEquals(
                new LicenseService.LicenseUrl("CC_BY", "de", "4.0"),
                underTest.parseLicenseUrl("https://creativecommons.org/licenses/by/4.0/deed.de")
        );
        assertEquals(
                new LicenseService.LicenseUrl("CC_BY_NC_SA", "en", "3.0"),
                underTest.parseLicenseUrl("https://creativecommons.org/licenses/by-nc-sa/3.0/de/deed.en")
        );
        assertEquals(
                new LicenseService.LicenseUrl("CC_0", "de", "1.0"),
                underTest.parseLicenseUrl("https://creativecommons.org/publicdomain/zero/1.0/deed.de")
        );
        assertEquals(
                new LicenseService.LicenseUrl("PDM", "en", "1.0"),
                underTest.parseLicenseUrl("http://creativecommons.org/publicdomain/mark/1.0/deed.en")
        );
        assertEquals(
                new LicenseService.LicenseUrl("EDU_NC_ND", "de", "1.0"),
                underTest.parseLicenseUrl("http://edu-sharing.net/licenses/edu-nc-nd/1.0/de")
        );
        assertEquals(
                new LicenseService.LicenseUrl("CUSTOM", "de", "1.0"),
                underTest.parseLicenseUrl("http://edu-sharing.net/licenses/custom-licence/1.0/de")
        );
        // no deed suffix -> language is null
        assertNull(underTest.parseLicenseUrl("https://creativecommons.org/licenses/by/4.0/").language());
    }

    @Test
    void parseLicenseUrlRoundTrip() {
        LicenseService.LicenseUrl parsed = underTest.parseLicenseUrl(
                underTest.getLicenseUrl("CC_BY_SA", "fr", "3.0", "de"));
        assertEquals(new LicenseService.LicenseUrl("CC_BY_SA", "de", "3.0"), parsed);
    }

    @Test
    void parseLicenseUrlInvalid() {
        assertThrows(IllegalArgumentException.class, () -> underTest.parseLicenseUrl(null));
        assertThrows(IllegalArgumentException.class, () -> underTest.parseLicenseUrl(""));
        assertThrows(IllegalArgumentException.class, () -> underTest.parseLicenseUrl("https://example.org/licenses/by/4.0/"));
        assertThrows(IllegalArgumentException.class, () -> underTest.parseLicenseUrl("https://creativecommons.org/licenses/by-xx/4.0/"));
        assertThrows(IllegalArgumentException.class, () -> underTest.parseLicenseUrl("not a url"));
    }
}