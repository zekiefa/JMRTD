package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import net.sf.scuba.data.Gender;

class MRZInfoBoundaryTest {

	private static MRZInfo build(String code) {
		return new MRZInfo(code, "UTO", "ERIKSSON", "ANNA MARIA",
				"L898902C3", "UTO", "740812", Gender.FEMALE, "120415", "ZE184226B");
	}

	@Test
	void testDocumentCodeBoundaries() {
		assertThrows(IllegalArgumentException.class, () -> build(null));
		assertThrows(IllegalArgumentException.class, () -> build(""));
		assertThrows(IllegalArgumentException.class, () -> build("XX"));

		assertEquals("P", build("P").getDocumentCode());
		assertEquals("PP", build("PP").getDocumentCode());
		assertEquals(MRZInfo.DOC_TYPE_ID3, build("P").getDocumentType());

		assertNotNull(new MRZInfo("I", "UTO", "XI85935", "OPT",
				"740812", Gender.FEMALE, "120415", "UTO", "OP2",
				"ERIKSSON", "ANNA MARIA"));
		assertNotNull(new MRZInfo("C", "UTO", "XI85935", "",
				"740812", Gender.FEMALE, "120415", "UTO", "",
				"ERIKSSON", "ANNA MARIA"));
		assertNotNull(new MRZInfo("A", "UTO", "XI85935", "",
				"740812", Gender.FEMALE, "120415", "UTO", "",
				"ERIKSSON", "ANNA MARIA"));
	}

	@Test
	void testCheckDigitVec() {
		assertEquals('6', MRZInfo.checkDigit("L898902C3"));
		assertEquals('2', MRZInfo.checkDigit("740812"));
		assertEquals('9', MRZInfo.checkDigit("120415"));
		assertEquals('0', MRZInfo.checkDigit(""));
		assertThrows(RuntimeException.class, () -> MRZInfo.checkDigit("!"));
	}

	@Test
	void testOptionalDataWidth() {
		/* TD1: identifier field max width kills boundary mutation */
		assertThrows(IllegalArgumentException.class, () -> new MRZInfo("I", "UTO", "X1",
				"0123456789ABCDEFX".substring(0, 16),
				"740812", Gender.MALE, "120415", "UTO", null,
				"ERIKSSON", "MARIA"));
	}
}
