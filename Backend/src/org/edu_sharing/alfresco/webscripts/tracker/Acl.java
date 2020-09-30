package org.edu_sharing.alfresco.webscripts.tracker;

import java.util.List;

public class Acl {
    long aclId;
    boolean inherits;

    List<Ace> aces;

    public long getAclId() {
        return aclId;
    }

    public void setAclId(long aclId) {
        this.aclId = aclId;
    }

    public List<Ace> getAces() {
        return aces;
    }

    public void setAces(List<Ace> aces) {
        this.aces = aces;
    }

    public boolean isInherits() {
        return inherits;
    }

    public void setInherits(boolean inherits) {
        this.inherits = inherits;
    }
}
