package org.jmrtd;

import static org.junit.jupiter.api.Assertions.*;

import net.sf.scuba.smartcards.CardServiceException;
import net.sf.scuba.smartcards.ISO7816;

import org.junit.jupiter.api.Test;

class PassportServiceAATest {

	private static byte[] swOK() {
		return new byte[] { (byte) 0x90, 0x00 };
	}

	@Test
	void testDoAAChallengeRules() throws Exception {
		PassportApduServiceTest.FakeCardService cardService = new PassportApduServiceTest.FakeCardService();
		cardService.on(ISO7816.INS_INTERNAL_AUTHENTICATE,
				new byte[] { (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0x90, 0x00 });
		cardService.open();
		PassportService service = new PassportService(cardService);
		service.open();

		byte[] challenge = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		byte[] resp = service.doAA(null, "SHA-1", "SHA1withRSA/ISO9796-2", challenge);
		assertNotNull(resp);

		assertThrows(CardServiceException.class,
				() -> service.doAA(null, "SHA-1", "SHA1withRSA/ISO9796-2", new byte[] { 1, 2 }));

		service.close();
		assertFalse(service.isOpen());
	}
}
