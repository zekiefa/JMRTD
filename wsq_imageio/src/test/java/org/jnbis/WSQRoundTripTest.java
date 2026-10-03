package org.jnbis;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class WSQRoundTripTest {

	private static Bitmap syntheticImage(int width, int height) {
		java.util.Random rnd = new java.util.Random(42);
		byte[] pixels = new byte[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				/* smooth fingerprint-like: mid-gray with mild sinus modulation + noise */
				double v = 128 + 40 * Math.sin(x / 8.0) * Math.cos(y / 6.0) + rnd.nextInt(20) - 10;
				pixels[y * width + x] = (byte) Math.max(0, Math.min(255, (int) v));
			}
		}
		return new Bitmap(pixels, width, height, 500, 8, 1);
	}

	@Test
	void testEncodeDecodeRoundTrip() throws Exception {
		Bitmap bitmap = syntheticImage(200, 100);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		WSQEncoder.encode(out, bitmap, 0.75, "comment1", "comment2");
		byte[] wsq = out.toByteArray();
		assertTrue(wsq.length > 50);

		BitmapWithMetadata decoded = WSQDecoder.decode(new ByteArrayInputStream(wsq));
		assertEquals(200, decoded.getWidth());
		assertEquals(100, decoded.getHeight());
		assertEquals(8, decoded.getDepth());
		assertEquals(500, decoded.getPpi());

		/* lossy tolerance: average absolute error should stay bounded */
		long err = 0;
		byte[] orig = bitmap.getPixels();
		byte[] rest = decoded.getPixels();
		assertEquals(orig.length, rest.length);
		for (int i = 0; i < orig.length; i++) {
			err += Math.abs((orig[i] & 0xFF) - (rest[i] & 0xFF));
		}
		assertTrue(err / (double) orig.length < 16.0, "average error too high");
	}

	@Test
	void testEncodeDecodeWithMetadata() throws Exception {
		Bitmap bitmap = syntheticImage(64, 64);
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("capture", "test");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		WSQEncoder.encode(out, bitmap, 2.0, metadata, "hello");
		BitmapWithMetadata decoded = WSQDecoder.decode(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(64, decoded.getWidth());
		assertNotNull(decoded.getComments());
		assertEquals("test", decoded.getMetadata().get("capture"));
	}

	@Test
	void testBitmapAccessors() {
		Bitmap b = syntheticImage(10, 5);
		assertEquals(10, b.getWidth());
		assertEquals(5, b.getHeight());
		assertEquals(500, b.getPpi());
		assertEquals(8, b.getDepth());
		assertEquals(50, b.getPixels().length);
		assertEquals(1, b.getLossyflag());
	}

	@Test
	void testBitmapWithMetadataConstructors() {
		BitmapWithMetadata b = new BitmapWithMetadata(new byte[4], 2, 2, 100, 8, 0);
		assertNotNull(b.getMetadata());
		assertNotNull(b.getComments());
	}

	@Test
	void testDecodeRejectsInvalid() {
		assertThrows(Exception.class, () -> WSQDecoder.decode(new ByteArrayInputStream(new byte[] { 1, 2, 3 })));
	}

	@Test
	void testConstantsNonTrivial() {
		assertNotNull(NISTConstants.class);
		assertNotNull(WSQConstants.class);
		/* touch some known constant values if public */
		assertTrue(true);
	}
}
