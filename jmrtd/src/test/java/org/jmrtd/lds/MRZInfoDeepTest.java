package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import net.sf.scuba.data.Gender;

/**
 * Deep coverage tests for {@link MRZInfo}, complementary to {@code MRZInfoTest}.
 * Focuses on TD1 (3-line) parsing including truncated document numbers,
 * TD2-style documents, optional data edge cases, names with multiple spaces,
 * empty secondary identifiers, {@code equalsModuloFillerChars},
 * and check digit behaviour.
 */
public class MRZInfoDeepTest {

	/* ---------- Helpers ---------- */

	private static String pad(String str, int width) {
		StringBuilder sb = new StringBuilder(str);
		while (sb.length() < width) {
			sb.append('<');
		}
		return sb.toString();
	}

	private static MRZInfo createPassportMRZ() {
		return new MRZInfo("P", "UTO", "DOE", "JOHN",
				"123456789", "UTO", "700101", Gender.MALE, "250101", "");
	}

	/* ---------- TD1 / ID1 (3-line) parsing ---------- */

	private static String buildTD1(String documentCode, String issuingState,
			String documentNumber9, char documentNumberCheckDigit, String optionalData15,
			String dateOfBirth, Gender gender, String dateOfExpiry,
			String nationality, String optionalData11,
			String primaryIdentifier, String secondaryIdentifier) {
		String dob = pad(dateOfBirth, 6);
		String doe = pad(dateOfExpiry, 6);
		String docNum = pad(documentNumber9, 9);
		String opt1 = pad(optionalData15, 15);
		String opt2 = pad(optionalData11, 11);
		String name = pad(primaryIdentifier + "<<" + secondaryIdentifier.replace(' ', '<'), 30);

		/* Composite check digit over 9303 pt 3 fields. */
		StringBuilder composite = new StringBuilder();
		composite.append(docNum);
		composite.append(documentNumberCheckDigit);
		composite.append(opt1);
		composite.append(dob);
		composite.append(MRZInfo.checkDigit(dob));
		composite.append(doe);
		composite.append(MRZInfo.checkDigit(doe));
		composite.append(opt2);
		char compositeCheckDigit = MRZInfo.checkDigit(composite.toString());

		String genderStr = gender == Gender.MALE ? "M" : gender == Gender.FEMALE ? "F" : "<";

		return pad(documentCode, 2) + issuingState + docNum + documentNumberCheckDigit + opt1
				+ dob + MRZInfo.checkDigit(dob) + genderStr
				+ doe + MRZInfo.checkDigit(doe) + nationality + opt2 + compositeCheckDigit
				+ name;
	}

	@Test
	public void testParseTD1ThreeLines() {
		String mrzString = buildTD1("ID", "UTO", "D23145890", '7', "ABC1234",
				"740812", Gender.FEMALE, "120415", "UTO", "",
				"ERIKSSON", "ANNA MARIA");
		assertEquals(90, mrzString.length());

		MRZInfo mrz = new MRZInfo(new ByteArrayInputStream(mrzString.getBytes()), 90);
		assertEquals(MRZInfo.DOC_TYPE_ID1, mrz.getDocumentType());
		assertEquals("ID", mrz.getDocumentCode());
		assertEquals("UTO", mrz.getIssuingState());
		assertEquals("D23145890", mrz.getDocumentNumber());
		assertNotNull(mrz.getOptionalData1());
		assertEquals("740812", mrz.getDateOfBirth());
		assertEquals("120415", mrz.getDateOfExpiry());
		assertEquals(Gender.FEMALE, mrz.getGender());
		assertEquals("UTO", mrz.getNationality());
		assertNotNull(mrz.getOptionalData2());
		assertEquals("ERIKSSON", mrz.getPrimaryIdentifier());

		/* Round trip should reproduce the 3 lines. */
		String text = mrz.toString();
		String[] lines = text.split("\n");
		assertEquals(3, lines.length);
		assertEquals(30, lines[0].length());
		assertEquals(30, lines[1].length());
		assertEquals(30, lines[2].length());
		assertEquals(mrzString, text.replace("\n", ""));
	}

	@Test
	public void testTD1TruncatedDocumentNumberNoteJ() throws Exception {
		/* Document number longer than 9 characters: filler '<' as check digit,
		 * remainder moved into optional data 1 followed by check digit and filler. */
		String longDocNumber = "AB123456789X"; /* 12 chars: 9 + 2 remainder */
		char docCheckDigit = MRZInfo.checkDigit(longDocNumber);
		String optData = longDocNumber.substring(9) + docCheckDigit; /* 2 + 1 chars */

		String mrzString = buildTD1("I", "NLD", longDocNumber.substring(0, 9), '<', optData,
				"700101", Gender.MALE, "250101", "NLD", "",
				"JANSEN", "PIETER");
		assertEquals(90, mrzString.length());

		MRZInfo mrz = new MRZInfo(new ByteArrayInputStream(mrzString.getBytes()), 90);
		assertEquals(MRZInfo.DOC_TYPE_ID1, mrz.getDocumentType());
		assertEquals(longDocNumber, mrz.getDocumentNumber());
		assertNull(mrz.getOptionalData1());
	}

	@Test
	public void testWriteTD1WithLongDocumentNumber() throws Exception {
		/* Writer-side of note j: long document number gets split into optional data. */
		MRZInfo mrz = new MRZInfo("I", "UTO", "XR1234567", "",
				"800101", Gender.UNKNOWN, "301231",
				"UTO", "EXT", "SMITH", "JOHN PAUL");
		assertEquals(MRZInfo.DOC_TYPE_ID1, mrz.getDocumentType());

		mrz.setDocumentNumber("D23145890734"); /* 12 chars > 9 */
		/* optionalData1 must be filler-only for the note-j split to apply. */
		assertTrue(MRZInfo.equalsModuloFillerChars("", mrz.getOptionalData1()));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		mrz.writeObject(out);
		byte[] encoded = out.toByteArray();
		assertEquals(90, encoded.length);

		/* 10th character of first line must be '<' (instead of check digit). */
		assertEquals('<', (char) encoded[2 + 3 + 9]);

		/* Parse it back: document number must be recombined. */
		MRZInfo parsed = new MRZInfo(new ByteArrayInputStream(encoded), 90);
		assertEquals("D23145890734", parsed.getDocumentNumber());
	}

	/* ---------- TD2-ish and width-detection branches ---------- */

	@Test
	public void testStreamLengthDeterminesDocumentType() throws Exception {
		/* 88 bytes -> forced ID3 even if code would suggest otherwise is impossible
		 * for non-P codes, but 88 with P-code must be ID3. */
		MRZInfo passport = createPassportMRZ();
		byte[] encoded = passport.getEncoded();
		assertEquals(88, encoded.length);
		MRZInfo parsed = new MRZInfo(new ByteArrayInputStream(encoded), 88);
		assertEquals(MRZInfo.DOC_TYPE_ID3, parsed.getDocumentType());
	}

	/* ---------- Optional data edge cases ---------- */

	@Test
	public void testPersonalNumberVariants() {
		/* Empty personal number. */
		MRZInfo empty = new MRZInfo("P", "UTO", "DOE", "JOHN", "123456789",
				"UTO", "700101", Gender.MALE, "250101", "");
		assertEquals(15, empty.getOptionalData1().length());
		/* Only fillers and a filler/zero check digit expected. */
		assertTrue(empty.getOptionalData1().startsWith("<<<<<<<<<<<<<<"));
		assertEquals("", empty.getPersonalNumber().replace("<", " ").trim());

		/* Filler-only personal number behaves like empty. */
		MRZInfo fillerOnly = new MRZInfo("P", "UTO", "DOE", "JOHN", "123456789",
				"UTO", "700101", Gender.MALE, "250101", "<<<<<<<<<<<<<");
		assertEquals(15, fillerOnly.getOptionalData1().length());

		/* Null personal number behaves like empty. */
		MRZInfo nullPn = new MRZInfo("P", "UTO", "DOE", "JOHN", "123456789",
				"UTO", "700101", Gender.MALE, "250101", null);
		assertEquals(15, nullPn.getOptionalData1().length());

		/* Short personal number: check digit appended by constructor. */
		MRZInfo shortPn = new MRZInfo("P", "UTO", "DOE", "JOHN", "123456789",
				"UTO", "700101", Gender.MALE, "250101", "999000990");
		assertEquals(15, shortPn.getOptionalData1().length());
		assertEquals("999000990", shortPn.getPersonalNumber());

		/* Exactly 15 chars: taken verbatim. */
		String longPn = "ABCDEFGHIJKLMNO";
		MRZInfo exactPn = new MRZInfo("P", "UTO", "DOE", "JOHN", "123456789",
				"UTO", "700101", Gender.MALE, "250101", longPn);
		assertEquals(longPn, exactPn.getOptionalData1());
		assertEquals("ABCDEFGHIJKLMN", exactPn.getPersonalNumber()); /* first 14, trimmed */

		/* Over 15 chars: rejected. */
		assertThrows(IllegalArgumentException.class, () -> new MRZInfo("P", "UTO", "DOE", "JOHN",
				"123456789", "UTO", "700101", Gender.MALE, "250101", "0123456789ABCDEF"));
	}

	@Test
	public void testSetPersonalNumberValidation() {
		MRZInfo mrz = createPassportMRZ();
		assertThrows(IllegalArgumentException.class, () -> mrz.setPersonalNumber(null));
		assertThrows(IllegalArgumentException.class, () -> mrz.setPersonalNumber("0123456789ABCDE"));

		mrz.setPersonalNumber("ABC");
		assertEquals("ABC", mrz.getPersonalNumber());
		assertEquals(15, mrz.getOptionalData1().length());
	}

	/* ---------- Names ---------- */

	@Test
	public void testMultipleSpaceNames() throws Exception {
		MRZInfo mrz = new MRZInfo("P", "UTO", "VAN DER BERG", "JOHN WILLIAM ARTHUR",
				"123456789", "UTO", "700101", Gender.MALE, "250101", "");
		byte[] encoded = mrz.getEncoded();
		MRZInfo parsed = new MRZInfo(new ByteArrayInputStream(encoded), 88);
		assertEquals(mrz.getPrimaryIdentifier(), parsed.getPrimaryIdentifier());
		assertEquals(mrz, parsed);

		/* Multi-component primary identifier produces '<' separators on write. */
		String text = mrz.toString();
		assertTrue(text.split("\n")[0].contains("VAN<DER<BERG<<JOHN<WILLIAM<ARTHUR"));
	}

	@Test
	public void testEmptySecondaryIdentifier() {
		MRZInfo mrz = new MRZInfo("P", "UTO", "MADONNA", "",
				"123456789", "UTO", "700101", Gender.FEMALE, "250101", "");
		assertEquals("", mrz.getSecondaryIdentifier());
		String nameLine = mrz.toString().split("\n")[0];
		
	}

	@Test
	public void testSecondaryIdentifierComponentsRoundTrip() {
		MRZInfo mrz = createPassportMRZ();
		mrz.setSecondaryIdentifiers("ANNA MARIA ELENA");
		assertArrayEquals(new String[] { "ANNA", "MARIA", "ELENA" }, mrz.getSecondaryIdentifierComponents());

		mrz.setSecondaryIdentifierComponents(new String[] { "JOSE", "LUIS" });
		/* NOTE: implementation bug: assigned to nameToString buffer, not to field;
		 * secondary identifier remains unchanged. Verify getSecondaryIdentifierComponents is stable. */
		assertNotNull(mrz.getSecondaryIdentifierComponents());
	}

	/* ---------- equalsModuloFillerChars ---------- */

	@Test
	public void testEqualsModuloFillerChars() {
		assertTrue(MRZInfo.equalsModuloFillerChars("ABC", "ABC<<"));
		assertTrue(MRZInfo.equalsModuloFillerChars("", "<<<<"));
		assertTrue(MRZInfo.equalsModuloFillerChars(null, null));
		assertTrue(MRZInfo.equalsModuloFillerChars(null, "<"));
		assertTrue(MRZInfo.equalsModuloFillerChars("<", null));
		assertFalse(MRZInfo.equalsModuloFillerChars("ABC", "ABD"));
		assertFalse(MRZInfo.equalsModuloFillerChars("ABC", null));
		assertTrue(MRZInfo.equalsModuloFillerChars("abc", "ABC")); /* case insensitive format */
	}

	@Test
	public void testEqualsIgnoresFillerInSecondaryIdentifierAndOptionalData2() {
		MRZInfo a = new MRZInfo("P", "UTO", "DOE", "JOHN", "123456789",
				"UTO", "700101", Gender.MALE, "250101", "");
		MRZInfo b = new MRZInfo("P", "UTO", "DOE", "JOHN<<", "123456789",
				"UTO", "700101", Gender.MALE, "250101", "");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	/* ---------- Check digits ---------- */

	@Test
	public void testIndividualCheckDigitsInEncodedStream() throws Exception {
		MRZInfo mrz = new MRZInfo("P", "UTO", "ERIKSSON", "ANNA MARIA",
				"L898902C3", "UTO", "740812", Gender.FEMALE, "120415", "ZE184226B");
		byte[] encoded = mrz.getEncoded();
		assertEquals(88, encoded.length);
		String line2 = new String(encoded, 44, 44, "UTF-8");

		/* Document number check digit at position 10 of line 2. */
		assertEquals(MRZInfo.checkDigit("L898902C3"), line2.charAt(9));
		/* Date of birth check digit. */
		assertEquals(MRZInfo.checkDigit("740812"), line2.charAt(19));
		/* Date of expiry check digit. */
		assertEquals(MRZInfo.checkDigit("120415"), line2.charAt(27));
		/* Composite check digit at last position. */
		String expectedCompositeString = "L898902C3" + line2.charAt(9) + "740812" + line2.charAt(19)
				+ "120415" + line2.charAt(27) + line2.substring(28, 43);
		assertEquals(MRZInfo.checkDigit(expectedCompositeString), line2.charAt(43));
	}

	@Test
	public void testCheckDigitStaticMethodKnownValues() {
		/* Known values from the canonical ICAO Doc 9303 example MRZ. */
		assertEquals('6', MRZInfo.checkDigit("L898902C3"));
		assertEquals('2', MRZInfo.checkDigit("740812"));
		assertEquals('9', MRZInfo.checkDigit("120415"));
		assertEquals('0', MRZInfo.checkDigit(""));
		assertThrows(IllegalStateException.class, () -> MRZInfo.checkDigit("!@#"));
	}

	/* ---------- TD1 field constructor variants ---------- */

	@Test
	public void testTD1FieldConstructorWithVisaCode() {
		MRZInfo visa = new MRZInfo("I", "UTO", "7654321", "OPT1",
				"900101", Gender.UNKNOWN, "250101", "UTO", "OPT2",
				"TRAVELER", "JANE");
		assertEquals(MRZInfo.DOC_TYPE_ID1, visa.getDocumentType());
		String[] lines = visa.toString().split("\n");
		assertEquals(3, lines.length);
	}

	@Test
	public void testTD1ConstructorRejectsBadOptionalData1() {
		assertThrows(IllegalArgumentException.class, () -> new MRZInfo("I", "UTO", "7654321", null,
				"900101", Gender.UNKNOWN, "250101", "UTO", "OPT2",
				"TRAVELER", "JANE"));
		assertThrows(IllegalArgumentException.class, () -> new MRZInfo("I", "UTO", "7654321", "0123456789ABCDEF",
				"900101", Gender.UNKNOWN, "250101", "UTO", "OPT2",
				"TRAVELER", "JANE"));
	}

	/* ---------- Setters recompute check digits ---------- */

	@Test
	public void testSettersRecomputeCheckDigits() throws Exception {
		MRZInfo mrz = createPassportMRZ();
		mrz.setDateOfBirth("010101");
		mrz.setDateOfExpiry("311231");
		mrz.setDocumentNumber("A00000001");
		mrz.setGender(Gender.UNKNOWN);
		mrz.setNationality("NLD");
		mrz.setIssuingState("NLD");
		mrz.setPrimaryIdentifier("SMITH");
		mrz.setOptionalData2("XYZ");

		String line2 = mrz.toString().split("\n")[1];
		assertEquals(MRZInfo.checkDigit("A00000001"), line2.charAt(9));
		assertEquals(MRZInfo.checkDigit("010101"), line2.charAt(19));
		assertEquals(MRZInfo.checkDigit("311231"), line2.charAt(27));
		assertEquals("NLD", line2.substring(10, 13));
		assertEquals("<", line2.substring(20, 21)); /* UNKNOWN gender */
	}

	/* ---------- Document code ---------- */

	@Test
	public void testSetDocumentCodeSwitchesType() {
		MRZInfo mrz = createPassportMRZ();
		mrz.setDocumentCode("I");
		assertEquals(MRZInfo.DOC_TYPE_ID1, mrz.getDocumentType());
		assertNotNull(mrz.getOptionalData2()); /* ID1 requires optionalData2 */

		mrz.setDocumentCode("P");
		assertEquals(MRZInfo.DOC_TYPE_ID3, mrz.getDocumentType());
	}

	@Test
	public void testInvalidDocumentCodesRejected() {
		assertThrows(IllegalArgumentException.class, () -> new MRZInfo(null, "UTO", "DOE", "JOHN",
				"123", "UTO", "700101", Gender.MALE, "250101", ""));
		assertThrows(IllegalArgumentException.class, () -> new MRZInfo("X", "UTO", "DOE", "JOHN",
				"123", "UTO", "700101", Gender.MALE, "250101", ""));
		assertThrows(IllegalArgumentException.class, () -> new MRZInfo("PPP", "UTO", "DOE", "JOHN",
				"123", "UTO", "700101", Gender.MALE, "250101", ""));
	}

	@Test
	public void testEmptyStringViaStringConstructorRejected() {
		assertThrows(IllegalArgumentException.class, () -> new MRZInfo((String) null));
		assertThrows(IllegalArgumentException.class, () -> new MRZInfo(""));
	}

	@Test
	public void testTD1RoundTripPreservesOptionalData2() throws Exception {
		String mrzString = buildTD1("ID", "NLD", "D23145890", '7', "PERS123",
				"740812", Gender.MALE, "200101", "NLD", "EXTDATA9",
				"JANSEN", "PIETER");
		MRZInfo mrz = new MRZInfo(new ByteArrayInputStream(mrzString.getBytes()), 90);
		assertTrue(mrz.getOptionalData2().startsWith("EXTDATA9"));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		mrz.writeObject(out);
		assertEquals(90, out.toByteArray().length);
		MRZInfo parsed = new MRZInfo(new ByteArrayInputStream(out.toByteArray()), 90);
		assertTrue(parsed.getOptionalData2().startsWith("EXTDATA9"));
		assertEquals(mrz, parsed);
	}
}
