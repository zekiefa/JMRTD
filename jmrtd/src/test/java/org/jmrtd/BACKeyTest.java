package org.jmrtd;

import static org.junit.jupiter.api.Assertions.*;

import java.text.SimpleDateFormat;

import org.junit.jupiter.api.Test;

class BACKeyTest {

	@Test
	void testStringConstructor() {
		BACKey key = new BACKey("L898902C", "740812", "120415");
		assertEquals("L898902C<", key.getDocumentNumber());
		assertEquals("740812", key.getDateOfBirth());
		assertEquals("120415", key.getDateOfExpiry());
	}

	@Test
	void testDateConstructor() throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
		BACKey key = new BACKey("L898902C", sdf.parse("740812"), sdf.parse("120415"));
		assertEquals("L898902C<", key.getDocumentNumber());
		assertEquals("740812", key.getDateOfBirth());
		assertEquals("120415", key.getDateOfExpiry());
	}

	@Test
	void testEqualsHashCodeToString() {
		BACKey a = new BACKey("L898902C", "740812", "120415");
		BACKey b = new BACKey("L898902C", "740812", "120415");
		BACKey c = new BACKey("L898902C", "740812", "120416");
		BACKey d = new BACKey("X000000", "740812", "120415");
		assertEquals(a, a);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertNotEquals(a, d);
		assertNotEquals(a, null);
		assertNotEquals(a, "nope");
		assertNotNull(a.toString());
	}

	@Test
	void testImplementsSpec() {
		BACKeySpec spec = new BACKey("L898902C", "740812", "120415");
		assertEquals("L898902C<", spec.getDocumentNumber());
		assertEquals("740812", spec.getDateOfBirth());
		assertEquals("120415", spec.getDateOfExpiry());
	}

	@Test
	void testDerivedKeysViaUtil() throws Exception {
		BACKey key = new BACKey("L898902C", "740812", "120415");
		byte[] seed = Util.computeKeySeedForBAC(key.getDocumentNumber(), key.getDateOfBirth(), key.getDateOfExpiry());
		assertEquals(24, Util.deriveKey(seed, Util.ENC_MODE).getEncoded().length);
		assertEquals(24, Util.deriveKey(seed, Util.MAC_MODE).getEncoded().length);
	}
}
