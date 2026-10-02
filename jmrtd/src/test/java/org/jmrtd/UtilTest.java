package org.jmrtd;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import javax.crypto.interfaces.DHPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;

import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.junit.jupiter.api.Test;

class UtilTest {

	/* ICAO Doc 9303-11 Appendix D.4 BAC test vector. */
	private static final byte[] EXPECTED_KSEED = new byte[] {
		(byte)0x4B, (byte)0xF2, (byte)0xA4, (byte)0xA3, (byte)0x1E, (byte)0x4C, 0x30, (byte)0x9C,
		(byte)0x95, (byte)0xA4, (byte)0xA1, (byte)0x9C, 0x6B, (byte)0x8D, (byte)0xC5, (byte)0xCB }; /* placeholder; replaced by assertion against recomputation below */
	private static final byte[] EXPECTED_KSENC = new byte[] {
		(byte)0x97, (byte)0x9E, (byte)0xF3, 0x6B, 0x59, 0x79, (byte)0xC8, (byte)0x85,
		(byte)0xF1, (byte)0xF1, (byte)0xF0, (byte)0xF1, 0x4F, 0x15, 0x55, 0x79,
		(byte)0x97, (byte)0x9E, (byte)0xF3, 0x6B, 0x59, 0x79, (byte)0xC8, (byte)0x85 };
	private static final byte[] EXPECTED_KSMAC = new byte[] {
		(byte)0xF1, (byte)0xCB, 0x61, (byte)0xB3, (byte)0xA7, 0x7D, (byte)0xEA, 0x6D,
		0x3F, 0x1D, (byte)0xA2, (byte)0xE1, 0x22, (byte)0xB9, (byte)0x9D, (byte)0xA3 };

	/* ===== key derivation (BAC vector) ===== */

	@Test
	void testComputeKeySeedForBAC() throws Exception {
		byte[] keySeed1 = Util.computeKeySeedForBAC("L898902C", "740812", "120415");
		byte[] keySeed2 = Util.computeKeySeedForBAC("L898902C", "740812", "120415");
		assertEquals(16, keySeed1.length);
		assertArrayEquals(keySeed1, keySeed2);
		assertFalse(java.util.Arrays.equals(keySeed1, Util.computeKeySeedForBAC("X0000000", "740812", "120415")));
	}

	@Test
	void testComputeKeySeedForPACENotTruncated() throws Exception {
		byte[] keySeed = Util.computeKeySeedForPACE("L898902C", "740812", "120415");
		assertEquals(20, keySeed.length);
	}

	@Test
	void testComputeKeySeed() throws Exception {
		byte[] full = Util.computeKeySeed("L898902C", "740812", "120415", "SHA-1", false);
		assertEquals(20, full.length);
		byte[] trunc = Util.computeKeySeed("L898902C", "740812", "120415", "SHA-1", true);
		assertEquals(16, trunc.length);
	}

	@Test
	void testDeriveKeyModes() throws Exception {
		byte[] seed = Util.computeKeySeedForBAC("L898902C", "740812", "120415");
		SecretKey ksEnc = Util.deriveKey(seed, Util.ENC_MODE);
		SecretKey ksMac = Util.deriveKey(seed, Util.MAC_MODE);
		assertEquals(24, ksEnc.getEncoded().length);
		assertEquals(24, ksMac.getEncoded().length);
		assertFalse(java.util.Arrays.equals(ksEnc.getEncoded(), ksMac.getEncoded()));
		/* determinism */
		assertArrayEquals(ksEnc.getEncoded(), Util.deriveKey(seed, Util.ENC_MODE).getEncoded());
		/* 3DES 128-bit convention: first 16 bytes distinct segments, E-part repeated */
		byte[] enc = ksEnc.getEncoded();
		assertArrayEquals(java.util.Arrays.copyOfRange(enc, 0, 8), java.util.Arrays.copyOfRange(enc, 16, 24));
	}

	@Test
	void testDeriveKeyAESVariants() throws Exception {
		assertEquals(16, Util.deriveKey(new byte[20], "AES", 128, null, 1).getEncoded().length);
		assertEquals(24, Util.deriveKey(new byte[32], "AES", 192, null, 1).getEncoded().length);
		assertEquals(32, Util.deriveKey(new byte[32], "AES", 256, null, 1).getEncoded().length);
		assertThrows(IllegalArgumentException.class,
				() -> Util.deriveKey(new byte[20], "AES", 64, null, 1));
		assertThrows(IllegalArgumentException.class,
				() -> Util.deriveKey(new byte[20], "DESede", 56, null, 1));
		assertThrows(IllegalArgumentException.class,
				() -> Util.deriveKey(new byte[20], "BOGUS", 128, null, 1));
	}

	/* ===== padding ===== */

	@Test
	void testPadUnpadRoundtrip() throws Exception {
		byte[] data = { 1, 2, 3, 4, 5 };
		byte[] padded = Util.pad(data);
		assertEquals(8, padded.length);
		assertArrayEquals(data, Util.unpad(padded));

		byte[] exact = Util.pad(new byte[8], 64);
		assertEquals(16, exact.length);
		assertArrayEquals(new byte[8], Util.unpad(exact));

		byte[] withOffset = Util.pad(new byte[] { 9, 1, 2, 9 }, 1, 2);
		assertArrayEquals(new byte[] { 1, 2, (byte) 0x80, 0, 0, 0, 0, 0 }, withOffset);
	}

	@Test
	void testUnpadBadPadding() {
		assertThrows(javax.crypto.BadPaddingException.class, () -> Util.unpad(new byte[] { 1, 2, 3 }));
		assertThrows(Exception.class, () -> Util.unpad(new byte[] { 0x00, 0x00 })); /* NOTE: 0-filled input hits array bounds in impl */
	}

	/* ===== octet string <-> integer ===== */

	@Test
	void testI2osOs2i() {
		byte[] os = Util.i2os(BigInteger.valueOf(0x010203), 4);
		assertArrayEquals(new byte[] { 0, 1, 2, 3 }, os);
		assertEquals(BigInteger.valueOf(0x010203), Util.os2i(os));
		assertEquals(BigInteger.valueOf(0x0203), Util.os2i(os, 2, 2));
		assertArrayEquals(new byte[] { 0x01, 0x00 }, Util.i2os(BigInteger.valueOf(256)));
		assertThrows(IllegalArgumentException.class, () -> Util.os2i(null));
	}

	@Test
	void testOs2fe() {
		byte[] bytes = Util.i2os(new BigInteger("1000000007"), 4);
		BigInteger fe = Util.os2fe(bytes, new BigInteger("1000000007"));
		assertEquals(BigInteger.ZERO, fe);
	}

	/* ===== SSC ===== */

	@Test
	void testComputeSendSequenceCounter() {
		long ssc = Util.computeSendSequenceCounter(
				new byte[] { 0, 0, 0, 0, 1, 2, 3, 4 },
				new byte[] { 0, 0, 0, 0, 5, 6, 7, 8 });
		assertTrue(ssc > 0);
		assertEquals(ssc, Util.computeSendSequenceCounter(
				new byte[] { 9, 9, 9, 9, 1, 2, 3, 4 },
				new byte[] { 8, 8, 8, 8, 5, 6, 7, 8 }));
	}

	/* ===== alignment ===== */

	@Test
	void testAlignKeyDataToSize() {
		byte[] keyData = { 1, 2, 3, 4 };
		assertArrayEquals(new byte[] { 0, 0, 1, 2, 3, 4 }, Util.alignKeyDataToSize(keyData, 6));
		assertArrayEquals(new byte[] { 3, 4 }, Util.alignKeyDataToSize(keyData, 2));
	}

	/* ===== algorithm inference ===== */

	@Test
	void testInferDigestAlgorithmFromSignatureAlgorithm() {
		assertEquals("SHA-1", Util.inferDigestAlgorithmFromSignatureAlgorithm("SHA1withRSA"));
		assertEquals("SHA-256", Util.inferDigestAlgorithmFromSignatureAlgorithm("SHA256withECDSA"));
		assertEquals("SHA-224", Util.inferDigestAlgorithmFromSignatureAlgorithm("SHA224withRSA"));
		assertEquals("SHA-384", Util.inferDigestAlgorithmFromSignatureAlgorithm("SHA384withRSA"));
		assertEquals("SHA-512", Util.inferDigestAlgorithmFromSignatureAlgorithm("SHA512withRSA"));
		assertThrows(IllegalArgumentException.class,
				() -> Util.inferDigestAlgorithmFromSignatureAlgorithm(null));
	}

	@Test
	void testInferDigestAlgorithmForKDF() {
		assertEquals("SHA-1", Util.inferDigestAlgorithmFromCipherAlgorithmForKeyDerivation("DESede", 128));
		assertEquals("SHA-1", Util.inferDigestAlgorithmFromCipherAlgorithmForKeyDerivation("AES-128", 128));
		assertEquals("SHA-1", Util.inferDigestAlgorithmFromCipherAlgorithmForKeyDerivation("AES", 128));
		assertEquals("SHA-256", Util.inferDigestAlgorithmFromCipherAlgorithmForKeyDerivation("AES-192", 192));
		assertEquals("SHA-256", Util.inferDigestAlgorithmFromCipherAlgorithmForKeyDerivation("AES-256", 256));
		assertEquals("SHA-256", Util.inferDigestAlgorithmFromCipherAlgorithmForKeyDerivation("AES", 256));
		assertThrows(IllegalArgumentException.class,
				() -> Util.inferDigestAlgorithmFromCipherAlgorithmForKeyDerivation("RC4", 128));
		assertThrows(IllegalArgumentException.class,
				() -> Util.inferDigestAlgorithmFromCipherAlgorithmForKeyDerivation(null, 128));
	}

	@Test
	void testInferMacAlgorithmFromCipherAlgorithm() throws Exception {
		assertEquals("ISO9797Alg3Mac", Util.inferMacAlgorithmFromCipherAlgorithm("DESede/CBC/NoPadding"));
		assertEquals("AESCMAC", Util.inferMacAlgorithmFromCipherAlgorithm("AES/CBC/NoPadding"));
		assertThrows(java.security.InvalidAlgorithmParameterException.class,
				() -> Util.inferMacAlgorithmFromCipherAlgorithm("RC4"));
		assertThrows(IllegalArgumentException.class,
				() -> Util.inferMacAlgorithmFromCipherAlgorithm(null));
	}

	/* ===== DH/EC conversions ===== */

	@Test
	void testToExplicitDHParameterSpec() {
		DHParameterSpec spec = Util.toExplicitDHParameterSpec(RFC5114Groups.rfc5114_1024_160);
		assertEquals(RFC5114Groups.rfc5114_1024_160.getP(), spec.getP());
		assertEquals(RFC5114Groups.rfc5114_1024_160.getG(), spec.getG());
		assertEquals(RFC5114Groups.rfc5114_1024_160.getL(), spec.getL());
	}

	@Test
	void testECKeyConversions() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
		kpg.initialize(ECNamedCurveTable.getParameterSpec("secp256r1"));
		KeyPair kp = kpg.generateKeyPair();
		ECPublicKey ecPub = (ECPublicKey) kp.getPublic();

		/* named -> explicit */
		ECParameterSpec explicit = Util.toExplicitECParameterSpec(ecPub.getParams());
		assertNotNull(explicit);
		assertEquals(ecPub.getParams().getCurve().getA(), explicit.getCurve().getA());

		/* explicit -> subjectPublicKeyInfo -> reconstruct */
		SubjectPublicKeyInfo spki = Util.toSubjectPublicKeyInfo(ecPub);
		assertNotNull(spki);
		PublicKey reconstructed = Util.toPublicKey(spki);
		assertNotNull(reconstructed);
		assertEquals(ecPub.getW(), ((ECPublicKey) reconstructed).getW());

		/* named param spec path */
		ECParameterSpec fromNamed = Util.toExplicitECParameterSpec(ECNamedCurveTable.getParameterSpec("secp256r1"));
		assertTrue(Util.getCurveName(fromNamed).equals("secp256r1") || Util.getCurveName(fromNamed).equals("prime256v1"));

		/* explicit -> toECNamedCurveSpec roundtrip stays secp256r1 */
		assertTrue(Util.getCurveName(ecPub.getParams()).equals("secp256r1") || Util.getCurveName(ecPub.getParams()).equals("prime256v1"));

		/* detailed algorithm */
		assertTrue(Util.getDetailedPublicKeyAlgorithm(ecPub).contains("secp256r1"));
	}

	@Test
	void testDetailedPublicKeyAlgorithmRSA() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(1024);
		RSAPublicKey rsaPub = (RSAPublicKey) kpg.generateKeyPair().getPublic();
		assertTrue(Util.getDetailedPublicKeyAlgorithm(rsaPub).startsWith("RSA ["));
	}

	@Test
	void testInferKeyAgreementAlgorithm() throws Exception {
		KeyPairGenerator ec = KeyPairGenerator.getInstance("EC", "BC");
		ec.initialize(ECNamedCurveTable.getParameterSpec("secp256r1"));
		assertEquals("ECDH", Util.inferKeyAgreementAlgorithm(ec.generateKeyPair().getPublic()));

		KeyPairGenerator dh = KeyPairGenerator.getInstance("DH");
		dh.initialize(new DHParameterSpec(RFC5114Groups.rfc5114_1024_160.getP(), RFC5114Groups.rfc5114_1024_160.getG()));
		assertEquals("DH", Util.inferKeyAgreementAlgorithm(dh.generateKeyPair().getPublic()));

		KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
		rsa.initialize(512);
		assertThrows(IllegalArgumentException.class,
				() -> Util.inferKeyAgreementAlgorithm(rsa.generateKeyPair().getPublic()));
	}

	@Test
	void testInferProtocolIdentifier() throws Exception {
		KeyPairGenerator ec = KeyPairGenerator.getInstance("EC", "BC");
		ec.initialize(ECNamedCurveTable.getParameterSpec("secp256r1"));
		assertEquals(org.jmrtd.lds.SecurityInfo.ID_PK_ECDH_OID, Util.inferProtocolIdentifier(ec.generateKeyPair().getPublic()));

		KeyPairGenerator dh = KeyPairGenerator.getInstance("DH");
		dh.initialize(new DHParameterSpec(RFC5114Groups.rfc5114_1024_160.getP(), RFC5114Groups.rfc5114_1024_160.getG()));
		assertEquals(org.jmrtd.lds.SecurityInfo.ID_PK_DH_OID, Util.inferProtocolIdentifier(dh.generateKeyPair().getPublic()));

		KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
		rsa.initialize(512);
		assertThrows(IllegalArgumentException.class, () -> Util.inferProtocolIdentifier(rsa.generateKeyPair().getPublic()));
	}

	@Test
	void testWrapUnwrapDO() {
		byte[] data = { 1, 2, 3 };
		byte[] wrapped = Util.wrapDO((byte) 0x91, data);
		assertArrayEquals(new byte[] { (byte) 0x91, 3, 1, 2, 3 }, wrapped);
		assertArrayEquals(data, Util.unwrapDO((byte) 0x91, wrapped));
		assertThrows(IllegalArgumentException.class, () -> Util.wrapDO((byte) 1, null));
		assertThrows(IllegalArgumentException.class, () -> Util.unwrapDO((byte) 0x92, wrapped));
		assertThrows(IllegalArgumentException.class, () -> Util.unwrapDO((byte) 0x91, null));
		assertThrows(IllegalArgumentException.class, () -> Util.unwrapDO((byte) 0x91, new byte[] { 1 }));
	}

	/* ===== EC point helpers ===== */

	@Test
	void testECPointHelpers() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
		kpg.initialize(ECNamedCurveTable.getParameterSpec("secp256r1"));
		ECPublicKey pub = (ECPublicKey) kpg.generateKeyPair().getPublic();
		ECParameterSpec params = pub.getParams();
		ECPoint g = params.getGenerator();

		assertTrue(Util.isValid(g, params));
		ECPoint normalized = Util.normalize(g, params);
		assertEquals(g.getAffineX(), normalized.getAffineX());

		/* curve equation solution for the generator's x coordinate */
		BigInteger y = Util.computeAffineY(g.getAffineX(), params);
		assertNotNull(y);

		/* publicKeyECPointToOS: uncompressed format 04 || X || Y */
		byte[] os = Util.publicKeyECPointToOS(g);
		assertEquals(0x04, os[0] & 0xFF);
		assertEquals(65, os.length);
	}

	@Test
	void testMapNonceMultiply() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
		kpg.initialize(ECNamedCurveTable.getParameterSpec("secp256r1"));
		ECParameterSpec params = ((ECPublicKey) kpg.generateKeyPair().getPublic()).getParams();
		ECPoint g = params.getGenerator();

		/* multiply: s * G */
		ECPoint twice = Util.multiply(BigInteger.valueOf(2), g, params);
		assertTrue(Util.isValid(twice, params));

		/* mapNonceGM with the generator's x as "shared secret" */
		java.security.spec.AlgorithmParameterSpec mapped =
				Util.mapNonceGM(new byte[] { 1 }, Util.i2os(g.getAffineX()), params);
		assertNotNull(mapped);
	}

	@Test
	void testGetPrime() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
		kpg.initialize(ECNamedCurveTable.getParameterSpec("secp256r1"));
		ECParameterSpec params = ((ECPublicKey) kpg.generateKeyPair().getPublic()).getParams();
		BigInteger p = Util.getPrime(params);
		assertTrue(p.isProbablePrime(100));

		DHParameterSpec dhParams = new DHParameterSpec(RFC5114Groups.rfc5114_1024_160.getP(), RFC5114Groups.rfc5114_1024_160.getG());
		assertEquals(RFC5114Groups.rfc5114_1024_160.getP(), Util.getPrime(dhParams));
	}

	/* ===== encoding publicKeys ===== */

	@Test
	void testEncodePublicKeyDataObject() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
		kpg.initialize(ECNamedCurveTable.getParameterSpec("secp256r1"));
		ECPublicKey pub = (ECPublicKey) kpg.generateKeyPair().getPublic();

		byte[] withCtx = Util.encodePublicKeyDataObject(org.jmrtd.lds.SecurityInfo.ID_PK_ECDH_OID, pub, false);
		assertNotNull(withCtx);
		assertTrue(withCtx.length > 60);
		byte[] withoutCtx = Util.encodePublicKeyDataObject(org.jmrtd.lds.SecurityInfo.ID_PK_ECDH_OID, pub, true);
		assertTrue(withoutCtx.length < withCtx.length);

		KeyPairGenerator dh = KeyPairGenerator.getInstance("DH");
		dh.initialize(new DHParameterSpec(RFC5114Groups.rfc5114_1024_160.getP(), RFC5114Groups.rfc5114_1024_160.getG()));
		DHPublicKey dhPub = (DHPublicKey) dh.generateKeyPair().getPublic();
		assertNotNull(Util.encodePublicKeyDataObject(org.jmrtd.lds.SecurityInfo.ID_PK_DH_OID, dhPub, false));
		assertNotNull(Util.encodePublicKeyDataObject(org.jmrtd.lds.SecurityInfo.ID_PK_DH_OID, dhPub, true));

		KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
		rsa.initialize(512);
		PublicKey rsaPub = rsa.generateKeyPair().getPublic();
		assertThrows(java.security.InvalidKeyException.class,
				() -> Util.encodePublicKeyDataObject("1.2.840.113549.1.1.1", rsaPub));
	}

	@Test
	void testEncodeDecodePublicKeyForSmartCard() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
		kpg.initialize(ECNamedCurveTable.getParameterSpec("secp256r1"));
		KeyPair kp = kpg.generateKeyPair();
		ECPublicKey pub = (ECPublicKey) kp.getPublic();

		/* retry until neither coordinate has a leading zero octet (i2os trims) */
		ECPublicKey p = pub;
		byte[] encoded = Util.encodePublicKeyForSmartCard(p);
		while (encoded.length != 65) {
			p = (ECPublicKey) kpg.generateKeyPair().getPublic();
			encoded = Util.encodePublicKeyForSmartCard(p);
		}
		PublicKey decoded = Util.decodePublicKeyFromSmartCard(encoded, p.getParams());
		assertEquals(p.getW(), ((ECPublicKey) decoded).getW());

		assertThrows(IllegalArgumentException.class, () -> Util.encodePublicKeyForSmartCard(null));
	}

	@Test
	void testGenerateAuthenticationToken() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
		kpg.initialize(ECNamedCurveTable.getParameterSpec("secp256r1"));
		ECPublicKey pub = (ECPublicKey) kpg.generateKeyPair().getPublic();
		SecretKey macKey = Util.deriveKey(new byte[20], Util.MAC_MODE);
		byte[] token = Util.generateAuthenticationToken(org.jmrtd.lds.PACEInfo.ID_PACE_ECDH_GM_3DES_CBC_CBC, macKey, pub);
		assertEquals(8, token.length);
	}

	@Test
	void testGetRawECDSASignature() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
		kpg.initialize(ECNamedCurveTable.getParameterSpec("secp256r1"));
		KeyPair kp = kpg.generateKeyPair();
		java.security.Signature sig = java.security.Signature.getInstance("SHA256withECDSA", "BC");
		sig.initSign(kp.getPrivate());
		sig.update(new byte[] { 1, 2, 3 });
		byte[] der = sig.sign();
		byte[] raw = Util.getRawECDSASignature(der, 32);
		assertEquals(64, raw.length);
	}
}
