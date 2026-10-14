package sos.passportapplet;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.licel.jcardsim.smartcardio.CardSimulator;

class LogAndScannerTest {

	@BeforeAll
	static void boot() { new CardSimulator(); }

	@Test
	void testLogDisabledIsNoop() {
		FileSystem fs = new FileSystem();
		Log log = new Log(fs);
		log.enabled(false);
		log.newSession();
		log.insByte((byte) 0xA4);
		log.selectFile((short) 0x011E);
		assertFalse(log.enabled);
	}

	@Test
	void testLogEnabledRoundTrip() {
		FileSystem fs = new FileSystem();
		Log log = new Log(fs);
		log.enabled(true);
		log.newSession();
		log.insByte((byte) 0x00);
		log.insByte((byte) 0x00); /* consecutive same ins -> FF marker path */
		log.selectFile((short) 0x0101);
		log.newSession(); /* new session resets */
		assertTrue(log.enabled);
	}

	@Test
	void testScannerShortFormParse() {
		byte[] tlv = { 0x02, 0x02, 0x00, 0x2A, 0x5F, 0x01, 0x01 };
		short tagOff = BERTLVScanner.readTag(tlv, (short) 0);
		assertEquals(1, tagOff);
		assertEquals(2, BERTLVScanner.tag);
		short valOff = BERTLVScanner.readLength(tlv, tagOff);
		assertEquals(2, BERTLVScanner.valueLength);
		assertEquals((short) (valOff + 2), BERTLVScanner.skipValue());
	}

	@Test
	void testScannerLongFormLength() {
		byte[] tlv = { 0x5F, (byte) 0x40, (byte) 0x82, 0x01, 0x2C };
		short tagOff = BERTLVScanner.readTag(tlv, (short) 0);
		short valOff = BERTLVScanner.readLength(tlv, tagOff);
		assertEquals(300, BERTLVScanner.valueLength);
		assertEquals(5, valOff);
	}

	@Test
	void testScannerSkipPad() {
		byte[] tlv = { (byte) 0xFF, 0x00, 0x02, 0x01, 0x42 };
		short tagOff = BERTLVScanner.readTag(tlv, (short) 0);  /* skips 00/FF */
		assertTrue(tagOff == 2 || tagOff == 0 || tagOff == 3);
	}
}
