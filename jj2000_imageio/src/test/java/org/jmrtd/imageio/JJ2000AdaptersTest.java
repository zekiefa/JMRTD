package org.jmrtd.imageio;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Locale;

import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.junit.jupiter.api.Test;

class JJ2000AdaptersTest {

	@Test
	void testInputAdapter() throws Exception {
		byte[] data = new byte[] { 5, 6, 7, 8, 9 };
		ImageInputStreamAdapter a = new ImageInputStreamAdapter(
				new MemoryCacheImageInputStream(new ByteArrayInputStream(data)));
		assertEquals(5, a.read());
		byte[] buf = new byte[4];
		assertEquals(4, a.read(buf, 0, 4));
		assertArrayEquals(new byte[] { 6, 7, 8, 9 }, buf);
		assertEquals(-1, a.read(buf));
		a.close();
	}

	@Test
	void testOutputAdapter() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageOutputStreamAdapter a = new ImageOutputStreamAdapter(
				new MemoryCacheImageOutputStream(bos));
		a.write(1);
		byte[] more = new byte[] { 2, 3 };
		a.write(more, 0, 2);
		a.write(more);
		a.flush();
		a.close();
		assertArrayEquals(new byte[] { 1, 2, 3, 2, 3 }, bos.toByteArray());
	}

	@Test
	void testMetadataFormat() {
		Object f = JJ2000MetadataFormat.getInstance();
		assertNotNull(f);
	}

	@Test
	void testReaderSpiFormatsAndNames() throws Exception {
		JJ2000ImageReaderSpi spi = new JJ2000ImageReaderSpi();
		assertTrue(spi.getFormatNames().length > 0);
		assertNotNull(spi.getDescription(Locale.ROOT));
	}

	@Test
	void testWriterSpiFormats() throws Exception {
		JJ2000ImageWriterSpi spi = new JJ2000ImageWriterSpi();
		assertTrue(java.util.Arrays.asList(spi.getFormatNames()).size() > 0);
	}

	@Test
	void testAdapterSkipMarkAvailable() throws Exception {
		byte[] data = new byte[] { 5, 6, 7, 8, 9 };
		ImageInputStreamAdapter a = new ImageInputStreamAdapter(
				new MemoryCacheImageInputStream(new ByteArrayInputStream(data)));
		a.skip(2);
		assertEquals(7, a.read());
		try { a.mark(4); a.reset(); } catch (Exception tolerated) { }
		try { a.available(); } catch (Exception tolerated) { }
		a.close();
	}

	@Test
	void testWriteParamDefaults() {
		JJ2000ImageWriteParam p = new JJ2000ImageWriteParam(java.util.Locale.getDefault());
		assertNotNull(p.getLocale());
	}
}
