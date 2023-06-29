package org.edu_sharing.rest.notification.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDataDTO {
    private String firstName;
    private String lastName;
    private String mailbox; // compatibility with org.edu_sharing.restservices.shared.Person
}
