package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.security.spec.ECParameterSpec;

import javax.crypto.spec.DHParameterSpec;

import org.bouncycastle.asn1.ASN1Sequence;
import org.junit.jupiter.api.Test;

public class PACEInfoTest {

	private static final String OID = SecurityInfo.ID_PACE_ECDH_GM_3DES_CBC_CBC;

	@Test
	public void testConstructAndGetters() {
		PACEInfo info = new PACEInfo(OID, 2, PACEInfo.PARAM_ID_ECP_BRAINPOOL_P256_R1);
		assertEquals(OID, info.getObjectIdentifier());
		assertEquals(2, info.getVersion());
		assertEquals(PACEInfo.PARAM_ID_ECP_BRAINPOOL_P256_R1, info.getParameterId());
		assertTrue(info.toString().contains("parameterId"));
	}

	@Test
	public void testConstructWithoutParameterId() {
		PACEInfo info = new PACEInfo(OID, 2, -1);
		assertEquals(-1, info.getParameterId());
		assertFalse(info.toString().contains("parameterId"));
	}

	@Test
	public void testInvalidOID() {
		assertThrows(IllegalArgumentException.class,
				() -> new PACEInfo("1.2.3.4.5", 2, 0));
	}

	@Test
	public void testInvalidVersion() {
		assertThrows(IllegalArgumentException.class,
				() -> new PACEInfo(OID, 1, 0));
	}

	@Test
	public void testEqualsAndHashCode() {
		PACEInfo a = new PACEInfo(OID, 2, 13);
		PACEInfo b = new PACEInfo(OID, 2, 13);
		PACEInfo c = new PACEInfo(OID, 2, 14);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertFalse(a.equals(null));
	}

	@Test
	public void testDERRoundTrip() throws Exception {
		PACEInfo info = new PACEInfo(OID, 2, 13);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		info.writeObject(out);
		byte[] encoded = out.toByteArray();

		PACEInfo viaFactory = PACEInfo.createPACEInfo(encoded);
		assertEquals(info, viaFactory);

		SecurityInfo viaSecurityInfoFactory = SecurityInfo.getInstance(
				ASN1Sequence.getInstance(encoded));
		assertTrue(viaSecurityInfoFactory instanceof PACEInfo);
		assertEquals(info, viaSecurityInfoFactory);
	}

	@Test
	public void testStaticOIDClassification() {
		assertEquals(PACEInfo.MappingType.GM, PACEInfo.toMappingType(OID));
		assertEquals(PACEInfo.MappingType.IM,
				PACEInfo.toMappingType(SecurityInfo.ID_PACE_DH_IM_AES_CBC_CMAC_128));
		assertEquals("ECDH", PACEInfo.toKeyAgreementAlgorithm(OID));
		assertEquals("DH", PACEInfo.toKeyAgreementAlgorithm(
				SecurityInfo.ID_PACE_DH_GM_3DES_CBC_CBC));
		assertEquals("DESede", PACEInfo.toCipherAlgorithm(OID));
		assertEquals("AES", PACEInfo.toCipherAlgorithm(
				SecurityInfo.ID_PACE_ECDH_GM_AES_CBC_CMAC_128));
		assertEquals("SHA-1", PACEInfo.toDigestAlgorithm(OID));
		assertEquals("SHA-256", PACEInfo.toDigestAlgorithm(
				SecurityInfo.ID_PACE_ECDH_GM_AES_CBC_CMAC_192));
		assertEquals(128, PACEInfo.toKeyLength(OID));
		assertEquals(192, PACEInfo.toKeyLength(
				SecurityInfo.ID_PACE_ECDH_GM_AES_CBC_CMAC_192));
		assertEquals(256, PACEInfo.toKeyLength(
				SecurityInfo.ID_PACE_ECDH_GM_AES_CBC_CMAC_256));
	}

	@Test
	public void testToParameterSpec() {
		assertTrue(PACEInfo.toParameterSpec(PACEInfo.PARAM_ID_GFP_1024_160)
				instanceof DHParameterSpec);
		assertTrue(PACEInfo.toParameterSpec(PACEInfo.PARAM_ID_ECP_NIST_P192_R1)
				instanceof ECParameterSpec);
		assertTrue(PACEInfo.toParameterSpec(PACEInfo.PARAM_ID_ECP_BRAINPOOL_P256_R1)
				instanceof ECParameterSpec);
		assertThrows(NumberFormatException.class, () -> PACEInfo.toParameterSpec(99));
	}
}
