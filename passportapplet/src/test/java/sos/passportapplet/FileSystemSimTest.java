package sos.passportapplet;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.licel.jcardsim.smartcardio.CardSimulator;

import javacard.framework.AID;
import javacard.framework.ISOException;

class FileSystemSimTest {

	@BeforeAll
	static void bootCardRuntime() {
		/* any jcardsim instance boots the JavaCard JCSystem runtime needed by Util.arrayCopy */
		new CardSimulator();
	}

	@Test
	void testCreateWriteReadRoundTrip() {
		FileSystem fs = new FileSystem();
		fs.createFile((short) 0x011E, (short) 16);
		byte[] data = { 1, 2, 3, 4 };
		fs.writeData((short) 0x011E, (short) 0, data, (short) 0, (short) 4);
		byte[] file = fs.getFile((short) 0x011E);
		assertNotNull(file);
		assertEquals(16, fs.getFileSize((short) 0x011E));
		assertEquals(4, file[3]);
	}

	@Test
	void testCreateTwiceKeepsAllocation() {
		FileSystem fs = new FileSystem();
		fs.createFile((short) 0x0101, (short) 8);
		byte[] pre = fs.getFile((short) 0x0101);
		fs.createFile((short) 0x0101, (short) 8);
		assertSame(pre, fs.getFile((short) 0x0101));
	}

	@Test
	void testReadMissingReturnsNull() {
		FileSystem fs = new FileSystem();
		assertNull(fs.getFile((short) 0x011C));
		assertTrue(fs.getFileSize((short) 0x011C) <= 0);
	}

	@Test
	void testWriteOverflowThrowsFileFull() {
		FileSystem fs = new FileSystem();
		fs.createFile((short) 0x0102, (short) 2);
		try {
			fs.writeData((short) 0x0102, (short) 0, new byte[] { 1, 2, 3 }, (short) 0, (short) 3);
			fail("expected file full");
		} catch (ISOException e) {
			assertEquals(javacard.framework.ISO7816.SW_FILE_FULL, e.getReason());
		}
	}

	@Test
	void testATRConstants() {
		assertArrayEquals(new byte[] { 0x4A, 0x4D, 0x52, 0x54, 0x44 }, ATRGlobal.ATR_HIST);
		assertEquals(5, ATRGlobal.ATR_HIST_LEN);
	}
}
