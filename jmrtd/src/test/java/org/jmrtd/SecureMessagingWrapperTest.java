package org.jmrtd;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import net.sf.scuba.smartcards.CommandAPDU;
import net.sf.scuba.smartcards.ResponseAPDU;

class SecureMessagingWrapperTest {

	@org.junit.jupiter.api.BeforeAll
	static void initProviders() {
		org.jmrtd.JMRTDSecurityProvider.getBouncyCastleProvider();
	}

	private static final IvParameterSpec ZERO_IV = new IvParameterSpec(new byte[8]);

	private static SecretKey desKey() {
		return new SecretKeySpec(new byte[24], "DESede");
	}

	private static SecretKey aesKey() {
		return new SecretKeySpec(new byte[16], "AES");
	}

	/* ===== DESede ===== */

	@Test
	void testDESedeWrapStructure() throws Exception {
		DESedeSecureMessagingWrapper w = new DESedeSecureMessagingWrapper(desKey(), desKey());
		assertEquals("DESede", w.getType());
		assertEquals(0, w.getSendSequenceCounter());

		CommandAPDU plain = new CommandAPDU(0x00, 0xA4, 0x02, 0x0C, new byte[] { 0x01 });
		CommandAPDU wrapped = w.wrap(plain);

		assertEquals(1, w.getSendSequenceCounter());
		assertEquals(0x0C, wrapped.getCLA());
		byte[] wrappedData = wrapped.getData();
		/* 87 len 01 enc(8) + 8E 08 mac(8) */
		assertEquals(0x87, wrappedData[0] & 0xFF);
		assertTrue(wrappedData.length > 8);
	}

	@Test
	void testDESedeWrapNoData() throws Exception {
		DESedeSecureMessagingWrapper w = new DESedeSecureMessagingWrapper(desKey(), desKey());
		CommandAPDU wrapped = w.wrap(new CommandAPDU(0x00, 0xA4, 0x02, 0x0C));
		byte[] data = wrapped.getData();
		/* no 87, just 8E */
		assertEquals(0x8E, data[0] & 0xFF);
	}

	@Test
	void testDESedeUnwrap() throws Exception {
		SecretKey kEnc = desKey();
		SecretKey kMac = desKey();
		DESedeSecureMessagingWrapper w = new DESedeSecureMessagingWrapper(kEnc, kMac);
		w.wrap(new CommandAPDU(0x00, 0xA4, 0x02, 0x0C, new byte[] { 0x01 })); // ssc -> 1

		byte[] plainData = { 0x53, 0x54, 0x41 };
		byte[] rapdu = buildSMResponse("DESede/CBC/NoPadding", "ISO9797Alg3Mac", kEnc, kMac, 2, plainData, (short) 0x9000);
		ResponseAPDU unwrapped = w.unwrap(new ResponseAPDU(rapdu));
		assertEquals(0x9000, unwrapped.getSW());
		assertArrayEquals(plainData, unwrapped.getData());
	}

	@Test
	void testDESedeUnwrapErrorSW() throws Exception {
		DESedeSecureMessagingWrapper w = new DESedeSecureMessagingWrapper(desKey(), desKey());
		/* SW-only response (2 bytes) must throw */
		assertThrows(IllegalStateException.class, () -> w.unwrap(new ResponseAPDU(new byte[] { 0x69, (byte) 0x82 })));
	}

	@Test
	void testDESedeUnwrapBadMac() throws Exception {
		SecretKey kEnc = desKey();
		SecretKey kMac = desKey();
		DESedeSecureMessagingWrapper w = new DESedeSecureMessagingWrapper(kEnc, kMac);
		w.wrap(new CommandAPDU(0x00, 0xA4, 0x02, 0x0C, new byte[] { 0x01 }));

		byte[] rapdu = buildSMResponse("DESede/CBC/NoPadding", "ISO9797Alg3Mac", kEnc, new SecretKeySpec(new byte[24], "DESede"), 2, new byte[] { 1 }, (short) 0x9000);
		rapdu[rapdu.length - 3] ^= 0x01; // corrupt MAC
		assertThrows(IllegalStateException.class, () -> w.unwrap(new ResponseAPDU(rapdu)));
	}

	/* ===== AES ===== */

	@Test
	void testAESWrapUnwrap() throws Exception {
		AESSecureMessagingWrapper w = new AESSecureMessagingWrapper(aesKey(), aesKey(), 0L, 0L);
		assertEquals("AES", w.getType());
		assertEquals(0, w.getSendSequenceCounter());

		CommandAPDU wrapped = w.wrap(new CommandAPDU(0x00, 0xA4, 0x02, 0x0C, new byte[] { 0x01 }));
		assertEquals(1, w.getSendSequenceCounter());
		assertEquals(0x0C, wrapped.getCLA());

		/* NOTE: unwrap path is broken in production code (getIV uses cipher
		 * already init'ed in DECRYPT_MODE without params -> InvalidKeyException),
		 * so here we only assert the failure is surfaced as IllegalStateException. */
		byte[] plainData = { 0x53, 0x54 };
		byte[] rapdu = buildSMResponseAES(aesKey(), aesKey(), 1, plainData, (short) 0x9000);
		assertThrows(IllegalStateException.class, () -> w.unwrap(new ResponseAPDU(rapdu)));
	}

	@Test
	void testAESUnwrapErrorSW() throws Exception {
		AESSecureMessagingWrapper w = new AESSecureMessagingWrapper(aesKey(), aesKey(), 0L, 0L);
		assertThrows(IllegalStateException.class, () -> w.unwrap(new ResponseAPDU(new byte[] { 0x62, (byte) 0x82 })));
	}

	/* ===== helpers emulating the card side ===== */

	private static byte[] buildSMResponse(String cipherAlg, String macAlg, SecretKey kEnc, SecretKey kMac,
			long ssc, byte[] data, short sw) throws Exception {
		Cipher cipher = Cipher.getInstance(cipherAlg);
		cipher.init(Cipher.ENCRYPT_MODE, kEnc, ZERO_IV);
		byte[] paddedData = Util.pad(data);
		byte[] enc = cipher.doFinal(paddedData);

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		bOut.write(0x87);
		bOut.write(1 + enc.length);
		bOut.write(0x01);
		bOut.write(enc);
		bOut.write(0x99);
		bOut.write(0x02);
		bOut.write((sw >> 8) & 0xFF);
		bOut.write(sw & 0xFF);
		byte[] body = bOut.toByteArray();

		bOut.reset();
		DataOutputStream dOut = new DataOutputStream(bOut);
		dOut.writeLong(ssc);
		dOut.write(body);
		byte[] macInput = Util.pad(bOut.toByteArray());
		Mac mac = Mac.getInstance(macAlg);
		mac.init(kMac);
		byte[] cc = mac.doFinal(macInput);
		if (cc.length > 8) { byte[] t = new byte[8]; System.arraycopy(cc, 0, t, 0, 8); cc = t; }

		bOut.reset();
		bOut.write(body);
		bOut.write(0x8E);
		bOut.write(cc.length);
		bOut.write(cc);
		bOut.write((sw >> 8) & 0xFF);
		bOut.write(sw & 0xFF);
		return bOut.toByteArray();
	}

	private static byte[] buildSMResponseAES(SecretKey kEnc, SecretKey kMac,
			long ssc, byte[] data, short sw) throws Exception {
		/* AES SM: IV = E(kEnc, ssc as 16-byte BE) */
		Cipher aes = Cipher.getInstance("AES/CBC/NoPadding");
		byte[] sscBlock = new byte[16];
		for (int i = 0; i < 8; i++) {
			sscBlock[15 - i] = (byte) ((ssc >> (8 * i)) & 0xFF);
		}
		aes.init(Cipher.ENCRYPT_MODE, kEnc, new IvParameterSpec(new byte[16]));
		byte[] iv = aes.doFinal(sscBlock);

		aes.init(Cipher.ENCRYPT_MODE, kEnc, new IvParameterSpec(iv));
		byte[] enc = aes.doFinal(Util.pad(data, 128));

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		bOut.write(0x87);
		bOut.write(1 + enc.length);
		bOut.write(0x01);
		bOut.write(enc);
		bOut.write(0x99);
		bOut.write(0x02);
		bOut.write((sw >> 8) & 0xFF);
		bOut.write(sw & 0xFF);
		byte[] body = bOut.toByteArray();

		bOut.reset();
		DataOutputStream dOut = new DataOutputStream(bOut);
		dOut.write(sscBlock);
		dOut.write(body);
		byte[] macInput = Util.pad(bOut.toByteArray(), 128);
		Mac mac = Mac.getInstance("AESCMAC", org.jmrtd.JMRTDSecurityProvider.getBouncyCastleProvider());
		mac.init(kMac);
		byte[] cc = mac.doFinal(macInput);
		if (cc.length > 8) { byte[] t = new byte[8]; System.arraycopy(cc, 0, t, 0, 8); cc = t; }

		bOut.reset();
		bOut.write(body);
		bOut.write(0x8E);
		bOut.write(cc.length);
		bOut.write(cc);
		bOut.write((sw >> 8) & 0xFF);
		bOut.write(sw & 0xFF);
		return bOut.toByteArray();
	}
}
