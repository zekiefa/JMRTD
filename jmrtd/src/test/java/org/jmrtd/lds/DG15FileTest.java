package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.PublicKey;

import org.junit.jupiter.api.Test;

public class DG15FileTest {

	@Test
	public void testRoundTripRSA() throws Exception {
		KeyPair keyPair = LDSTestHelper.generateRSAKeyPair();
		PublicKey publicKey = keyPair.getPublic();
		DG15File dg15 = new DG15File(publicKey);
		assertEquals(LDSFile.EF_DG15_TAG, dg15.getTag());
		assertEquals(publicKey, dg15.getPublicKey());
		assertTrue(dg15.toString().startsWith("DG15File"));
		assertTrue(dg15.getLength() > 0);

		byte[] encoded = dg15.getEncoded();
		DG15File copy = new DG15File(new ByteArrayInputStream(encoded));
		assertEquals(dg15, copy);
		assertEquals(dg15.hashCode(), copy.hashCode());
		assertEquals(publicKey, copy.getPublicKey());
	}

	@Test
	public void testRoundTripEC() throws Exception {
		java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("EC");
		generator.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
		PublicKey publicKey = generator.generateKeyPair().getPublic();
		DG15File dg15 = new DG15File(publicKey);
		DG15File copy = new DG15File(new ByteArrayInputStream(dg15.getEncoded()));
		assertEquals(publicKey.getEncoded().length > 1, true);
		assertEquals(dg15, copy);
	}

	@Test
	public void testBadKeyBytes() {
		byte[] content = new byte[] { 0x01, 0x02, 0x03, 0x04 };
		byte[] file = new byte[content.length + 2];
		file[0] = (byte) LDSFile.EF_DG15_TAG;
		file[1] = (byte) content.length;
		System.arraycopy(content, 0, file, 2, content.length);
		assertThrows(IllegalArgumentException.class, () -> new DG15File(new ByteArrayInputStream(file)));
	}

	@Test
	public void testEquals() throws Exception {
		KeyPair keyPair = LDSTestHelper.generateRSAKeyPair();
		DG15File dg15 = new DG15File(keyPair.getPublic());
		assertEquals(dg15, new DG15File(keyPair.getPublic()));
		assertNotEquals(dg15, null);
		assertNotEquals(dg15, new Object());
		KeyPair other = LDSTestHelper.generateRSAKeyPair();
		assertNotEquals(dg15, new DG15File(other.getPublic()));
	}
}
