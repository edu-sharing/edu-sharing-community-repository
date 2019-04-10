package org.edu_sharing.repository.server.tools.security;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.edu_sharing.repository.client.tools.CCConstants;

public class Encryption {

	//ie "RSA"
	String algorithm;
	
	public Encryption(String algorithm) {
		this.algorithm = algorithm;
	}
	
	public  String decrypt(byte[] buffer, Key key) {
	    try{
	    	return new String(decryptAsBytes(buffer,key), "UTF8");
	    }catch(UnsupportedEncodingException e){
	    	e.printStackTrace();
	    	return null;
	    }
	    
	}
	
	public byte[] decryptAsBytes(byte[] buffer, Key key) {
		 try {
		        Cipher rsa;
		        rsa = Cipher.getInstance(algorithm);
		        		        
		        rsa.init(Cipher.DECRYPT_MODE, key);
		        byte[] bytes = rsa.doFinal(buffer);
		        return bytes;
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
		return null;
	}
	
	public byte[] encrypt(String text, Key key) throws Exception {
	     return encrypt(text.getBytes(),key);
	}
	
	public byte[] encrypt(byte[] text, Key key) throws Exception {
	     Cipher cipher = Cipher.getInstance(algorithm);
	     cipher.init(Cipher.ENCRYPT_MODE, key);
	     byte[] encrypted = cipher.doFinal(text);
	     return encrypted;
	}
	
	public PrivateKey getPemPrivateKey(File f)
			throws IOException, GeneralSecurityException {
		
		FileInputStream fis = new FileInputStream(f);
		DataInputStream dis = new DataInputStream(fis);
		byte[] keyBytes = new byte[(int) f.length()];
		dis.readFully(keyBytes);
		dis.close();

		String b64privKeyString = new String(keyBytes);
		
		return getPemPrivateKey(b64privKeyString);
		
	}
	
	public PrivateKey getPemPrivateKey(String  b64privKeyString)
			throws GeneralSecurityException {
		
		String privKeyPEM = b64privKeyString.replace("-----BEGIN PRIVATE KEY-----\n", "");
		privKeyPEM = privKeyPEM.replace("-----END PRIVATE KEY-----", "");

		Base64 b64 = new Base64();

		byte[] decoded = b64.decode(privKeyPEM.getBytes());

		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
		KeyFactory kf = KeyFactory.getInstance(algorithm);
		return kf.generatePrivate(spec);
		
	}
	
	public PublicKey getPemPublicKey(File f)
			throws IOException, GeneralSecurityException  {
		FileInputStream fis = new FileInputStream(f);
		DataInputStream dis = new DataInputStream(fis);
		byte[] keyBytes = new byte[(int) f.length()];
		dis.readFully(keyBytes);
		dis.close();

		String b64pubKeyString = new String(keyBytes);
		return getPemPublicKey(b64pubKeyString);
	}
	
	public PublicKey getPemPublicKey(String  b64pubKeyString)
			throws GeneralSecurityException {
		String publicKeyPEM = b64pubKeyString.replace("-----BEGIN PUBLIC KEY-----\n", "");
		publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");
		

		Base64 b64 = new Base64();
		byte[] decoded = b64.decode(publicKeyPEM.getBytes());

		X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
		KeyFactory kf = KeyFactory.getInstance(algorithm);
		return kf.generatePublic(spec);
	}

	public static void main(String[] args){
		//testRSA();
		//testAESwithRSAKeyEncr();	
		
		test2();
	}
	
	private static void testRSA(){
		
		Encryption e = new Encryption("RSA");
		
		String pubkey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArFrFvCdsEwDuxaEgRwxBjBUhzc91hfaAQ52CWxI9Z73QNcI8kE5J+u25uEQeuxZgxyMciyFpu0R8XTpssumSYgM+VaUp4eeo5gkwwJyJ08FRyoOFM9uPdC9rnWBuYJLzFG9PIXuZKpfnIyVXiPVR3tW1jPzQDqcyucBq/qYLduXDsWdEngIMDCzbmIZlRCPmzfhWF8tgSK3VpUIp/sEv78zQMXqikmIXxFTBxEf0OBlKAwpYNIvVl6Yd8fwcuokOKSgjIKTVmVDpoN7lOm9CkNsJe4agmz6gRCxp6Cf/3dSAnRYndUXkf4jVuCMwSaVZkIdGub3wHRYfxqHhIqh0vQIDAQAB";
		String privkey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCsWsW8J2wTAO7FoSBHDEGMFSHNz3WF9oBDnYJbEj1nvdA1wjyQTkn67bm4RB67FmDHIxyLIWm7RHxdOmyy6ZJiAz5VpSnh56jmCTDAnInTwVHKg4Uz2490L2udYG5gkvMUb08he5kql+cjJVeI9VHe1bWM/NAOpzK5wGr+pgt25cOxZ0SeAgwMLNuYhmVEI+bN+FYXy2BIrdWlQin+wS/vzNAxeqKSYhfEVMHER/Q4GUoDClg0i9WXph3x/By6iQ4pKCMgpNWZUOmg3uU6b0KQ2wl7hqCbPqBELGnoJ//d1ICdFid1ReR/iNW4IzBJpVmQh0a5vfAdFh/GoeEiqHS9AgMBAAECggEBAJMKEd22aC5CQnIaRzY1z4nZ0XALCIFUThLhQn+9rONffU0Pu6mFEPpha2s3ulvz+HaLjuNnixN3lv3WRTvRps+1Yq2gbX3LrD0sJAp+wdsE92llEPOv4c/GUPH/fcsoMvcBCypOjtpWo5tJVUeDuuf3vJ5eZIXLwKvV67H6KJeygDFxs5zIbl689qAprQOdSYCh+qcl2DdBjQj6VgfBUAVw5putzrfCOsViCxlMKLc/OXKknRClnkgk6Nh0JuubHPn0FA/zQbMdXG6/J0fciIYRlSiMv1681y45Mg+pvPZazCq7Fanv5ixwX7XxifHJsFusNWN7PkNRmOPPY9H3zEECgYEA4HhOwRDZZ+nNqZ+lZ7tqtNg92Bq7ynWW2mFpv1uv1NtU/LpHOXTwO1W+EqsYikvEVOp+UMHeHoXd6gOTv3qZ0NMy188vbIaYLE4Y3vnd0hItyPRxWYCDVst1xXq15NxMEip+kRNUPHoLXWLXzy48fttLgYcdHRyF5MrgbMSnV60CgYEAxJB0D2yO5GCXIa9l/xI5ou9qtPbmdlTvC3GZQy6OdfVE5Zlm35mkdA2SPri+JUuxXcGsyCtHmeUhL3qnR04cLB6Z9BuCyChL1SCl06vTPP2kztHKKhnGD6IOtAcfKfktiq/JDvxP/H88QmrfbFGeWevJQbCKK4K1i7/6ugj/c1ECgYBVGmfJXOkAM61spQFh/bVsERwJZcFS9hgWfZ5JFarpSarEEp/UUdiOtmhUzZeFP0qVYz42wfnDmr9S7L5Ed2f/u0LofT2eqLzi900IpLUL4yaGrN6Yr1ecPZFOKMLznO4eaQwJVsm2EBNk+YeWnhIDioLq/d303o6I+r3xnvFPFQKBgBD+dCpJNmlkOGy/H1BYIQeHL3CJ0A729NvGbHu0cLieBeDbzFsUi3VFHxmxGy9aRfFXJzr/EuepOZ9DOWcex6zA0IqcPkhTw4qfkfak6lRhkXgYByRwqgD7/81suDMGyfsti3JyOfAYh/suBjb1L32Stwmo26k7WbrFVEX1NTChAoGBANdgbZVW5G75STrjyEq+kyFPvS6MRWXmSxixUQ/pkexxo33n61cBRchVm2tSLBPXZox9c2j5gOcEcwWHuS/paGfzA9Hw8IN4g2RKOxumji2io/K8sF8epvFj0oyskaAq2LqXhuqkidUAmFs9v76az7im02qo0AKQTsOpfTOrvcZ6";
		try{
			
			String testString = "ticket=TICKET_ticket12345&u=testuser&t="+System.currentTimeMillis() +"&n=node_id";
			String encrypted = java.util.Base64.getEncoder().encodeToString(e.encrypt(testString, e.getPemPublicKey(pubkey)));
			System.out.println("encrypted:" + encrypted +" \\n length:" + encrypted.length());
			
			PrivateKey privKey = e.getPemPrivateKey(privkey);
			byte[] signature = new Signing().sign(privKey, encrypted, CCConstants.SECURITY_SIGN_ALGORITHM);
			String signatureStr = java.util.Base64.getEncoder().encodeToString(signature);
			
			
			String url = "enc=" + URLEncoder.encode(encrypted) + "&sig="+URLEncoder.encode(signatureStr);
			
			System.out.println("url:" + url);
			
			String decrypted = e.decrypt(java.util.Base64.getDecoder().decode(encrypted.getBytes()), e.getPemPrivateKey(privkey));
			System.out.println("decrypted:" + decrypted);
			
			System.out.println(new Signing().verify(e.getPemPublicKey(pubkey), signature, encrypted, CCConstants.SECURITY_SIGN_ALGORITHM));
		
		
			String etest="nYBw4q0SaimGE5Furj6Gru86bD58PIsUO%2Ff7r4ldZmoyBDD29iT0rq0x2RmiMnTidLU9DhHUPokkvFX26UDWCIBOFBBmN9bt4as6TOPd2BJ%2FY1eHId4YB4GHqmiW68936FE4GMwoOtOT69zBuuH0MHPZbxEdQeAIJg2HF6g6eGiEefk4w8dSpwQhmW9Qp62p8g0eBZnwPAva8l6fj8Pq9KBy1DB07Q2px9tiy3OYxtc0i6beyqa9vp1cj3GeG4GYXF6%2B7zDBEVFUmMwSmnJvoPXZmkjikD5bQcBENF40jc2QtK642WoFPAc0QV5f%2B7OGMhEnTC5briucMtXBnnMqDg%3D%3D";
			etest = URLDecoder.decode(etest);
			byte[]  b = java.util.Base64.getDecoder().decode(etest);
			System.out.println(e.decrypt(b, privKey));
		
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	private static void test2() {
		String pubkey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArFrFvCdsEwDuxaEgRwxBjBUhzc91hfaAQ52CWxI9Z73QNcI8kE5J+u25uEQeuxZgxyMciyFpu0R8XTpssumSYgM+VaUp4eeo5gkwwJyJ08FRyoOFM9uPdC9rnWBuYJLzFG9PIXuZKpfnIyVXiPVR3tW1jPzQDqcyucBq/qYLduXDsWdEngIMDCzbmIZlRCPmzfhWF8tgSK3VpUIp/sEv78zQMXqikmIXxFTBxEf0OBlKAwpYNIvVl6Yd8fwcuokOKSgjIKTVmVDpoN7lOm9CkNsJe4agmz6gRCxp6Cf/3dSAnRYndUXkf4jVuCMwSaVZkIdGub3wHRYfxqHhIqh0vQIDAQAB";
		String privkey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCsWsW8J2wTAO7FoSBHDEGMFSHNz3WF9oBDnYJbEj1nvdA1wjyQTkn67bm4RB67FmDHIxyLIWm7RHxdOmyy6ZJiAz5VpSnh56jmCTDAnInTwVHKg4Uz2490L2udYG5gkvMUb08he5kql+cjJVeI9VHe1bWM/NAOpzK5wGr+pgt25cOxZ0SeAgwMLNuYhmVEI+bN+FYXy2BIrdWlQin+wS/vzNAxeqKSYhfEVMHER/Q4GUoDClg0i9WXph3x/By6iQ4pKCMgpNWZUOmg3uU6b0KQ2wl7hqCbPqBELGnoJ//d1ICdFid1ReR/iNW4IzBJpVmQh0a5vfAdFh/GoeEiqHS9AgMBAAECggEBAJMKEd22aC5CQnIaRzY1z4nZ0XALCIFUThLhQn+9rONffU0Pu6mFEPpha2s3ulvz+HaLjuNnixN3lv3WRTvRps+1Yq2gbX3LrD0sJAp+wdsE92llEPOv4c/GUPH/fcsoMvcBCypOjtpWo5tJVUeDuuf3vJ5eZIXLwKvV67H6KJeygDFxs5zIbl689qAprQOdSYCh+qcl2DdBjQj6VgfBUAVw5putzrfCOsViCxlMKLc/OXKknRClnkgk6Nh0JuubHPn0FA/zQbMdXG6/J0fciIYRlSiMv1681y45Mg+pvPZazCq7Fanv5ixwX7XxifHJsFusNWN7PkNRmOPPY9H3zEECgYEA4HhOwRDZZ+nNqZ+lZ7tqtNg92Bq7ynWW2mFpv1uv1NtU/LpHOXTwO1W+EqsYikvEVOp+UMHeHoXd6gOTv3qZ0NMy188vbIaYLE4Y3vnd0hItyPRxWYCDVst1xXq15NxMEip+kRNUPHoLXWLXzy48fttLgYcdHRyF5MrgbMSnV60CgYEAxJB0D2yO5GCXIa9l/xI5ou9qtPbmdlTvC3GZQy6OdfVE5Zlm35mkdA2SPri+JUuxXcGsyCtHmeUhL3qnR04cLB6Z9BuCyChL1SCl06vTPP2kztHKKhnGD6IOtAcfKfktiq/JDvxP/H88QmrfbFGeWevJQbCKK4K1i7/6ugj/c1ECgYBVGmfJXOkAM61spQFh/bVsERwJZcFS9hgWfZ5JFarpSarEEp/UUdiOtmhUzZeFP0qVYz42wfnDmr9S7L5Ed2f/u0LofT2eqLzi900IpLUL4yaGrN6Yr1ecPZFOKMLznO4eaQwJVsm2EBNk+YeWnhIDioLq/d303o6I+r3xnvFPFQKBgBD+dCpJNmlkOGy/H1BYIQeHL3CJ0A729NvGbHu0cLieBeDbzFsUi3VFHxmxGy9aRfFXJzr/EuepOZ9DOWcex6zA0IqcPkhTw4qfkfak6lRhkXgYByRwqgD7/81suDMGyfsti3JyOfAYh/suBjb1L32Stwmo26k7WbrFVEX1NTChAoGBANdgbZVW5G75STrjyEq+kyFPvS6MRWXmSxixUQ/pkexxo33n61cBRchVm2tSLBPXZox9c2j5gOcEcwWHuS/paGfzA9Hw8IN4g2RKOxumji2io/K8sF8epvFj0oyskaAq2LqXhuqkidUAmFs9v76az7im02qo0AKQTsOpfTOrvcZ6";
		
		String testString = "Test1234";
		
		try {
			Encryption e = new Encryption("RSA");
			PublicKey pk = e.getPemPublicKey(pubkey);
			/*byte[] encrypted = e.encrypt(testString, pk);
			System.out.println("Base64 encrypted:" + java.util.Base64.getEncoder().encodeToString(encrypted));
			*/
			
			byte[] plainBytes = testString.getBytes();
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, pk);
			byte[] encryptedBytes = cipher.doFinal(plainBytes);
			String encryptedUsername = java.util.Base64.getEncoder().encodeToString(encryptedBytes);
			encryptedUsername = URLEncoder.encode(encryptedUsername, "UTF-8");
			
			
			
			
			PrivateKey privKey = e.getPemPrivateKey(privkey);
			System.out.println("decrypted: " + e.decrypt(encryptedBytes, privKey));
			
		} catch (GeneralSecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
	}
	
	private static void testAESwithRSAKeyEncr(){
		
		try{
			KeyGenerator keygen = KeyGenerator.getInstance("AES");
			keygen.init(128);
			SecretKey aesKey = keygen.generateKey();
			
			Encryption e = new Encryption("AES");
			byte[] encrypted = e.encrypt("Test!", aesKey);
			
			System.out.println("Base64 encrypted:" + java.util.Base64.getEncoder().encodeToString(encrypted));
			System.out.println(e.decrypt(encrypted, aesKey));
			
			/**
			 * prepare key for transport
			 */
			String secretKeyEncoded = java.util.Base64.getEncoder().encodeToString(aesKey.getEncoded());
			Encryption eRSA = new Encryption("RSA");
			String pubkey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArFrFvCdsEwDuxaEgRwxBjBUhzc91hfaAQ52CWxI9Z73QNcI8kE5J+u25uEQeuxZgxyMciyFpu0R8XTpssumSYgM+VaUp4eeo5gkwwJyJ08FRyoOFM9uPdC9rnWBuYJLzFG9PIXuZKpfnIyVXiPVR3tW1jPzQDqcyucBq/qYLduXDsWdEngIMDCzbmIZlRCPmzfhWF8tgSK3VpUIp/sEv78zQMXqikmIXxFTBxEf0OBlKAwpYNIvVl6Yd8fwcuokOKSgjIKTVmVDpoN7lOm9CkNsJe4agmz6gRCxp6Cf/3dSAnRYndUXkf4jVuCMwSaVZkIdGub3wHRYfxqHhIqh0vQIDAQAB";
			String aesKeyEncrypted = java.util.Base64.getEncoder().encodeToString(eRSA.encrypt(secretKeyEncoded, eRSA.getPemPublicKey(pubkey)));
			
			System.out.println("aesKeyEncrypted:" + aesKeyEncrypted);
			
			/**
			 * at target server:
			 */
			String privkey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCsWsW8J2wTAO7FoSBHDEGMFSHNz3WF9oBDnYJbEj1nvdA1wjyQTkn67bm4RB67FmDHIxyLIWm7RHxdOmyy6ZJiAz5VpSnh56jmCTDAnInTwVHKg4Uz2490L2udYG5gkvMUb08he5kql+cjJVeI9VHe1bWM/NAOpzK5wGr+pgt25cOxZ0SeAgwMLNuYhmVEI+bN+FYXy2BIrdWlQin+wS/vzNAxeqKSYhfEVMHER/Q4GUoDClg0i9WXph3x/By6iQ4pKCMgpNWZUOmg3uU6b0KQ2wl7hqCbPqBELGnoJ//d1ICdFid1ReR/iNW4IzBJpVmQh0a5vfAdFh/GoeEiqHS9AgMBAAECggEBAJMKEd22aC5CQnIaRzY1z4nZ0XALCIFUThLhQn+9rONffU0Pu6mFEPpha2s3ulvz+HaLjuNnixN3lv3WRTvRps+1Yq2gbX3LrD0sJAp+wdsE92llEPOv4c/GUPH/fcsoMvcBCypOjtpWo5tJVUeDuuf3vJ5eZIXLwKvV67H6KJeygDFxs5zIbl689qAprQOdSYCh+qcl2DdBjQj6VgfBUAVw5putzrfCOsViCxlMKLc/OXKknRClnkgk6Nh0JuubHPn0FA/zQbMdXG6/J0fciIYRlSiMv1681y45Mg+pvPZazCq7Fanv5ixwX7XxifHJsFusNWN7PkNRmOPPY9H3zEECgYEA4HhOwRDZZ+nNqZ+lZ7tqtNg92Bq7ynWW2mFpv1uv1NtU/LpHOXTwO1W+EqsYikvEVOp+UMHeHoXd6gOTv3qZ0NMy188vbIaYLE4Y3vnd0hItyPRxWYCDVst1xXq15NxMEip+kRNUPHoLXWLXzy48fttLgYcdHRyF5MrgbMSnV60CgYEAxJB0D2yO5GCXIa9l/xI5ou9qtPbmdlTvC3GZQy6OdfVE5Zlm35mkdA2SPri+JUuxXcGsyCtHmeUhL3qnR04cLB6Z9BuCyChL1SCl06vTPP2kztHKKhnGD6IOtAcfKfktiq/JDvxP/H88QmrfbFGeWevJQbCKK4K1i7/6ugj/c1ECgYBVGmfJXOkAM61spQFh/bVsERwJZcFS9hgWfZ5JFarpSarEEp/UUdiOtmhUzZeFP0qVYz42wfnDmr9S7L5Ed2f/u0LofT2eqLzi900IpLUL4yaGrN6Yr1ecPZFOKMLznO4eaQwJVsm2EBNk+YeWnhIDioLq/d303o6I+r3xnvFPFQKBgBD+dCpJNmlkOGy/H1BYIQeHL3CJ0A729NvGbHu0cLieBeDbzFsUi3VFHxmxGy9aRfFXJzr/EuepOZ9DOWcex6zA0IqcPkhTw4qfkfak6lRhkXgYByRwqgD7/81suDMGyfsti3JyOfAYh/suBjb1L32Stwmo26k7WbrFVEX1NTChAoGBANdgbZVW5G75STrjyEq+kyFPvS6MRWXmSxixUQ/pkexxo33n61cBRchVm2tSLBPXZox9c2j5gOcEcwWHuS/paGfzA9Hw8IN4g2RKOxumji2io/K8sF8epvFj0oyskaAq2LqXhuqkidUAmFs9v76az7im02qo0AKQTsOpfTOrvcZ6";
			
			//decrypt aes key
			String aesKeyDecrypted = eRSA.decrypt(java.util.Base64.getDecoder().decode(aesKeyEncrypted), eRSA.getPemPrivateKey(privkey));
			System.out.println("AES Key Base64: " + aesKeyDecrypted);
			byte[] decodedKey = java.util.Base64.getDecoder().decode(aesKeyDecrypted);
			SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
			
			//decrypt aes
			System.out.println("decrypted2:" +  e.decrypt(encrypted, originalKey));
			
			
			System.out.println("-------test without double base64----------");
			byte[] nKeyEncrypted = eRSA.encrypt(aesKey.getEncoded(),  eRSA.getPemPublicKey(pubkey));
			
			byte[] nKeyDecrypted = eRSA.decryptAsBytes(nKeyEncrypted, eRSA.getPemPrivateKey(privkey));
			System.out.println(java.util.Base64.getEncoder().encodeToString(nKeyDecrypted));
			
			SecretKey nOriginalKey = new SecretKeySpec(nKeyDecrypted, 0, nKeyDecrypted.length, "AES");
			System.out.println(e.decrypt(encrypted, nOriginalKey));
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	public void testNoBlow() {
		String privkey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC22p5yzMxFeZvxdjP665skcE65+Y+Iq5Ir8D4Lsu9oeM7DpBflPopm04aPqqL/51C++A+j4PV6rQbwUP2Y/j0klkdte6yt7o/AXisVqzBwlXzLx6AGzJ73xG/40NLruJAOQc3Uhk9axfK2NBuO6dkNEVAoh9VD2/tLyzYWeYXT9VTRRQMzP9e5/luZFGGuiyC46UAjeBXFNA/xSCxKRni1FipGnzlp7W8/Bs/oXTrTEzGhfu+k4/42IGjNIWWBY8E2JDTnDjvQJgRqnL4eD6rtM2RAFAeJ6hjxxhqWOVVDpl5rbXPrSblyqTbISqDvc0pVj+Obvkv8zq5rw0CK4NEBAgMBAAECggEALIB8e5gYRvhX2aZY9EKPGYkzv9Na+Xo4MvVfRF2iIpz0RW0UW7ii2m+0mhZ/YSSc5D1fIRVI6ThfLfGoJqoJm3PvoletKQ+ccMb6WWW2flADdVz6N21pawel8Lpx+uiKNs4RGB4wBfmTHoEEC/4h0ypZheO5JbDOMh4c5c9hoLnz3GiKrjkYMdz7oKHp0F5mefhv7f3FtWS5cVzQ9LR3OQvfOfLLKrIVPEXDzCDmG3rC8pt3Wnd05/qnrRtOO4N/aAVNbK9+J3rq4610/ImQIOISNWH4GBlqXr4ULsVZHhxXi38wJEsXic6qnGpiwygB2a/yfvqnl97EIRY/0bLIAQKBgQD+fT48f137qCF7/TBl/0CO9XCwLUCciHkeTFxqhuGCU4E5QOr6VTazPbpq1MOV5DlyYrbUF519WUu42JVN8uaXH4w97F1gb3aIGRSTa3URgWG5gcqjfHbViInvAG767qHS/ExZXDD/FaL9TitiOFq9eKVF7jTK8A/RF2fTVIjJoQKBgQC38IJQFGlZLnH8049T0+HYN8F2uVv5+Q5LhhGAcmHp/4/dghes30dhJ82ATKGZ8tljGGpHbx7F85jTZdh4Ih9idtGobhOl8nfJH/7c82fWiHMEiWHsgococbwCzdhZsuNDHVupF7pS24dC1T0kwN/H3kSXFRs692ShbsUFqFKLYQKBgD5z19ka3KKMgSMfYEg3ms06eFwTzt5bM3X3AVr3HNJR0c2OG9wxG0pjZKMg8ZqQNnTWus1TukdvqYCETieh62q8yDrODaBD2k/N79m5rjIMkUHoEyWJvtPqPQdVEOcttrLDTa1ETZXb5nLJQyZIdCQ5Pj3qFjSTMx554vXese7hAoGBAJ7mvyy+9WN9pCtt+FvKp0hKom8WIkcIyTilUWlNZxjnFjmdsSY+nUNpRzaW/OQpz3K0HNwpOsR3dIbrbN4bXFGLpAOFVKP/m0u8wEfQXtwOdI5uwJG5EVn3JxylB0o2J+5K5mxleykpTOiHnpKprKjVcxuxuBxwrBZpwvroNqcBAoGBANVvcLjWE61xSJwvqHPaQwHxYYWpSThKfxGOubjrmvHj/e5RFH35Z2M2KOQw+URbe9mq787eOBafj0zfT/f4p8jequQZgvO3QY+L7s9MIQUZSpHVZ6srWBLh8RPKPXwfjerPcee6AC7oyXS5IU94mjmLi2gwzjSN4pefkYcYUnCq";
		String pubkey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAttqecszMRXmb8XYz+uubJHBOufmPiKuSK/A+C7LvaHjOw6QX5T6KZtOGj6qi/+dQvvgPo+D1eq0G8FD9mP49JJZHbXusre6PwF4rFaswcJV8y8egBsye98Rv+NDS67iQDkHN1IZPWsXytjQbjunZDRFQKIfVQ9v7S8s2FnmF0/VU0UUDMz/Xuf5bmRRhrosguOlAI3gVxTQP8UgsSkZ4tRYqRp85ae1vPwbP6F060xMxoX7vpOP+NiBozSFlgWPBNiQ05w470CYEapy+Hg+q7TNkQBQHieoY8cYaljlVQ6Zea21z60m5cqk2yEqg73NKVY/jm75L/M6ua8NAiuDRAQIDAQAB";
		
	}
}

