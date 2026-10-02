package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

/**
 * Covers FingerImageInfo and FingerInfo (an AbstractListInfo implementation).
 */
public class FingerAndFingerImageInfoTest {

	@Test
	public void testFingerImageInfoGetters() {
		FingerInfo fingerInfo = LDSTestHelper.createTestFingerInfo();
		FingerImageInfo info = fingerInfo.getFingerImageInfos().get(0);
		assertEquals(FingerImageInfo.POSITION_RIGHT_THUMB, info.getPosition());
		assertEquals(0, info.getViewNumber());
		assertEquals(100, info.getQuality());
		assertEquals(ImageInfo.TYPE_FINGER, info.getType());
		assertEquals(800, info.getWidth());
		assertEquals(600, info.getHeight());
		assertEquals(5, info.getImageLength());
		assertTrue(info.getRecordLength() >= info.getImageLength());
		assertNotNull(info.getFormatType());
		assertNotNull(info.toString());
	}

	@Test
	public void testFingerImageInfoRoundTrip() throws Exception {
		FingerInfo fingerInfo = LDSTestHelper.createTestFingerInfo();
		FingerImageInfo info = fingerInfo.getFingerImageInfos().get(0);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new java.io.DataOutputStream(out); /* no-op, documents big-endian default */
		info.writeObject(out);
		FingerImageInfo copy = new FingerImageInfo(
				new ByteArrayInputStream(out.toByteArray()), FingerInfo.COMPRESSION_JPEG);
		assertEquals(info, copy);
		assertEquals(info.hashCode(), copy.hashCode());
		assertEquals(info.getPosition(), copy.getPosition());
	}

	@Test
	public void testFingerInfoGetters() {
		FingerInfo fingerInfo = LDSTestHelper.createTestFingerInfo();
		assertEquals(0, fingerInfo.getCaptureDeviceId());
		assertEquals(20, fingerInfo.getAcquisitionLevel());
		assertEquals(FingerInfo.SCALE_UNITS_PPI, fingerInfo.getScaleUnits());
		assertEquals(500, fingerInfo.getHorizontalScanningResolution());
		assertEquals(500, fingerInfo.getVerticalScanningResolution());
		assertEquals(500, fingerInfo.getHorizontalImageResolution());
		assertEquals(500, fingerInfo.getVerticalImageResolution());
		assertEquals(8, fingerInfo.getDepth());
		assertEquals(FingerInfo.COMPRESSION_JPEG, fingerInfo.getCompressionAlgorithm());
		assertNotNull(fingerInfo.getStandardBiometricHeader());
		assertNotNull(fingerInfo.toString());
	}

	@Test
	public void testFingerInfoListApi() {
		FingerInfo fingerInfo = LDSTestHelper.createTestFingerInfo();
		fingerInfo.addFingerImageInfo(
				LDSTestHelper.createTestFingerInfo().getFingerImageInfos().get(0));
		assertEquals(2, fingerInfo.getFingerImageInfos().size());
		fingerInfo.removeFingerImageInfo(1);
		assertEquals(1, fingerInfo.getFingerImageInfos().size());
	}

	@Test
	public void testFingerInfoRoundTrip() throws Exception {
		FingerInfo fingerInfo = LDSTestHelper.createTestFingerInfo();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		fingerInfo.writeObject(out);
		FingerInfo copy = new FingerInfo(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(fingerInfo, copy);
		assertEquals(fingerInfo.hashCode(), copy.hashCode());
		assertEquals(fingerInfo.getFingerImageInfos().size(), copy.getFingerImageInfos().size());
	}

	@Test
	public void testFingerInfoEquals() {
		FingerInfo a = LDSTestHelper.createTestFingerInfo();
		FingerInfo b = LDSTestHelper.createTestFingerInfo();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, null);
		assertNotEquals(a, new Object());
	}
}
