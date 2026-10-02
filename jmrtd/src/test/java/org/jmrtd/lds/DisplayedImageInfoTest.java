package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

public class DisplayedImageInfoTest {

	private static final byte[] IMAGE_BYTES = { 9, 8, 7, 6, 5, 4, 3, 2, 1 };

	@Test
	public void testConstructPortrait() {
		DisplayedImageInfo info = new DisplayedImageInfo(
				ImageInfo.TYPE_PORTRAIT, IMAGE_BYTES);
		assertEquals(ImageInfo.TYPE_PORTRAIT, info.getType());
		assertEquals(ImageInfo.JPEG_MIME_TYPE, info.getMimeType());
		assertEquals(IMAGE_BYTES.length, info.getImageLength());
		assertEquals(DisplayedImageInfo.DISPLAYED_PORTRAIT_TAG, info.getDisplayedImageTag());
		assertTrue(info.getRecordLength() > IMAGE_BYTES.length);
		assertTrue(info.toString().contains("Portrait"));
	}

	@Test
	public void testConstructSignature() {
		DisplayedImageInfo info = new DisplayedImageInfo(
				ImageInfo.TYPE_SIGNATURE_OR_MARK, IMAGE_BYTES);
		assertEquals(ImageInfo.TYPE_SIGNATURE_OR_MARK, info.getType());
		assertEquals(ImageInfo.JPEG_MIME_TYPE, info.getMimeType());
		assertEquals(DisplayedImageInfo.DISPLAYED_SIGNATURE_OR_MARK_TAG,
				info.getDisplayedImageTag());
	}

	@Test
	public void testRoundTrip() throws Exception {
		DisplayedImageInfo info = new DisplayedImageInfo(
				ImageInfo.TYPE_PORTRAIT, IMAGE_BYTES);
		byte[] encoded = info.getEncoded();
		DisplayedImageInfo copy = new DisplayedImageInfo(new ByteArrayInputStream(encoded));
		assertEquals(info.getType(), copy.getType());
		assertEquals(info.getMimeType(), copy.getMimeType());
		assertEquals(info, copy);
		assertEquals(info.hashCode(), copy.hashCode());
		byte[] copyBytes = new byte[copy.getImageLength()];
		copy.getImageInputStream().read(copyBytes);
		assertArrayEquals(IMAGE_BYTES, copyBytes);
	}

	@Test
	public void testInvalidTag() {
		/* 0x5F41 is not a valid displayed image tag. */
		byte[] bad = { 0x5F, 0x41, 0x01, 0x00 };
		assertThrows(IllegalArgumentException.class,
				() -> new DisplayedImageInfo(new ByteArrayInputStream(bad)));
	}

	@Test
	public void testInvalidType() {
		assertThrows(NumberFormatException.class,
				() -> new DisplayedImageInfo(ImageInfo.TYPE_IRIS, IMAGE_BYTES));
	}
}
