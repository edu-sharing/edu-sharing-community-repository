package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;

import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import java.util.UUID;

@Slf4j
@UpdateService
public class KeyGenerator {

	@UpdateRoutine(
			id = "KeyGenerator",
			description = "generates public and private key in homeApplication if they don't exsist",
			order = 1800,
			auto = true
	)
	public void execute(boolean test) {
		ApplicationInfo homeRepo = ApplicationInfoList.getHomeRepository();

		Signing s = new Signing();

		KeyPair kp = s.generateKeys();
		try {
			String file = PropertiesHelper.Config.getAbsolutePathForConfigFile(
					PropertiesHelper.Config.getPropertyFilePath(CCConstants.REPOSITORY_FILE_HOME)
			);
			if (homeRepo.getPublicKey() == null) {
				log.info("will set public key");
				if (!test) {

					String pubKeyString = "-----BEGIN PUBLIC KEY-----\n"
							+ new String(new Base64().encode(kp.getPublic().getEncoded())) + "-----END PUBLIC KEY-----";

					PropertiesHelper.setProperty(ApplicationInfo.KEY_PUBLIC_KEY, pubKeyString,
							file, PropertiesHelper.XML);
				}

			}

			if (homeRepo.getPrivateKey() == null) {
				log.info("will set private key");

				if (!test) {
					PropertiesHelper.setProperty(ApplicationInfo.KEY_PRIVATE_KEY,
							new String(new Base64().encode(kp.getPrivate().getEncoded())),
							file, PropertiesHelper.XML);
				}

			}

			if(homeRepo.getLtiKid() == null){
				log.info("will set lti kid");

				if (!test) {
					PropertiesHelper.setProperty(ApplicationInfo.KEY_LTI_KID,
							UUID.randomUUID().toString(),
							file, PropertiesHelper.XML);
				}

			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	public static void main(String[] args) {

		Signing s = new Signing();

		KeyPair kp = s.generateKeys();

		System.out.println(new String(new Base64().encode(kp.getPublic().getEncoded())));

		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(1024);
			KeyPair keyPair = keyPairGenerator.genKeyPair();
			String publicKeyFilename = "public";

			byte[] publicKeyBytes = keyPair.getPublic().getEncoded();

			FileOutputStream fos = new FileOutputStream(publicKeyFilename);
			fos.write(publicKeyBytes);
			fos.close();

			System.out.println(PropertiesHelper.getProperty(ApplicationInfo.KEY_PRIVATE_KEY,
					CCConstants.REPOSITORY_FILE_HOME, PropertiesHelper.XML));

			Signing signing = new Signing();

			PrivateKey pk = signing.getPemPrivateKey(PropertiesHelper.getProperty(ApplicationInfo.KEY_PRIVATE_KEY,
					CCConstants.REPOSITORY_FILE_HOME, PropertiesHelper.XML), CCConstants.SECURITY_KEY_ALGORITHM);

			String data = "hello world";
			byte[] signature = signing.sign(pk, data, CCConstants.SECURITY_SIGN_ALGORITHM);

			PublicKey pubK = signing.getPemPublicKey(PropertiesHelper.getProperty(ApplicationInfo.KEY_PUBLIC_KEY,
					CCConstants.REPOSITORY_FILE_HOME, PropertiesHelper.XML), CCConstants.SECURITY_KEY_ALGORITHM);
			System.out.println("verify:" + signing.verify(pubK, signature, data, CCConstants.SECURITY_SIGN_ALGORITHM));

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
