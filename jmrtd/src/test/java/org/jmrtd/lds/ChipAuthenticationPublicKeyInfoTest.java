package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.junit.jupiter.api.Test;

public class ChipAuthenticationPublicKeyInfoTest {

	private static SubjectPublicKeyInfo createECPublicKeyInfo() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(256);
		KeyPair keyPair = generator.generateKeyPair();
		return SubjectPublicKeyInfo.getInstance(
				ASN1Primitive.fromByteArray(keyPair.getPublic().getEncoded()));
	}

	@Test
	public void testConstructWithKeyId() throws Exception {
		SubjectPublicKeyInfo spki = createECPublicKeyInfo();
		ChipAuthenticationPublicKeyInfo info = new ChipAuthenticationPublicKeyInfo(
				SecurityInfo.ID_PK_ECDH_OID, spki, BigInteger.ONE);
		assertEquals(SecurityInfo.ID_PK_ECDH_OID, info.getObjectIdentifier());
		assertEquals(BigInteger.ONE, info.getKeyId());
		assertEquals(1, info.getKeyId().intValue());
	}

	@Test
	public void testInvalidOID() throws Exception {
		SubjectPublicKeyInfo spki = createECPublicKeyInfo();
		assertThrows(IllegalArgumentException.class,
				() -> new ChipAuthenticationPublicKeyInfo("1.2.3.4", spki, BigInteger.ONE));
	}

	@Test
	public void testCheckRequiredIdentifier() {
		assertTrue(ChipAuthenticationPublicKeyInfo.checkRequiredIdentifier(
				SecurityInfo.ID_PK_ECDH_OID));
		assertTrue(ChipAuthenticationPublicKeyInfo.checkRequiredIdentifier(
				SecurityInfo.ID_PK_DH_OID));
	}

	@Test
	public void testEqualsAndHashCode() throws Exception {
		SubjectPublicKeyInfo spki = createECPublicKeyInfo();
		ChipAuthenticationPublicKeyInfo a = new ChipAuthenticationPublicKeyInfo(
				SecurityInfo.ID_PK_ECDH_OID, spki, BigInteger.ONE);
		ChipAuthenticationPublicKeyInfo b = new ChipAuthenticationPublicKeyInfo(
				SecurityInfo.ID_PK_ECDH_OID, spki, BigInteger.ONE);
		ChipAuthenticationPublicKeyInfo c = new ChipAuthenticationPublicKeyInfo(
				SecurityInfo.ID_PK_ECDH_OID, spki, BigInteger.TEN);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertNotEquals(null, a);
	}

	@Test
	public void testDERRoundTrip() throws Exception {
		SubjectPublicKeyInfo spki = createECPublicKeyInfo();
		ChipAuthenticationPublicKeyInfo info = new ChipAuthenticationPublicKeyInfo(
				SecurityInfo.ID_PK_ECDH_OID, spki, BigInteger.valueOf(3));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		info.writeObject(out);
		SecurityInfo decoded = SecurityInfo.getInstance(
				ASN1Sequence.getInstance(out.toByteArray()));
		assertTrue(decoded instanceof ChipAuthenticationPublicKeyInfo);
		assertEquals(info, decoded);
		assertEquals(info.hashCode(), decoded.hashCode());
		assertEquals(BigInteger.valueOf(3),
				((ChipAuthenticationPublicKeyInfo) decoded).getKeyId());
	}

	@Test
	public void testSubjectPublicKeyNotNull() throws Exception {
		SubjectPublicKeyInfo spki = createECPublicKeyInfo();
		ChipAuthenticationPublicKeyInfo info = new ChipAuthenticationPublicKeyInfo(
				SecurityInfo.ID_PK_ECDH_OID, spki, BigInteger.ONE);
		assertNotNull(info.getSubjectPublicKey());
		assertNotNull(info.toString());
	}
}
