package org.jmrtd.cert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.sf.scuba.data.Country;

public class CVCPrincipalTest {

	@Test
	public void testFromName() {
		CVCPrincipal principal = new CVCPrincipal("NLTEST00001");
		assertEquals(Country.getInstance("NL"), principal.getCountry());
		assertEquals("TEST", principal.getMnemonic());
		assertEquals("00001", principal.getSeqNumber());
		assertEquals("NLTEST00001", principal.getName());
		assertEquals("NL/TEST/00001", principal.toString());
	}

	@Test
	public void testFromNameMinimalLength() {
		CVCPrincipal principal = new CVCPrincipal("NL00001");
		assertEquals(Country.getInstance("NL"), principal.getCountry());
		assertEquals("", principal.getMnemonic());
		assertEquals("00001", principal.getSeqNumber());
	}

	@Test
	public void testFromNameLowerCaseCountry() {
		CVCPrincipal principal = new CVCPrincipal("nltest00001");
		assertEquals(Country.getInstance("NL"), principal.getCountry());
	}

	@Test
	public void testFromNameNull() {
		assertThrows(IllegalArgumentException.class, () -> new CVCPrincipal(null));
	}

	@Test
	public void testFromNameTooShort() {
		assertThrows(IllegalArgumentException.class, () -> new CVCPrincipal("NL0001"));
		assertThrows(IllegalArgumentException.class, () -> new CVCPrincipal(""));
	}

	@Test
	public void testFromNameTooLong() {
		assertThrows(IllegalArgumentException.class,
				() -> new CVCPrincipal("NL123456789X00001"));
	}

	@Test
	public void testFromFields() {
		Country country = Country.getInstance("DE");
		CVCPrincipal principal = new CVCPrincipal(country, "MNEMONIC", "12345");
		assertEquals(country, principal.getCountry());
		assertEquals("MNEMONIC", principal.getMnemonic());
		assertEquals("12345", principal.getSeqNumber());
		assertEquals("DEMNEMONIC12345", principal.getName());
	}

	@Test
	public void testFromFieldsInvalidMnemonic() {
		Country country = Country.getInstance("DE");
		assertThrows(IllegalArgumentException.class,
				() -> new CVCPrincipal(country, null, "12345"));
		assertThrows(IllegalArgumentException.class,
				() -> new CVCPrincipal(country, "0123456789", "12345"));
	}

	@Test
	public void testFromFieldsInvalidSeqNumber() {
		Country country = Country.getInstance("DE");
		assertThrows(IllegalArgumentException.class,
				() -> new CVCPrincipal(country, "MNEMO", null));
		assertThrows(IllegalArgumentException.class,
				() -> new CVCPrincipal(country, "MNEMO", "1234"));
		assertThrows(IllegalArgumentException.class,
				() -> new CVCPrincipal(country, "MNEMO", "123456"));
	}

	@Test
	public void testEqualsAndHashCode() {
		CVCPrincipal p1 = new CVCPrincipal("NLTEST00001");
		CVCPrincipal p2 = new CVCPrincipal(Country.getInstance("NL"), "TEST", "00001");
		CVCPrincipal p3 = new CVCPrincipal("NLTEST00002");
		CVCPrincipal p4 = new CVCPrincipal("DETEST00001");
		CVCPrincipal p5 = new CVCPrincipal("NLOTHR00001");

		assertEquals(p1, p1);
		assertEquals(p1, p2);
		assertEquals(p1.hashCode(), p2.hashCode());
		assertNotEquals(p1, null);
		assertNotEquals(p1, "NLTEST00001");
		assertNotEquals(p1, p3);
		assertNotEquals(p1, p4);
		assertNotEquals(p1, p5);
	}

	@Test
	public void testSerializationContract() {
		CVCPrincipal p1 = new CVCPrincipal("NLTEST00001");
		assertTrue(p1 instanceof java.io.Serializable);
		assertTrue(p1 instanceof java.security.Principal);
	}
}
