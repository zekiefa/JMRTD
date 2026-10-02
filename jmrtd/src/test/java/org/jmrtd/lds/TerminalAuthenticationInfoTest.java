package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;

import org.bouncycastle.asn1.ASN1Sequence;
import org.junit.jupiter.api.Test;

public class TerminalAuthenticationInfoTest {

	@Test
	public void testDefaultConstructor() {
		TerminalAuthenticationInfo info = new TerminalAuthenticationInfo();
		assertEquals(SecurityInfo.ID_TA_OID, info.getObjectIdentifier());
		assertEquals(-1, info.getFileId());
		assertEquals(-1, info.getShortFileId());
		assertTrue(info.toString().contains("TerminalAuthenticationInfo"));
	}

	@Test
	public void testFileIdConstructor() {
		TerminalAuthenticationInfo info = new TerminalAuthenticationInfo((short) 0x011C, (byte) 0x1C);
		assertEquals(0x011C, info.getFileId());
		assertEquals((byte) 0x1C, info.getShortFileId());
	}

	@Test
	public void testEqualsAndHashCode() {
		TerminalAuthenticationInfo a = new TerminalAuthenticationInfo();
		TerminalAuthenticationInfo b = new TerminalAuthenticationInfo();
		TerminalAuthenticationInfo c = new TerminalAuthenticationInfo((short) 0x011C, (byte) 0x1C);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertNotEquals(null, a);
	}

	@Test
	public void testDERRoundTripWithoutFileId() throws Exception {
		TerminalAuthenticationInfo info = new TerminalAuthenticationInfo();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		info.writeObject(out);
		SecurityInfo decoded = SecurityInfo.getInstance(
				ASN1Sequence.getInstance(out.toByteArray()));
		assertTrue(decoded instanceof TerminalAuthenticationInfo);
		assertEquals(info, decoded);
	}

	@Test
	public void testDERRoundTripWithFileId() throws Exception {
		TerminalAuthenticationInfo info = new TerminalAuthenticationInfo((short) 0x011C, (byte) 0x1C);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		info.writeObject(out);
		SecurityInfo decoded = SecurityInfo.getInstance(
				ASN1Sequence.getInstance(out.toByteArray()));
		assertTrue(decoded instanceof TerminalAuthenticationInfo);
		assertEquals(info, decoded);
		assertEquals(0x011C,
				((TerminalAuthenticationInfo) decoded).getFileId());
		assertEquals((byte) 0x1C,
				((TerminalAuthenticationInfo) decoded).getShortFileId());
	}
}
