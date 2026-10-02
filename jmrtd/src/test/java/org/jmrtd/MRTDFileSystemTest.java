/*
 * JMRTD - A Java API for accessing machine readable travel documents.
 *
 * Tests for {@link MRTDFileSystem} (and {@link PassportService#getInputStream(short)})
 * using a fake card that answers SELECT FILE / READ BINARY from in-memory files.
 */

package org.jmrtd;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardServiceException;
import net.sf.scuba.smartcards.FileInfo;
import net.sf.scuba.smartcards.ISO7816;

/**
 * Unit tests for the MRTD file system layer against a fake card.
 *
 * Note: this version of {@link MRTDFileSystem} exposes file queries through
 * {@code selectFile}, {@code readBinary} and {@code getSelectedPath} (there are
 * no {@code hasFile}/{@code getFileList} methods); those are covered here.
 *
 * @author The JMRTD team (info@jmrtd.org)
 */
public class MRTDFileSystemTest {

	private static final String MRZ_TD3 = "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
			+ "L898902C36UTO7408122F1204159ZE184226B<<<<<<10";

	private static final byte[] SW_NO_ERROR = new byte[] { (byte) 0x90, 0x00 };
	private static final byte[] SW_FILE_NOT_FOUND = new byte[] { 0x6A, (byte) 0x82 };

	private static byte[] dg1Bytes;
	private static byte[] comBytes;

	static {
		try {
			byte[] mrz = MRZ_TD3.getBytes("ISO-8859-1");
			/* EF.DG1: 61 5B 5F01 58 <88 MRZ bytes>. */
			ByteArrayOutputStream dg1Out = new ByteArrayOutputStream();
			dg1Out.write(0x61);
			dg1Out.write(2 + 1 + mrz.length); /* 0x5B */
			dg1Out.write(0x5F);
			dg1Out.write(0x01);
			dg1Out.write(mrz.length); /* 88 = 0x58 */
			dg1Out.write(mrz, 0, mrz.length);
			dg1Bytes = dg1Out.toByteArray();

			/* EF.COM: 60 06 5C 04 01 1E 01 01. */
			comBytes = new byte[] { 0x60, 0x06, 0x5C, 0x04, 0x01, 0x1E, 0x01, 0x01 };
		} catch (UnsupportedEncodingException uee) {
			throw new IllegalStateException(uee);
		}
	}

	private PassportApduServiceTest.FakeCardService cardService;
	private PassportService service;
	private final Map<Short, byte[]> files = new HashMap<Short, byte[]>();
	private short[] selected = new short[1];

	@BeforeEach
	public void setUp() throws CardServiceException {
		cardService = new PassportApduServiceTest.FakeCardService();
		service = new PassportService(cardService);

		files.clear();
		files.put(Short.valueOf(PassportService.EF_DG1), dg1Bytes);
		files.put(Short.valueOf(PassportService.EF_COM), comBytes);

		cardService.onFunction(ISO7816.INS_SELECT_FILE, (capdu) -> {
			if (capdu.getP1() == 0x02) {
				byte[] data = capdu.getData();
				selected[0] = (short) (((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
				return files.containsKey(Short.valueOf(selected[0])) ? SW_NO_ERROR : SW_FILE_NOT_FOUND;
			}
			return SW_NO_ERROR; /* SELECT APPLET and others. */
		});
		cardService.onFunction(ISO7816.INS_READ_BINARY, (capdu) -> {
			byte[] fileBytes = files.get(Short.valueOf(selected[0]));
			if (fileBytes == null) {
				return SW_FILE_NOT_FOUND;
			}
			int offset = ((capdu.getP1() & 0xFF) << 8) | (capdu.getP2() & 0xFF);
			int le = capdu.getNe();
			int length = Math.max(0, Math.min(le, fileBytes.length - offset));
			byte[] response = new byte[length + 2];
			System.arraycopy(fileBytes, offset, response, 0, length);
			response[length] = (byte) 0x90;
			response[length + 1] = 0x00;
			return response;
		});

		service.open();
	}

	@Test
	public void testGetInputStreamDG1() throws Exception {
		CardFileInputStream inputStream = service.getInputStream(PassportService.EF_DG1);
		assertNotNull(inputStream);
		assertEquals(dg1Bytes.length, inputStream.getLength());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int b;
		while ((b = inputStream.read()) >= 0) {
			out.write(b);
		}
		assertArrayEquals(dg1Bytes, out.toByteArray());
	}

	@Test
	public void testGetInputStreamCOM() throws Exception {
		CardFileInputStream inputStream = service.getInputStream(PassportService.EF_COM);
		assertEquals(comBytes.length, inputStream.getLength());

		byte[] read = new byte[comBytes.length];
		for (int i = 0; i < read.length; i++) {
			read[i] = (byte) inputStream.read();
		}
		assertArrayEquals(comBytes, read);
	}

	@Test
	public void testGetInputStreamUnknownFile() throws CardServiceException {
		assertThrows(CardServiceException.class, () -> service.getInputStream(PassportService.EF_DG2));
	}

	@Test
	public void testReadBinaryOnFileSystem() throws CardServiceException {
		MRTDFileSystem fs = new MRTDFileSystem(service);
		fs.selectFile(PassportService.EF_DG1);

		byte[] prefix = fs.readBinary(0, 8);
		assertEquals(8, prefix.length);
		assertEquals(0x61, prefix[0] & 0xFF);

		byte[] whole = fs.readBinary(0, dg1Bytes.length);
		assertArrayEquals(dg1Bytes, whole);
	}

	@Test
	public void testSelectFileReselectsOnChangeOnly() throws CardServiceException {
		MRTDFileSystem fs = new MRTDFileSystem(service);
		fs.selectFile(PassportService.EF_DG1);
		assertEquals(0, cardService.getTransmitCount()); /* Lazy: no APDU yet. */
		fs.readBinary(0, 1);
		int countAfterFirstRead = cardService.getTransmitCount();
		assertTrue(countAfterFirstRead > 0);

		fs.selectFile(PassportService.EF_DG1); /* Same file: no re-selection logic. */
		fs.readBinary(0, 1); /* Already buffered: no new APDUs. */
		assertEquals(countAfterFirstRead, cardService.getTransmitCount());
	}

	@Test
	public void testGetSelectedPath() throws CardServiceException {
		MRTDFileSystem fs = new MRTDFileSystem(service);
		fs.selectFile(PassportService.EF_COM);

		FileInfo[] path = fs.getSelectedPath();
		assertNotNull(path);
		assertEquals(1, path.length);
		assertEquals(PassportService.EF_COM, path[0].getFID());
		assertEquals(comBytes.length, path[0].getFileLength());
	}

	@Test
	public void testReadBinaryNoFileSelected() {
		MRTDFileSystem fs = new MRTDFileSystem(service);
		assertThrows(CardServiceException.class, () -> fs.readBinary(0, 8));
	}
}
