package org.edu_sharing.repository.server.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DomainUtilsTest {

    @Test
    void getRootDomain() {
        String subdomainString = "https://my-sub.example.com/test";
        Assertions.assertEquals("example.com",  DomainUtils.getRootDomain(subdomainString));

        String wwwString = "http://www.example.com/test";
        Assertions.assertEquals("example.com", DomainUtils.getRootDomain(wwwString));

        String ukString = "http://www.example.co.uk/test";
        Assertions.assertEquals("example.co.uk", DomainUtils.getRootDomain(ukString));

        String nullString = null;
        Assertions.assertNull(DomainUtils.getRootDomain(nullString));

        String lolString = "lol, this is a malformed URL, amirite?!";
        Assertions.assertNull(DomainUtils.getRootDomain(lolString));
    }
}