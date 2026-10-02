package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.List;

import net.sf.scuba.tlv.TLVOutputStream;

import org.junit.jupiter.api.Test;

public class DG11FileTest {

	@Test
	public void testRoundTrip() throws Exception {
		DG11File dg11 = LDSTestHelper.createTestDG11File();
		byte[] encoded = dg11.getEncoded();
		DG11File copy = new DG11File(new ByteArrayInputStream(encoded));

		/* Note: DG11 write replaces spaces with '<' for some fields, so compare
		 * the twice-encoded version instead of the in-memory original. */
		DG11File copyTwice = new DG11File(new ByteArrayInputStream(copy.getEncoded()));
		assertEquals(copy, copyTwice);
		assertEquals(copy.hashCode(), copyTwice.hashCode());
		assertEquals(copy.toString(), copyTwice.toString());

		assertEquals("ERIKSSON<<ANNA<MARIA", copy.getNameOfHolder());
		assertEquals(Arrays.asList("JOHNNY", "JOHN"), copy.getOtherNames());
		assertEquals("9990009908", copy.getPersonalNumber());
		assertNotNull(copy.getFullDateOfBirth());
		assertEquals(Arrays.asList("AMSTERDAM", "NETHERLANDS"), copy.getPlaceOfBirth());
		assertEquals(2, copy.getPermanentAddress().size());
		assertEquals("+31 20 123 4567", copy.getTelephone());
		assertEquals("SOFTWARE<ENGINEER", copy.getProfession());
		assertEquals("MRS", copy.getTitle());
		assertEquals("A<PERSONAL<SUMMARY", copy.getPersonalSummary());
		assertArrayEquals(new byte[] { 1, 2, 3, 4 }, copy.getProofOfCitizenship());
		assertEquals(2, copy.getOtherValidTDNumbers().size());
		assertEquals("NONE", copy.getCustodyInformation());
	}

	@Test
	public void testMinimalConstructor() throws Exception {
		DG11File dg11 = new DG11File(null, null, null, null, null, null, null, null, null, null, null, null, null);
		assertTrue(dg11.getTagPresenceList().isEmpty());
		assertTrue(dg11.getOtherNames().isEmpty());
		assertNull(dg11.getNameOfHolder());
		assertNull(dg11.getPersonalNumber());
		assertNull(dg11.getFullDateOfBirth());
		assertTrue(dg11.getPlaceOfBirth().isEmpty());
		assertNull(dg11.getPermanentAddress());
		assertNull(dg11.getTelephone());
		assertNull(dg11.getProfession());
		assertNull(dg11.getTitle());
		assertNull(dg11.getPersonalSummary());
		assertNull(dg11.getProofOfCitizenship());
		assertTrue(dg11.getOtherValidTDNumbers().isEmpty());
		assertNull(dg11.getCustodyInformation());
		assertNotNull(dg11.toString());

		/* Minimal files cannot be written (empty tag list): document that limitation. */
		assertThrows(Exception.class, () -> dg11.getEncoded());
	}

	@Test
	public void testTagPresenceList() {
		DG11File dg11 = LDSTestHelper.createTestDG11File();
		List<Integer> tags = dg11.getTagPresenceList();
		assertTrue(tags.contains(DG11File.FULL_NAME_TAG));
		assertTrue(tags.contains(DG11File.OTHER_NAME_TAG));
		assertTrue(tags.contains(DG11File.PERSONAL_NUMBER_TAG));
		assertTrue(tags.contains(DG11File.FULL_DATE_OF_BIRTH_TAG));
		assertTrue(tags.contains(DG11File.PLACE_OF_BIRTH_TAG));
		assertTrue(tags.contains(DG11File.PERMANENT_ADDRESS_TAG));
		assertTrue(tags.contains(DG11File.TELEPHONE_TAG));
		assertTrue(tags.contains(DG11File.PROFESSION_TAG));
		assertTrue(tags.contains(DG11File.TITLE_TAG));
		assertTrue(tags.contains(DG11File.PERSONAL_SUMMARY_TAG));
		assertTrue(tags.contains(DG11File.PROOF_OF_CITIZENSHIP_TAG));
		assertTrue(tags.contains(DG11File.OTHER_VALID_TD_NUMBERS_TAG));
		assertTrue(tags.contains(DG11File.CUSTODY_INFORMATION_TAG));
		/* cached second call */
		assertSame(tags, dg11.getTagPresenceList());
	}

	@Test
	public void testGetTag() {
		assertEquals(LDSFile.EF_DG11_TAG, LDSTestHelper.createTestDG11File().getTag());
	}

	@Test
	public void testEquals() {
		DG11File dg11 = LDSTestHelper.createTestDG11File();
		DG11File same = LDSTestHelper.createTestDG11File();
		DG11File minimal = new DG11File(null, null, null, null, null, null, null, null, null, null, null, null, null);
		assertEquals(dg11, dg11);
		assertEquals(dg11, same);
		assertNotEquals(dg11, minimal);
		assertNotEquals(dg11, null);
		assertNotEquals(dg11, new Object());
	}

	@Test
	public void testReadErrorsWrongTagListTag() {
		byte[] bad = new byte[] { 0x6B, 0x05, 0x61, 0x02, 0x5F, 0x1F, 0x00 };
		assertThrows(Exception.class, () -> new DG11File(new ByteArrayInputStream(bad)));
	}

	@Test
	public void testReadFieldTagMismatch() throws Exception {
		/* tag list says 5F0E but field actually encoded is 5F10 */
		ByteArrayOutputStream content = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(content);
		tlvOut.writeTag(DG11File.TAG_LIST_TAG);
		DataOutputStream dataOut = new DataOutputStream(tlvOut);
		dataOut.writeShort(0x5F0E);
		dataOut.flush();
		tlvOut.writeValueEnd();
		tlvOut.writeTag(0x5F10);
		tlvOut.writeValue("X".getBytes("UTF-8"));
		tlvOut.flush();
		ByteArrayOutputStream full = new ByteArrayOutputStream();
		TLVOutputStream fullTLV = new TLVOutputStream(full);
		fullTLV.writeTag(LDSFile.EF_DG11_TAG);
		fullTLV.writeValue(content.toByteArray());
		fullTLV.flush();
		assertThrows(Exception.class, () -> new DG11File(new ByteArrayInputStream(full.toByteArray())));
	}

	@Test
	public void testFullDateOfBirthAsText() throws Exception {
		/* 8 ASCII bytes encoding of date (not valid CGI, but readField only sees declared tag). */
		ByteArrayOutputStream content = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(content);
		tlvOut.writeTag(DG11File.TAG_LIST_TAG);
		DataOutputStream dataOut = new DataOutputStream(tlvOut);
		dataOut.writeShort(0x5F2B);
		dataOut.flush();
		tlvOut.writeValueEnd();
		tlvOut.writeTag(0x5F2B);
		tlvOut.writeValue("19740812".getBytes("UTF-8"));
		tlvOut.flush();
		ByteArrayOutputStream full = new ByteArrayOutputStream();
		TLVOutputStream fullTLV = new TLVOutputStream(full);
		fullTLV.writeTag(LDSFile.EF_DG11_TAG);
		fullTLV.writeValue(content.toByteArray());
		fullTLV.flush();
		DG11File dg11 = new DG11File(new ByteArrayInputStream(full.toByteArray()));
		assertNotNull(dg11.getFullDateOfBirth());
	}
}
