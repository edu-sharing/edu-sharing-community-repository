package org.edu_sharing.service.authentication.totp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OneTimeToken {
    private String username;
    private String secret;
}
