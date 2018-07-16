package org.edu_sharing.restservices.sharing.v1.model;

import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Person;
import org.edu_sharing.service.share.ShareServiceImpl;

public class SharingInfo {
    private boolean passwordMatches;
    private boolean password;
    private Person invitedBy = null;
    private Node node;
    public SharingInfo(Share share, Node node, String passwordCheck) {
        this.password=share.getPassword()!=null;
        setInvitedBy(convertToPerson(share));
        if(passwordCheck!=null && !passwordCheck.isEmpty()){
            this.passwordMatches=ShareServiceImpl.encryptPassword(passwordCheck).equals(share.getPassword());
        }
        this.node=node;
    }

    private Person convertToPerson(Share share) {
        Person ref = new Person();
        ref.setFirstName((String) share.getProperties()
                .get(CCConstants.NODECREATOR_FIRSTNAME));
        ref.setLastName((String) share.getProperties()
                .get(CCConstants.NODECREATOR_LASTNAME));
        ref.setMailbox((String) share.getProperties().get(CCConstants.NODECREATOR_EMAIL));
        return ref;
    }

    public Person getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(Person invitedBy) {
        this.invitedBy = invitedBy;
    }

    public boolean isPasswordMatches() {
        return passwordMatches;
    }

    public void setPasswordMatches(boolean passwordMatches) {
        this.passwordMatches = passwordMatches;
    }

    public boolean isPassword() {
        return password;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}
