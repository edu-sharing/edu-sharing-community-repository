package org.edu_sharing.saml;

import org.joda.time.DateTime;
import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.encryption.DecryptionException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.SAMLStatusException;
import org.springframework.security.saml.context.SAMLMessageContext;

import static org.springframework.security.saml.util.SAMLUtil.isDateTimeSkewValid;

/**
 * <bean id="logoutprofile" class="org.edu_sharing.saml.ESSingleLogoutProfileImpl"/>
 */
public class ESSingleLogoutProfileImpl extends org.springframework.security.saml.websso.SingleLogoutProfileImpl{

    @Override
    protected LogoutRequest getLogoutRequest(SAMLMessageContext context, SAMLCredential credential, Endpoint bindingService) throws SAMLException, MetadataProviderException {

        SAMLObjectBuilder<LogoutRequest> builder = (SAMLObjectBuilder<LogoutRequest>) builderFactory.getBuilder(LogoutRequest.DEFAULT_ELEMENT_NAME);
        LogoutRequest request = builder.buildObject();
        buildCommonAttributes(context.getLocalEntityId(), request, bindingService);

        // Add session indexes
        SAMLObjectBuilder<SessionIndex> sessionIndexBuilder = (SAMLObjectBuilder<SessionIndex>) builderFactory.getBuilder(SessionIndex.DEFAULT_ELEMENT_NAME);
        for (String sidx : ((ESSAMLCredential)credential).getAuthStatementSessionIndexs()) {
            SessionIndex index = sessionIndexBuilder.buildObject();
            index.setSessionIndex(sidx);
            request.getSessionIndexes().add(index);
        }

        if (request.getSessionIndexes().size() == 0) {
            throw new SAMLException("No session indexes to logout user for were found");
        }

        ESSAMLCredential essamlCredential = (ESSAMLCredential)credential;
        ESSAMLCredential.ESNameID esNameID = essamlCredential.getEsNameID();

        SAMLObjectBuilder<NameID> nameIDBuilder = (SAMLObjectBuilder<NameID>) builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME);
        NameID nameID = nameIDBuilder.buildObject();
        nameID.setFormat(esNameID.getFormat());
        nameID.setNameQualifier(esNameID.getNameQualifier());
        nameID.setSPNameQualifier(esNameID.getSPNameQualifier());
        nameID.setSPProvidedID(esNameID.getSPProvidedID());
        nameID.setValue(esNameID.getValue());
        request.setNameID(nameID);

        return request;

    }

    @Override
    public boolean processLogoutRequest(SAMLMessageContext context, SAMLCredential credential) throws SAMLException {
        if(credential == null){
            return true;
        }
        if(credential.getAuthenticationAssertion() != null) {
            return super.processLogoutRequest(context, credential);
        }

        SAMLObject message = context.getInboundSAMLMessage();

        // Verify type
        if (message == null || !(message instanceof LogoutRequest)) {
            throw new SAMLException("Message is not of a LogoutRequest object type");
        }

        LogoutRequest logoutRequest = (LogoutRequest) message;

        // Make sure request was authenticated if required, authentication is done as part of the binding processing
        if (!context.isInboundSAMLMessageAuthenticated() && context.getLocalExtendedMetadata().isRequireLogoutRequestSigned()) {
            throw new SAMLStatusException(StatusCode.REQUEST_DENIED_URI, "LogoutRequest is required to be signed by the entity policy");
        }

        // Verify destination
        try {
            verifyEndpoint(context.getLocalEntityEndpoint(), logoutRequest.getDestination());
        } catch (SAMLException e) {
            throw new SAMLStatusException(StatusCode.REQUEST_DENIED_URI, "Destination of the LogoutRequest does not match any of the single logout endpoints");
        }

        // Verify issuer
        try {
            if (logoutRequest.getIssuer() != null) {
                Issuer issuer = logoutRequest.getIssuer();
                verifyIssuer(issuer, context);
            }
        } catch (SAMLException e) {
            throw new SAMLStatusException(StatusCode.REQUEST_DENIED_URI, "Issuer of the LogoutRequest is unknown");
        }

        // Verify issue time
        DateTime time = logoutRequest.getIssueInstant();
        if (!isDateTimeSkewValid(getResponseSkew(), time)) {
            throw new SAMLStatusException(StatusCode.REQUESTER_URI, "LogoutRequest issue instant is either too old or with date in the future");
        }

        // Check whether any user is logged in
        if (credential == null) {
            throw new SAMLStatusException(StatusCode.UNKNOWN_PRINCIPAL_URI, "No user is logged in");
        }

        // Find index for which the logout is requested
        boolean indexFound = false;

        //edu customization
        ESSAMLCredential essamlCredential = (credential instanceof ESSAMLCredential) ? (ESSAMLCredential) credential : null;
        if(essamlCredential == null) return false;

        if(essamlCredential.getAuthStatementSessionIndexs() != null){
            for (String statementIndex : ((ESSAMLCredential)credential).getAuthStatementSessionIndexs()) {
                if (statementIndex != null) {
                    for (SessionIndex index : logoutRequest.getSessionIndexes()) {
                        if (statementIndex.equals(index.getSessionIndex())) {
                            indexFound = true;
                        }
                    }
                }
            }
        } else {
            indexFound = true;
        }
        //END edu customization

        // Fail if sessionIndex is not found in any assertion
        if (!indexFound) {

            // Check logout request still valid and store request
            //if (logoutRequest.getNotOnOrAfter() != null) {
            // TODO store request for assertions possibly arriving later
            //}

            throw new SAMLStatusException(StatusCode.REQUESTER_URI, "The SessionIndex was not found");

        }

        try {
            // Fail if NameId doesn't correspond to the currently logged user
            NameID nameID = getNameID(context, logoutRequest);
            if (nameID == null || !equalsNameID(essamlCredential.getEsNameID(), nameID)) {
                throw new SAMLStatusException(StatusCode.UNKNOWN_PRINCIPAL_URI, "The requested NameID is invalid");
            }
        } catch (DecryptionException e) {
            throw new SAMLStatusException(StatusCode.RESPONDER_URI, "The NameID can't be decrypted", e);
        }

        return true;
    }

    private boolean equalsNameID(ESSAMLCredential.ESNameID a, NameID b) {
        boolean equals = !differ(a.getSPProvidedID(), b.getSPProvidedID());
        equals = equals && !differ(a.getValue(), b.getValue());
        equals = equals && !differ(a.getFormat(), b.getFormat());
        equals = equals && !differ(a.getNameQualifier(), b.getNameQualifier());
        equals = equals && !differ(a.getSPNameQualifier(), b.getSPNameQualifier());
        equals = equals && !differ(a.getSPProvidedID(), b.getSPProvidedID());
        return equals;
    }

    private boolean differ(Object a, Object b) {
        if (a == null) {
            return b != null;
        } else {
            return !a.equals(b);
        }
    }
}
