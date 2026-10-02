package org.jmrtd.cert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.KeyPair;
import java.security.NoSuchProviderException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jmrtd.cert.CVCAuthorizationTemplate.Permission;
import org.jmrtd.cert.CVCAuthorizationTemplate.Role;
import org.junit.jupiter.api.Test;

public class CVCertificateBuilderTest {

	@Test
	public void testCreateCertificateAllRolesAndPermissions() throws Exception {
		KeyPair keyPair = CVCTestUtil.generateECKeyPair();
		for (Role role: Role.values()) {
			for (Permission permission: Permission.values()) {
				CardVerifiableCertificate cert = CVCertificateBuilder.createCertificate(
						keyPair.getPublic(), keyPair.getPrivate(), CVCTestUtil.SIGNATURE_ALGORITHM,
						CVCTestUtil.CA_REF, CVCTestUtil.HOLDER_REF,
						new CVCAuthorizationTemplate(role, permission),
						CVCTestUtil.dateFromDaysAgo(1), CVCTestUtil.dateInDays(30),
						BouncyCastleProvider.PROVIDER_NAME);
				assertNotNull(cert);
				CVCAuthorizationTemplate template = cert.getAuthorizationTemplate();
				assertEquals(role, template.getRole());
				assertEquals(permission, template.getAccessRight());
			}
		}
	}

	@Test
	public void testUnknownProviderThrows() throws Exception {
		KeyPair keyPair = CVCTestUtil.generateECKeyPair();
		assertThrows(NoSuchProviderException.class, () -> CVCertificateBuilder.createCertificate(
				keyPair.getPublic(), keyPair.getPrivate(), CVCTestUtil.SIGNATURE_ALGORITHM,
				CVCTestUtil.CA_REF, CVCTestUtil.HOLDER_REF,
				new CVCAuthorizationTemplate(Role.IS, Permission.READ_ACCESS_NONE),
				CVCTestUtil.dateFromDaysAgo(1), CVCTestUtil.dateInDays(30),
				"NoSuchProviderXYZ"));
	}
}
