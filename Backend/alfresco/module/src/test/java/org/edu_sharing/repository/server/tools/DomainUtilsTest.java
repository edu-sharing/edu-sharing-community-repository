package org.edu_sharing.repository.server.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DomainUtilsTest {

    @Test
    void getRootDomain() {
        String subdomainString = "my-sub.example.com";
        Assertions.assertEquals("example.com",  DomainUtils.getRootDomain(subdomainString));

        String wwwString = "www.example.com";
        Assertions.assertEquals("example.com", DomainUtils.getRootDomain(wwwString));

        String ukString = "www.example.co.uk";
        Assertions.assertEquals("example.co.uk", DomainUtils.getRootDomain(ukString));

        String nullString = null;
        Assertions.assertNull(DomainUtils.getRootDomain(nullString));

        String lolString = "lol, this is a malformed URL, amirite?!";
        Assertions.assertThrows(IllegalArgumentException.class, () -> DomainUtils.getRootDomain(lolString));
    }
}