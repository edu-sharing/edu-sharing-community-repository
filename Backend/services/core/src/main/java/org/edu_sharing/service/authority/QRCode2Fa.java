package org.edu_sharing.service.authority;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QRCode2Fa {
    byte[] qrCode;
    String code;
}
