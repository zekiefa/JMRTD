package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import net.sf.scuba.data.Country;

public class ICAOCountryTest {

	@Test
	public void testLookupKnownICAOCodes() {
		assertSame(ICAOCountry.DE, ICAOCountry.getInstance("D<<"));
		assertSame(ICAOCountry.UNO, ICAOCountry.getInstance("UNO"));
		assertSame(ICAOCountry.UNA, ICAOCountry.getInstance("UNA"));
		assertSame(ICAOCountry.UNK, ICAOCountry.getInstance("UNK"));
		assertSame(ICAOCountry.XOM, ICAOCountry.getInstance("XOM"));
		assertSame(ICAOCountry.XXA, ICAOCountry.getInstance("XXA"));
		assertSame(ICAOCountry.XXB, ICAOCountry.getInstance("XXB"));
		assertSame(ICAOCountry.XXC, ICAOCountry.getInstance("XXC"));
		assertSame(ICAOCountry.XXX, ICAOCountry.getInstance("XXX"));
		assertSame(ICAOCountry.GBD, ICAOCountry.getInstance("GBD"));
		assertSame(ICAOCountry.GBN, ICAOCountry.getInstance("GBN"));
		assertSame(ICAOCountry.XCC, ICAOCountry.getInstance("XCC"));
	}

	@Test
	public void testLookupFallsBackToISOCountries() {
		Country country = ICAOCountry.getInstance("NLD");
		assertNotNull(country);
		assertEquals("NLD", country.toAlpha3Code());
	}

	@Test
	public void testLookupUnknownCode() {
		assertThrows(IllegalArgumentException.class, () -> ICAOCountry.getInstance("QQQ"));
	}

	@Test
	public void testConstantFields() {
		assertEquals("DE", ICAOCountry.DE.toAlpha2Code());
		assertEquals("D<<", ICAOCountry.DE.toAlpha3Code());
		assertEquals("Germany", ICAOCountry.DE.getName());
		assertEquals("German", ICAOCountry.DE.getNationality());

		assertEquals("UN", ICAOCountry.UNO.toAlpha2Code());
		assertEquals("United Nations Organization", ICAOCountry.UNO.getName());

		assertEquals("XX", ICAOCountry.XXX.toAlpha2Code());
		assertEquals("Unspecified", ICAOCountry.XXX.getName());
		assertEquals("Unspecified", ICAOCountry.XXX.getNationality());
	}

	@Test
	public void testValueOf() {
		assertEquals(-1, ICAOCountry.DE.valueOf());
		assertEquals(-1, ICAOCountry.XXX.valueOf());
	}

	@Test
	public void testToString() {
		assertNotNull(ICAOCountry.DE.toString());
		assertNotNull(ICAOCountry.UNO.toString());
	}
}
