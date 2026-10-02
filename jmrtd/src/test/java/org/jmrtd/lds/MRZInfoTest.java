package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

import net.sf.scuba.data.Gender;

public class MRZInfoTest {

	private static final String TD3_MRZ =
			"P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
			+ "L898902C36UTO7408122F1204159ZE184226B<<<<<<10";

	@Test
	public void testParseTD3String() {
		MRZInfo mrz = new MRZInfo(TD3_MRZ);
		assertEquals(MRZInfo.DOC_TYPE_ID3, mrz.getDocumentType());
		assertEquals("P", mrz.getDocumentCode());
		assertEquals("UTO", mrz.getIssuingState());
		assertEquals("UTO", mrz.getNationality());
		assertEquals("ERIKSSON", mrz.getPrimaryIdentifier());
		assertArrayEquals(new String[] { "ANNA", "MARIA" },
				mrz.getSecondaryIdentifierComponents());
		assertEquals("L898902C3", mrz.getDocumentNumber());
		assertEquals("740812", mrz.getDateOfBirth());
		assertEquals("120415", mrz.getDateOfExpiry());
		assertEquals(Gender.FEMALE, mrz.getGender());
		assertEquals("ZE184226B", mrz.getPersonalNumber());
	}

	@Test
	public void testParseTD3WithNewlines() {
		String withNewlines = TD3_MRZ.substring(0, 44) + "\n" + TD3_MRZ.substring(44);
		MRZInfo mrz = new MRZInfo(withNewlines);
		assertEquals("ERIKSSON", mrz.getPrimaryIdentifier());
		assertEquals("L898902C3", mrz.getDocumentNumber());
	}

	@Test
	public void testParseTD3FromInputStream() {
		MRZInfo mrz = new MRZInfo(new ByteArrayInputStream(TD3_MRZ.getBytes()), TD3_MRZ.length());
		assertEquals(MRZInfo.DOC_TYPE_ID3, mrz.getDocumentType());
		assertEquals("ERIKSSON", mrz.getPrimaryIdentifier());
	}

	@Test
	public void testFieldConstructorTD3() {
		MRZInfo mrz = LDSTestHelper.createTestMRZInfo();
		assertEquals("P", mrz.getDocumentCode());
		assertEquals(MRZInfo.DOC_TYPE_ID3, mrz.getDocumentType());
		assertEquals("UTO", mrz.getIssuingState());
		assertEquals("ERIKSSON", mrz.getPrimaryIdentifier());
		assertEquals("L898902C3", mrz.getDocumentNumber());
		assertEquals("740812", mrz.getDateOfBirth());
		assertEquals(Gender.FEMALE, mrz.getGender());
		assertEquals("120415", mrz.getDateOfExpiry());
		assertEquals("ZE184226B", mrz.getPersonalNumber());
	}

	@Test
	public void testFieldConstructorEqualsParsed() {
		MRZInfo parsed = new MRZInfo(TD3_MRZ);
		MRZInfo constructed = LDSTestHelper.createTestMRZInfo();
		assertEquals(constructed.getDocumentNumber(), parsed.getDocumentNumber());
		assertEquals(constructed.getDateOfBirth(), parsed.getDateOfBirth());
		assertEquals(constructed.getDateOfExpiry(), parsed.getDateOfExpiry());
		assertEquals(constructed.getNationality(), parsed.getNationality());
		assertEquals(constructed.getDocumentCode(), parsed.getDocumentCode());
		assertEquals(constructed.getPrimaryIdentifier(), parsed.getPrimaryIdentifier());
	}

	@Test
	public void testEncodeDecodeRoundTrip() {
		MRZInfo mrz = LDSTestHelper.createTestMRZInfo();
		String encoded = new String(mrz.getEncoded());
		MRZInfo decoded = new MRZInfo(encoded);
		assertEquals(mrz, decoded);
	}

	@Test
	public void testToStringID3() {
		MRZInfo mrz = new MRZInfo(TD3_MRZ);
		String str = mrz.toString();
		String[] lines = str.split("\n");
		assertEquals(2, lines.length);
		assertEquals(44, lines[0].length());
		assertEquals(44, lines[1].length());
		assertTrue(lines[0].startsWith("P<UTOERIKSSON<<"));
	}

	@Test
	public void testCheckDigit() {
		assertEquals('6', MRZInfo.checkDigit("L898902C3"));
		assertEquals('2', MRZInfo.checkDigit("740812"));
		assertEquals('9', MRZInfo.checkDigit("120415"));
	}

	@Test
	public void testEqualsNotEquals() {
		MRZInfo mrz = new MRZInfo(TD3_MRZ);
		assertNotEquals(null, mrz);
		assertNotEquals("not an mrz", mrz);
		MRZInfo other = new MRZInfo("P", "UTO", "OTHER", "ANNA MARIA",
				"L898902C3", "UTO", "740812", Gender.FEMALE, "120415", "ZE184226B");
		assertNotEquals(mrz, other);
	}

	@Test
	public void testSetters() {
		MRZInfo mrz = LDSTestHelper.createTestMRZInfo();
		mrz.setDateOfBirth("800101");
		assertEquals("800101", mrz.getDateOfBirth());
		mrz.setDateOfExpiry("250101");
		assertEquals("250101", mrz.getDateOfExpiry());
		mrz.setDocumentNumber("S12345678");
		assertEquals("S12345678", mrz.getDocumentNumber());
		mrz.setGender(Gender.MALE);
		assertEquals(Gender.MALE, mrz.getGender());
		mrz.setNationality("NLD");
		assertEquals("NLD", mrz.getNationality());
		mrz.setIssuingState("NLD");
		assertEquals("NLD", mrz.getIssuingState());
		mrz.setPrimaryIdentifier("SMITH");
		assertEquals("SMITH", mrz.getPrimaryIdentifier());
		mrz.setPersonalNumber("123456");
		assertEquals("123456", mrz.getPersonalNumber());
	}

	@Test
	public void testNullStringConstructor() {
		assertThrows(IllegalArgumentException.class, () -> new MRZInfo((String) null));
	}

	@Test
	public void testTooShortString() {
		assertThrows(IllegalArgumentException.class, () -> new MRZInfo("SHORT"));
	}

	@Test
	public void testMissingPrimaryIdentifierDelimiter() {
		/* valid length but no "<<": parser tolerates, primary identifier degrades gracefully */
		String bad = ("P<UTOERIKSSON" + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"
				+ "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<").substring(0, 44)
				+ TD3_MRZ.substring(44);
		MRZInfo mrz = new MRZInfo(bad);
		assertNotNull(mrz.getPrimaryIdentifier());
	}

	@Test
	public void testInvalidDocumentCodeFieldConstructor() {
		assertThrows(IllegalArgumentException.class, () -> new MRZInfo("X1", "UTO",
				"ERIKSSON", "ANNA MARIA", "L898902C3", "UTO", "740812",
				Gender.FEMALE, "120415", "ZE184226B"));
	}

	@Test
	public void testID1FieldConstructor() {
		MRZInfo mrz = new MRZInfo("I", "UTO", "D23145890734", "",
				"900101", Gender.FEMALE, "180101", "UTO", "",
				"ERIKSSON", "ANNA MARIA");
		assertEquals("I", mrz.getDocumentCode());
		assertEquals(MRZInfo.DOC_TYPE_ID1, mrz.getDocumentType());
	}
}
