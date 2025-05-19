package org.edu_sharing.service.authentication.totp;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OneTimeTokenService {

    // Generate a new TOTP key
    public String generateKey() {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }


    // Validate the TOTP code
    public boolean isValid(String secret, int code) {
        if (secret == null) {
            return true;
        }

        GoogleAuthenticator gAuth = new GoogleAuthenticator(
                new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder().build()
        );
        return gAuth.authorize(secret, code);
    }

    // Generate a QR code URL for Google Authenticator
    public String generateQRUrl(String secret,String username) {
        String url = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                ApplicationInfoList.getHomeRepository().getDomain(),
                username,
                new GoogleAuthenticatorKey.Builder(secret).build());
        try {
            return generateQRBase64(url);
        } catch (Exception e) {
            return null;
        }
    }

    public byte[] generateQRCode(String username, String secret) {
        String url = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                ApplicationInfoList.getHomeRepository().getDomain(),
                username,
                new GoogleAuthenticatorKey.Builder(secret).build());
        try {
            return generateQR(url);
        } catch (Exception e) {
            return null;
        }
    }

    // Generate a QR code image in Base64 format
    public static String generateQRBase64(String qrCodeText) {
        byte[] image = generateQR(qrCodeText);
        if (image == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(image);
    }

    public static byte[] generateQR(String qrCodeText) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hintMap = new HashMap<>();
            hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, 200, 200, hintMap);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            return baos.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("Error generating QR code image: {}", e.getMessage(), e);
            return null;
        }
    }
}
