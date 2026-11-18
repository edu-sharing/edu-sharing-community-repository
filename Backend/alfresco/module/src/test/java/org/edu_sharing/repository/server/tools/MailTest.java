package org.edu_sharing.repository.server.tools;

import org.apache.commons.mail2.jakarta.HtmlEmail;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Sends a mail against a minimal in-process SMTP server to verify that the jakarta mail libraries on the
 * classpath are compatible with each other.
 * Regression: cxf 4.1.x pulled in angus-mail 2.x (needs jakarta.mail-api >= 2.1) next to com.sun.mail:jakarta.mail 2.0.2
 * -> NoClassDefFoundError jakarta/mail/util/LineInputStream on every mail
 */
class MailTest {

    @Test
    void smtpTransportWorksWithShippedMailLibraries() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread smtp = new Thread(() -> fakeSmtp(server));
            smtp.setDaemon(true);
            smtp.start();

            HtmlEmail email = new HtmlEmail();
            email.setHostName("localhost");
            email.setSmtpPort(server.getLocalPort());
            email.setSocketConnectionTimeout(java.time.Duration.ofSeconds(5));
            email.setSocketTimeout(java.time.Duration.ofSeconds(5));
            email.setFrom("from@example.org");
            email.addTo("to@example.org");
            email.setSubject("test");
            email.setHtmlMsg("<p>test</p>");

            assertDoesNotThrow(email::send);
        }
    }

    private static void fakeSmtp(ServerSocket server) {
        try (Socket s = server.accept();
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.US_ASCII), true)) {
            out.print("220 localhost\r\n");
            out.flush();
            String line;
            boolean data = false;
            while ((line = in.readLine()) != null) {
                if (data) {
                    if (line.equals(".")) {
                        data = false;
                        out.print("250 OK\r\n");
                        out.flush();
                    }
                    continue;
                }
                if (line.startsWith("DATA")) {
                    out.print("354 go ahead\r\n");
                } else if (line.startsWith("QUIT")) {
                    out.print("221 bye\r\n");
                    out.flush();
                    return;
                } else {
                    out.print("250 OK\r\n");
                }
                out.flush();
            }
        } catch (IOException ignored) {
            // client closed connection
        }
    }
}
