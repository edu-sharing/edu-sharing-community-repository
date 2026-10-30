package org.edu_sharing.service.handleservice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * reads unencrypted PEM private keys (PKCS#8 "PRIVATE KEY" or PKCS#1 "RSA PRIVATE KEY") without additional dependencies
 */
class PemKeyReader {

    private static final String PEM_BEGIN = "-----BEGIN ";
    private static final String PKCS8 = "PRIVATE KEY";
    private static final String PKCS1_RSA = "RSA PRIVATE KEY";

    /**
     * AlgorithmIdentifier for rsaEncryption (1.2.840.113549.1.1.1) incl. the version of the PrivateKeyInfo
     */
    private static final byte[] PKCS8_RSA_HEADER = {
            0x02, 0x01, 0x00,
            0x30, 0x0d, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00
    };

    private PemKeyReader() {
    }

    static boolean isPem(byte[] bytes) {
        return new String(bytes, StandardCharsets.US_ASCII).trim().startsWith(PEM_BEGIN);
    }

    static PrivateKey readPrivateKey(byte[] bytes) throws Exception {
        String pem = new String(bytes, StandardCharsets.US_ASCII);
        if (pem.contains(PEM_BEGIN + PKCS1_RSA)) {
            byte[] pkcs8 = toPkcs8(decode(pem, PKCS1_RSA));
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
        }
        if (pem.contains(PEM_BEGIN + PKCS8)) {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decode(pem, PKCS8));
            for (String algorithm : new String[]{"RSA", "DSA", "EC"}) {
                try {
                    return KeyFactory.getInstance(algorithm).generatePrivate(spec);
                } catch (Exception ignored) {
                    // try next algorithm
                }
            }
            throw new IOException("unsupported key algorithm in pem private key");
        }
        throw new IOException("unsupported pem private key, only unencrypted \"" + PKCS8 + "\" and \"" + PKCS1_RSA + "\" are supported");
    }

    private static byte[] decode(String pem, String type) throws IOException {
        String begin = PEM_BEGIN + type + "-----";
        String end = "-----END " + type + "-----";
        int start = pem.indexOf(begin);
        int stop = pem.indexOf(end);
        if (start < 0 || stop < 0) {
            throw new IOException("invalid pem private key");
        }
        return Base64.getMimeDecoder().decode(pem.substring(start + begin.length(), stop));
    }

    /**
     * wraps a PKCS#1 RSAPrivateKey into a PKCS#8 PrivateKeyInfo
     */
    private static byte[] toPkcs8(byte[] pkcs1) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(PKCS8_RSA_HEADER);
        writeDer(body, 0x04, pkcs1);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        writeDer(result, 0x30, body.toByteArray());
        return result.toByteArray();
    }

    private static void writeDer(ByteArrayOutputStream out, int tag, byte[] content) throws IOException {
        out.write(tag);
        int length = content.length;
        if (length < 0x80) {
            out.write(length);
        } else {
            int bytes = 0;
            for (int l = length; l > 0; l >>= 8) {
                bytes++;
            }
            out.write(0x80 | bytes);
            for (int i = bytes - 1; i >= 0; i--) {
                out.write((length >> (8 * i)) & 0xff);
            }
        }
        out.write(content);
    }
}
