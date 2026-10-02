package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.junit.jupiter.api.Test;

public class ActiveAuthenticationInfoTest {

	@Test
	public void testConstructAndGetters() {
		ActiveAuthenticationInfo info = new ActiveAuthenticationInfo(
				ActiveAuthenticationInfo.ECDSA_PLAIN_SHA256_OID);
		assertEquals(SecurityInfo.ID_AA_OID, info.getObjectIdentifier());
		assertEquals(ActiveAuthenticationInfo.ECDSA_PLAIN_SHA256_OID,
				info.getSignatureAlgorithmOID());
		assertTrue(info.toString().contains(ActiveAuthenticationInfo.ECDSA_PLAIN_SHA256_OID));
	}

	@Test
	public void testInvalidSignatureAlgorithmOID() {
		assertThrows(IllegalArgumentException.class,
				() -> new ActiveAuthenticationInfo("1.2.3.4.5"));
	}

	@Test
	public void testEqualsAndHashCode() {
		ActiveAuthenticationInfo a = new ActiveAuthenticationInfo(
				ActiveAuthenticationInfo.ECDSA_PLAIN_SHA1_OID);
		ActiveAuthenticationInfo b = new ActiveAuthenticationInfo(
				ActiveAuthenticationInfo.ECDSA_PLAIN_SHA1_OID);
		ActiveAuthenticationInfo c = new ActiveAuthenticationInfo(
				ActiveAuthenticationInfo.ECDSA_PLAIN_SHA512_OID);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertFalse(a.equals(null));
		assertEquals(a, a);
	}

	@Test
	public void testDERRoundTrip() throws Exception {
		ActiveAuthenticationInfo info = new ActiveAuthenticationInfo(
				ActiveAuthenticationInfo.ECDSA_PLAIN_SHA224_OID);
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		info.writeObject(out);
		byte[] encoded = out.toByteArray();
		SecurityInfo decoded = SecurityInfo.getInstance(
				org.bouncycastle.asn1.ASN1Sequence.getInstance(encoded));
		assertTrue(decoded instanceof ActiveAuthenticationInfo);
		assertEquals(info, decoded);
		assertEquals(info.hashCode(), decoded.hashCode());
	}

	@Test
	public void testFactoryFromManualDER() throws Exception {
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new ASN1ObjectIdentifier(SecurityInfo.ID_AA_OID));
		v.add(new ASN1Integer(1));
		v.add(new ASN1ObjectIdentifier(ActiveAuthenticationInfo.ECDSA_PLAIN_SHA384_OID));
		byte[] encoded = new DERSequence(v).getEncoded();
		SecurityInfo decoded = SecurityInfo.getInstance(
				org.bouncycastle.asn1.ASN1Sequence.getInstance(encoded));
		assertTrue(decoded instanceof ActiveAuthenticationInfo);
		assertEquals(ActiveAuthenticationInfo.ECDSA_PLAIN_SHA384_OID,
				((ActiveAuthenticationInfo) decoded).getSignatureAlgorithmOID());
	}

	@Test
	public void testLookupMnemonicByOID() throws Exception {
		assertEquals("SHA1withECDSA", ActiveAuthenticationInfo.lookupMnemonicByOID(
				ActiveAuthenticationInfo.ECDSA_PLAIN_SHA1_OID));
		assertEquals("SHA256withECDSA", ActiveAuthenticationInfo.lookupMnemonicByOID(
				ActiveAuthenticationInfo.ECDSA_PLAIN_SHA256_OID));
		assertEquals("RIPEMD160withECDSA", ActiveAuthenticationInfo.lookupMnemonicByOID(
				ActiveAuthenticationInfo.ECDSA_PLAIN_RIPEMD160_OID));
		assertThrows(java.security.NoSuchAlgorithmException.class,
				() -> ActiveAuthenticationInfo.lookupMnemonicByOID("1.2.3"));
	}
}
