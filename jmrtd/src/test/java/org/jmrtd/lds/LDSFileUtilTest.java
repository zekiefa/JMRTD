package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;

import org.jmrtd.PassportService;
import org.junit.jupiter.api.Test;

public class LDSFileUtilTest {

	@Test
	public void testLookupFIDByTag() {
		assertEquals(PassportService.EF_COM, LDSFileUtil.lookupFIDByTag(LDSFile.EF_COM_TAG));
		assertEquals(PassportService.EF_DG1, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG1_TAG));
		assertEquals(PassportService.EF_DG2, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG2_TAG));
		assertEquals(PassportService.EF_DG3, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG3_TAG));
		assertEquals(PassportService.EF_DG4, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG4_TAG));
		assertEquals(PassportService.EF_DG5, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG5_TAG));
		assertEquals(PassportService.EF_DG6, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG6_TAG));
		assertEquals(PassportService.EF_DG7, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG7_TAG));
		assertEquals(PassportService.EF_DG8, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG8_TAG));
		assertEquals(PassportService.EF_DG9, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG9_TAG));
		assertEquals(PassportService.EF_DG10, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG10_TAG));
		assertEquals(PassportService.EF_DG11, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG11_TAG));
		assertEquals(PassportService.EF_DG12, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG12_TAG));
		assertEquals(PassportService.EF_DG13, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG13_TAG));
		assertEquals(PassportService.EF_DG14, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG14_TAG));
		assertEquals(PassportService.EF_DG15, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG15_TAG));
		assertEquals(PassportService.EF_DG16, LDSFileUtil.lookupFIDByTag(LDSFile.EF_DG16_TAG));
		assertEquals(PassportService.EF_SOD, LDSFileUtil.lookupFIDByTag(LDSFile.EF_SOD_TAG));
		assertThrows(NumberFormatException.class, () -> LDSFileUtil.lookupFIDByTag(0x00));
	}

	@Test
	public void testLookupDataGroupNumberByTag() {
		int[] tags = { LDSFile.EF_DG1_TAG, LDSFile.EF_DG2_TAG, LDSFile.EF_DG3_TAG,
				LDSFile.EF_DG4_TAG, LDSFile.EF_DG5_TAG, LDSFile.EF_DG6_TAG,
				LDSFile.EF_DG7_TAG, LDSFile.EF_DG8_TAG, LDSFile.EF_DG9_TAG,
				LDSFile.EF_DG10_TAG, LDSFile.EF_DG11_TAG, LDSFile.EF_DG12_TAG,
				LDSFile.EF_DG13_TAG, LDSFile.EF_DG14_TAG, LDSFile.EF_DG15_TAG,
				LDSFile.EF_DG16_TAG };
		for (int i = 0; i < tags.length; i++) {
			assertEquals(i + 1, LDSFileUtil.lookupDataGroupNumberByTag(tags[i]));
		}
		assertThrows(NumberFormatException.class, () -> LDSFileUtil.lookupDataGroupNumberByTag(0x5C));
	}

	@Test
	public void testLookupTagByDataGroupNumber() {
		int[] tags = { LDSFile.EF_DG1_TAG, LDSFile.EF_DG2_TAG, LDSFile.EF_DG3_TAG,
				LDSFile.EF_DG4_TAG, LDSFile.EF_DG5_TAG, LDSFile.EF_DG6_TAG,
				LDSFile.EF_DG7_TAG, LDSFile.EF_DG8_TAG, LDSFile.EF_DG9_TAG,
				LDSFile.EF_DG10_TAG, LDSFile.EF_DG11_TAG, LDSFile.EF_DG12_TAG,
				LDSFile.EF_DG13_TAG, LDSFile.EF_DG14_TAG, LDSFile.EF_DG15_TAG,
				LDSFile.EF_DG16_TAG };
		for (int i = 0; i < tags.length; i++) {
			assertEquals(tags[i], LDSFileUtil.lookupTagByDataGroupNumber(i + 1));
		}
		assertThrows(NumberFormatException.class, () -> LDSFileUtil.lookupTagByDataGroupNumber(17));
	}

	@Test
	public void testLookupFIDByDataGroupNumber() {
		for (int i = 1; i <= 16; i++) {
			short fid = LDSFileUtil.lookupFIDByDataGroupNumber(i);
			assertEquals(i, LDSFileUtil.lookupDataGroupNumberByFID(fid));
		}
		assertThrows(NumberFormatException.class, () -> LDSFileUtil.lookupFIDByDataGroupNumber(0));
		assertThrows(NumberFormatException.class, () -> LDSFileUtil.lookupDataGroupNumberByFID(PassportService.EF_COM));
	}

	@Test
	public void testLookupTagByFID() {
		assertEquals(LDSFile.EF_COM_TAG, LDSFileUtil.lookupTagByFID(PassportService.EF_COM));
		assertEquals(LDSFile.EF_SOD_TAG, LDSFileUtil.lookupTagByFID(PassportService.EF_SOD));
		for (int i = 1; i <= 16; i++) {
			assertEquals(LDSFileUtil.lookupTagByDataGroupNumber(i),
					LDSFileUtil.lookupTagByFID(LDSFileUtil.lookupFIDByDataGroupNumber(i)));
		}
		assertThrows(NumberFormatException.class, () -> LDSFileUtil.lookupTagByFID((short) 0x3FFF));
	}

	@Test
	public void testLookupFileNameByTag() {
		assertEquals("EF_COM", LDSFileUtil.lookupFileNameByTag(LDSFile.EF_COM_TAG));
		assertEquals("EF_SOD", LDSFileUtil.lookupFileNameByTag(LDSFile.EF_SOD_TAG));
		for (int i = 1; i <= 16; i++) {
			assertEquals("EF_DG" + i, LDSFileUtil.lookupFileNameByTag(LDSFileUtil.lookupTagByDataGroupNumber(i)));
		}
		assertTrue(LDSFileUtil.lookupFileNameByTag(0x5C).startsWith("File with tag"));
	}

	@Test
	public void testLookupFileNameByFID() {
		assertEquals("EF_COM", LDSFileUtil.lookupFileNameByFID(PassportService.EF_COM));
		assertEquals("EF_SOD", LDSFileUtil.lookupFileNameByFID(PassportService.EF_SOD));
		for (int i = 1; i <= 16; i++) {
			assertEquals("EF_DG" + i, LDSFileUtil.lookupFileNameByFID(LDSFileUtil.lookupFIDByDataGroupNumber(i)));
		}
		assertTrue(LDSFileUtil.lookupFileNameByFID(0x3FFF).startsWith("File with FID"));
	}

	@Test
	public void testGetLDSFile() throws Exception {
		byte[] dg1Bytes = new DG1File(LDSTestHelper.createTestMRZInfo()).getEncoded();
		assertTrue(LDSFileUtil.getLDSFile(PassportService.EF_DG1, new ByteArrayInputStream(dg1Bytes)) instanceof DG1File);

		byte[] comBytes = new COMFile("1.7", "4.0.0", new int[] { LDSFile.EF_DG1_TAG }).getEncoded();
		assertTrue(LDSFileUtil.getLDSFile(PassportService.EF_COM, new ByteArrayInputStream(comBytes)) instanceof COMFile);

		byte[] cvcaBytes = new CVCAFile("NLDKK4CVC", null).getEncoded();
		assertTrue(LDSFileUtil.getLDSFile(PassportService.EF_CVCA, new ByteArrayInputStream(cvcaBytes)) instanceof CVCAFile);

		assertThrows(IllegalArgumentException.class,
				() -> LDSFileUtil.getLDSFile(PassportService.EF_DG8, new ByteArrayInputStream(new byte[4])));
		assertThrows(IllegalArgumentException.class,
				() -> LDSFileUtil.getLDSFile(PassportService.EF_DG9, new ByteArrayInputStream(new byte[4])));
		assertThrows(IllegalArgumentException.class,
				() -> LDSFileUtil.getLDSFile(PassportService.EF_DG10, new ByteArrayInputStream(new byte[4])));
		assertThrows(IllegalArgumentException.class,
				() -> LDSFileUtil.getLDSFile(PassportService.EF_DG13, new ByteArrayInputStream(new byte[4])));
		assertThrows(IllegalArgumentException.class,
				() -> LDSFileUtil.getLDSFile(PassportService.EF_DG16, new ByteArrayInputStream(new byte[4])));

		/* Unknown FID, garbage content: CVCAFile parse must fail. */
		assertThrows(NumberFormatException.class,
				() -> LDSFileUtil.getLDSFile((short) 0x3FFF, new ByteArrayInputStream(new byte[] { 0x00 })));
		/* Unknown FID, valid CVCA content */
		LDSFile file = LDSFileUtil.getLDSFile((short) 0x3FFE, new ByteArrayInputStream(cvcaBytes));
		assertTrue(file instanceof CVCAFile);
	}
}
