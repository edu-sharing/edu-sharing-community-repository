package org.edu_sharing.rest.notification.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserData {
    private String firstName;
    private String lastName;
    private String mailbox; // compatibility with org.edu_sharing.restservices.shared.Person
}
