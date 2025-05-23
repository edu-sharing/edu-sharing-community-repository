package org.edu_sharing.service.license;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}