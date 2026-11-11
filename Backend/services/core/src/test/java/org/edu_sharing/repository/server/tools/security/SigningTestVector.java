package org.edu_sharing.repository.server.tools.security;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.mockito.Mockito.when;

/**
 * The test vector for the user assertion of a request. It is the same in the B-API ({@code TestVector.kt} of the
 * bildungs-api): a key pair made for the tests, fixed inputs and one fixed JWS. If the format changes, change both.
 */
public final class SigningTestVector {
    private SigningTestVector() {
    }

    /** A key pair made for this test only. */
    public static final String PRIVATE_KEY = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCs9HgR1++y+inUxPnnoZIaHhpg"
            + "YBy6d9mVADWKJOTdwog1LTBzi/eoCXZo4XSPdGjl6ABtGcozj2d60YpS2TRePYIjwRSSfQCG3N+p"
            + "RIeQVlrYxleNADQoQvE4hkriL+NPtph4KSYVC0e9nxOlyorkYdO06xNoRtPyHJpxFbRfOIgTk64B"
            + "c24GtUnfkB2UP7fq4C3wagUTM7EZx9DFyniGDedSD093EYVimczrme9QRF6wOeSXd57Yax0wtnCJ"
            + "aZ2YUWJVZktXNOALt0hD/W+rYNuEzBvRGET6UME+UySCgugQEAuPjfFReQmySTWY3XOeyoEaT7zj"
            + "Gg3xlfSv1h43AgMBAAECggEAJ9Zxt6505FVSAFPUukFRjVmEutE4nMzRXcw8CNNhsiDGTfVYNS02"
            + "wMpZL2RK/GYUaWxseLTZidHMxJHyWg30zYY+bSzip7FKR0Yl8G+8CAeUCl15Oh2jZeixdlMceYgN"
            + "REWrTfYwcuVzw14aYPRL1Loh0Qjaogi6dgiXnFejAdF7Op5AAq5smb9851hEKIdTDrEz3LK9HFY9"
            + "IyXWMXWE64+OkQmJDp2fJfObb67T9zuM66LzF+RrMs8WM19wdpY3WEb+Ea16JiyySfbFXzrlCvTU"
            + "h9WZw57eR7AHRWek/ri5fSb7k5oz2iBFfXd/fEIkbq7fDVkaKGI1HSfoMSfuwQKBgQDThR0Eoeq2"
            + "hFiCC99e67KWKU/ecqeaviJ9TxpIIwW6T7FMYAnUT9/c8a4cljY4YLjsiO0+2Eh4TTO19ON2nNNq"
            + "hUH89mMhVfhptSh/Oewrk3shKi7z5sxz7k3n7FplGFehss60s2bTcyG21gCRWSvga4/8X4UmWCxe"
            + "KwzYnh1zwQKBgQDRU0LVaoQS57tFmG8QTRFh/tBp2MQ93YT4pnIwgUNVUB9U0dCrisw/vKMMUjJh"
            + "FM+o4EUl1qnNg+kuXQbhki9yq2/RN5L6DTHWl8v99XvJk+j1c8UXi9wCdIOMlU6/ONLxIcgUqzJg"
            + "xgMC9J6j1Dc7Urc6jpyAsQ/3+sBFN+sv9wKBgQCOXvq5M4MYdtxcLEGdUmhSSlxIDkRiE7iDP8sg"
            + "4vHoBdwA3GPBVEIKJWJcO38jU3clPsTxmAKvdsdnof24yLZE7I0OB4mR1pvuCtHHUOV2Lz6hhBM2"
            + "eBMFtVEa2iu+ADR70i7v0hFUIhp/Rmv7aVnvJHc2F9U3igHTIIb1BvgsAQKBgQCJTpFfhrA96m3I"
            + "WBY6XiNR4aD6XWtUnXM025G5yYDOfHEg5UR40eHqySU21tAfKFSTFRGmoWne2nFhrTnvS0H+eQAu"
            + "/8JUbcVMW2Q/IQDjGDfOkfMt39sgbSdBV0tBWdfcvmkNTcMNAbZGAQnysyjLrBVuvs9gB0VPR9dJ"
            + "UEXgvwKBgQC0bMlJT8c4zY29sywBRYIZQxKwVwU0+FuYUleV9mD3wBfPnp4tWF4FDRYYEN1IMilY"
            + "/kRqfWc9he9p2o2KpQT5Fm8w53cvgC2jk4QJYFWziyIvoW73iFh6Vzh/cBCoCrRbK5wHqKhcC0Qo"
            + "QbuC+Dn/SvNZUeeUw5S4842ou5WhZQ==";

    public static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArPR4Edfvsvop1MT556GSGh4aYGAcunfZ"
            + "lQA1iiTk3cKINS0wc4v3qAl2aOF0j3Ro5egAbRnKM49netGKUtk0Xj2CI8EUkn0AhtzfqUSHkFZa"
            + "2MZXjQA0KELxOIZK4i/jT7aYeCkmFQtHvZ8TpcqK5GHTtOsTaEbT8hyacRW0XziIE5OuAXNuBrVJ"
            + "35AdlD+36uAt8GoFEzOxGcfQxcp4hg3nUg9PdxGFYpnM65nvUEResDnkl3ee2GsdMLZwiWmdmFFi"
            + "VWZLVzTgC7dIQ/1vq2DbhMwb0RhE+lDBPlMkgoLoEBALj43xUXkJskk1mN1znsqBGk+84xoN8ZX0"
            + "r9YeNwIDAQAB";

    public static final String APP_ID = "repo-app-1";
    public static final String AUDIENCE = "bildungs-api";
    public static final String API_KEY = "test-key-0123456789";
    public static final String METHOD = "POST";
    public static final String PATH = "/api/v1/edu-sharing/chat/completion?a=b%20c";
    /** Seconds; the assertion is valid from IAT for 30 seconds. */
    public static final long IAT = 1793883800L;
    public static final String JTI = "AAAAAAAAAAAAAAAAAAAAAA";
    public static final String USER = "Jürgen+Müller@ex";
    /** RFC 7638 thumbprint (SHA-256) of the public key above. */
    public static final String KID = "_AsUI_lBKBlwpjc6XhIArFZ18ngbVqeyCnS9KQoPWPs";
    /** Base64url of the SHA-256 of API_KEY. */
    public static final String ATH = "CwJtyvUt0tfos3ftdRgr_MZUl6_4hbOjfdG-GnXKOU0";

    /**
     * The fixed assertion (RS512). RSASSA-PKCS1-v1_5 is deterministic and the header and claims are written in a fixed order, so the signer gives exactly
     * these bytes; the B-API verifies the same string in its TestVector.kt.
     */
    public static final String TOKEN = "eyJhbGciOiJSUzUxMiIsInR5cCI6ImJhcGktdXNlcitqd3QiLCJraWQiOiJfQXNVSV9sQktCbHdw"
            + "amM2WGhJQXJGWjE4bmdiVnFleUNuUzlLUW9QV1BzIn0.eyJpc3MiOiJyZXBvLWFwcC0xIiwiYXVk"
            + "IjoiYmlsZHVuZ3MtYXBpIiwic3ViIjoiSsO8cmdlbitNw7xsbGVyQGV4IiwiaWF0IjoxNzkzODgz"
            + "ODAwLCJleHAiOjE3OTM4ODM4MzAsImp0aSI6IkFBQUFBQUFBQUFBQUFBQUFBQUFBQUEiLCJodG0i"
            + "OiJQT1NUIiwiaHBxIjoiL2FwaS92MS9lZHUtc2hhcmluZy9jaGF0L2NvbXBsZXRpb24_YT1iJTIw"
            + "YyIsImF0aCI6IkN3SnR5dlV0MHRmb3MzZnRkUmdyX01aVWw2XzRoYk9qZmRHLUduWEtPVTAifQ.O"
            + "2l4fH9_mgyHzc8kfHmMemi6Llof1fcyDvcgKSDFOuNkeBFP2NSGC_DYr7HQi4pPmZR41-"
            + "ve5yVgOnGroqJJ2lo9WaY4j7JEOoQAiNNaAEEtJfRm-mnP75NshjpSE_4Y0S_DfK95_NPIkqKbda"
            + "LREWrkAh5ja31EFXdoNDwORLYeoMhSLgcC5DM492HOaO4uJi1UV4C1cDbpn5N3E79KChJ1CcmBD2"
            + "jrg3z-fd6nNmLKs0XVGKCuOGo_TBZHgDsOZ0zvAs8MWSInbMccSjjsx9pkkjDZVpSOs-"
            + "Rk_CDKwACiiJAedQhPwn7nSaN8tda4EVPMOqFiNNLHnsyf-rZHrA";

    /** The PEM the repository keeps its private key in. */
    public static String privateKeyPem() {
        return "-----BEGIN PRIVATE KEY-----\n" + PRIVATE_KEY + "\n-----END PRIVATE KEY-----";
    }

    public static PublicKey publicKey() throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY)));
    }

    public static boolean verify(String data, String signatureB64, String algorithm) throws Exception {
        Signature signature = Signature.getInstance(algorithm);
        signature.initVerify(publicKey());
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        return signature.verify(Base64.getDecoder().decode(signatureB64));
    }

    /** A signer that has the home application of the vector (app id and key) instead of a running repository. */
    public static HomeRepositorySigner signer(String algorithm) {
        ApplicationInfo home = Mockito.mock(ApplicationInfo.class);
        when(home.getAppId()).thenReturn(APP_ID);
        when(home.getPrivateKey()).thenReturn(privateKeyPem());
        return signerOf(home, algorithm);
    }

    public static HomeRepositorySigner signerOf(ApplicationInfo home, String algorithm) {
        return new HomeRepositorySigner(algorithm) {
            @Override
            protected ApplicationInfo homeRepository() {
                return home;
            }
        };
    }
}
