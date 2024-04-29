package org.edu_sharing.saml;

import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.SessionIndex;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.context.SAMLMessageContext;

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
}
