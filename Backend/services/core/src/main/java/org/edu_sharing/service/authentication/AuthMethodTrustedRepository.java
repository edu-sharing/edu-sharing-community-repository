/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.service.authentication;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component("ccAuthMethodTrustedRepository")
public class AuthMethodTrustedRepository implements AuthMethodInterface {

	public String authenticate(Map<String, String> params) throws AuthenticationException {
		throw new RuntimeException("old soap method should not be used");
	}

}
