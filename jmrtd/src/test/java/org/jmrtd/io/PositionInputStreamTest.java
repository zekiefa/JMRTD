package org.jmrtd.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class PositionInputStreamTest {

	@Test
	void testReadSingleByteTracksPosition() throws IOException {
		PositionInputStream in = new PositionInputStream(new ByteArrayInputStream(new byte[] { 10, 20, 30 }));
		assertEquals(0, in.getPosition());
		assertEquals(10, in.read());
		assertEquals(1, in.getPosition());
		assertEquals(20, in.read());
		assertEquals(30, in.read());
		assertEquals(3, in.getPosition());
	}

	@Test
	void testReadByteArrayTracksPosition() throws IOException {
		byte[] data = new byte[256];
		for (int i = 0; i < data.length; i++) { data[i] = (byte) i; }
		PositionInputStream in = new PositionInputStream(new ByteArrayInputStream(data));
		byte[] dest = new byte[100];
		int n = in.read(dest);
		assertEquals(100, n);
		assertEquals(100, in.getPosition());
		assertEquals(0, dest[0]);
		assertEquals(99, dest[99]);
		n = in.read(dest, 0, 50);
		assertEquals(50, n);
		assertEquals(150, in.getPosition());
		assertEquals((byte) 100, dest[0]);
	}

	@Test
	void testSkipTracksPosition() throws IOException {
		PositionInputStream in = new PositionInputStream(new ByteArrayInputStream(new byte[64]));
		assertEquals(10, in.skip(10));
		assertEquals(10, in.getPosition());
		assertEquals(54, in.skip(100)); // limited by carrier size
		assertEquals(64, in.getPosition());
	}

	@Test
	void testMarkReset() throws IOException {
		PositionInputStream in = new PositionInputStream(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 }));
		in.read();
		in.mark(4);
		assertEquals(1, in.getPosition());
		in.read();
		in.read();
		assertEquals(3, in.getPosition());
		in.reset();
		assertEquals(1, in.getPosition());
	}

	@Test
	void testMarkSupported() {
		PositionInputStream in = new PositionInputStream(new ByteArrayInputStream(new byte[4]));
		assertTrue(in.markSupported());
	}

	@Test
	void testSkipZeroLogs() throws IOException {
		PositionInputStream in = new PositionInputStream(new ByteArrayInputStream(new byte[0]));
		assertEquals(0, in.skip(5));
		assertEquals(0, in.getPosition());
	}
}
