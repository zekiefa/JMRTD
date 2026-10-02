package org.jmrtd.cert;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jmrtd.cert.CVCAuthorizationTemplate.Permission;
import org.jmrtd.cert.CVCAuthorizationTemplate.Role;

/**
 * Shared helper to create synthetic CVC certificates for tests.
 */
final class CVCTestUtil {

	static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
	static final CVCPrincipal CA_REF = new CVCPrincipal(net.sf.scuba.data.Country.getInstance("NL"), "CVCA", "00001");
	static final CVCPrincipal HOLDER_REF = new CVCPrincipal(net.sf.scuba.data.Country.getInstance("NL"), "DV", "00002");

	static {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private CVCTestUtil() {
	}

	static KeyPair generateECKeyPair() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(new ECGenParameterSpec("secp256r1"));
		return generator.generateKeyPair();
	}

	static Date dateFromDaysAgo(int days) {
		return new Date(System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L);
	}

	static Date dateInDays(int days) {
		return new Date(System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L);
	}

	static CardVerifiableCertificate createCertificate(Role role, Permission permission) throws Exception {
		KeyPair keyPair = generateECKeyPair();
		return CVCertificateBuilder.createCertificate(
				keyPair.getPublic(), keyPair.getPrivate(), SIGNATURE_ALGORITHM,
				CA_REF, HOLDER_REF,
				new CVCAuthorizationTemplate(role, permission),
				dateFromDaysAgo(1), dateInDays(30),
				BouncyCastleProvider.PROVIDER_NAME);
	}
}
