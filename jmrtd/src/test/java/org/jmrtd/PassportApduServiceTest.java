/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Tests for {@link PassportApduService} using a programmable fake card service.
 */

package org.jmrtd;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sf.scuba.smartcards.APDUEvent;
import net.sf.scuba.smartcards.APDUListener;
import net.sf.scuba.smartcards.CardService;
import net.sf.scuba.smartcards.CardServiceException;
import net.sf.scuba.smartcards.CommandAPDU;
import net.sf.scuba.smartcards.ISO7816;
import net.sf.scuba.smartcards.ResponseAPDU;

/**
 * Unit tests for {@link PassportApduService} backed by a programmable
 * fake {@link net.sf.scuba.smartcards.CardService} implementation.
 *
 * @author The JMRTD team (info@jmrtd.org)
 */
public class PassportApduServiceTest {

	private static final byte[] ATR = new byte[] { 0x3B, (byte) 0x8A, 0x01, 0x02 };
	private static final byte[] SW_NO_ERROR = new byte[] { (byte) 0x90, 0x00 };

	private FakeCardService cardService;
	private PassportApduService service;

	@BeforeEach
	public void setUp() throws CardServiceException {
		cardService = new FakeCardService();
		cardService.setATR(ATR);
		service = new PassportApduService(cardService);
	}

	/**
	 * A fully programmable fake {@link CardService}.
	 *
	 * <ul>
	 * <li>Responses can be scripted per INS code ({@link #on(int, byte[])} or
	 * {@link #onFunction(int, Function)}), where the byte array is the full
	 * response including the trailing status word.</li>
	 * <li>Alternatively, a queue of scripted responses
	 * ({@link #scriptResponse(byte[])}) is consumed in order, taking precedence
	 * over the INS map.</li>
	 * <li>Every transmitted command is recorded and counted, and registered
	 * APDU listeners are notified.</li>
	 * </ul>
	 */
	public static class FakeCardService extends CardService {

		private boolean isOpen;
		private byte[] atr;
		private boolean extendedAPDULengthSupported;
		private boolean connectionLost;

		private final Map<Integer, Function<CommandAPDU, byte[]>> responders;
		private final Deque<byte[]> scriptedResponses;
		private final List<CommandAPDU> transmittedCommands;

		public FakeCardService() {
			this.responders = new HashMap<Integer, Function<CommandAPDU, byte[]>>();
			this.scriptedResponses = new ArrayDeque<byte[]>();
			this.transmittedCommands = new ArrayList<CommandAPDU>();
			this.isOpen = false;
			this.atr = new byte[0];
			this.extendedAPDULengthSupported = false;
			this.connectionLost = false;
		}

		public void setATR(byte[] atr) {
			this.atr = atr;
		}

		public void setExtendedAPDULengthSupported(boolean extendedAPDULengthSupported) {
			this.extendedAPDULengthSupported = extendedAPDULengthSupported;
		}

		public void setConnectionLost(boolean connectionLost) {
			this.connectionLost = connectionLost;
		}

		/**
		 * Registers a static response (full bytes including SW) for commands
		 * with the given INS byte.
		 */
		public FakeCardService on(int ins, byte[] response) {
			responders.put(Integer.valueOf(ins & 0xFF), (capdu) -> response);
			return this;
		}

		/**
		 * Registers a dynamic responder for commands with the given INS byte.
		 * The function must return the full response bytes including SW.
		 */
		public FakeCardService onFunction(int ins, Function<CommandAPDU, byte[]> responder) {
			responders.put(Integer.valueOf(ins & 0xFF), responder);
			return this;
		}

		/**
		 * Enqueues a scripted response (full bytes including SW). Scripted
		 * responses are consumed in order and take precedence over the INS map.
		 */
		public FakeCardService scriptResponse(byte[] response) {
			scriptedResponses.add(response);
			return this;
		}

		public int getTransmitCount() {
			return transmittedCommands.size();
		}

		public List<CommandAPDU> getTransmittedCommands() {
			return transmittedCommands;
		}

		@Override
		public void open() throws CardServiceException {
			isOpen = true;
		}

		@Override
		public void close() {
			isOpen = false;
		}

		@Override
		public boolean isOpen() {
			return isOpen;
		}

		@Override
		public byte[] getATR() throws CardServiceException {
			if (!isOpen) {
				throw new CardServiceException("Not open");
			}
			return atr;
		}

		@Override
		public boolean isExtendedAPDULengthSupported() {
			return extendedAPDULengthSupported;
		}

		@Override
		public boolean isConnectionLost(Exception e) {
			return connectionLost;
		}

		@Override
		public ResponseAPDU transmit(CommandAPDU capdu) throws CardServiceException {
			if (!isOpen) {
				throw new CardServiceException("Not open");
			}
			transmittedCommands.add(capdu);
			byte[] responseBytes = null;
			if (!scriptedResponses.isEmpty()) {
				responseBytes = scriptedResponses.poll();
			} else {
				Function<CommandAPDU, byte[]> responder = responders.get(Integer.valueOf(capdu.getINS() & 0xFF));
				if (responder == null) {
					throw new CardServiceException("No responder for INS " + Integer.toHexString(capdu.getINS()));
				}
				responseBytes = responder.apply(capdu);
			}
			if (responseBytes == null || responseBytes.length < 2) {
				throw new CardServiceException("Fake produced invalid response");
			}
			ResponseAPDU rapdu = new ResponseAPDU(responseBytes);
			notifyExchangedAPDU(new APDUEvent(this, "FAKE", transmittedCommands.size(), capdu, rapdu));
			return rapdu;
		}
	}

	/* Tests below. */

	@Test
	public void testOpenIsOpenClose() throws CardServiceException {
		assertFalse(service.isOpen());
		service.open();
		assertTrue(cardService.isOpen());
		assertTrue(service.isOpen());
		service.close();
		assertFalse(cardService.isOpen());
		assertFalse(service.isOpen());
	}

	@Test
	public void testGetATR() throws CardServiceException {
		assertNull(service.getATR());
		service.open();
		assertArrayEquals(ATR, service.getATR());
	}

	@Test
	public void testIsConnectionLost() throws CardServiceException {
		cardService.setConnectionLost(false);
		assertFalse(service.isConnectionLost(new CardServiceException("test")));
		cardService.setConnectionLost(true);
		assertTrue(service.isConnectionLost(new CardServiceException("test")));
	}

	@Test
	public void testTransmitPassThrough() throws CardServiceException {
		byte[] expectedResponse = new byte[] { 0x01, 0x02, 0x03, (byte) 0x90, 0x00 };
		cardService.scriptResponse(expectedResponse);
		service.open();

		CommandAPDU capdu = new CommandAPDU(ISO7816.CLA_ISO7816, 0xCA, 0x00, 0x00, 3);
		ResponseAPDU rapdu = service.transmit(capdu);

		assertEquals(1, cardService.getTransmitCount());
		assertEquals(capdu, cardService.getTransmittedCommands().get(0));
		assertArrayEquals(expectedResponse, rapdu.getBytes());
	}

	@Test
	public void testAddAndRemoveAPDUListener() throws CardServiceException {
		service.open();
		cardService.scriptResponse(new byte[] { (byte) 0x90, 0x00 });

		final List<APDUEvent> events = new ArrayList<APDUEvent>();
		APDUListener listener = new APDUListener() {
			public void exchangedAPDU(APDUEvent event) {
				events.add(event);
			}
		};

		service.addAPDUListener(listener);
		service.transmit(new CommandAPDU(ISO7816.CLA_ISO7816, 0xCA, 0, 0, 0));
		assertEquals(1, events.size());
		APDUEvent event = events.get(0);
		assertNotNull(event.getCommandAPDU());
		assertNotNull(event.getResponseAPDU());
		assertEquals(0x9000, event.getResponseAPDU().getSW() & 0xFFFF);

		service.removeAPDUListener(listener);
		cardService.scriptResponse(new byte[] { (byte) 0x90, 0x00 });
		service.transmit(new CommandAPDU(ISO7816.CLA_ISO7816, 0xCA, 0, 0, 0));
		assertEquals(1, events.size());
	}

	@Test
	public void testSendSelectFile() throws CardServiceException {
		cardService.on(ISO7816.INS_SELECT_FILE, SW_NO_ERROR);
		service.open();

		service.sendSelectFile((short) 0x0101);

		assertEquals(1, cardService.getTransmitCount());
		CommandAPDU sent = cardService.getTransmittedCommands().get(0);
		assertEquals(ISO7816.INS_SELECT_FILE & 0xFF, sent.getINS());
		byte[] data = sent.getData();
		assertEquals(2, data.length);
		assertEquals(0x01, data[0]);
		assertEquals(0x01, data[1]);
	}

	@Test
	public void testSendSelectFileNotFound() throws CardServiceException {
		cardService.on(ISO7816.INS_SELECT_FILE, new byte[] { 0x6A, (byte) 0x82 });
		service.open();

		assertThrows(CardServiceException.class, () -> service.sendSelectFile((short) 0x0101));
	}

	@Test
	public void testSendReadBinary() throws CardServiceException {
		byte[] fileBytes = new byte[] { 0x61, 0x05, 0x5F, 0x01, 0x03, 0x01, 0x02, 0x03 };
		cardService.onFunction(ISO7816.INS_READ_BINARY, (capdu) -> {
			int offset = ((capdu.getP1() & 0xFF) << 8) | (capdu.getP2() & 0xFF);
			int le = capdu.getNe();
			int length = Math.min(le, fileBytes.length - offset);
			byte[] response = new byte[length + 2];
			System.arraycopy(fileBytes, offset, response, 0, length);
			response[length] = (byte) 0x90;
			response[length + 1] = 0x00;
			return response;
		});
		service.open();

		byte[] result = service.sendReadBinary((short) 0, 8, false);
		assertArrayEquals(fileBytes, result);
	}

	@Test
	public void testSendGetChallenge() throws CardServiceException {
		byte[] challenge = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		cardService.on(ISO7816.INS_GET_CHALLENGE,
				new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, (byte) 0x90, 0x00 });
		service.open();

		byte[] result = service.sendGetChallenge();
		assertArrayEquals(challenge, result);
	}

	@Test
	public void testSendMutualAuthenticate() throws CardServiceException {
		cardService.on(ISO7816.INS_EXTERNAL_AUTHENTICATE, SW_NO_ERROR);
		service.open();

		service.sendMutualAuthenticate(null, new byte[] { 0x01, 0x02, 0x03, 0x04 });

		assertEquals(1, cardService.getTransmitCount());
		assertEquals(ISO7816.INS_EXTERNAL_AUTHENTICATE & 0xFF, cardService.getTransmittedCommands().get(0).getINS());
	}

	@Test
	public void testSendMutualAuthenticateErrorStatusWord() throws CardServiceException {
		cardService.on(ISO7816.INS_EXTERNAL_AUTHENTICATE, new byte[] { 0x69, (byte) 0x82 });
		service.open();

		assertThrows(CardServiceException.class,
				() -> service.sendMutualAuthenticate(null, new byte[] { 0x01 }));
	}

	@Test
	public void testSendMSESetATMutualAuthRejectsBadKeyReference() {
		assertThrows(IllegalArgumentException.class,
				() -> service.sendMSESetATMutualAuth(null, "0.4.0.127.0.7.2.2.4.2.2", 0x7F, null));
	}
}
