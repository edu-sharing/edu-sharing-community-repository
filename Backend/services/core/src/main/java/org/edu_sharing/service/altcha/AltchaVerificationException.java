package org.edu_sharing.service.altcha;

import lombok.Getter;

/**
 * Thrown if an ALTCHA payload is not accepted. The message intentionally contains no details.
 */
@Getter
public class AltchaVerificationException extends Exception {

    public enum Reason {
        /**
         * missing, invalid, expired or tampered payload (HTTP 403)
         */
        INVALID(403),
        /**
         * payload was already used (HTTP 409)
         */
        REPLAYED(409);

        @Getter
        private final int status;

        Reason(int status) {
            this.status = status;
        }
    }

    private final Reason reason;

    public AltchaVerificationException(Reason reason) {
        super("ALTCHA verification failed");
        this.reason = reason;
    }
}
