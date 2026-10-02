package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1Sequence;
import org.junit.jupiter.api.Test;

public class ChipAuthenticationInfoTest {

	@Test
	public void testConstructWithoutKeyId() {
		ChipAuthenticationInfo info = new ChipAuthenticationInfo(
				SecurityInfo.ID_CA_ECDH_3DES_CBC_CBC_OID, 1);
		assertEquals(SecurityInfo.ID_CA_ECDH_3DES_CBC_CBC_OID, info.getObjectIdentifier());
		assertEquals(BigInteger.valueOf(-1), info.getKeyId());
		assertTrue(info.toString().contains("ChipAuthenticationInfo"));
	}

	@Test
	public void testConstructWithKeyId() {
		ChipAuthenticationInfo info = new ChipAuthenticationInfo(
				SecurityInfo.ID_CA_DH_3DES_CBC_CBC_OID, 1, BigInteger.valueOf(42));
		assertEquals(BigInteger.valueOf(42), info.getKeyId());
	}

	@Test
	public void testInvalidOID() {
		assertThrows(IllegalArgumentException.class,
				() -> new ChipAuthenticationInfo("1.2.3.4", 1));
	}

	@Test
	public void testInvalidVersion() {
		assertThrows(IllegalArgumentException.class,
				() -> new ChipAuthenticationInfo(SecurityInfo.ID_CA_DH_3DES_CBC_CBC_OID, 3));
	}

	@Test
	public void testEqualsAndHashCode() {
		ChipAuthenticationInfo a = new ChipAuthenticationInfo(
				SecurityInfo.ID_CA_ECDH_3DES_CBC_CBC_OID, 1, BigInteger.ONE);
		ChipAuthenticationInfo b = new ChipAuthenticationInfo(
				SecurityInfo.ID_CA_ECDH_3DES_CBC_CBC_OID, 1, BigInteger.ONE);
		ChipAuthenticationInfo c = new ChipAuthenticationInfo(
				SecurityInfo.ID_CA_ECDH_3DES_CBC_CBC_OID, 1, BigInteger.TEN);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertNotEquals(null, a);
	}

	@Test
	public void testDERRoundTripWithKeyId() throws Exception {
		ChipAuthenticationInfo info = new ChipAuthenticationInfo(
				SecurityInfo.ID_CA_DH_AES_CBC_CMAC_128_OID, 1, BigInteger.valueOf(7));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		info.writeObject(out);
		SecurityInfo decoded = SecurityInfo.getInstance(
				ASN1Sequence.getInstance(out.toByteArray()));
		assertTrue(decoded instanceof ChipAuthenticationInfo);
		assertEquals(info, decoded);
		assertEquals(BigInteger.valueOf(7),
				((ChipAuthenticationInfo) decoded).getKeyId());
	}

	@Test
	public void testDERRoundTripWithoutKeyId() throws Exception {
		ChipAuthenticationInfo info = new ChipAuthenticationInfo(
				SecurityInfo.ID_CA_ECDH_AES_CBC_CMAC_256_OID, 1);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		info.writeObject(out);
		SecurityInfo decoded = SecurityInfo.getInstance(
				ASN1Sequence.getInstance(out.toByteArray()));
		assertEquals(info, decoded);
	}
}
