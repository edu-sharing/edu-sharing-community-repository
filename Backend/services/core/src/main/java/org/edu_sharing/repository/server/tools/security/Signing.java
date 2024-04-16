package org.edu_sharing.repository.server.tools.security;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.apache.commons.codec.binary.Base64;

public class Signing {

	// CLEANUP local path?
	static String filePrivKey = "C:/Users/rudi/Desktop/hell/generated/priv.key";
	static String filePubKey = "C:/Users/rudi/Desktop/hell/generated/pub.key";
	
	public byte[] sign(PrivateKey privateKey, String data, String algorithm) {
		return sign(privateKey, data.getBytes(), algorithm);
	}

	public byte[] sign(PrivateKey privateKey, byte[] data, String algorithm) {
		try {

			Signature dsa = Signature.getInstance(algorithm);
			dsa.initSign(privateKey);

			dsa.update(data);
			byte[] realSig = dsa.sign();
			return realSig;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public byte[] sign(String b64PrivateKey, String data, String algorithm) throws GeneralSecurityException {
		return this.sign(getPemPrivateKey(b64PrivateKey, "RSA"), data, algorithm);
	}
	
	public boolean verify(PublicKey publicKey, byte[] realSig, String data, String algorithm) {
		try {

			Signature dsa = Signature.getInstance(algorithm);
			dsa.initVerify(publicKey);
			dsa.update(data.getBytes());
			return dsa.verify(realSig);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean verify(String b64publicKey, byte[] realSig, String data, String algorithm) throws GeneralSecurityException {
		return this.verify(getPemPublicKey(b64publicKey, "RSA"), realSig, data, algorithm);
	}

	public PrivateKey getPemPrivateKey(File f, String algorithm)
			throws IOException, GeneralSecurityException {
		
		FileInputStream fis = new FileInputStream(f);
		DataInputStream dis = new DataInputStream(fis);
		byte[] keyBytes = new byte[(int) f.length()];
		dis.readFully(keyBytes);
		dis.close();

		String b64privKeyString = new String(keyBytes);
		
		return getPemPrivateKey(b64privKeyString, algorithm);
		
	}
	
	public PrivateKey getPemPrivateKey(String  b64privKeyString, String algorithm)
			throws GeneralSecurityException {
		
		String privKeyPEM = b64privKeyString.replace("-----BEGIN PRIVATE KEY-----\n", "");
		privKeyPEM = privKeyPEM.replace("-----END PRIVATE KEY-----", "");
		// System.out.println("Private key\n"+privKeyPEM);

		Base64 b64 = new Base64();

		byte[] decoded = b64.decode(privKeyPEM.getBytes());

		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
		KeyFactory kf = KeyFactory.getInstance(algorithm);
		return kf.generatePrivate(spec);
		
	}

	public PublicKey getPemPublicKey(File f, String algorithm)
			throws IOException, GeneralSecurityException  {
		FileInputStream fis = new FileInputStream(f);
		DataInputStream dis = new DataInputStream(fis);
		byte[] keyBytes = new byte[(int) f.length()];
		dis.readFully(keyBytes);
		dis.close();

		String b64pubKeyString = new String(keyBytes);
		return getPemPublicKey(b64pubKeyString, algorithm);
	}
	
	public PublicKey getPemPublicKey(String  b64pubKeyString, String algorithm)
			throws GeneralSecurityException {
		String publicKeyPEM = b64pubKeyString.replace("-----BEGIN PUBLIC KEY-----\n", "");
		publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");

		Base64 b64 = new Base64();
		byte[] decoded = b64.decode(publicKeyPEM.getBytes());

		X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
		KeyFactory kf = KeyFactory.getInstance(algorithm);
		return kf.generatePublic(spec);
	}
	
	public KeyPair generateKeys(){
		try{
			
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		    keyPairGenerator.initialize(2048);
		    KeyPair pair = keyPairGenerator.genKeyPair();
				 		
			return pair;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public static void main(String[] args) {
		
		Signing instance = new Signing();
		instance.generateKeys();
	}

}
