package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.junit.jupiter.api.Test;

public class SecurityInfoTest {

	@Test
	public void testOIDConstants() {
		assertEquals("2.23.136.1.1.5", SecurityInfo.ID_AA_OID);
		assertTrue(SecurityInfo.ID_PACE.startsWith("0.4.0.127.0.7"));
		assertTrue(SecurityInfo.ID_PACE_ECDH_GM_3DES_CBC_CBC
				.startsWith(SecurityInfo.ID_PACE_ECDH_GM));
	}

	@Test
	public void testGetInstanceReturnsNullForUnknownOID() throws Exception {
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new ASN1ObjectIdentifier("1.2.3.4.5"));
		v.add(new ASN1Integer(1));
		byte[] encoded = new DERSequence(v).getEncoded();
		/* unknown OID may return null or throw IllegalArgumentException depending on parse path */
		try {
			assertNull(SecurityInfo.getInstance(ASN1Sequence.getInstance(encoded)));
		} catch (IllegalArgumentException ok) {
			/* tolerated */
		}
	}

	@Test
	public void testGetInstanceMalformedInputThrows() {
		ASN1Sequence malformed = new DERSequence(new ASN1EncodableVector());
		assertThrows(IllegalArgumentException.class,
				() -> SecurityInfo.getInstance(malformed));
	}

	@Test
	public void testLookupMnemonicByOID() throws Exception {
		assertEquals("id_PK_DH", SecurityInfo.lookupMnemonicByOID(SecurityInfo.ID_PK_DH_OID));
		assertEquals("id_PK_ECDH", SecurityInfo.lookupMnemonicByOID(SecurityInfo.ID_PK_ECDH_OID));
		assertEquals("id_TA", SecurityInfo.lookupMnemonicByOID(SecurityInfo.ID_TA_OID));
		assertThrows(java.security.NoSuchAlgorithmException.class,
				() -> SecurityInfo.lookupMnemonicByOID("1.2.3.4"));
	}

	@Test
	public void testWriteObjectProducesDERBytes() throws Exception {
		SecurityInfo info = new TerminalAuthenticationInfo();
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		info.writeObject(out);
		byte[] der = out.toByteArray();
		assertNotNull(der);
		assertTrue(der.length > 0);
		/* must be parseable DER sequence */
		ASN1Sequence.getInstance(der);
	}
}
