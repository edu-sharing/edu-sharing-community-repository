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
package org.edu_sharing.repository.server.tools;

import java.net.URLEncoder;
import java.util.Base64;

import net.sourceforge.blowfishj.BinConverter;
import net.sourceforge.blowfishj.BlowfishCBC;

public class BlowFish {
	
	static String iv = "initvect";
	public static byte[] encrypt(String toEncrypt, String key){
		
		if(toEncrypt == null || key == null) return null;
		int nrOfEightByteBlocks = toEncrypt.length() / 8;
		int modulo = toEncrypt.length() % 8;
		int spacesToAdd = 0;
		if(modulo != 0) spacesToAdd = 8 - modulo;
		
		for(int i = 0; i < spacesToAdd; i++){
			toEncrypt +=" ";
		}
		
		BlowfishCBC blowfish = new BlowfishCBC(key.getBytes(),0,key.getBytes().length,iv.getBytes(),0);
		
		byte[] orginalBytes = toEncrypt.getBytes();
		blowfish.encrypt(orginalBytes, 0, orginalBytes, 0, orginalBytes.length);
		
		return orginalBytes;
	}
	
	/**
	 * Attention the "toDecrypt" bytes will be changed(decrypted)
	 * 
	 * 
	 * @param toDecrypt
	 * @param key
	 * @return a new String constructed with the decrypted bytes
	 */
	public static String decrypt(byte[] toDecrypt, String key){
		BlowfishCBC blowfish = new BlowfishCBC(key.getBytes(),0,key.getBytes().length,iv.getBytes(),0);
		blowfish.setCBCIV(iv.getBytes());
		
		byte[] orginalBytes = toDecrypt;
		blowfish.decrypt(orginalBytes, 0, orginalBytes, 0, orginalBytes.length);
		return new String(orginalBytes);
	}
	
	public static String getBase64Encoded(byte[] bytes){
		if(bytes != null) {
			
			return Base64.getEncoder().encodeToString(bytes);
		}
		else return null;
	}
	
	public static byte[] getBase64Decoded(String todecode){
		try{
		if(todecode != null) {
			return Base64.getDecoder().decode(todecode);
		}
		}catch(Exception e){
			e.printStackTrace();
			
		}
		return null;
	}
	
	public static void main(String[] args) {
		
		String test = "test@metacoon.net";
		String key = "thetestkey";
		String iv = "initvect";
		BlowFish.setIv(iv);
		
		byte[] encrypted = encrypt(test, key);
		
		String byteHexString = BinConverter.bytesToHexStr(encrypted); 
		System.out.println(
				"> CBC encrypted: " + byteHexString +" bytes:"+encrypted + " size:"+encrypted.length);
		
	//	String decrypted = decrypt(encrypted,key);
	//	System.out.println("> decrypted:"+decrypted+"");
		
		
		String b64enc = getBase64Encoded(encrypted);
		System.out.println("> CBC encrypted B64: "+ b64enc+"  (encrypted:"+BinConverter.bytesToHexStr(encrypted)+")" + " bytes:"+encrypted+ " size:"+encrypted.length);
		
		String b64dec = new String(getBase64Decoded(b64enc));
		System.out.println("> b64dec:"+b64dec);
		
		byte[] mybytes = new byte[encrypted.length];
		BinConverter.hexStrToBytes(byteHexString,mybytes, 0, 0,mybytes.length);
		
		
		String original = decrypt(mybytes,key);
		System.out.println("> original:"+original);
		
		/**
		 * TEST2
		 */
		
		String nochnTest = getBase64Encoded(encrypt(test, key));
		System.out.println(">> b64enc:"+nochnTest);
		
		System.out.println(">> b64dec:"+new String(getBase64Decoded(nochnTest)));
		
		System.out.println(">> decrypt:"+BlowFish.decrypt(getBase64Decoded(nochnTest), key));
	}

	/**
	 * encrypts, encodes Base64 and URLEncodes
	 * 
	 * @param toEncrypt
	 * @param iv
	 * @param key
	 * @return
	 */
	public static String encryptForUrl(String toEncrypt, String iv, String key){
		String encrypted = null;
		BlowFish.setIv(iv);
		encrypted = BlowFish.getBase64Encoded(BlowFish.encrypt(toEncrypt, key));
		encrypted = URLEncoder.encode(encrypted);
		return encrypted;
	}
	
	
	public static String getIv() {
		return iv;
	}


	public static void setIv(String iv) {
		BlowFish.iv = iv;
	}
	
}
