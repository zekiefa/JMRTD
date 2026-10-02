package org.jmrtd.cert;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.SignatureException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jmrtd.cert.CVCAuthorizationTemplate.Permission;
import org.jmrtd.cert.CVCAuthorizationTemplate.Role;
import org.junit.jupiter.api.Test;

public class CardVerifiableCertificateTest {

	@Test
	public void testGetters() throws Exception {
		CardVerifiableCertificate cert = CVCTestUtil.createCertificate(Role.IS, Permission.READ_ACCESS_DG3);

		assertEquals(CVCTestUtil.CA_REF, cert.getAuthorityReference());
		assertEquals(CVCTestUtil.HOLDER_REF, cert.getHolderReference());
		assertEquals(new CVCAuthorizationTemplate(Role.IS, Permission.READ_ACCESS_DG3),
				cert.getAuthorizationTemplate());
		assertNotNull(cert.getSigAlgName());
		assertNotNull(cert.getSigAlgOID());
		assertNotNull(cert.getSignature());
		assertTrue(cert.getSignature().length > 0);
		assertNotNull(cert.getCertBodyData());
		assertNotNull(cert.getNotBefore());
		assertNotNull(cert.getNotAfter());
		assertTrue(cert.getNotBefore().before(cert.getNotAfter()));
		assertNotNull(cert.getEncoded());
		assertNotNull(cert.toString());
		assertNotNull(cert.getPublicKey());
	}

	@Test
	public void testVerify() throws Exception {
		KeyPair keyPair = CVCTestUtil.generateECKeyPair();
		CardVerifiableCertificate cert = CVCertificateBuilder.createCertificate(
				keyPair.getPublic(), keyPair.getPrivate(), CVCTestUtil.SIGNATURE_ALGORITHM,
				CVCTestUtil.CA_REF, CVCTestUtil.HOLDER_REF,
				new CVCAuthorizationTemplate(Role.DV_D, Permission.READ_ACCESS_DG4),
				CVCTestUtil.dateFromDaysAgo(1), CVCTestUtil.dateInDays(30),
				BouncyCastleProvider.PROVIDER_NAME);

		cert.verify(keyPair.getPublic(), BouncyCastleProvider.PROVIDER_NAME);
		cert.verify(keyPair.getPublic());
	}

	@Test
	public void testVerifyWrongKeyThrows() throws Exception {
		KeyPair keyPair = CVCTestUtil.generateECKeyPair();
		KeyPair otherKeyPair = CVCTestUtil.generateECKeyPair();
		CardVerifiableCertificate cert = CVCertificateBuilder.createCertificate(
				keyPair.getPublic(), keyPair.getPrivate(), CVCTestUtil.SIGNATURE_ALGORITHM,
				CVCTestUtil.CA_REF, CVCTestUtil.HOLDER_REF,
				new CVCAuthorizationTemplate(Role.IS, Permission.READ_ACCESS_NONE),
				CVCTestUtil.dateFromDaysAgo(1), CVCTestUtil.dateInDays(30),
				BouncyCastleProvider.PROVIDER_NAME);

		assertThrows(SignatureException.class,
				() -> cert.verify(otherKeyPair.getPublic(), BouncyCastleProvider.PROVIDER_NAME));
	}

	@Test
	public void testEqualsAndHashCode() throws Exception {
		KeyPair keyPair = CVCTestUtil.generateECKeyPair();
		CardVerifiableCertificate cert1 = CVCertificateBuilder.createCertificate(
				keyPair.getPublic(), keyPair.getPrivate(), CVCTestUtil.SIGNATURE_ALGORITHM,
				CVCTestUtil.CA_REF, CVCTestUtil.HOLDER_REF,
				new CVCAuthorizationTemplate(Role.IS, Permission.READ_ACCESS_DG3_AND_DG4),
				CVCTestUtil.dateFromDaysAgo(1), CVCTestUtil.dateInDays(30),
				BouncyCastleProvider.PROVIDER_NAME);
		CardVerifiableCertificate cert2 = new CardVerifiableCertificate(
				CVCTestUtil.CA_REF, CVCTestUtil.HOLDER_REF, keyPair.getPublic(),
				CVCTestUtil.SIGNATURE_ALGORITHM,
				cert1.getNotBefore(), cert1.getNotAfter(),
				Role.IS, Permission.READ_ACCESS_DG3_AND_DG4,
				cert1.getSignature());
		CardVerifiableCertificate other = CVCTestUtil.createCertificate(Role.IS, Permission.READ_ACCESS_DG3_AND_DG4);

		assertEquals(cert1, cert1);
		assertArrayEquals(cert1.getEncoded(), cert2.getEncoded());
		assertEquals(cert1.hashCode(), cert1.hashCode());
		assertNotEquals(cert1, null);
		assertNotEquals(cert1, "notACertificate");
		assertNotEquals(cert1, other);
	}

	@Test
	public void testDirectConstructor() throws Exception {
		KeyPair keyPair = CVCTestUtil.generateECKeyPair();
		CardVerifiableCertificate cert = new CardVerifiableCertificate(
				CVCTestUtil.CA_REF, CVCTestUtil.HOLDER_REF, keyPair.getPublic(),
				CVCTestUtil.SIGNATURE_ALGORITHM,
				CVCTestUtil.dateFromDaysAgo(1), CVCTestUtil.dateInDays(30),
				Role.DV_F, Permission.READ_ACCESS_DG4,
				new byte[] { 1, 2, 3, 4 });

		assertEquals(CVCTestUtil.CA_REF, cert.getAuthorityReference());
		assertEquals(Role.DV_F, cert.getAuthorizationTemplate().getRole());
		assertEquals(Permission.READ_ACCESS_DG4, cert.getAuthorizationTemplate().getAccessRight());
		assertEquals(4, cert.getSignature().length);
	}

	@Test
	public void testRSACertificate() throws Exception {
		java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair rsaKeyPair = generator.generateKeyPair();

		CardVerifiableCertificate cert = CVCertificateBuilder.createCertificate(
				rsaKeyPair.getPublic(), rsaKeyPair.getPrivate(), "SHA256withRSA",
				CVCTestUtil.CA_REF, CVCTestUtil.HOLDER_REF,
				new CVCAuthorizationTemplate(Role.DV_D, Permission.READ_ACCESS_DG3),
				CVCTestUtil.dateFromDaysAgo(1), CVCTestUtil.dateInDays(30),
				BouncyCastleProvider.PROVIDER_NAME);

		cert.verify(rsaKeyPair.getPublic(), BouncyCastleProvider.PROVIDER_NAME);
		cert.verify(rsaKeyPair.getPublic());
		assertTrue(cert.getPublicKey() instanceof java.security.interfaces.RSAPublicKey);
		assertNotNull(cert.getSigAlgName());
		assertNotNull(cert.getSigAlgOID());
		assertEquals(CVCTestUtil.CA_REF, cert.getAuthorityReference());
		assertTrue(cert.getSignature().length > 0);
	}

	@Test
	public void testVerifyWithoutProvidersThrows() throws Exception {
		KeyPair keyPair = CVCTestUtil.generateECKeyPair();
		CardVerifiableCertificate cert = CVCertificateBuilder.createCertificate(
				keyPair.getPublic(), keyPair.getPrivate(), CVCTestUtil.SIGNATURE_ALGORITHM,
				CVCTestUtil.CA_REF, CVCTestUtil.HOLDER_REF,
				new CVCAuthorizationTemplate(Role.IS, Permission.READ_ACCESS_NONE),
				CVCTestUtil.dateFromDaysAgo(1), CVCTestUtil.dateInDays(30),
				BouncyCastleProvider.PROVIDER_NAME);

		java.security.Provider[] providers = java.security.Security.getProviders();
		for (java.security.Provider provider: providers) {
			java.security.Security.removeProvider(provider.getName());
		}
		try {
			assertThrows(java.security.NoSuchAlgorithmException.class,
					() -> cert.verify(keyPair.getPublic()));
		} finally {
			for (java.security.Provider provider: providers) {
				java.security.Security.addProvider(provider);
			}
		}
	}

	@Test
	public void testDirectConstructorBadKeyThrows() throws Exception {
		KeyPairGeneratorHelper rsaPair = new KeyPairGeneratorHelper();
		java.security.PublicKey rsaPublicKey = rsaPair.rsaPublicKey();
		/* EJBCA KeyFactory casts the key to ECPublicKey for ECDSA algorithms. */
		assertThrows(ClassCastException.class, () -> new CardVerifiableCertificate(
				CVCTestUtil.CA_REF, CVCTestUtil.HOLDER_REF, rsaPublicKey,
				CVCTestUtil.SIGNATURE_ALGORITHM, // ECDSA alg with RSA key => ConstructionException
				CVCTestUtil.dateFromDaysAgo(1), CVCTestUtil.dateInDays(30),
				Role.IS, Permission.READ_ACCESS_NONE,
				new byte[] { 1 }));
	}

	private static class KeyPairGeneratorHelper {
		java.security.PublicKey rsaPublicKey() throws Exception {
			java.security.KeyPairGenerator g = java.security.KeyPairGenerator.getInstance("RSA");
			g.initialize(1024);
			return g.generateKeyPair().getPublic();
		}
	}
}
