package sos.passportapplet;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BERTLVScannerTest {

	@Test
	void testUniversalPrimitiveShortForm() {
		/* 30 03 02 01 2A (SEQUENCE, len 3, INTEGER 2 bytes) — valueOffset after tag+len */
		byte[] in = new byte[] { 0x30, 0x03, 0x02, 0x01, 0x2A };
		short afterTag = BERTLVScanner.readTag(in, (short) 0);
		assertEquals(1, (int) afterTag);
		assertEquals(0x30, (int) ioTag());
		assertEquals(BERTLVScanner.UNIVERSAL_CLASS, ioTagClass());
		BERTLVScanner.readLength(in, afterTag);
		assertEquals(3, (int) BERTLVScanner.skipValue() - 2);
	}

	@Test
	void testFillerBytesSkipped() {
		byte[] in = new byte[] { 0, 0, (byte) 0x30, 0x00 };
		short afterTag = BERTLVScanner.readTag(in, (short) 0);
		assertEquals(3, (int) afterTag);
	}

	@Test
	void testContextSpecificPrimitive() {
		/* 81 len 05 */
		byte[] in = new byte[] { (byte) 0x81, 0x05 };
		short afterTag = BERTLVScanner.readTag(in, (short) 0);
		assertEquals(1, (int) afterTag);
		short valueOff = BERTLVScanner.readLength(in, afterTag);
		assertEquals(2, (int) valueOff);
	}

	@Test
	void testLongFormLength() {
		byte[] in = new byte[] { 0x30, (byte) 0x82, 0x01, 0x00 };
		short afterTag = BERTLVScanner.readTag(in, (short) 0);
		short valueOff = BERTLVScanner.readLength(in, afterTag);
		assertEquals(4, (int) valueOff);
	}

	private short ioTag() {
		try {
			java.lang.reflect.Field f = BERTLVScanner.class.getDeclaredField("tag");
			f.setAccessible(true);
			return f.getShort(null);
		} catch (Exception e) { throw new IllegalStateException(e); }
	}
	private short ioTagClass() {
		try {
			java.lang.reflect.Field f = BERTLVScanner.class.getDeclaredField("tagClass");
			f.setAccessible(true);
			return f.getShort(null);
		} catch (Exception e) { throw new IllegalStateException(e); }
	}
}
