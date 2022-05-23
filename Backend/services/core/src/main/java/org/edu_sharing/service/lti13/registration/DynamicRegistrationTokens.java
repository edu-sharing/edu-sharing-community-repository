package org.edu_sharing.service.lti13.registration;

import java.util.ArrayList;

public class DynamicRegistrationTokens {
    ArrayList<DynamicRegistrationToken> registrationLinks = new ArrayList<>();

    public ArrayList<DynamicRegistrationToken> getRegistrationLinks() {
        return registrationLinks;
    }

    public DynamicRegistrationToken get(String tokenStr){
        DynamicRegistrationToken token = new DynamicRegistrationToken();
        token.setToken(tokenStr);
        int tokenIdx = registrationLinks.indexOf(token);
        if(tokenIdx != -1) {
            return registrationLinks.get(tokenIdx);
        }
        return null;
    }

    public void update(DynamicRegistrationToken token){
        int tokenIdx = registrationLinks.indexOf(token);
        if(tokenIdx != -1) {
            registrationLinks.set(tokenIdx,token);
        }else{
            registrationLinks.add(token);
        }
    }
}
