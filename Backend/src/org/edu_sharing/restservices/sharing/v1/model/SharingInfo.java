package org.edu_sharing.restservices.sharing.v1.model;

import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.share.ShareServiceImpl;

public class SharingInfo {
    private boolean passwordMatches;
    private boolean password;
    private Node node;
    public SharingInfo(Share share, Node node, String passwordCheck) {
        this.password=share.getPassword()!=null;
        if(passwordCheck!=null && !passwordCheck.isEmpty()){
            this.passwordMatches=ShareServiceImpl.encryptPassword(passwordCheck).equals(share.getPassword());
        }
        this.node=node;
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
