package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class DG2FileTest {

	private List<FaceInfo> sampleFaceInfos() {
		List<FaceInfo> faceInfos = new ArrayList<FaceInfo>();
		faceInfos.add(LDSTestHelper.createTestFaceInfo());
		return faceInfos;
	}

	@Test
	public void testConstruct() {
		DG2File dg2 = new DG2File(sampleFaceInfos());
		assertEquals(LDSFile.EF_DG2_TAG, dg2.getTag());
		assertEquals(1, dg2.getFaceInfos().size());
		assertTrue(dg2.toString().startsWith("DG2File"));
		assertTrue(dg2.getLength() > 0);
	}

	@Test
	public void testRoundTrip() throws Exception {
		DG2File dg2 = new DG2File(sampleFaceInfos());
		byte[] encoded = dg2.getEncoded();
		DG2File copy = new DG2File(new ByteArrayInputStream(encoded));
		assertEquals(dg2, copy);
		assertEquals(dg2.hashCode(), copy.hashCode());
		assertEquals(1, copy.getFaceInfos().size());
		assertEquals(1, copy.getFaceInfos().get(0).getFaceImageInfos().size());
	}

	@Test
	public void testAddRemove() {
		DG2File dg2 = new DG2File(new ArrayList<FaceInfo>());
		dg2.addFaceInfo(LDSTestHelper.createTestFaceInfo());
		dg2.addFaceInfo(LDSTestHelper.createTestFaceInfo());
		assertEquals(2, dg2.getFaceInfos().size());
		dg2.removeFaceInfo(0);
		assertEquals(1, dg2.getFaceInfos().size());
		/* getSubRecords returns a copy */
		List<FaceInfo> copy = dg2.getSubRecords();
		copy.clear();
		assertEquals(1, dg2.getFaceInfos().size());
	}

	@Test
	public void testEquals() {
		DG2File dg2 = new DG2File(sampleFaceInfos());
		assertEquals(dg2, dg2);
		assertEquals(dg2, new DG2File(sampleFaceInfos()));
		assertNotEquals(dg2, null);
		assertNotEquals(dg2, new Object());
		assertNotEquals(dg2, new DG2File(new ArrayList<FaceInfo>()));
		assertNotEquals(dg2.hashCode(), new DG2File(new ArrayList<FaceInfo>()).hashCode());

		/* Mismatching sub-record should break equality. */
		DG2File a = new DG2File(sampleFaceInfos());
		DG2File b = new DG2File(sampleFaceInfos());
		b.removeFaceInfo(0);
		b.addFaceInfo(new FaceInfo(new ArrayList<FaceImageInfo>()));
		assertNotEquals(a, b);
	}

	@Test
	public void testEmptyFacialInfo() throws Exception {
		FaceInfo empty = new FaceInfo(new ArrayList<FaceImageInfo>());
		FaceInfo otherEmpty = new FaceInfo(new ArrayList<FaceImageInfo>());
		assertEquals(empty, otherEmpty);
		assertEquals(empty.hashCode(), otherEmpty.hashCode());
		assertNotEquals(empty, null);
		assertNotEquals(empty, new Object());
		assertEquals(empty, empty);
	}
}
