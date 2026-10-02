package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import org.jmrtd.cbeff.CBEFFInfo;
import org.junit.jupiter.api.Test;

/**
 * Deep coverage tests for {@link FingerImageInfo} (and {@link FingerInfo} carriage).
 * Builds synthetic ISO/IEC 19794-4 finger records (format identifier
 * 0x46495200, "FIR", 0x00) and verifies header field parsing, positions,
 * impression types, multiple views per record, and subtype mapping.
 */
public class FingerImageInfoDeepTest {

	private static final int FORMAT_IDENTIFIER = 0x46495200; /* "FIR", 0x00 */
	private static final int VERSION_NUMBER = 0x30313000; /* "010", 0x00 */

	/** Builds a finger image record header (14 bytes) plus image data. */
	private static byte[] buildImageRecord(int position, int viewCount, int viewNumber,
			int quality, int impressionType, int width, int height, byte[] imageBytes) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(out);
		dataOut.writeInt(14 + imageBytes.length);
		dataOut.writeByte(position);
		dataOut.writeByte(viewCount);
		dataOut.writeByte(viewNumber);
		dataOut.writeByte(quality);
		dataOut.writeByte(impressionType);
		dataOut.writeShort(width);
		dataOut.writeShort(height);
		dataOut.writeByte(0x00); /* RFU */
		dataOut.write(imageBytes);
		dataOut.flush();
		return out.toByteArray();
	}

	/** Builds a full finger record: general record header (32 bytes) + image records. */
	private static byte[] buildFingerRecord(int captureDeviceId, int acquisitionLevel,
			int scaleUnits, int scanResH, int scanResV, int imageResH, int imageResV,
			int depth, int compressionAlgorithm, byte[]... imageRecords) throws Exception {
		long dataLength = 0;
		for (byte[] r: imageRecords) {
			dataLength += r.length;
		}
		long recordLength = 32 + dataLength;

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(out);
		dataOut.writeInt(FORMAT_IDENTIFIER);			/* 4 */
		dataOut.writeInt(VERSION_NUMBER);				/* +4 = 8 */
		/* 6 byte record length, big endian. */
		for (int i = 5; i >= 0; i--) {
			dataOut.writeByte((int)((recordLength >> (8 * i)) & 0xFF));
		}												/* +6 = 14 */
		dataOut.writeShort(captureDeviceId);			/* +2 = 16 */
		dataOut.writeShort(acquisitionLevel);			/* +2 = 18 */
		dataOut.writeByte(imageRecords.length);			/* +1 = 19 */
		dataOut.writeByte(scaleUnits);					/* +1 = 20 */
		dataOut.writeShort(scanResH);					/* +2 = 22 */
		dataOut.writeShort(scanResV);					/* +2 = 24 */
		dataOut.writeShort(imageResH);					/* +2 = 26 */
		dataOut.writeShort(imageResV);					/* +2 = 28 */
		dataOut.writeByte(depth);						/* +1 = 29 */
		dataOut.writeByte(compressionAlgorithm);		/* +1 = 30 */
		dataOut.writeShort(0x0000);						/* +2 = 32, RFU */
		for (byte[] r: imageRecords) {
			dataOut.write(r);
		}
		dataOut.flush();
		return out.toByteArray();
	}

	private static byte[] readAll(java.io.InputStream in) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[512];
		int bytesRead;
		while ((bytesRead = in.read(buffer)) != -1) {
			out.write(buffer, 0, bytesRead);
		}
		return out.toByteArray();
	}

	@Test
	public void testParseImageRecordFields() throws Exception {
		byte[] imageBytes = new byte[] { 0x55, 0x66, 0x77, 0x11, 0x22 };
		byte[] record = buildImageRecord(
				FingerImageInfo.POSITION_LEFT_INDEX_FINGER, 2, 1, 80,
				FingerImageInfo.IMPRESSION_TYPE_LIVE_SCAN_ROLLED,
				400, 500, imageBytes);

		FingerImageInfo info = new FingerImageInfo(new ByteArrayInputStream(record), FingerInfo.COMPRESSION_JPEG);

		assertEquals(14 + imageBytes.length, info.getRecordLength());
		assertEquals(FingerImageInfo.POSITION_LEFT_INDEX_FINGER, info.getPosition());
		assertEquals(2, info.getViewCount());
		assertEquals(1, info.getViewNumber());
		assertEquals(80, info.getQuality());
		assertEquals(FingerImageInfo.IMPRESSION_TYPE_LIVE_SCAN_ROLLED, info.getImpressionType());
		assertEquals(400, info.getWidth());
		assertEquals(500, info.getHeight());
		assertEquals(FingerInfo.COMPRESSION_JPEG, info.getCompressionAlgorithm());
		assertEquals(ImageInfo.TYPE_FINGER, info.getType());
		assertEquals("image/jpeg", info.getMimeType());
		assertEquals(imageBytes.length, info.getImageLength());
		assertArrayEquals(imageBytes, readAll(info.getImageInputStream()));
		assertArrayEquals(new byte[] { 0x00, 0x09 }, info.getFormatType());
	}

	@Test
	public void testFullFingerRecordMultipleViews() throws Exception {
		byte[] view1 = buildImageRecord(
				FingerImageInfo.POSITION_RIGHT_THUMB, 2, 0, 90,
				FingerImageInfo.IMPRESSION_TYPE_LIVE_SCAN_PLAIN, 300, 300,
				new byte[] { 1, 2, 3 });
		byte[] view2 = buildImageRecord(
				FingerImageInfo.POSITION_RIGHT_THUMB, 2, 1, 85,
				FingerImageInfo.IMPRESSION_TYPE_LIVE_SCAN_PLAIN, 300, 300,
				new byte[] { 4, 5, 6, 7 });

		byte[] fingerRecord = buildFingerRecord(0x123, 30, FingerInfo.SCALE_UNITS_PPI,
				500, 500, 500, 500, 8, FingerInfo.COMPRESSION_JPEG, view1, view2);

		FingerInfo fingerInfo = new FingerInfo(new ByteArrayInputStream(fingerRecord));
		assertEquals(2, fingerInfo.getFingerImageInfos().size());
		assertEquals(0x123, fingerInfo.getCaptureDeviceId());
		assertEquals(30, fingerInfo.getAcquisitionLevel());
		assertEquals(FingerInfo.SCALE_UNITS_PPI, fingerInfo.getScaleUnits());
		assertEquals(500, fingerInfo.getHorizontalScanningResolution());
		assertEquals(500, fingerInfo.getVerticalScanningResolution());
		assertEquals(500, fingerInfo.getHorizontalImageResolution());
		assertEquals(500, fingerInfo.getVerticalImageResolution());
		assertEquals(8, fingerInfo.getDepth());
		assertEquals(FingerInfo.COMPRESSION_JPEG, fingerInfo.getCompressionAlgorithm());

		FingerImageInfo first = fingerInfo.getFingerImageInfos().get(0);
		assertEquals(0, first.getViewNumber());
		assertEquals(2, first.getViewCount());
		assertEquals(90, first.getQuality());

		FingerImageInfo second = fingerInfo.getFingerImageInfos().get(1);
		assertEquals(1, second.getViewNumber());
		assertArrayEquals(new byte[] { 4, 5, 6, 7 }, readAll(second.getImageInputStream()));
	}

	@Test
	public void testPositionsAndImpressionTypes() throws Exception {
		int[] positions = {
				FingerImageInfo.POSITION_UNKNOWN_FINGER,
				FingerImageInfo.POSITION_RIGHT_THUMB,
				FingerImageInfo.POSITION_RIGHT_MIDDLE_FINGER,
				FingerImageInfo.POSITION_LEFT_LITTLE_FINGER,
				FingerImageInfo.POSITION_PLAIN_RIGHT_FOUR_FINGERS,
				FingerImageInfo.POSITION_PLAIN_THUMBS,
				FingerImageInfo.POSITION_RIGHT_FULL_PALM,
				FingerImageInfo.POSITION_LEFT_WRITER_S_PALM,
				FingerImageInfo.POSITION_LEFT_HYPOTHENAR
		};
		int[] impressionTypes = {
				FingerImageInfo.IMPRESSION_TYPE_LIVE_SCAN_PLAIN,
				FingerImageInfo.IMPRESSION_TYPE_LIVE_SCAN_ROLLED,
				FingerImageInfo.IMPRESSION_TYPE_NON_LIVE_SCAN_PLAIN,
				FingerImageInfo.IMPRESSION_TYPE_NON_LIVE_SCAN_ROLLED,
				FingerImageInfo.IMPRESSION_TYPE_LATENT,
				FingerImageInfo.IMPRESSION_TYPE_SWIPE,
				FingerImageInfo.IMPRESSION_TYPE_LIVE_SCAN_CONTACTLESS
		};
		for (int position: positions) {
			for (int impressionType: impressionTypes) {
				byte[] record = buildImageRecord(position, 1, 0, 50, impressionType, 10, 10, new byte[] { 42 });
				FingerImageInfo info = new FingerImageInfo(new ByteArrayInputStream(record), FingerInfo.COMPRESSION_WSQ);
				assertEquals(position, info.getPosition());
				assertEquals(impressionType, info.getImpressionType());
				assertEquals("image/x-wsq", info.getMimeType());
				assertEquals(15, info.getRecordLength());
				assertNotNull(info.toString());
			}
		}
	}

	@Test
	public void testBiometricSubtypeMapping() throws Exception {
		byte[] record;

		record = buildImageRecord(FingerImageInfo.POSITION_RIGHT_THUMB, 1, 0, 10, 0, 1, 1, new byte[] { 1 });
		assertEquals(CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_THUMB,
				new FingerImageInfo(new ByteArrayInputStream(record), FingerInfo.COMPRESSION_JPEG).getBiometricSubtype());

		record = buildImageRecord(FingerImageInfo.POSITION_LEFT_MIDDLE_FINGER, 1, 0, 10, 0, 1, 1, new byte[] { 1 });
		assertEquals(CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_MIDDLE_FINGER,
				new FingerImageInfo(new ByteArrayInputStream(record), FingerInfo.COMPRESSION_JPEG).getBiometricSubtype());

		record = buildImageRecord(FingerImageInfo.POSITION_UNKNOWN_FINGER, 1, 0, 10, 0, 1, 1, new byte[] { 1 });
		assertEquals(CBEFFInfo.BIOMETRIC_SUBTYPE_NONE,
				new FingerImageInfo(new ByteArrayInputStream(record), FingerInfo.COMPRESSION_JPEG).getBiometricSubtype());

		record = buildImageRecord(FingerImageInfo.POSITION_PLAIN_LEFT_FOUR_FINGERS, 1, 0, 10, 0, 1, 1, new byte[] { 1 });
		assertEquals(CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_LEFT,
				new FingerImageInfo(new ByteArrayInputStream(record), FingerInfo.COMPRESSION_JPEG).getBiometricSubtype());
	}

	@Test
	public void testConstructWriteParseRoundTrip() throws Exception {
		byte[] imageBytes = new byte[] { 9, 8, 7, 6, 5, 4 };
		FingerImageInfo original = new FingerImageInfo(
				FingerImageInfo.POSITION_RIGHT_INDEX_FINGER, 3, 2, 95,
				FingerImageInfo.IMPRESSION_TYPE_SWIPE,
				222, 333,
				new ByteArrayInputStream(imageBytes), imageBytes.length,
				FingerInfo.COMPRESSION_JPEG2000);

		assertEquals(14 + imageBytes.length, original.getRecordLength());
		assertEquals("image/jpeg2000", original.getMimeType());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		original.writeObject(out);
		byte[] encoded = out.toByteArray();

		FingerImageInfo parsed = new FingerImageInfo(new ByteArrayInputStream(encoded), FingerInfo.COMPRESSION_JPEG2000);
		assertEquals(original.getRecordLength(), parsed.getRecordLength());
		assertEquals(FingerImageInfo.POSITION_RIGHT_INDEX_FINGER, parsed.getPosition());
		assertEquals(3, parsed.getViewCount());
		assertEquals(2, parsed.getViewNumber());
		assertEquals(95, parsed.getQuality());
		assertEquals(FingerImageInfo.IMPRESSION_TYPE_SWIPE, parsed.getImpressionType());
		assertEquals(222, parsed.getWidth());
		assertEquals(333, parsed.getHeight());
		assertEquals(original, parsed);
		assertEquals(original.hashCode(), parsed.hashCode());
		assertArrayEquals(imageBytes, readAll(parsed.getImageInputStream()));
	}

	@Test
	public void testQualityOutOfRangeRejected() {
		assertThrows(IllegalArgumentException.class, () -> new FingerImageInfo(
				FingerImageInfo.POSITION_RIGHT_THUMB, 1, 0, 101, 0,
				10, 10, new ByteArrayInputStream(new byte[] { 1 }), 1,
				FingerInfo.COMPRESSION_JPEG));
		assertThrows(IllegalArgumentException.class, () -> new FingerImageInfo(
				FingerImageInfo.POSITION_RIGHT_THUMB, 1, 0, -1, 0,
				10, 10, new ByteArrayInputStream(new byte[] { 1 }), 1,
				FingerInfo.COMPRESSION_JPEG));
	}

	@Test
	public void testNullImageRejected() {
		assertThrows(IllegalArgumentException.class, () -> new FingerImageInfo(
				FingerImageInfo.POSITION_RIGHT_THUMB, 1, 0, 50, 0, 10, 10, null, 0,
				FingerInfo.COMPRESSION_JPEG));
	}

	@Test
	public void testFingerRecordBadMagic() throws Exception {
		byte[] imageRecord = buildImageRecord(
				FingerImageInfo.POSITION_RIGHT_THUMB, 1, 0, 50, 0, 10, 10, new byte[] { 1 });
		byte[] fingerRecord = buildFingerRecord(0, 30, FingerInfo.SCALE_UNITS_PPI,
				500, 500, 500, 500, 8, FingerInfo.COMPRESSION_JPEG, imageRecord);
		fingerRecord[0] = (byte) 'X'; /* corrupt "FIR" marker */
		assertThrows(IllegalArgumentException.class, () -> new FingerInfo(new ByteArrayInputStream(fingerRecord)));
	}

	@Test
	public void testToStringContent() throws Exception {
		byte[] imageBytes = new byte[] { 1 };
		byte[] record = buildImageRecord(
				FingerImageInfo.POSITION_LEFT_THUMB, 1, 0, 60,
				FingerImageInfo.IMPRESSION_TYPE_LATENT, 100, 200, imageBytes);
		FingerImageInfo info = new FingerImageInfo(new ByteArrayInputStream(record), FingerInfo.COMPRESSION_PNG);

		String text = info.toString();
		assertTrue(text.contains("quality: 60"));
		assertTrue(text.contains("Left thumb"));
		assertTrue(text.contains("Latent"));
		assertTrue(text.contains("100"));
		assertTrue(text.contains("200"));
		assertTrue(text.contains("image/png"));
	}
}
