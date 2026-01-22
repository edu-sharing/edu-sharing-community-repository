package org.edu_sharing.restservices.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.restservices.PersonDao;

import java.io.Serializable;

;

@Data
public class UserProfile implements Serializable {

    private String firstName = null;
    private String lastName = null;
    private String email = null;
    private String avatar = null;
    private String primaryAffiliation = null;
    private String about = null;
    @Schema(nullable = true)
    private String[] skills = null;
    private String[] types = null;
    private String VCard;

    public UserProfile() {

    }

    public UserProfile(String firstName, String lastName, String email, String avatarId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        if (avatarId != null) {
            this.avatar = PersonDao.getAvatar(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, avatarId));
        }
    }
    public UserProfile(User user) {
            this(user.getGivenName(), user.getSurname(), user.getEmail(), user.getAvatarNodeId());
        }
    }
