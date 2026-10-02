package org.jmrtd.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

class SplittableInputStreamTest {

	private static byte[] data(int length) {
		byte[] result = new byte[length];
		for (int i = 0; i < length; i++) { result[i] = (byte) (i & 0xFF); }
		return result;
	}

	@Test
	void testBasicReadSkipAvailable() throws IOException {
		SplittableInputStream in = new SplittableInputStream(new ByteArrayInputStream(data(32)), 32);
		assertEquals(32, in.getLength());
		assertEquals(0, in.getPosition());
		assertEquals(0, in.read());
		assertEquals(1, in.getPosition());
		assertTrue(in.skip(3) >= 1);
		assertTrue(in.read() >= 0);
		in.available(); // may be 0 or buffered length; just exercise
		in.close();
	}

	@Test
	void testSplitStreamAtPosition() throws IOException {
		byte[] bytes = data(16);
		SplittableInputStream in = new SplittableInputStream(new ByteArrayInputStream(bytes), 16);
		in.read(new byte[8]);
		assertEquals(8, in.getPosition());

		InputStream copy = in.getInputStream(4);
		assertEquals(4, copy.read());
	}

	@Test
	void testMarkReset() throws IOException {
		SplittableInputStream in = new SplittableInputStream(new ByteArrayInputStream(data(16)), 16);
		assertTrue(in.markSupported());
		in.read(new byte[4]);
		in.mark(16);
		in.read(new byte[4]);
		assertEquals(8, in.getPosition());
		in.reset();
		assertEquals(4, in.getPosition());
		assertEquals(4, in.read());
	}

	@Test
	void testUpdateFrom() throws IOException {
		SplittableInputStream a = new SplittableInputStream(new ByteArrayInputStream(data(16)), 16);
		SplittableInputStream b = new SplittableInputStream(new ByteArrayInputStream(data(16)), 16);
		b.read(new byte[4]);
		a.updateFrom(b);
		assertEquals(4, a.inputStreamBuffer.getBytesBuffered());
	}
}
