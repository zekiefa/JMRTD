package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;

import net.sf.scuba.data.Gender;

import org.junit.jupiter.api.Test;

public class DG1FileTest {

	@Test
	public void testConstruct() {
		MRZInfo mrzInfo = LDSTestHelper.createTestMRZInfo();
		DG1File dg1 = new DG1File(mrzInfo);
		assertEquals(LDSFile.EF_DG1_TAG, dg1.getTag());
		assertEquals(mrzInfo, dg1.getMRZInfo());
		assertTrue(dg1.toString().startsWith("DG1File"));
		assertTrue(dg1.toString().contains("ERIKSSON"));
		assertTrue(dg1.getLength() > 0);
	}

	@Test
	public void testTD3RoundTrip() throws Exception {
		MRZInfo mrzInfo = LDSTestHelper.createTestMRZInfo();
		DG1File dg1 = new DG1File(mrzInfo);
		byte[] encoded = dg1.getEncoded();
		DG1File copy = new DG1File(new ByteArrayInputStream(encoded));
		assertEquals(dg1, copy);
		assertEquals(dg1.hashCode(), copy.hashCode());
		MRZInfo copyMRZ = copy.getMRZInfo();
		assertEquals("P", copyMRZ.getDocumentCode());
		assertEquals("UTO", copyMRZ.getIssuingState());
		assertEquals("ERIKSSON", copyMRZ.getPrimaryIdentifier());
		assertEquals("L898902C3", copyMRZ.getDocumentNumber());
		assertEquals(Gender.FEMALE, copyMRZ.getGender());
	}

	@Test
	public void testTD1RoundTrip() throws Exception {
		MRZInfo mrzInfo = new MRZInfo("I", "UTO",
				"XI85935", "<<",
				"740812", Gender.FEMALE, "120415",
				"UTO", "ZE184226B<<",
				"ERIKSSON", "ANNA MARIA");
		DG1File dg1 = new DG1File(mrzInfo);
		DG1File copy = new DG1File(new ByteArrayInputStream(dg1.getEncoded()));
		assertEquals(dg1, copy);
	}

	@Test
	public void testEquals() {
		DG1File dg1 = new DG1File(LDSTestHelper.createTestMRZInfo());
		DG1File same = new DG1File(LDSTestHelper.createTestMRZInfo());
		DG1File different = new DG1File(new MRZInfo("P", "UTO", "VANDERSTEEN", "MARTIN",
				"L898902C3", "UTO", "740812", Gender.MALE, "120415", "ZE184226B"));
		assertEquals(dg1, same);
		assertNotEquals(dg1, different);
		assertNotEquals(dg1, null);
		assertNotEquals(dg1, new Object());
	}

	@Test
	public void testReadWrongTag() {
		/* DG5 tag prefix instead of DG1 tag. */
		byte[] bad = new byte[] { 0x62, 0x01, 0x00 };
		assertThrows(Exception.class, () -> new DG1File(new ByteArrayInputStream(bad)));
	}
}
