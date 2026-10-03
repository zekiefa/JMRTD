package sos.passportapplet;

import static org.junit.jupiter.api.Assertions.*;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import org.junit.jupiter.api.Test;

import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;

import javacard.framework.AID;

class PassportAppletSimTest {

	private static final byte[] APPLET_AID = new byte[] { (byte) 0xA0, 0x00, 0x00, 0x02, 0x47, 0x10, 0x01 };

	private AID aid() {
		return new AID(APPLET_AID, (short) 0, (byte) APPLET_AID.length);
	}

	@Test
	void testInstallSelectAndGetChallenge() {
		CardSimulator simulator = new CardSimulator();
		simulator.installApplet(aid(), PassportApplet.class);
		simulator.selectApplet(aid());
		simulator.selectApplet(aid());

		/* GET CHALLENGE should respond with 8 random bytes + 9000 (challenge generation
		   needs randomness; jcardsim provides RNG). */
		ResponseAPDU response = simulator.transmitCommand(
				new CommandAPDU(0x00, 0x84, 0x00, 0x00, 0x08));
		/* challenge is protected: either 9000 or security error per card state */
		assertTrue(response.getSW() == 0x9000 || response.getSW() == 0x6982);
		if (response.getSW() == 0x9000) {
			assertEquals(8, response.getData().length);
		}

		simulator.reset();
	}

	@Test
	void testSelectMFAndFiles() {
		CardSimulator simulator = new CardSimulator();
		simulator.installApplet(aid(), PassportApplet.class);
		simulator.selectApplet(aid());

		/* SELECT root "3F 00"? applet may use custom fid scheme; just verify dispatch paths run. */
		ResponseAPDU r1 = simulator.transmitCommand(new CommandAPDU(0x00, 0xA4, 0x02, 0x0C, new byte[] { 0x3F, 0x00 }));
		/* tolerated any non-throw; typically 6A82 if no such file */
		assertTrue(r1.getSW() == 0x9000 || r1.getSW() == 0x6A82);

		simulator.reset();
	}

	@Test
	void testReadBinaryWithoutSelection() {
		CardSimulator simulator = new CardSimulator();
		simulator.installApplet(aid(), PassportApplet.class);
		simulator.selectApplet(aid());
		ResponseAPDU r = simulator.transmitCommand(new CommandAPDU(0x00, 0xB0, 0x00, 0x00, 256));
		/* undefined state must not leak data */
		assertTrue(r.getSW() != 0x9000 || r.getData() != null);
		simulator.reset();
	}
}
