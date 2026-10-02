package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class DG14FileTest {

	private static final short CVCA_FILE_ID = 0x0104;

	private Set<SecurityInfo> sampleSecurityInfos() {
		Set<SecurityInfo> infos = new HashSet<SecurityInfo>();
		infos.add(new TerminalAuthenticationInfo(CVCA_FILE_ID, (byte) 0x04));
		infos.add(new ChipAuthenticationInfo(SecurityInfo.ID_CA_ECDH_AES_CBC_CMAC_128_OID, ChipAuthenticationInfo.VERSION_NUM, BigInteger.ONE));
		infos.add(new ActiveAuthenticationInfo(ActiveAuthenticationInfo.ECDSA_PLAIN_SHA256_OID));
		return infos;
	}

	@Test
	public void testConstruct() {
		DG14File dg14 = new DG14File(sampleSecurityInfos());
		assertEquals(LDSFile.EF_DG14_TAG, dg14.getTag());
		assertEquals(3, dg14.getSecurityInfos().size());
		assertTrue(dg14.toString().startsWith("DG14File"));
		assertTrue(dg14.getLength() > 0);
	}

	@Test
	public void testNullSecurityInfos() {
		assertThrows(IllegalArgumentException.class, () -> new DG14File((java.util.Collection<SecurityInfo>) null));
	}

	@Test
	public void testRoundTrip() throws Exception {
		DG14File dg14 = new DG14File(sampleSecurityInfos());
		byte[] encoded = dg14.getEncoded();
		DG14File copy = new DG14File(new ByteArrayInputStream(encoded));
		assertEquals(dg14, copy);
		assertEquals(dg14.hashCode(), copy.hashCode());
		assertEquals(3, copy.getSecurityInfos().size());
	}

	@Test
	public void testCVCAFileIds() {
		DG14File dg14 = new DG14File(sampleSecurityInfos());
		List<Short> fids = dg14.getCVCAFileIds();
		assertEquals(1, fids.size());
		assertEquals(Short.valueOf(CVCA_FILE_ID), fids.get(0));
		assertEquals(0x04, dg14.getCVCAShortFileId(CVCA_FILE_ID));
		assertEquals(-1, dg14.getCVCAShortFileId(0x9999));

		/* Default TA info has no EF.CVCA listing. */
		Set<SecurityInfo> infos = new HashSet<SecurityInfo>();
		infos.add(new TerminalAuthenticationInfo());
		DG14File noTARef = new DG14File(infos);
		assertTrue(noTARef.getCVCAFileIds().isEmpty());
	}

	@Test
	public void testChipAuthenticationInfos() throws Exception {
		DG14File dg14 = new DG14File(new ByteArrayInputStream(new DG14File(sampleSecurityInfos()).getEncoded()));
		Map<BigInteger, String> map = dg14.getChipAuthenticationInfos();
		assertEquals(1, map.size());
		assertEquals(SecurityInfo.ID_CA_ECDH_AES_CBC_CMAC_128_OID, map.get(BigInteger.ONE));
	}

	@Test
	public void testActiveAuthenticationInfos() {
		DG14File dg14 = new DG14File(sampleSecurityInfos());
		List<ActiveAuthenticationInfo> aaInfos = dg14.getActiveAuthenticationInfos();
		assertEquals(1, aaInfos.size());
		assertEquals(ActiveAuthenticationInfo.ECDSA_PLAIN_SHA256_OID, aaInfos.get(0).getSignatureAlgorithmOID());
	}

	@Test
	public void testNoPublicKeyInfos() {
		DG14File dg14 = new DG14File(sampleSecurityInfos());
		assertThrows(IllegalStateException.class, () -> dg14.getChipAuthenticationPublicKeyInfos());
	}

	@Test
	public void testEquals() {
		DG14File dg14 = new DG14File(sampleSecurityInfos());
		assertEquals(dg14, new DG14File(sampleSecurityInfos()));
		assertNotEquals(dg14, null);
		assertNotEquals(dg14, new Object());
		assertNotEquals(dg14, new DG14File(new ArrayList<SecurityInfo>()));
	}
}
