package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.sf.scuba.data.Gender;

/**
 * Covers AbstractImageInfo (via FaceImageInfo), FaceImageInfo, and FaceInfo
 * (an AbstractListInfo implementation).
 */
public class FaceAndFaceImageInfoTest {

	@Test
	public void testFaceImageInfoGetters() {
		FaceInfo faceInfo = LDSTestHelper.createTestFaceInfo();
		FaceImageInfo info = faceInfo.getFaceImageInfos().get(0);
		assertEquals(Gender.FEMALE, info.getGender());
		assertEquals(FaceImageInfo.EyeColor.BLUE, info.getEyeColor());
		assertEquals(ImageInfo.TYPE_PORTRAIT, info.getType());
		assertEquals(ImageInfo.JPEG_MIME_TYPE, info.getMimeType());
		assertEquals(800, info.getWidth());
		assertEquals(600, info.getHeight());
		assertEquals(5, info.getImageLength());
		assertTrue(info.getRecordLength() >= info.getImageLength());
		assertNotNull(info.getFeaturePoints());
		assertEquals(3, info.getPoseAngle().length);
		assertEquals(3, info.getPoseAngleUncertainty().length);
		assertNotNull(info.toString());
	}

	@Test
	public void testFaceImageInfoRoundTrip() throws Exception {
		FaceInfo faceInfo = LDSTestHelper.createTestFaceInfo();
		FaceImageInfo info = faceInfo.getFaceImageInfos().get(0);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		info.writeObject(out);
		FaceImageInfo copy = new FaceImageInfo(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(info, copy);
		assertEquals(info.hashCode(), copy.hashCode());
		assertEquals(info.getWidth(), copy.getWidth());
		assertEquals(info.getHeight(), copy.getHeight());
	}

	@Test
	public void testFaceImageInfoAbstractImageInfoApi() {
		FaceImageInfo info = LDSTestHelper.createTestFaceInfo().getFaceImageInfos().get(0);
		byte[] encoded = info.getEncoded();
		assertNotNull(encoded);
		assertEquals((long) encoded.length, info.getRecordLength());
		assertNotNull(info.getImageInputStream());
	}

	@Test
	public void testFaceInfoConstructorsAndListApi() {
		FaceInfo faceInfo = LDSTestHelper.createTestFaceInfo();
		assertEquals(1, faceInfo.getFaceImageInfos().size());
		assertNotNull(faceInfo.getStandardBiometricHeader());

		faceInfo.addFaceImageInfo(LDSTestHelper.createTestFaceInfo().getFaceImageInfos().get(0));
		assertEquals(2, faceInfo.getFaceImageInfos().size());
		faceInfo.removeFaceImageInfo(1);
		assertEquals(1, faceInfo.getFaceImageInfos().size());
	}

	@Test
	public void testFaceInfoRoundTrip() throws Exception {
		FaceInfo faceInfo = LDSTestHelper.createTestFaceInfo();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		faceInfo.writeObject(out);
		FaceInfo copy = new FaceInfo(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(faceInfo, copy);
		assertEquals(faceInfo.hashCode(), copy.hashCode());
		assertEquals(faceInfo.getFaceImageInfos().size(), copy.getFaceImageInfos().size());
	}

	@Test
	public void testFaceInfoEmptyList() throws Exception {
		FaceInfo faceInfo = new FaceInfo(new ArrayList<FaceImageInfo>());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		faceInfo.writeObject(out);
		FaceInfo copy = new FaceInfo(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(0, copy.getFaceImageInfos().size());
		assertEquals(faceInfo, copy);
	}

	@Test
	public void testFaceInfoEquals() {
		FaceInfo a = LDSTestHelper.createTestFaceInfo();
		FaceInfo b = LDSTestHelper.createTestFaceInfo();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, null);
		assertNotEquals(a, new Object());
	}

	@Test
	public void testFeaturePoint() {
		FaceImageInfo.FeaturePoint point = new FaceImageInfo.FeaturePoint(1, 2, 3, 10, 20);
		assertEquals(1, point.getType());
		assertEquals(2, point.getMajorCode());
		assertEquals(3, point.getMinorCode());
		assertEquals(10, point.getX());
		assertEquals(20, point.getY());
		assertNotNull(point.toString());
	}

	@Test
	public void testEyeColorEnumValues() {
		assertEquals(FaceImageInfo.EYE_COLOR_BLUE, FaceImageInfo.EyeColor.BLUE.toInt());
		assertEquals(FaceImageInfo.EYE_COLOR_UNSPECIFIED, FaceImageInfo.EyeColor.UNSPECIFIED.toInt());
		List<String> names = new ArrayList<String>();
		for (FaceImageInfo.EyeColor c : FaceImageInfo.EyeColor.values()) { names.add(c.name()); }
		assertTrue(names.contains("BROWN"));
		assertTrue(names.contains("GREEN"));
	}
}
