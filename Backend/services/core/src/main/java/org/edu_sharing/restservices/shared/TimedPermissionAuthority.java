package org.edu_sharing.restservices.shared;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An Authority + from + to permission timing
 * Used to prevent duplicate authorities when resolving permissions
 */
@AllArgsConstructor
public class TimedPermissionAuthority {
    @Getter
    @Setter
    private Authority authority;
    @Getter
    @Setter
    private Long from, to;
    @Getter
    @Setter
    private ArrayList<String> permissions;
    public boolean isTimed(){
        return from != null || to != null;
    }

    @Override
    public boolean equals(Object o) {
        TimedPermissionAuthority that = (TimedPermissionAuthority) o;
        if (
                !authority.equals(that.authority) ||
                !ListUtils.isEqualList(permissions, that.permissions)
        ) {
            return false;
        }
        if(!this.isTimed()) {
            return !that.isTimed();
        }
        return that.isTimed();
    }

    @Override
    public int hashCode() {
        return Objects.hash(authority.hashCode(), permissions, from != null, to != null);
    }

    public boolean equalsIgnorePermissions(TimedPermissionAuthority that) {
        if (
                !authority.equals(that.authority)
        ) {
            return false;
        }
        if(!this.isTimed()) {
            return !that.isTimed();
        }
        return that.isTimed();
    }

    public boolean equalsIgnoreFromTo(TimedPermissionAuthority that) {
        return authority.equals(that.authority) &&
                ListUtils.isEqualList(permissions, that.permissions);
    }
}
