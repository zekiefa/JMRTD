package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Covers IrisImageInfo, IrisBiometricSubtypeInfo, and IrisInfo
 * (an AbstractListInfo implementation).
 */
public class IrisAndIrisImageInfoTest {

	private static final byte[] IMAGE_BYTES = { 1, 2, 3, 4, 5 };

	private static IrisImageInfo createTestIrisImageInfo() throws Exception {
		return new IrisImageInfo(1, 100, 10, 5, 800, 600,
				new ByteArrayInputStream(IMAGE_BYTES), IMAGE_BYTES.length,
				IrisInfo.IMAGEFORMAT_MONO_JPEG);
	}

	@Test
	public void testIrisImageInfoGetters() throws Exception {
		IrisImageInfo info = createTestIrisImageInfo();
		assertEquals(1, info.getImageNumber());
		assertEquals(100, info.getQuality());
		assertEquals(10, info.getRotationAngle());
		assertEquals(5, info.getRotationAngleUncertainty());
		assertEquals(ImageInfo.TYPE_IRIS, info.getType());
		assertEquals(ImageInfo.JPEG_MIME_TYPE, info.getMimeType());
		assertEquals(800, info.getWidth());
		assertEquals(600, info.getHeight());
		assertEquals(IMAGE_BYTES.length, info.getImageLength());
		assertTrue(info.getRecordLength() >= info.getImageLength());
		assertNotNull(info.toString());
	}

	@Test
	public void testIrisImageInfoRoundTrip() throws Exception {
		IrisImageInfo info = createTestIrisImageInfo();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		info.writeObject(out);
		IrisImageInfo copy = new IrisImageInfo(new ByteArrayInputStream(out.toByteArray()),
				IrisInfo.IMAGEFORMAT_MONO_JPEG);
		assertEquals(info, copy);
		assertEquals(info.hashCode(), copy.hashCode());
	}

	@Test
	public void testIrisBiometricSubtypeInfo() throws Exception {
		List<IrisImageInfo> images = new ArrayList<IrisImageInfo>();
		images.add(createTestIrisImageInfo());
		IrisBiometricSubtypeInfo subtype = new IrisBiometricSubtypeInfo(
				IrisBiometricSubtypeInfo.EYE_LEFT, IrisInfo.IMAGEFORMAT_MONO_JPEG, images);
		assertEquals(IrisBiometricSubtypeInfo.EYE_LEFT, subtype.getBiometricSubtype());
		assertEquals(IrisInfo.IMAGEFORMAT_MONO_JPEG, subtype.getImageFormat());
		assertEquals(1, subtype.getIrisImageInfos().size());
		assertTrue(subtype.getRecordLength() > 0);
		assertNotNull(subtype.toString());

		subtype.addIrisImageInfo(createTestIrisImageInfo());
		assertEquals(2, subtype.getIrisImageInfos().size());
		subtype.removeIrisImageInfo(1);
		assertEquals(1, subtype.getIrisImageInfos().size());
	}

	@Test
	public void testIrisBiometricSubtypeInfoRoundTrip() throws Exception {
		List<IrisImageInfo> images = new ArrayList<IrisImageInfo>();
		images.add(createTestIrisImageInfo());
		IrisBiometricSubtypeInfo subtype = new IrisBiometricSubtypeInfo(
				IrisBiometricSubtypeInfo.EYE_RIGHT, IrisInfo.IMAGEFORMAT_MONO_JPEG, images);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		subtype.writeObject(out);
		IrisBiometricSubtypeInfo copy = new IrisBiometricSubtypeInfo(
				new ByteArrayInputStream(out.toByteArray()), IrisInfo.IMAGEFORMAT_MONO_JPEG);
		assertEquals(IrisBiometricSubtypeInfo.EYE_RIGHT, copy.getBiometricSubtype());
		assertEquals(1, copy.getIrisImageInfos().size());
		assertEquals(subtype, copy);
	}

	@Test
	public void testIrisInfoGetters() {
		IrisInfo irisInfo = LDSTestHelper.createTestIrisInfo();
		assertEquals(IrisInfo.CAPTURE_DEVICE_UNDEF, irisInfo.getCaptureDeviceId());
		assertEquals(IrisInfo.ORIENTATION_UNDEF, irisInfo.getHorizontalOrientation());
		assertEquals(IrisInfo.ORIENTATION_UNDEF, irisInfo.getVerticalOrientation());
		assertEquals(IrisInfo.SCAN_TYPE_UNDEF, irisInfo.getScanType());
		assertEquals(IrisInfo.IROCC_UNDEF, irisInfo.getIrisOcclusion());
		assertEquals(IrisInfo.IROCC_ZEROFILL, irisInfo.getOcclusionFilling());
		assertEquals(IrisInfo.IRBNDY_UNDEF, irisInfo.getBoundaryExtraction());
		assertEquals(160, irisInfo.getIrisDiameter());
		assertEquals(IrisInfo.IMAGEFORMAT_MONO_JPEG, irisInfo.getImageFormat());
		assertEquals(100, irisInfo.getRawImageWidth());
		assertEquals(2, irisInfo.getRawImageHeight());
		assertEquals(IrisInfo.INTENSITY_DEPTH_UNDEF, irisInfo.getIntensityDepth());
		assertEquals(IrisInfo.TRANS_UNDEF, irisInfo.getImageTransformation());
		assertArrayEquals(new byte[16], irisInfo.getDeviceUniqueId());
		assertEquals(0, irisInfo.getIrisBiometricSubtypeInfos().size());
		assertNotNull(irisInfo.getStandardBiometricHeader());
		assertNotNull(irisInfo.toString());
	}

	@Test
	public void testIrisInfoListApi() {
		IrisInfo irisInfo = LDSTestHelper.createTestIrisInfo();
		IrisBiometricSubtypeInfo subtype = new IrisBiometricSubtypeInfo(
				IrisBiometricSubtypeInfo.EYE_UNDEF, IrisInfo.IMAGEFORMAT_MONO_JPEG,
				new ArrayList<IrisImageInfo>());
		irisInfo.addIrisBiometricSubtypeInfo(subtype);
		assertEquals(1, irisInfo.getIrisBiometricSubtypeInfos().size());
		irisInfo.removeIrisBiometricSubtypeInfo(0);
		assertEquals(0, irisInfo.getIrisBiometricSubtypeInfos().size());
	}

	@Test
	public void testIrisInfoRoundTrip() throws Exception {
		IrisInfo irisInfo = LDSTestHelper.createTestIrisInfo();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		irisInfo.writeObject(out);
		IrisInfo copy = new IrisInfo(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(irisInfo, copy);
		assertEquals(irisInfo.hashCode(), copy.hashCode());
	}

	@Test
	public void testIrisInfoEquals() {
		IrisInfo a = LDSTestHelper.createTestIrisInfo();
		IrisInfo b = LDSTestHelper.createTestIrisInfo();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, null);
		assertNotEquals(a, new Object());
	}
}
