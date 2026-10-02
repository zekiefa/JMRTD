package org.jmrtd.cbeff;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.List;

import org.jmrtd.cbeff.CBEFFTestUtil.TestBiometricDataBlock;
import org.jmrtd.cbeff.CBEFFTestUtil.TestBiometricDataBlockDecoder;
import org.jmrtd.cbeff.CBEFFTestUtil.TestBiometricDataBlockEncoder;
import org.junit.jupiter.api.Test;

import net.sf.scuba.tlv.TLVInputStream;
import net.sf.scuba.tlv.TLVOutputStream;

public class ISO781611EncoderDecoderTest {

	private static ISO781611Encoder<TestBiometricDataBlock> newEncoder() {
		return new ISO781611Encoder<TestBiometricDataBlock>(new TestBiometricDataBlockEncoder());
	}

	private static ISO781611Decoder newDecoder() {
		return new ISO781611Decoder(new TestBiometricDataBlockDecoder());
	}

	@Test
	public void testEncodeDecodeSimpleRoundTrip() throws Exception {
		TestBiometricDataBlock bdb = new TestBiometricDataBlock(CBEFFTestUtil.createSBH(), new byte[] { 1, 2, 3, 4, 5 });
		SimpleCBEFFInfo<TestBiometricDataBlock> info = new SimpleCBEFFInfo<TestBiometricDataBlock>(bdb);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		newEncoder().encode(info, out);

		ComplexCBEFFInfo decoded = newDecoder().decode(new ByteArrayInputStream(out.toByteArray()));
		assertNotNull(decoded);
		assertEquals(1, decoded.getSubRecords().size());

		CBEFFInfo subInfo = decoded.getSubRecords().get(0);
		assertTrue(subInfo instanceof SimpleCBEFFInfo);
		TestBiometricDataBlock decodedBDB = ((SimpleCBEFFInfo<TestBiometricDataBlock>) subInfo).getBiometricDataBlock();
		assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, decodedBDB.getData());
		CBEFFTestUtil.assertSBHEquals(CBEFFTestUtil.createSBH(), decodedBDB.getStandardBiometricHeader());
	}

	@Test
	public void testEncodeDecodeComplexRoundTrip() throws Exception {
		ComplexCBEFFInfo input = new ComplexCBEFFInfo();
		input.add(new SimpleCBEFFInfo<TestBiometricDataBlock>(
				new TestBiometricDataBlock(CBEFFTestUtil.createSBH(), new byte[] { 1, 2, 3 })));
		input.add(new SimpleCBEFFInfo<TestBiometricDataBlock>(
				new TestBiometricDataBlock(CBEFFTestUtil.createSBH(), new byte[] { 4, 5 })));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		newEncoder().encode(input, out);

		ComplexCBEFFInfo decoded = newDecoder().decode(new ByteArrayInputStream(out.toByteArray()));
		assertNotNull(decoded);
		assertEquals(2, decoded.getSubRecords().size());
		TestBiometricDataBlock bdb1 = ((SimpleCBEFFInfo<TestBiometricDataBlock>) decoded.getSubRecords().get(0)).getBiometricDataBlock();
		TestBiometricDataBlock bdb2 = ((SimpleCBEFFInfo<TestBiometricDataBlock>) decoded.getSubRecords().get(1)).getBiometricDataBlock();
		assertArrayEquals(new byte[] { 1, 2, 3 }, bdb1.getData());
		assertArrayEquals(new byte[] { 4, 5 }, bdb2.getData());
		CBEFFTestUtil.assertSBHEquals(CBEFFTestUtil.createSBH(), bdb1.getStandardBiometricHeader());
		CBEFFTestUtil.assertSBHEquals(CBEFFTestUtil.createSBH(), bdb2.getStandardBiometricHeader());
	}

	@Test
	public void testEncodeDecodeWithTLVStreams() throws Exception {
		/* Covers the instanceof branches with already-wrapped TLV streams. */
		TestBiometricDataBlock bdb = new TestBiometricDataBlock(CBEFFTestUtil.createSBH(), new byte[] { 9, 8, 7 });
		SimpleCBEFFInfo<TestBiometricDataBlock> info = new SimpleCBEFFInfo<TestBiometricDataBlock>(bdb);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(out);
		newEncoder().encode(info, tlvOut);
		tlvOut.flush();

		TLVInputStream tlvIn = new TLVInputStream(new ByteArrayInputStream(out.toByteArray()));
		ComplexCBEFFInfo decoded = newDecoder().decode(tlvIn);
		assertEquals(1, decoded.getSubRecords().size());
		TestBiometricDataBlock decodedBDB = ((SimpleCBEFFInfo<TestBiometricDataBlock>) decoded.getSubRecords().get(0)).getBiometricDataBlock();
		assertArrayEquals(new byte[] { 9, 8, 7 }, decodedBDB.getData());
	}

	@Test
	public void testEncodeUnsupportedInfoWritesNothing() throws Exception {
		CBEFFInfo unsupported = new CBEFFInfo() { };
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		newEncoder().encode(unsupported, out);
		assertEquals(0, out.toByteArray().length);
	}

	@Test
	public void testDecodeWrongGroupTagThrows() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(out);
		tlvOut.writeTag(0x7F62); /* Wrong group tag. */
		tlvOut.writeValue(new byte[] { 0x02, 0x01, 0x00 });
		byte[] bytes = out.toByteArray();

		assertThrows(IllegalArgumentException.class,
				() -> newDecoder().decode(new ByteArrayInputStream(bytes)));
	}

	@Test
	public void testDecodeWrongBITCountTagThrows() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(out);
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG);
		tlvOut.writeTag(0x03); /* Wrong BIT count tag (expected 0x02). */
		tlvOut.writeValue(new byte[] { 0x00 });
		tlvOut.writeValueEnd();
		byte[] bytes = out.toByteArray();

		assertThrows(IllegalArgumentException.class,
				() -> newDecoder().decode(new ByteArrayInputStream(bytes)));
	}

	@Test
	public void testDecodeBITCountWrongLengthThrows() throws Exception {
		byte[] bytes = new byte[] {
				0x7F, 0x61, 0x04,   // group tag, length 4
				0x02, 0x02, 0x00, 0x01 // count tag with length 2 (invalid)
		};
		assertThrows(IllegalArgumentException.class,
				() -> newDecoder().decode(new ByteArrayInputStream(bytes)));
	}

	@Test
	public void testDecodeWrongBITTagThrows() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(out);
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG);
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFO_COUNT_TAG);
		tlvOut.writeValue(new byte[] { 0x01 });
		tlvOut.writeTag(0x7F62); /* Wrong BIT tag (expected 7F60). */
		tlvOut.writeValue(new byte[] { 0x00 });
		tlvOut.writeValueEnd();
		byte[] bytes = out.toByteArray();

		assertThrows(IllegalArgumentException.class,
				() -> newDecoder().decode(new ByteArrayInputStream(bytes)));
	}

	@Test
	public void testDecodeUnsupportedBHTTagThrows() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(out);
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG);
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFO_COUNT_TAG);
		tlvOut.writeValue(new byte[] { 0x01 });
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFORMATION_TEMPLATE_TAG);
		tlvOut.writeTag(0x30); /* Not A0-range, not SMT. */
		tlvOut.writeValue(new byte[] { 0x00 });
		tlvOut.writeValueEnd();
		tlvOut.writeValueEnd();
		byte[] bytes = out.toByteArray();

		assertThrows(IllegalArgumentException.class,
				() -> newDecoder().decode(new ByteArrayInputStream(bytes)));
	}

	@Test
	public void testDecodeAlternativeBHTTagLogsWarning() throws Exception {
		/* BHT tag A2 (in the A0 range but not the base A1) triggers a warning but parses. */
		ByteArrayOutputStream bitValue = new ByteArrayOutputStream();
		TLVOutputStream bitOut = new TLVOutputStream(bitValue);
		bitOut.writeTag(0xA2);
		bitOut.writeValue(new byte[0]);
		bitOut.writeTag(ISO781611.BIOMETRIC_DATA_BLOCK_TAG);
		bitOut.writeValue(new byte[] { 0x0A, 0x0B });

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(out);
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG);
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFO_COUNT_TAG);
		tlvOut.writeValue(new byte[] { 0x01 });
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFORMATION_TEMPLATE_TAG);
		tlvOut.writeValue(bitValue.toByteArray());
		tlvOut.writeValueEnd();

		ComplexCBEFFInfo decoded = newDecoder().decode(new ByteArrayInputStream(out.toByteArray()));
		assertEquals(1, decoded.getSubRecords().size());
		TestBiometricDataBlock bdb = ((SimpleCBEFFInfo<TestBiometricDataBlock>) decoded.getSubRecords().get(0)).getBiometricDataBlock();
		assertArrayEquals(new byte[] { 0x0A, 0x0B }, bdb.getData());
		assertTrue(bdb.getStandardBiometricHeader().getElements().isEmpty());
	}

	@Test
	public void testDecodeWrongBDBTagThrows() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(out);
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG);
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFO_COUNT_TAG);
		tlvOut.writeValue(new byte[] { 0x01 });
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFORMATION_TEMPLATE_TAG);
		tlvOut.writeTag(ISO781611.BIOMETRIC_HEADER_TEMPLATE_BASE_TAG);
		tlvOut.writeValue(new byte[0]);
		tlvOut.writeTag(0x4200); /* Not a valid BDB tag. */
		tlvOut.writeValue(new byte[] { 0x00 });
		tlvOut.writeValueEnd();
		tlvOut.writeValueEnd();
		byte[] bytes = out.toByteArray();

		assertThrows(IllegalArgumentException.class,
				() -> newDecoder().decode(new ByteArrayInputStream(bytes)));
	}

	@Test
	public void testDecodeStaticallyProtectedBITPlainValue() throws Exception {
		/* SMT with plain value DOs (0x81): BHT and BDB packed as plain values. */
		ByteArrayOutputStream bhtOut = new ByteArrayOutputStream();
		TLVOutputStream bhtTlv = new TLVOutputStream(bhtOut);
		bhtTlv.writeTag(ISO781611.BIOMETRIC_HEADER_TEMPLATE_BASE_TAG);
		bhtTlv.writeTag(ISO781611.FORMAT_OWNER_TAG);
		bhtTlv.writeValue(new byte[] { 0x01, 0x01 });
		bhtTlv.writeValueEnd();

		ByteArrayOutputStream bdbOut = new ByteArrayOutputStream();
		TLVOutputStream bdbTlv = new TLVOutputStream(bdbOut);
		bdbTlv.writeTag(ISO781611.BIOMETRIC_DATA_BLOCK_TAG);
		bdbTlv.writeValue(new byte[] { 0x55, 0x66 });

		ByteArrayOutputStream smtOut = new ByteArrayOutputStream();
		TLVOutputStream smtTlv = new TLVOutputStream(smtOut);
		smtTlv.writeTag(ISO781611.SMT_DO_PV);
		smtTlv.writeValue(bhtOut.toByteArray());
		smtTlv.writeTag(ISO781611.SMT_DO_PV);
		smtTlv.writeValue(bdbOut.toByteArray());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(out);
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG);
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFO_COUNT_TAG);
		tlvOut.writeValue(new byte[] { 0x01 });
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFORMATION_TEMPLATE_TAG);
		tlvOut.writeTag(ISO781611.SMT_TAG);
		tlvOut.writeValue(smtOut.toByteArray());
		tlvOut.writeValueEnd();
		tlvOut.writeValueEnd();

		ComplexCBEFFInfo decoded = newDecoder().decode(new ByteArrayInputStream(out.toByteArray()));
		/* The SMT-protected BIT currently yields a null sub-record (FIXME in decoder). */
		assertEquals(1, decoded.getSubRecords().size());
		assertNull(decoded.getSubRecords().get(0));
	}

	@Test
	public void testDecodeStaticallyProtectedBITEncryptedThrows() throws Exception {
		byte[] bytes = bytesForSMTDO(ISO781611.SMT_DO_CG);
		assertThrows(AccessControlException.class,
				() -> newDecoder().decode(new ByteArrayInputStream(bytes)));
	}

	@Test
	public void testDecodeStaticallyProtectedBITMACNull() throws Exception {
		/* Skipping a MAC DO leads to a missing plain value: decoder hits NPE (FIXME in decoder). */
		byte[] bytes = bytesForSMTDO(ISO781611.SMT_DO_CC);
		assertThrows(NullPointerException.class,
				() -> newDecoder().decode(new ByteArrayInputStream(bytes)));
	}

	@Test
	public void testDecodeStaticallyProtectedBITSignatureNull() throws Exception {
		byte[] bytes = bytesForSMTDO(ISO781611.SMT_DO_DS);
		assertThrows(NullPointerException.class,
				() -> newDecoder().decode(new ByteArrayInputStream(bytes)));
	}

	private static byte[] bytesForSMTDO(int doTag) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(out);
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFORMATION_GROUP_TEMPLATE_TAG);
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFO_COUNT_TAG);
		tlvOut.writeValue(new byte[] { 0x01 });
		tlvOut.writeTag(ISO781611.BIOMETRIC_INFORMATION_TEMPLATE_TAG);
		tlvOut.writeTag(ISO781611.SMT_TAG);
		tlvOut.writeTag(doTag);
		tlvOut.writeValue(new byte[] { 0x01, 0x02, 0x03 });
		tlvOut.writeValueEnd(); /* SMT_TAG */
		tlvOut.writeValueEnd(); /* 7F60 */
		tlvOut.writeValueEnd(); /* 7F61 */
		return out.toByteArray();
	}

	@Test
	public void testListOfDecodedIsCopy() throws Exception {
		TestBiometricDataBlock bdb = new TestBiometricDataBlock(CBEFFTestUtil.createSBH(), new byte[] { 1 });
		SimpleCBEFFInfo<TestBiometricDataBlock> info = new SimpleCBEFFInfo<TestBiometricDataBlock>(bdb);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		newEncoder().encode(info, out);
		ComplexCBEFFInfo decoded = newDecoder().decode(new ByteArrayInputStream(out.toByteArray()));
		List<CBEFFInfo> records = decoded.getSubRecords();
		records.clear();
		assertEquals(1, decoded.getSubRecords().size());
	}
}
