package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.List;

import net.sf.scuba.tlv.TLVOutputStream;
import net.sf.scuba.util.Hex;

import org.junit.jupiter.api.Test;

public class DG12FileTest {

	@Test
	public void testRoundTrip() throws Exception {
		DG12File dg12 = LDSTestHelper.createTestDG12File();
		byte[] encoded = dg12.getEncoded();
		DG12File copy = new DG12File(new ByteArrayInputStream(encoded));

		assertEquals(dg12, copy);
		assertEquals(dg12.hashCode(), copy.hashCode());
		assertEquals(dg12.toString(), copy.toString());

		assertEquals("ISSUING AUTHORITY", copy.getIssuingAuthority());
		assertNotNull(copy.getDateOfIssue());
		assertTrue(copy.getNamesOfOtherPersons() == null || copy.getNamesOfOtherPersons().isEmpty());
		assertEquals("NONE", copy.getEndorsementsAndObservations());
		assertEquals("PAID", copy.getTaxOrExitRequirements());
		assertArrayEquals(new byte[] { 1, 2, 3 }, copy.getImageOfFront());
		assertArrayEquals(new byte[] { 4, 5, 6 }, copy.getImageOfRear());
		assertNotNull(copy.getDateAndTimeOfPersonalization());
		assertEquals("SERIAL123", copy.getPersonalizationSystemSerialNumber());
		assertEquals(LDSFile.EF_DG12_TAG, copy.getTag());
	}


	@Test
	public void testMinimalConstructor() throws Exception {
		DG12File dg12 = new DG12File(null, null, null, null, null, null, null, null, null);
		assertTrue(dg12.getTagPresenceList().isEmpty());
		assertNull(dg12.getIssuingAuthority());
		assertNull(dg12.getDateOfIssue());
		assertNotNull(dg12.getNamesOfOtherPersons());
		assertTrue(dg12.getNamesOfOtherPersons().isEmpty());
		assertNull(dg12.getEndorsementsAndObservations());
		assertNull(dg12.getTaxOrExitRequirements());
		assertNull(dg12.getImageOfFront());
		assertNull(dg12.getImageOfRear());
		assertNull(dg12.getDateAndTimeOfPersonalization());
		assertNull(dg12.getPersonalizationSystemSerialNumber());
		assertNotNull(dg12.toString());

		assertThrows(Exception.class, () -> dg12.getEncoded());
	}

	@Test
	public void testTagPresenceList() {
		DG12File dg12 = LDSTestHelper.createTestDG12File();
		java.util.List<Integer> tags = dg12.getTagPresenceList();
		assertTrue(tags.contains(DG12File.ISSUING_AUTHORITY_TAG));
		assertTrue(tags.contains(DG12File.DATE_OF_ISSUE_TAG));
		assertTrue(tags.contains(DG12File.ENDORSEMENTS_AND_OBSERVATIONS_TAG));
		assertTrue(tags.contains(DG12File.TAX_OR_EXIT_REQUIREMENTS_TAG));
		assertTrue(tags.contains(DG12File.IMAGE_OF_FRONT_TAG));
		assertTrue(tags.contains(DG12File.IMAGE_OF_REAR_TAG));
		assertTrue(tags.contains(DG12File.DATE_AND_TIME_OF_PERSONALIZATION));
		assertTrue(tags.contains(DG12File.PERSONALIZATION_SYSTEM_SERIAL_NUMBER_TAG));
		assertSame(tags, dg12.getTagPresenceList());
	}

	@Test
	public void testEquals() {
		DG12File dg12 = LDSTestHelper.createTestDG12File();
		assertEquals(dg12, dg12);
		assertEquals(dg12, LDSTestHelper.createTestDG12File());
		assertNotEquals(dg12, null);
		assertNotEquals(dg12, new Object());
		assertNotEquals(dg12, new DG12File(null, null, null, null, null, null, null, null, null));
	}

	@Test
	public void testNamesOfOtherPersonsRoundTrip() throws Exception {
		/* DG12's A0-constructed field is only consistent as last written field;
		 * use a file containing only the names field. */
		DG12File dg12 = LDSTestHelper.createTestDG12FileWithNames();
		List<String> expectedNames = dg12.getNamesOfOtherPersons();
		DG12File copy = new DG12File(new ByteArrayInputStream(dg12.getEncoded()));
		java.util.List<String> parsedNames = copy.getNamesOfOtherPersons();
		assertEquals(expectedNames, parsedNames);
		assertEquals(dg12, copy);
	}

	@Test
	public void testReadErrorsWrongTagListTag() {
		byte[] bad = new byte[] { 0x6C, 0x05, 0x61, 0x02, 0x5F, 0x1F, 0x00 };
		assertThrows(Exception.class, () -> new DG12File(new ByteArrayInputStream(bad)));
	}

	/**
	 * Builds a raw DG12 file payload with one field for custom decoding tests.
	 */
	private static byte[] buildDG12(int fieldTag, byte[] value) throws Exception {
		ByteArrayOutputStream content = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(content);
		tlvOut.writeTag(0x5C);
		DataOutputStream dataOut = new DataOutputStream(tlvOut);
		dataOut.writeShort(fieldTag);
		dataOut.flush();
		tlvOut.writeValueEnd();
		tlvOut.writeTag(fieldTag);
		tlvOut.writeValue(value);
		tlvOut.flush();
		ByteArrayOutputStream full = new ByteArrayOutputStream();
		TLVOutputStream fullTLV = new TLVOutputStream(full);
		fullTLV.writeTag(LDSFile.EF_DG12_TAG);
		fullTLV.writeValue(content.toByteArray());
		fullTLV.flush();
		return full.toByteArray();
	}

	@Test
	public void testDateOfIssueHexEncoding() throws Exception {
		/* 4-byte hex-ish binary encoding (French/Belgian style). */
		DG12File dg12 = new DG12File(new ByteArrayInputStream(
				buildDG12(DG12File.DATE_OF_ISSUE_TAG, Hex.hexStringToBytes("20120415"))));
		assertNotNull(dg12.getDateOfIssue());
	}

	@Test
	public void testDateOfIssueBadLength() throws Exception {
		byte[] bytes = buildDG12(DG12File.DATE_OF_ISSUE_TAG, new byte[] { 0x01, 0x02 });
		assertThrows(Exception.class, () -> new DG12File(new ByteArrayInputStream(bytes)));
	}

	@Test
	public void testUnknownFieldTag() throws Exception {
		/* tag list declares a tag the switch does not know (but readField matches declared vs actual) */
		ByteArrayOutputStream content = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(content);
		tlvOut.writeTag(0x5C);
		DataOutputStream dataOut = new DataOutputStream(tlvOut);
		dataOut.writeShort(0x5F20);
		dataOut.flush();
		tlvOut.writeValueEnd();
		tlvOut.writeTag(0x5F20);
		tlvOut.writeValue(new byte[] { 1 });
		tlvOut.flush();
		ByteArrayOutputStream full = new ByteArrayOutputStream();
		TLVOutputStream fullTLV = new TLVOutputStream(full);
		fullTLV.writeTag(LDSFile.EF_DG12_TAG);
		fullTLV.writeValue(content.toByteArray());
		fullTLV.flush();
		assertThrows(Exception.class, () -> new DG12File(new ByteArrayInputStream(full.toByteArray())));
	}
}
