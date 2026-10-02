package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class CardAccessFileTest {

	private Set<SecurityInfo> sampleSecurityInfos() {
		Set<SecurityInfo> infos = new HashSet<SecurityInfo>();
		infos.add(new PACEInfo(SecurityInfo.ID_PACE_ECDH_GM_AES_CBC_CMAC_128, 2, 13));
		infos.add(new ChipAuthenticationInfo(SecurityInfo.ID_CA_ECDH_AES_CBC_CMAC_128_OID, ChipAuthenticationInfo.VERSION_NUM, BigInteger.ONE));
		return infos;
	}

	@Test
	public void testConstruct() {
		CardAccessFile file = new CardAccessFile(sampleSecurityInfos());
		assertEquals(2, file.getSecurityInfos().size());
		assertTrue(file.toString().startsWith("CardAccessFile"));
		assertEquals(1, file.getPACEInfos().size());
		PACEInfo paceInfo = file.getPACEInfos().iterator().next();
		assertEquals(SecurityInfo.ID_PACE_ECDH_GM_AES_CBC_CMAC_128, paceInfo.getObjectIdentifier());
	}

	@Test
	public void testNullSecurityInfos() {
		assertThrows(IllegalArgumentException.class, () -> new CardAccessFile((java.util.Collection<SecurityInfo>) null));
	}

	@Test
	public void testRoundTrip() throws Exception {
		CardAccessFile file = new CardAccessFile(sampleSecurityInfos());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		file.writeContent(out);
		out.flush();
		CardAccessFile copy = new CardAccessFile(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(file, copy);
		assertEquals(file.hashCode(), copy.hashCode());
		assertEquals(2, copy.getSecurityInfos().size());
		assertEquals(1, copy.getPACEInfos().size());
	}

	@Test
	public void testEmpty() throws Exception {
		CardAccessFile file = new CardAccessFile(new HashSet<SecurityInfo>());
		assertTrue(file.getSecurityInfos().isEmpty());
		assertTrue(file.getPACEInfos().isEmpty());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		file.writeContent(out);
		CardAccessFile copy = new CardAccessFile(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(file, copy);
	}

	@Test
	public void testEquals() {
		CardAccessFile file = new CardAccessFile(sampleSecurityInfos());
		assertEquals(file, new CardAccessFile(sampleSecurityInfos()));
		assertNotEquals(file, null);
		assertNotEquals(file, new Object());
		assertNotEquals(file, new CardAccessFile(new HashSet<SecurityInfo>()));
	}
}
