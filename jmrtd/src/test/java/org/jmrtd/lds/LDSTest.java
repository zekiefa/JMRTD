package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.jmrtd.PassportService;
import org.junit.jupiter.api.Test;

public class LDSTest {

	private COMFile createCOMFile() {
		return new COMFile("1.7", "4.0.0", new int[] { LDSFile.EF_DG1_TAG, LDSFile.EF_DG2_TAG });
	}

	private SODFile createSODFile() throws Exception {
		java.security.KeyPair keyPair = LDSTestHelper.generateRSAKeyPair();
		java.security.cert.X509Certificate cert =
				LDSTestHelper.createSelfSignedCertificate("C=NL,O=JMRTD,CN=DocSigner", keyPair, "SHA256withRSA");
		byte[] bytes = LDSTestHelper.createSODFileBytes(LDSTestHelper.createDataGroupHashes(),
				cert, keyPair.getPrivate(), false, null, null);
		return new SODFile(new java.io.ByteArrayInputStream(bytes));
	}

	@Test
	public void testAddAndGetFiles() throws Exception {
		LDS lds = new LDS();
		COMFile com = createCOMFile();
		DG1File dg1 = new DG1File(LDSTestHelper.createTestMRZInfo());
		DG2File dg2 = new DG2File(Arrays.asList(LDSTestHelper.createTestFaceInfo()));
		lds.add(com);
		lds.add(dg1);
		lds.add(dg2);

		assertEquals(com, lds.getCOMFile());
		assertEquals(dg1, lds.getDG1File());
		assertEquals(dg2, lds.getDG2File());

		List<Short> fileList = lds.getFileList();
		assertTrue(fileList.contains(PassportService.EF_COM));
		assertTrue(fileList.contains(PassportService.EF_DG1));
		assertTrue(fileList.contains(PassportService.EF_DG2));

		assertTrue(lds.getLength() > 0);
		assertTrue(lds.getPosition() >= 0);
		assertTrue(lds.getLength(PassportService.EF_DG1) > 0);

		InputStream inputStream = lds.getInputStream(PassportService.EF_DG1);
		assertNotNull(inputStream);
		DG1File copy = new DG1File(inputStream);
		assertEquals(dg1, copy);

		/* Adding same file again exercises put/update paths. */
		lds.add(dg1);
		assertEquals(dg1, lds.getDG1File());
	}

	@Test
	public void testAddNullAndUnsupported() {
		LDS lds = new LDS();
		lds.add((LDSFile) null); /* no-op */
		LDSFile unsupported = new LDSFile() {
			private static final long serialVersionUID = 1L;
			public byte[] getEncoded() { return new byte[0]; }
			public int getLength() { return 0; }
		};
		assertThrows(IllegalArgumentException.class, () -> lds.add(unsupported));
	}

	@Test
	public void testAddBytesAndStreams() throws Exception {
		LDS lds = new LDS();
		byte[] dg1Bytes = new DG1File(LDSTestHelper.createTestMRZInfo()).getEncoded();
		lds.add(PassportService.EF_DG1, dg1Bytes);
		DG1File dg1 = lds.getDG1File();
		assertEquals("ERIKSSON", dg1.getMRZInfo().getPrimaryIdentifier());

		LDS lds2 = new LDS();
		lds2.add(PassportService.EF_DG1, new ByteArrayInputStream(dg1Bytes), dg1Bytes.length);
		assertEquals(dg1, lds2.getDG1File());
	}

	@Test
	public void testGetFileNotPresent() {
		LDS lds = new LDS();
		assertThrows(java.io.IOException.class, () -> lds.getFile(PassportService.EF_DG1));
	}

	@Test
	public void testAddAllAndDataGroupList() throws Exception {
		LDS lds = new LDS();
		COMFile com = createCOMFile();
		SODFile sod = createSODFile();
		lds.addAll(Arrays.asList(com, sod, new DG1File(LDSTestHelper.createTestMRZInfo())));
		assertEquals(com, lds.getCOMFile());
		assertEquals(sod, lds.getSODFile());
		/* COM declares DG1 & DG2; SOD declares hashes for DG1 and DG2. */
		List<Short> dgList = lds.getDataGroupList();
		assertTrue(dgList.contains(PassportService.EF_DG1));
		assertTrue(dgList.contains(PassportService.EF_DG2));
	}

	@Test
	public void testIsSameDocument() throws Exception {
		LDS lds1 = new LDS();
		lds1.add(new DG1File(LDSTestHelper.createTestMRZInfo()));
		LDS lds2 = new LDS();
		lds2.add(new DG1File(LDSTestHelper.createTestMRZInfo()));
		assertTrue(lds1.isSameDocument(lds2));

		LDS other = new LDS();
		other.add(new DG1File(new MRZInfo("P", "UTO", "VANDERSTEEN", "MARTIN",
				"L898902C3", "UTO", "740812", net.sf.scuba.data.Gender.MALE, "120415", "ZE184226B")));
		assertFalse(lds1.isSameDocument(other));
		assertFalse(lds1.isSameDocument(null));
		assertFalse(lds1.isSameDocument(new LDS()));
	}

	@Test
	public void testUpdateFrom() throws Exception {
		byte[] dg1Bytes = new DG1File(LDSTestHelper.createTestMRZInfo()).getEncoded();
		LDS target = new LDS();
		target.add(PassportService.EF_DG1, new ByteArrayInputStream(dg1Bytes), dg1Bytes.length);

		LDS source = new LDS();
		source.add(PassportService.EF_DG2, new DG2File(Arrays.asList(LDSTestHelper.createTestFaceInfo())).getEncoded());

		target.updateFrom(source);
		List<Short> fileList = target.getFileList();
		assertTrue(fileList.contains(PassportService.EF_DG1));
		assertTrue(fileList.contains(PassportService.EF_DG2));

		/* Update with the same file via fetchers path. */
		target.updateFrom(source);
		target.updateFrom(null);
	}

	@Test
	public void testCVCAFile() throws Exception {
		LDS lds = new LDS();
		CVCAFile cvca = new CVCAFile("NLDKK4CVC", null);
		lds.add(cvca);
		LDSFile file = lds.getFile(PassportService.EF_CVCA);
		assertEquals(cvca, file);
	}

	@Test
	public void testGetCVCAFileWithoutDG14() {
		LDS lds = new LDS();
		assertThrows(java.io.IOException.class, () -> lds.getCVCAFile());
	}
}
