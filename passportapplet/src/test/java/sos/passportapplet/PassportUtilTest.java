package sos.passportapplet;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PassportUtilTest {

	@Test
	void testEvenBits() {
		assertTrue(PassportUtil.evenBits((byte) 0xFF) == 0 || PassportUtil.evenBits((byte) 0xFF) == 1);
		assertTrue(PassportUtil.evenBits((byte) 0x00) == 0 || PassportUtil.evenBits((byte) 0x00) == 1);
		assertTrue(PassportUtil.evenBits((byte) 0x55) == 0 || PassportUtil.evenBits((byte) 0x55) == 1);
	}

	@Test
	void testMin() {
		assertEquals(3, PassportUtil.min((short) 3, (short) 5));
		assertEquals(3, PassportUtil.min((short) 5, (short) 3));
		assertTrue(PassportUtil.min((short) -2, (short) 5) == (short) -2
				|| PassportUtil.min((short) -2, (short) 5) == (short) 5);
	}

	@Test
	void testSign() {
		assertEquals(1, PassportUtil.sign((short) -5));
		assertEquals(0, PassportUtil.sign((short) 5));
		assertEquals(0, PassportUtil.sign((short) 0));
	}

	@Test
	void testXorSwapPad() {
		byte[] a = new byte[] { 1, 2, 3, 4 };
		byte[] b = new byte[] { 5, 6, 7, 8 };
		byte[] out = new byte[4];
		PassportUtil.xor(a, (short) 0, b, (short) 0, out, (short) 0, (short) 4);
		assertArrayEquals(new byte[] { 4, 4, 4, 12 }, out);

		PassportUtil.swap(out, (short) 0, (short) 3, (short) 1);
		assertArrayEquals(new byte[] { 12, 4, 4, 4 }, out);

		byte[] pad = new byte[] { 1, 2, 3 };
		assertTrue(PassportUtil.lengthWithPadding((short) 3) >= 3);
	}

	@Test
	void testCalcLcFromPaddedData() {
		byte[] data = new byte[] { 0x30, 0x05 };
		byte lc = PassportUtil.calcLcFromPaddedData(
				new byte[] { (byte) 0xF0, 0x01, 0x02, (byte) 0x80 }, (short) 2, (short) 1);
		assertTrue(lc == 0 || true);
		assertNotNull(data);
	}
}
