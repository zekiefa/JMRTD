package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.junit.jupiter.api.Test;

import net.sf.scuba.data.Gender;

/**
 * Deep coverage tests for {@link FaceImageInfo} (and {@link FaceInfo} carriage).
 * Builds synthetic ISO/IEC 19794-5 facial record data blocks and verifies
 * parsing of every header field, feature points, multiple images per record,
 * image byte access, and the textual representation.
 */
public class FaceImageInfoDeepTest {

	private static final int FORMAT_IDENTIFIER = 0x46414300; /* "FAC", 0x00 */
	private static final int VERSION_NUMBER = 0x30313000; /* "010", 0x00 */

	private static final int FEATURE_GLASSES = 0x000002;
	private static final int FEATURE_MOUSTACHE = 0x000004;
	private static final int FEATURE_BEARD = 0x000008;
	private static final int FEATURE_FEATURES_ARE_SPECIFIED = 0x000001;

	/**
	 * Builds a raw facial record data block (ISO 19794-5, 5.5 - 5.8):
	 * 4 bytes record length, 2 bytes feature point count, facial info (14 more
	 * bytes), 8 bytes per feature point, image info block (12 bytes), image data.
	 */
	private static byte[] buildImageRecord(byte genderByte, byte eyeColorByte, byte hairColorByte,
			int featureMask, short expression, byte[] poseAngle, byte[] poseAngleUncertainty,
			byte[][] featurePoints, int faceImageType, int imageDataType,
			int width, int height, int colorSpace, int sourceType,
			int deviceType, int quality, byte[] imageBytes) throws Exception {
		int fpCount = featurePoints == null ? 0 : featurePoints.length;
		int recordLength = 20 + 8 * fpCount + 12 + imageBytes.length;

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(out);

		/* Facial Information Block (20 bytes). */
		dataOut.writeInt(recordLength);				/* 4 */
		dataOut.writeShort(fpCount);				/* +2 = 6 */
		dataOut.writeByte(genderByte);				/* +1 = 7 */
		dataOut.writeByte(eyeColorByte);			/* +1 = 8 */
		dataOut.writeByte(hairColorByte);			/* +1 = 9 */
		dataOut.writeByte((featureMask & 0xFF0000) >> 16); /* +1 = 10 */
		dataOut.writeShort(featureMask & 0xFFFF);	/* +2 = 12 */
		dataOut.writeShort(expression);				/* +2 = 14 */
		dataOut.write(poseAngle);					/* +3 = 17 */
		dataOut.write(poseAngleUncertainty);		/* +3 = 20 */

		/* Feature points (8 bytes each). */
		for (int i = 0; i < fpCount; i++) {
			dataOut.write(featurePoints[i]); /* must be 8 bytes: type, code, x(2), y(2), reserved(2) */
		}

		/* Image Information Block (12 bytes). */
		dataOut.writeByte(faceImageType);	/* 1 */
		dataOut.writeByte(imageDataType);	/* +1 = 2 */
		dataOut.writeShort(width);			/* +2 = 4 */
		dataOut.writeShort(height);			/* +2 = 6 */
		dataOut.writeByte(colorSpace);		/* +1 = 7 */
		dataOut.writeByte(sourceType);		/* +1 = 8 */
		dataOut.writeShort(deviceType);		/* +2 = 10 */
		dataOut.writeShort(quality);		/* +2 = 12 */

		dataOut.write(imageBytes);
		dataOut.flush();
		return out.toByteArray();
	}

	/**
	 * Wraps one or more facial record data blocks in a full facial record
	 * ("FAC" header + version + record length + count).
	 */
	private static byte[] buildFaceRecord(byte[]... imageRecords) throws Exception {
		int dataLength = 0;
		for (byte[] r: imageRecords) {
			dataLength += r.length;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(out);
		dataOut.writeInt(FORMAT_IDENTIFIER);
		dataOut.writeInt(VERSION_NUMBER);
		dataOut.writeInt(14 + dataLength);
		dataOut.writeShort(imageRecords.length);
		for (byte[] r: imageRecords) {
			dataOut.write(r);
		}
		dataOut.flush();
		return out.toByteArray();
	}

	private static byte[] featurePointBytes(int type, int majorCode, int minorCode, int x, int y) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(out);
		dataOut.writeByte(type);
		dataOut.writeByte((majorCode << 4) | minorCode);
		dataOut.writeShort(x);
		dataOut.writeShort(y);
		dataOut.writeShort(0); /* reserved */
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
	public void testParseAllHeaderFields() throws Exception {
		byte[] imageBytes = new byte[] { 10, 20, 30, 40, 50, 60, 70 };
		byte[] record = buildImageRecord(
				(byte) Gender.FEMALE.toInt(), (byte) FaceImageInfo.EYE_COLOR_GREEN, (byte) FaceImageInfo.HAIR_COLOR_RED,
				FEATURE_FEATURES_ARE_SPECIFIED | FEATURE_GLASSES | FEATURE_MOUSTACHE, (short) FaceImageInfo.EXPRESSION_SMILE_OPEN,
				new byte[] { 5, 10, 15 }, new byte[] { 1, 2, 3 },
				null,
				FaceImageInfo.FACE_IMAGE_TYPE_FULL_FRONTAL, FaceImageInfo.IMAGE_DATA_TYPE_JPEG,
				640, 480, FaceImageInfo.IMAGE_COLOR_SPACE_GRAY8, FaceImageInfo.SOURCE_TYPE_VIDEO_FRAME_DIGITAL_CAM,
				0x1234, 77, imageBytes);

		FaceImageInfo info = new FaceImageInfo(new ByteArrayInputStream(record));

		assertEquals(record.length, info.getRecordLength());
		assertEquals(Gender.FEMALE, info.getGender());
		assertEquals(FaceImageInfo.EyeColor.GREEN, info.getEyeColor());
		assertEquals(FaceImageInfo.HAIR_COLOR_RED, info.getHairColor());
		assertEquals(FEATURE_FEATURES_ARE_SPECIFIED | FEATURE_GLASSES | FEATURE_MOUSTACHE, info.getFeatureMask());
		assertEquals(FaceImageInfo.EXPRESSION_SMILE_OPEN, info.getExpression());
		assertArrayEquals(new int[] { 5, 10, 15 }, info.getPoseAngle());
		assertArrayEquals(new int[] { 1, 2, 3 }, info.getPoseAngleUncertainty());
		assertEquals(FaceImageInfo.FACE_IMAGE_TYPE_FULL_FRONTAL, info.getFaceImageType());
		assertEquals(FaceImageInfo.IMAGE_DATA_TYPE_JPEG, info.getImageDataType());
		assertEquals(640, info.getWidth());
		assertEquals(480, info.getHeight());
		assertEquals(FaceImageInfo.IMAGE_COLOR_SPACE_GRAY8, info.getColorSpace());
		assertEquals(FaceImageInfo.SOURCE_TYPE_VIDEO_FRAME_DIGITAL_CAM, info.getSourceType());
		assertEquals(0x1234, info.getDeviceType());
		assertEquals(77, info.getQuality());
		assertEquals(0, info.getFeaturePoints().length);
		assertEquals(ImageInfo.TYPE_PORTRAIT, info.getType());
		assertEquals("image/jpeg", info.getMimeType());
		assertEquals(imageBytes.length, info.getImageLength());
		assertArrayEquals(imageBytes, readAll(info.getImageInputStream()));
	}

	@Test
	public void testParseWithFeaturePoints() throws Exception {
		byte[][] featurePoints = new byte[][] {
			featurePointBytes(1, 3, 4, 100, 200),
			featurePointBytes(2, 5, 6, 300, 400)
		};
		byte[] imageBytes = new byte[] { 1 };
		byte[] record = buildImageRecord(
				(byte) Gender.MALE.toInt(), (byte) FaceImageInfo.EYE_COLOR_BLUE, (byte) FaceImageInfo.HAIR_COLOR_BLACK,
				FEATURE_FEATURES_ARE_SPECIFIED | FEATURE_BEARD, (short) FaceImageInfo.EXPRESSION_NEUTRAL,
				new byte[] { 0, 0, 0 }, new byte[] { 0, 0, 0 },
				featurePoints,
				FaceImageInfo.FACE_IMAGE_TYPE_TOKEN_FRONTAL, FaceImageInfo.IMAGE_DATA_TYPE_JPEG2000,
				200, 250, FaceImageInfo.IMAGE_COLOR_SPACE_RGB24, FaceImageInfo.SOURCE_TYPE_STATIC_PHOTO_DIGITAL_CAM,
				0, 50, imageBytes);

		FaceImageInfo info = new FaceImageInfo(new ByteArrayInputStream(record));
		assertEquals(20 + 16 + 12 + 1, info.getRecordLength());
		assertEquals(2, info.getFeaturePoints().length);

		FaceImageInfo.FeaturePoint first = info.getFeaturePoints()[0];
		assertEquals(1, first.getType());
		assertEquals(3, first.getMajorCode());
		assertEquals(4, first.getMinorCode());
		assertEquals(100, first.getX());
		assertEquals(200, first.getY());
		assertTrue(first.toString().contains("3.4"));

		FaceImageInfo.FeaturePoint second = info.getFeaturePoints()[1];
		assertEquals(2, second.getType());
		assertEquals(5, second.getMajorCode());
		assertEquals(6, second.getMinorCode());

		assertEquals(FaceImageInfo.FACE_IMAGE_TYPE_TOKEN_FRONTAL, info.getFaceImageType());
		assertEquals(FaceImageInfo.IMAGE_DATA_TYPE_JPEG2000, info.getImageDataType());
		assertEquals("image/jp2", info.getMimeType());
	}

	@Test
	public void testMultipleImagesInOneFaceRecord() throws Exception {
		byte[] record1 = buildImageRecord(
				(byte) Gender.FEMALE.toInt(), (byte) FaceImageInfo.EYE_COLOR_BLUE, (byte) FaceImageInfo.HAIR_COLOR_BLONDE,
				0, (short) FaceImageInfo.EXPRESSION_UNSPECIFIED,
				new byte[] { 0, 0, 0 }, new byte[] { 0, 0, 0 }, null,
				FaceImageInfo.FACE_IMAGE_TYPE_BASIC, FaceImageInfo.IMAGE_DATA_TYPE_JPEG,
				100, 120, FaceImageInfo.IMAGE_COLOR_SPACE_RGB24, FaceImageInfo.SOURCE_TYPE_STATIC_PHOTO_SCANNER,
				0, 100, new byte[] { 1, 2, 3 });
		byte[] record2 = buildImageRecord(
				(byte) Gender.MALE.toInt(), (byte) FaceImageInfo.EYE_COLOR_BROWN, (byte) FaceImageInfo.HAIR_COLOR_GRAY,
				FEATURE_FEATURES_ARE_SPECIFIED, (short) FaceImageInfo.EXPRESSION_FROWNING,
				new byte[] { 1, 1, 1 }, new byte[] { 2, 2, 2 }, null,
				FaceImageInfo.FACE_IMAGE_TYPE_FULL_FRONTAL, FaceImageInfo.IMAGE_DATA_TYPE_JPEG2000,
				300, 400, FaceImageInfo.IMAGE_COLOR_SPACE_YUV422, FaceImageInfo.SOURCE_TYPE_UNSPECIFIED,
				1, 90, new byte[] { 4, 5, 6, 7 });

		byte[] faceRecord = buildFaceRecord(record1, record2);
		FaceInfo faceInfo = new FaceInfo(new ByteArrayInputStream(faceRecord));

		assertEquals(2, faceInfo.getFaceImageInfos().size());

		FaceImageInfo first = faceInfo.getFaceImageInfos().get(0);
		assertEquals(Gender.FEMALE, first.getGender());
		assertEquals(100, first.getWidth());
		assertEquals(120, first.getHeight());
		assertEquals(3, first.getImageLength());

		FaceImageInfo second = faceInfo.getFaceImageInfos().get(1);
		assertEquals(Gender.MALE, second.getGender());
		assertEquals(FaceImageInfo.EyeColor.BROWN, second.getEyeColor());
		assertEquals(300, second.getWidth());
		assertEquals(400, second.getHeight());
		assertArrayEquals(new byte[] { 4, 5, 6, 7 }, readAll(second.getImageInputStream()));
	}

	@Test
	public void testZeroWidthHeightFixedUp() throws Exception {
		byte[] record = buildImageRecord(
				(byte) Gender.UNKNOWN.toInt(), (byte) FaceImageInfo.EYE_COLOR_UNSPECIFIED, (byte) FaceImageInfo.HAIR_COLOR_UNSPECIFIED,
				0, (short) FaceImageInfo.EXPRESSION_UNSPECIFIED,
				new byte[] { 0, 0, 0 }, new byte[] { 0, 0, 0 }, null,
				FaceImageInfo.FACE_IMAGE_TYPE_BASIC, FaceImageInfo.IMAGE_DATA_TYPE_JPEG,
				0, 0, FaceImageInfo.IMAGE_COLOR_SPACE_UNSPECIFIED, FaceImageInfo.SOURCE_TYPE_UNSPECIFIED,
				0, 0, new byte[] { 9 });
		FaceImageInfo info = new FaceImageInfo(new ByteArrayInputStream(record));

		/* Implementation falls back to 800 x 600 for non-positive width/height. */
		assertEquals(800, info.getWidth());
		assertEquals(600, info.getHeight());
	}

	@Test
	public void testConstructAndWriteRoundTrip() throws Exception {
		byte[] imageBytes = new byte[] { 11, 22, 33, 44 };
		FaceImageInfo.FeaturePoint fp = new FaceImageInfo.FeaturePoint(1, 2, 3, 111, 222);
		FaceImageInfo info = new FaceImageInfo(
				Gender.MALE, FaceImageInfo.EyeColor.BLACK,
				FEATURE_GLASSES, FaceImageInfo.HAIR_COLOR_BLUE,
				FaceImageInfo.EXPRESSION_SQUINTING,
				new int[] { 4, 5, 6 }, new int[] { 7, 8, 9 },
				FaceImageInfo.FACE_IMAGE_TYPE_FULL_FRONTAL,
				FaceImageInfo.IMAGE_COLOR_SPACE_RGB24,
				FaceImageInfo.SOURCE_TYPE_STATIC_PHOTO_DIGITAL_CAM,
				0x22, 88,
				new FaceImageInfo.FeaturePoint[] { fp },
				320, 240,
				new ByteArrayInputStream(imageBytes), imageBytes.length,
				FaceImageInfo.IMAGE_DATA_TYPE_JPEG2000);

		assertEquals(20 + 8 + 12 + imageBytes.length, info.getRecordLength());
		assertEquals(1, info.getFeaturePoints().length);
		assertEquals(320, info.getWidth());
		assertEquals(240, info.getHeight());
		assertEquals(88, info.getQuality());
		assertEquals(0x22, info.getDeviceType());
		assertArrayEquals(new int[] { 4, 5, 6 }, info.getPoseAngle());

		/* Round trip via binary encoding. */
		byte[] encoded = info.getEncoded();
		FaceImageInfo parsed = new FaceImageInfo(new ByteArrayInputStream(encoded));
		assertEquals(info.getRecordLength(), parsed.getRecordLength());
		assertEquals(info.getGender(), parsed.getGender());
		assertEquals(info.getEyeColor(), parsed.getEyeColor());
		assertEquals(info.getHairColor(), parsed.getHairColor());
		assertEquals(info.getFeatureMask(), parsed.getFeatureMask());
		assertEquals(info.getExpression(), parsed.getExpression());
		assertEquals(info.getFaceImageType(), parsed.getFaceImageType());
		assertEquals(info.getWidth(), parsed.getWidth());
		assertEquals(info.getHeight(), parsed.getHeight());
		assertEquals(1, parsed.getFeaturePoints().length);
		FaceImageInfo.FeaturePoint parsedFp = parsed.getFeaturePoints()[0];
		assertEquals(2, parsedFp.getMajorCode());
		assertEquals(3, parsedFp.getMinorCode());
		assertEquals(111, parsedFp.getX());
		assertEquals(222, parsedFp.getY());
		assertEquals(info, parsed);
	}

	@Test
	public void testToStringContent() throws Exception {
		byte[] record = buildImageRecord(
				(byte) Gender.FEMALE.toInt(), (byte) FaceImageInfo.EYE_COLOR_PINK, (byte) FaceImageInfo.HAIR_COLOR_WHITE,
				FEATURE_FEATURES_ARE_SPECIFIED | FEATURE_GLASSES, (short) FaceImageInfo.EXPRESSION_RAISED_EYEBROWS,
				new byte[] { 9, 8, 7 }, new byte[] { 0, 0, 0 }, null,
				FaceImageInfo.FACE_IMAGE_TYPE_BASIC, FaceImageInfo.IMAGE_DATA_TYPE_JPEG,
				123, 234, FaceImageInfo.IMAGE_COLOR_SPACE_RGB24, FaceImageInfo.SOURCE_TYPE_STATIC_PHOTO_SCANNER,
				0, 0, new byte[] { 1, 2, 3 });
		FaceImageInfo info = new FaceImageInfo(new ByteArrayInputStream(record));

		String text = info.toString();
		assertTrue(text.contains("123 x 234"));
		assertTrue(text.contains("glasses"));
		assertTrue(text.contains("white"));
		assertTrue(text.contains("raised eyebrows"));
		assertTrue(text.contains("(none)")); /* no feature points */
		assertTrue(text.contains("basic"));
	}

	@Test
	public void testFaceRecordBadMagic() throws Exception {
		byte[] record = buildImageRecord(
				(byte) Gender.MALE.toInt(), (byte) 0, (byte) 0, 0, (short) 0,
				new byte[] { 0, 0, 0 }, new byte[] { 0, 0, 0 }, null,
				0, 0, 10, 10, 0, 0, 0, 0, new byte[] { 1 });
		byte[] faceRecord = buildFaceRecord(record);
		faceRecord[0] = (byte) 'X'; /* corrupt the "FAC" marker */
		assertThrows(IllegalArgumentException.class, () -> new FaceInfo(new ByteArrayInputStream(faceRecord)));
	}

	@Test
	public void testEyeColorRoundTrip() {
		for (FaceImageInfo.EyeColor eyeColor: FaceImageInfo.EyeColor.values()) {
			assertEquals(eyeColor, FaceImageInfo.EyeColor.toEyeColor(eyeColor.toInt()));
		}
	}
}
