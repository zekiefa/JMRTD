package org.jmrtd;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.junit.jupiter.api.Test;

class SmallClassesTest {

	@Test
	void testBACDeniedException() {
		List<BACKeySpec> tried = new ArrayList<BACKeySpec>();
		tried.add(new BACKey("L898902C", "740812", "120415"));
		BACDeniedException e = new BACDeniedException("failed", tried, 0x6982);
		assertTrue(e.getMessage().startsWith("failed"));
		assertEquals(0x6982, e.getSW());
		assertEquals(1, e.getTriedEntries().size());
		assertTrue(e instanceof net.sf.scuba.smartcards.CardServiceException);
	}

	@Test
	void testPACEException() {
		PACEException e1 = new PACEException("pace failed");
		assertEquals("pace failed", e1.getMessage());
		PACEException e2 = new PACEException("pace failed", 0x6300);
		assertTrue(e2.getMessage().startsWith("pace failed"));
		assertEquals(0x6300, e2.getSW());
	}

	@Test
	void testChipAuthenticationResult() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
		kpg.initialize(ECNamedCurveTable.getParameterSpec("secp256r1"));
		java.security.KeyPair kp = kpg.generateKeyPair();
		PublicKey pub = kp.getPublic();
		byte[] keyHash = { 1, 2, 3 };

		ChipAuthenticationResult result = new ChipAuthenticationResult(
				java.math.BigInteger.valueOf(42), pub, keyHash, kp);
		assertEquals(java.math.BigInteger.valueOf(42), result.getKeyId());
		assertSame(pub, result.getPublicKey());
		assertArrayEquals(keyHash, result.getKeyHash());
		assertSame(kp, result.getKeyPair());
		assertNotNull(result.toString());
	}

	@Test
	void testTerminalAuthenticationResult() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
		kpg.initialize(ECNamedCurveTable.getParameterSpec("secp256r1"));
		java.security.KeyPair kp = kpg.generateKeyPair();
		PrivateKey priv = kp.getPrivate();

		ChipAuthenticationResult chipResult = new ChipAuthenticationResult(
				java.math.BigInteger.valueOf(-1), kp.getPublic(), new byte[] { 1 }, kp);
		List<org.jmrtd.cert.CardVerifiableCertificate> certs =
				new ArrayList<org.jmrtd.cert.CardVerifiableCertificate>();
		byte[] challenge = { 9, 8, 7 };

		TerminalAuthenticationResult result = new TerminalAuthenticationResult(
				chipResult, null, certs, priv, "L898902C3", challenge);

		assertSame(chipResult, result.getChipAuthenticationResult());
		assertNull(result.getCAReference());
		assertTrue(result.getCVCertificates().isEmpty());
		assertSame(priv, result.getTerminalKey());
		assertEquals("L898902C3", result.getDocumentNumber());
		assertArrayEquals(challenge, result.getCardChallenge());
		assertNotNull(result.toString());
	}

	@Test
	void testJMRTDSecurityProvider() {
		Provider bc = JMRTDSecurityProvider.getBouncyCastleProvider();
		assertNotNull(bc);
		assertSame(bc, JMRTDSecurityProvider.getBouncyCastleProvider());
	}

	@Test
	void testRFC5114Groups() {
		assertEquals(1024, RFC5114Groups.rfc5114_1024_160.getP().bitLength());
		assertEquals(160, RFC5114Groups.rfc5114_1024_160.getQ().bitLength());
		assertEquals(2048, RFC5114Groups.rfc5114_2048_224.getP().bitLength());
		assertEquals(224, RFC5114Groups.rfc5114_2048_224.getQ().bitLength());
		assertEquals(2048, RFC5114Groups.rfc5114_2048_256.getP().bitLength());
		assertEquals(256, RFC5114Groups.rfc5114_2048_256.getQ().bitLength());
		assertNotEquals(RFC5114Groups.rfc5114_1024_160.getP(), RFC5114Groups.rfc5114_2048_224.getP());
		assertTrue(RFC5114Groups.rfc5114_1024_160.getG().compareTo(java.math.BigInteger.ZERO) > 0);
	}

	@Test
	void testSecureMessagingWrapperIsAbstract() {
		assertTrue(java.lang.reflect.Modifier.isAbstract(SecureMessagingWrapper.class.getModifiers()));
	}

	@Test
	void testJMRTDSecurityProviderDeep() {
		Provider p = JMRTDSecurityProvider.getInstance();
		assertNotNull(p);
		Provider bc = JMRTDSecurityProvider.getBouncyCastleProvider();
		assertNotNull(bc);
		/* singleton consistency */
		assertSame(bc, JMRTDSecurityProvider.getBouncyCastleProvider());
		assertNotNull(p.getName());
	}

	@Test
	void testBACKeyDateParsingEquality() throws Exception {
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyMMdd");
		BACKey viaDates = new BACKey("L898902C", sdf.parse("740812"), sdf.parse("120415"));
		BACKey viaStrings = new BACKey("L898902C", "740812", "120415");
		assertEquals(viaStrings, viaDates);
		assertEquals(viaStrings.hashCode(), viaDates.hashCode());
	}
}
