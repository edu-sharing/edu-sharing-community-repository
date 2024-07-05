package org.edu_sharing.alfresco.service.guest;

import lombok.Data;

import java.util.List;

@Data
public class GuestConfig {
    private boolean enabled;
    private String username;
    private String password;
    private List<String> groups;
}
