package org.jmrtd.cbeff;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared helper implementations for CBEFF tests.
 */
final class CBEFFTestUtil {

	private CBEFFTestUtil() {
	}

	/** A trivial BDB holding a byte payload and an SBH. */
	static class TestBiometricDataBlock implements BiometricDataBlock {
		private static final long serialVersionUID = 1L;

		private final StandardBiometricHeader sbh;
		private final byte[] data;

		TestBiometricDataBlock(StandardBiometricHeader sbh, byte[] data) {
			this.sbh = sbh;
			this.data = data;
		}

		public StandardBiometricHeader getStandardBiometricHeader() {
			return sbh;
		}

		byte[] getData() {
			return data;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) { return true; }
			if (other == null) { return false; }
			if (getClass() != other.getClass()) { return false; }
			TestBiometricDataBlock otherBDB = (TestBiometricDataBlock) other;
			return java.util.Arrays.equals(data, otherBDB.data);
		}

		@Override
		public int hashCode() {
			return java.util.Arrays.hashCode(data);
		}
	}

	/** Encoder writing the raw payload. */
	static class TestBiometricDataBlockEncoder implements BiometricDataBlockEncoder<TestBiometricDataBlock> {
		public void encode(TestBiometricDataBlock bdb, OutputStream out) throws IOException {
			out.write(bdb.getData());
		}
	}

	/** Decoder reading the raw payload. */
	static class TestBiometricDataBlockDecoder implements BiometricDataBlockDecoder<TestBiometricDataBlock> {
		public TestBiometricDataBlock decode(InputStream in, StandardBiometricHeader sbh, int index, int length) throws IOException {
			byte[] data = new byte[length];
			int read = 0;
			while (read < length) {
				int r = in.read(data, read, length - read);
				if (r < 0) { break; }
				read += r;
			}
			return new TestBiometricDataBlock(sbh, data);
		}
	}

	static StandardBiometricHeader createSBH() {
		Map<Integer, byte[]> elements = new HashMap<Integer, byte[]>();
		elements.put(ISO781611.BIOMETRIC_TYPE_TAG, new byte[] { CBEFFInfo.BIOMETRIC_TYPE_FINGERPRINT });
		elements.put(ISO781611.BIOMETRIC_SUBTYPE_TAG, new byte[] { (byte) (CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_RIGHT | CBEFFInfo.BIOMETRIC_SUBTYPE_MASK_THUMB) });
		elements.put(ISO781611.FORMAT_OWNER_TAG, new byte[] { 0x01, 0x01 });
		elements.put(ISO781611.FORMAT_TYPE_TAG, new byte[] { 0x00, 0x07 });
		return new StandardBiometricHeader(elements);
	}

	static void assertSBHEquals(StandardBiometricHeader expected, StandardBiometricHeader actual) {
		assertEquals(expected.getElements().keySet(), actual.getElements().keySet());
		for (Map.Entry<Integer, byte[]> entry: expected.getElements().entrySet()) {
			assertArrayEquals(entry.getValue(), actual.getElements().get(entry.getKey()));
		}
	}
}
