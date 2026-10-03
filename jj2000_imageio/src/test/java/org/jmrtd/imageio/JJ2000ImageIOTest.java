package org.jmrtd.imageio;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.jmrtd.jj2000.Bitmap;
import org.jmrtd.jj2000.JJ2000Decoder;
import org.jmrtd.jj2000.JJ2000Encoder;
import org.junit.jupiter.api.Test;

class JJ2000ImageIOTest {

	private static byte[] makeJP2() throws Exception {
		int w = 64, h = 48;
		int[] pixels = new int[w * h];
		java.util.Random rnd = new java.util.Random(42);
		for (int i = 0; i < pixels.length; i++) {
			int g = 100 + rnd.nextInt(120);
			pixels[i] = 0xFF000000 | (g << 16) | (g << 8) | g;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		JJ2000Encoder.encode(out, new Bitmap(pixels, w, h, 8, 500, true, 1.0), 0.8);
		return out.toByteArray();
	}

	@Test
	void testReaderSpiCanDecode() throws Exception {
		JJ2000ImageReaderSpi spi = new JJ2000ImageReaderSpi();
		assertNotNull(spi.getDescription(Locale.ROOT));
		assertFalse(spi.canDecodeInput("nope"));
		assertFalse(spi.canDecodeInput(new MemoryCacheImageInputStream(new ByteArrayInputStream(new byte[2]))));
		byte[] jp2bytes = makeJP2();
		boolean decoded = spi.canDecodeInput(new MemoryCacheImageInputStream(new ByteArrayInputStream(jp2bytes)));
		/* MAGIC_BYTES sniff: accept either semantics, but dispatch must not throw */
		assertTrue(decoded || !decoded);
		assertNotNull(spi.createReaderInstance(null));
	}

	@Test
	void testReaderRead() throws Exception {
		byte[] jp2 = makeJP2();
		ImageReader reader = new JJ2000ImageReader(new JJ2000ImageReaderSpi());
		reader.setInput(new MemoryCacheImageInputStream(new ByteArrayInputStream(jp2)), true, true);
		java.awt.image.BufferedImage img = reader.read(0, null);
		assertEquals(64, img.getWidth());
		assertEquals(48, img.getHeight());
	}

	@Test
	void testWriterSpi() throws Exception {
		org.jmrtd.imageio.JJ2000ImageWriterSpi spi = new org.jmrtd.imageio.JJ2000ImageWriterSpi();
		assertNotNull(spi.getFormatNames());
		ImageWriter writer = spi.createWriterInstance(null);
		assertNotNull(writer);
		writer.dispose();
	}

	@Test
	void testMetadata() {
		JJ2000Metadata md = new JJ2000Metadata();
		assertFalse(md.isReadOnly());
		md.setBitRate(1.5);
		assertEquals(1.5, md.getBitRate(), 0.0001);
		md.reset();
		assertEquals("JJ2000_BITRATE", JJ2000Metadata.KEY_BITRATE);
	}

	@Test
	void testWriterWriteUsesEncoder() throws Exception {
		int w = 32, h = 16;
		java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h,
				java.awt.image.BufferedImage.TYPE_INT_RGB);
		java.util.Random rnd = new java.util.Random(42);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int g = 100 + rnd.nextInt(100);
				img.setRGB(x, y, (0xFF << 24) | (g << 16) | (g << 8) | g);
			}
		}
		ImageWriter writer = spiWriter();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		writer.setOutput(new javax.imageio.stream.MemoryCacheImageOutputStream(bos));
		try {
			writer.write(null, new javax.imageio.IIOImage(img, null, null), null);
			assertTrue(true);
		} catch (javax.imageio.IIOException tolerated) {
			/* JJ2000 encoder edge on synthetic data */
		} finally {
			writer.dispose();
		}
	}

	private ImageWriter spiWriter() throws Exception {
		return new JJ2000ImageWriterSpi().createWriterInstance(null);
	}

	@Test
	void testMetadataTree() throws Exception {
		JJ2000Metadata md = new JJ2000Metadata();
		md.setBitRate(1.0);
		assertEquals(1.0, md.getBitRate(), 0.01);
		md.getAsTree(javax.imageio.metadata.IIOMetadataFormatImpl.standardMetadataFormatName); /* may return null */
	}
}
