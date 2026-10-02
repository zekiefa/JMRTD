/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Tests for {@link PassportService} (open/close, select applet, BAC) using
 * the programmable fake card service from {@link PassportApduServiceTest}.
 */

package org.jmrtd;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sf.scuba.smartcards.CardServiceException;
import net.sf.scuba.smartcards.CommandAPDU;
import net.sf.scuba.smartcards.ISO7816;

/**
 * Unit tests for {@link PassportService} against a fake card.
 *
 * @author The JMRTD team (info@jmrtd.org)
 */
public class PassportServiceTest {

	private static final String DOC_NUMBER = "L898902C";
	private static final String DOC_NUMBER_FIXED = "L898902C<";
	private static final String DATE_OF_BIRTH = "740812";
	private static final String DATE_OF_EXPIRY = "120415";

	private static final byte[] SW_NO_ERROR = new byte[] { (byte) 0x90, 0x00 };

	private PassportApduServiceTest.FakeCardService cardService;
	private PassportService service;
	private BACKeySpec bacKey;

	@BeforeEach
	public void setUp() throws CardServiceException {
		cardService = new PassportApduServiceTest.FakeCardService();
		cardService.setATR(new byte[] { 0x3B, 0x01 });
		service = new PassportService(cardService);
		bacKey = new BACKey(DOC_NUMBER, DATE_OF_BIRTH, DATE_OF_EXPIRY);
	}

	@Test
	public void testOpenCloseIsOpen() throws CardServiceException {
		assertFalse(service.isOpen());
		service.open();
		assertTrue(service.isOpen());
		assertTrue(cardService.isOpen());
		service.close();
		assertFalse(service.isOpen());
		assertFalse(cardService.isOpen());
	}

	@Test
	public void testSelectAppletWithoutPACE() throws CardServiceException {
		cardService.on(ISO7816.INS_SELECT_FILE, SW_NO_ERROR);
		service.open();

		service.sendSelectApplet(false);

		assertEquals(1, cardService.getTransmitCount());
		CommandAPDU sent = cardService.getTransmittedCommands().get(0);
		assertEquals(ISO7816.INS_SELECT_FILE & 0xFF, sent.getINS());
		assertEquals(0x04, sent.getP1());
		assertArrayEquals(PassportApduService.APPLET_AID, sent.getData());
	}

	@Test
	public void testSelectAppletErrorStatusWord() throws CardServiceException {
		cardService.on(ISO7816.INS_SELECT_FILE, new byte[] { 0x6A, (byte) 0x82 });
		service.open();

		assertThrows(CardServiceException.class, () -> service.sendSelectApplet(false));
	}

	@Test
	public void testGetWrapperNullBeforeBAC() {
		assertNull(service.getWrapper());
	}

	/**
	 * Full fake-card BAC: GET CHALLENGE returns a fixed rndICC; EXTERNAL
	 * AUTHENTICATE decrypts the terminal cryptogram with kEnc, and answers with
	 * E(kEnc, rndICC || rndIFD || kICC) || MAC(kMac) so that the service
	 * computation proceeds exactly as with a real passport.
	 */
	@Test
	public void testDoBAC() throws Exception {
		final byte[] rndICC = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };
		final byte[] kICC = new byte[16];
		for (int i = 0; i < kICC.length; i++) {
			kICC[i] = (byte) (0xA0 + i);
		}

		final byte[] keySeed = Util.computeKeySeedForBAC(DOC_NUMBER_FIXED, DATE_OF_BIRTH, DATE_OF_EXPIRY);
		final SecretKey kEnc = Util.deriveKey(keySeed, Util.ENC_MODE);
		final SecretKey kMac = Util.deriveKey(keySeed, Util.MAC_MODE);
		final Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
		final Mac mac = Mac.getInstance("ISO9797Alg3Mac", JMRTDSecurityProvider.getBouncyCastleProvider());
		final IvParameterSpec zeroIV = new IvParameterSpec(new byte[8]);

		cardService.on(ISO7816.INS_SELECT_FILE, SW_NO_ERROR);
		cardService.on(ISO7816.INS_GET_CHALLENGE, withSW(rndICC));
		cardService.onFunction(ISO7816.INS_EXTERNAL_AUTHENTICATE, (capdu) -> {
			try {
				byte[] data = capdu.getData();
				assertEquals(40, data.length);

				/* Decrypt the terminal cryptogram to recover rndIFD. */
				cipher.init(Cipher.DECRYPT_MODE, kEnc, zeroIV);
				byte[] plaintext = cipher.doFinal(data, 0, 32);
				byte[] rndIFD = Arrays.copyOfRange(plaintext, 0, 8);

				/* Build response cryptogram: rndICC || rndIFD || kICC. */
				byte[] cardPlaintext = new byte[32];
				System.arraycopy(rndICC, 0, cardPlaintext, 0, 8);
				System.arraycopy(rndIFD, 0, cardPlaintext, 8, 8);
				System.arraycopy(kICC, 0, cardPlaintext, 16, 16);
				cipher.init(Cipher.ENCRYPT_MODE, kEnc, zeroIV);
				byte[] cardCiphertext = cipher.doFinal(cardPlaintext);

				mac.init(kMac);
				byte[] mICC = mac.doFinal(Util.pad(cardCiphertext));

				byte[] response = new byte[32 + 8 + 2];
				System.arraycopy(cardCiphertext, 0, response, 0, 32);
				System.arraycopy(mICC, 0, response, 32, 8);
				response[40] = (byte) 0x90;
				response[41] = 0x00;
				return response;
			} catch (GeneralSecurityException gse) {
				throw new IllegalStateException(gse);
			}
		});

		service.open();
		service.sendSelectApplet(false);
		service.doBAC(bacKey);

		assertNotNull(service.getWrapper());
		assertTrue(service.getWrapper() instanceof DESedeSecureMessagingWrapper);
	}

	@Test
	public void testDoBACFailsWhenChallengeFails() throws CardServiceException {
		cardService.on(ISO7816.INS_SELECT_FILE, SW_NO_ERROR);
		cardService.on(ISO7816.INS_GET_CHALLENGE, new byte[] { 0x6A, (byte) 0x88 });
		service.open();
		service.sendSelectApplet(false);

		assertThrows(CardServiceException.class, () -> service.doBAC(bacKey));
		assertNull(service.getWrapper());
	}

	private static byte[] withSW(byte[] data) {
		byte[] result = new byte[data.length + 2];
		System.arraycopy(data, 0, result, 0, data.length);
		result[data.length] = (byte) 0x90;
		result[data.length + 1] = 0x00;
		return result;
	}
}
