package org.edu_sharing.saml;

import org.opensaml.saml2.core.*;
import org.springframework.security.saml.SAMLCredential;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ESSAMLCredential extends SAMLCredential {

    HashMap<String,String[]> esAttributes = new HashMap<>();

    List<String> authStatementSessionIndexs = new ArrayList<>();

    ESNameID esNameID;

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

        for (AuthnStatement authnStatement : authenticationAssertion.getAuthnStatements()) {
            authStatementSessionIndexs.add(authnStatement.getSessionIndex());
        }

        esNameID = new ESNameID();
        esNameID.setNameQualifier(nameID.getNameQualifier());
        esNameID.setFormat(nameID.getFormat());
        esNameID.setSPNameQualifier(nameID.getSPNameQualifier());
        esNameID.setValue(nameID.getValue());
        esNameID.setSPProvidedID(nameID.getSPProvidedID());
    }

    public ESNameID getEsNameID() {
        return esNameID;
    }

    public String[] getEsAttributeAsStringArray(String attName){
        return esAttributes.get(attName);
    }

    public List<String> getAuthStatementSessionIndexs() {
        return authStatementSessionIndexs;
    }

    public class ESNameID implements NameIDType{

        String value;
        String nameQualifier;
        String sPNameQualifier;
        String format;
        String sPProvidedID;

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void setValue(String newValue) {
            this.value = newValue;
        }

        @Override
        public String getNameQualifier() {
            return nameQualifier;
        }

        @Override
        public void setNameQualifier(String newNameQualifier) {
            this.nameQualifier = newNameQualifier;
        }

        @Override
        public String getSPNameQualifier() {
            return sPNameQualifier;
        }

        @Override
        public void setSPNameQualifier(String newSPNameQualifier) {
            this.sPNameQualifier = newSPNameQualifier;
        }

        @Override
        public String getFormat() {
            return this.format;
        }

        @Override
        public void setFormat(String newFormat) {
            this.format = newFormat;
        }

        @Override
        public String getSPProvidedID() {
            return sPProvidedID;
        }

        @Override
        public void setSPProvidedID(String newSPProvidedID) {
            this.sPProvidedID = newSPProvidedID;
        }
    }
}
