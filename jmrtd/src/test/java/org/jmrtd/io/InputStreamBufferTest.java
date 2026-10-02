package org.jmrtd.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jmrtd.io.InputStreamBuffer.SubInputStream;
import org.junit.jupiter.api.Test;

class InputStreamBufferTest {

	private static byte[] data(int length) {
		byte[] result = new byte[length];
		for (int i = 0; i < length; i++) { result[i] = (byte) (i & 0xFF); }
		return result;
	}

	@Test
	void testConstruction() {
		InputStreamBuffer b = new InputStreamBuffer(new ByteArrayInputStream(data(64)), 64);
		assertEquals(64, b.getLength());
		assertEquals(0, b.getPosition());
		assertEquals(0, b.getBytesBuffered());
		assertTrue(b.toString().startsWith("InputStreamBuffer"));
	}

	@Test
	void testStreamReadsAndBuffers() throws IOException {
		byte[] bytes = data(32);
		InputStreamBuffer b = new InputStreamBuffer(new ByteArrayInputStream(bytes), 32);
		SubInputStream in = b.getInputStream();

		assertTrue(in.markSupported());
		assertEquals(0, in.getPosition());

		int v = in.read();
		assertEquals(0, v);
		assertEquals(1, in.getPosition());
		assertEquals(1, b.getBytesBuffered());

		/* Second identical stream re-reads buffered prefix from buffer. */
		SubInputStream in2 = b.getInputStream();
		assertEquals(0, in2.read());
		assertEquals(1, b.getBytesBuffered()); // still 1, not duplicated
	}

	@Test
	void testBlockReadAndSkip() throws IOException {
		byte[] bytes = data(16);
		InputStreamBuffer b = new InputStreamBuffer(new ByteArrayInputStream(bytes), 16);
		SubInputStream in = b.getInputStream();

		byte[] dest = new byte[8];
		int n = in.read(dest, 0, 4);
		assertEquals(4, n);
		assertEquals(4, in.getPosition());
		assertArrayEquals(new byte[] { 0, 1, 2, 3, 0, 0, 0, 0 }, dest);

		/* skip beyond buffered part into carrier */
		long skipped = in.skip(4);
		assertEquals(4, skipped);
		assertEquals(8, in.getPosition());

		/* available reflects buffered data from position */
		SubInputStream in2 = b.getInputStream();
		assertEquals(4, in2.available());

		in2.skip(4);
		n = in2.read(dest);
		assertTrue(n > 0);
	}

	@Test
	void testReadEdgeCases() throws IOException {
		byte[] bytes = data(8);
		InputStreamBuffer b = new InputStreamBuffer(new ByteArrayInputStream(bytes), 8);
		SubInputStream in = b.getInputStream();

		assertThrows(NullPointerException.class, () -> in.read(null, 0, 1));
		assertThrows(IndexOutOfBoundsException.class, () -> in.read(new byte[4], -1, 1));
		assertThrows(IndexOutOfBoundsException.class, () -> in.read(new byte[4], 0, 5));
		assertEquals(0, in.read(new byte[4], 0, 0));

		in.read(new byte[] { 0 }); // buffer 1 byte at 0
	}

	@Test
	void testReadPastEnd() throws IOException {
		byte[] bytes = data(4);
		InputStreamBuffer b = new InputStreamBuffer(new ByteArrayInputStream(bytes), 4);
		SubInputStream in = b.getInputStream();
		in.read(new byte[4]);
		assertEquals(-1, in.read());
		SubInputStream in2 = b.getInputStream();
		byte[] dest = new byte[4];
		assertEquals(4, in2.read(dest, 0, 4));
		assertEquals(-1, in2.read(dest, 0, 1));
	}

	@Test
	void testMarkReset() throws IOException {
		InputStreamBuffer b = new InputStreamBuffer(new ByteArrayInputStream(data(16)), 16);
		SubInputStream in = b.getInputStream();
		assertThrows(IOException.class, () -> in.reset());
		in.read(new byte[4]);
		in.mark(16);
		in.read(new byte[4]);
		assertEquals(8, in.getPosition());
		in.reset();
		assertEquals(4, in.getPosition());
		assertNotNull(in.getBuffer());
		in.close(); // no-op
	}

	@Test
	void testUpdateFrom() throws IOException {
		InputStreamBuffer a = new InputStreamBuffer(new ByteArrayInputStream(data(16)), 16);
		InputStreamBuffer b = new InputStreamBuffer(new ByteArrayInputStream(data(16)), 16);
		b.getInputStream().read(new byte[4]); // b buffers first 4
		a.updateFrom(b);
		assertEquals(4, a.getBytesBuffered());
	}

	@Test
	void testCarrierWithoutMarkSupport() throws IOException {
		InputStream nonMarking = new InputStream() {
			private int idx = 0;
			private byte[] d = data(16);
			public int read() { return idx < d.length ? d[idx++] & 0xFF : -1; }
			public int read(byte[] buf, int off, int len) {
				if (idx >= d.length) { return -1; }
				int n = Math.min(len, d.length - idx);
				System.arraycopy(d, idx, buf, off, n);
				idx += n;
				return n;
			}
			public boolean markSupported() { return false; }
			public long skip(long n) {
				long s = Math.min(n, d.length - idx);
				idx += (int) s;
				return s;
			}
		};
		InputStreamBuffer b = new InputStreamBuffer(nonMarking, 16);
		SubInputStream in = b.getInputStream();
		assertEquals(0, in.read());
		long skipped = in.skip(4);
		assertTrue(skipped >= 1);
	}
}
