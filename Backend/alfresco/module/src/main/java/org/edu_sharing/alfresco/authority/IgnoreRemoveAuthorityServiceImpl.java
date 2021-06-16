package org.edu_sharing.alfresco.authority;

import org.alfresco.repo.security.authority.AuthorityServiceImpl;
import org.apache.log4j.Logger;

import java.util.Set;

/**
 * register as bean like authorityService in authority-services-context.xml.
 * overwrite authorityService ref in bean userRegistrySynchronizer default-synchronization-context.xml,
 * to prevent non ldap managed groups removed
 */
public class IgnoreRemoveAuthorityServiceImpl extends AuthorityServiceImpl {

    Logger logger = Logger.getLogger(IgnoreRemoveAuthorityServiceImpl.class);

    @Override
    public void removeAuthority(String parentName, String childName) {
        logger.debug("remove from groups deactivated: " + childName + " from " +parentName+" ");
       /* Set<String> zones = this.getAuthorityZones(parentName);
        if(zones == null){
            logger.warn("no zone found for parent "+parentName);
            return;
        }

        boolean ldapManaged = false;
        for(String zone : zones){
            logger.debug("zone:" + zone +" for parent:"+parentName);
            if(zone.equals("AUTH.EXT.ldap1")){
                ldapManaged = true;
            }
        }

        if(ldapManaged){
            logger.debug("parent "+parentName+" is ldap managed. default handling");
            super.removeAuthority(parentName,childName);
        }else{
            logger.debug("parent "+parentName+" NOT ldap managed. will do nothing");
        }*/
    }
}
