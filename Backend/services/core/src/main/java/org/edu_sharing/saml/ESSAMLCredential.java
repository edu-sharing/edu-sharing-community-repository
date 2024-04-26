package org.edu_sharing.saml;

import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.NameID;
import org.springframework.security.saml.SAMLCredential;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ESSAMLCredential extends SAMLCredential {

    HashMap<String,String[]> esAttributes = new HashMap<>();

    public ESSAMLCredential(NameID nameID, Assertion authenticationAssertion, String remoteEntityID, String localEntityID) {
        this(nameID, authenticationAssertion, remoteEntityID, Collections.<Attribute>emptyList(), localEntityID);
    }

    public ESSAMLCredential(NameID nameID, Assertion authenticationAssertion, String remoteEntityID, List<Attribute> attributes, String localEntityID) {
        this(nameID, authenticationAssertion, remoteEntityID, null, attributes, localEntityID);
    }

    public ESSAMLCredential(NameID nameID, Assertion authenticationAssertion, String remoteEntityID, String relayState, List<Attribute> attributes, String localEntityID) {
        this(nameID, authenticationAssertion, remoteEntityID, relayState, attributes, localEntityID, null);
    }

    public ESSAMLCredential(NameID nameID, Assertion authenticationAssertion, String remoteEntityID, String relayState, List<Attribute> attributes, String localEntityID, Serializable additionalData) {
        super(nameID, authenticationAssertion, remoteEntityID, relayState, attributes, localEntityID, additionalData);
        for(Attribute att : attributes){
            esAttributes.put(att.getName(),this.getAttributeAsStringArray(att.getName()));
        }
    }

    public String[] getEsAttributeAsStringArray(String attName){
        return esAttributes.get(attName);
    }


}
