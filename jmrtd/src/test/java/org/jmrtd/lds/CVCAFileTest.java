package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import org.jmrtd.PassportService;

public class CVCAFileTest {

	@Test
	public void testConstructors() throws Exception {
		CVCAFile cvca = new CVCAFile("NLDKK4CVC", null);
		assertEquals(PassportService.EF_CVCA, cvca.getFID());
		assertEquals("NLDKK4CVC", cvca.getCAReference().getName());
		assertNull(cvca.getAltCAReference());
		assertEquals(CVCAFile.LENGTH, cvca.getLength());
		assertEquals(CVCAFile.LENGTH, cvca.getEncoded().length);
		assertTrue(cvca.toString().contains("NLDKK4CVC"));

		CVCAFile withAlt = new CVCAFile((short) 0x0201, "NLDKK4CVC", "NLDKK4CVB");
		assertEquals((short) 0x0201, withAlt.getFID());
		assertNotNull(withAlt.getAltCAReference());
		assertTrue(withAlt.toString().contains("Alternative"));

		CVCAFile single = new CVCAFile((short) 0x0202, "NLDKK4CVC");
		assertEquals((short) 0x0202, single.getFID());
		assertNull(single.getAltCAReference());
	}

	@Test
	public void testConstructorValidation() {
		assertThrows(IllegalArgumentException.class, () -> new CVCAFile(null, null));
		assertThrows(IllegalArgumentException.class, () -> new CVCAFile("WAAAAAAAAAAAAAAAATOVERLONG", null));
		assertThrows(IllegalArgumentException.class, () -> new CVCAFile("NLDKK4CVC", "WAAAAAAAAAAAAAAAATOVERLONG"));
	}

	@Test
	public void testRoundTrip() throws Exception {
		CVCAFile cvca = new CVCAFile("NLDKK4CVC", "NLDKK4CVB");
		byte[] encoded = cvca.getEncoded();
		CVCAFile copy = new CVCAFile(new ByteArrayInputStream(encoded));
		assertEquals(cvca, copy);
		assertEquals(cvca.hashCode(), copy.hashCode());

		CVCAFile copy2 = new CVCAFile(PassportService.EF_CVCA, new ByteArrayInputStream(encoded));
		assertEquals(cvca, copy2);
	}

	@Test
	public void testRoundTripNoAlt() throws Exception {
		CVCAFile cvca = new CVCAFile("NLDKK4CVC", null);
		CVCAFile copy = new CVCAFile(new ByteArrayInputStream(cvca.getEncoded()));
		assertEquals(cvca, copy);
		assertNull(copy.getAltCAReference());
	}

	@Test
	public void testReadWrongTag() {
		byte[] bad = new byte[CVCAFile.LENGTH];
		bad[0] = 0x43; /* not CAR_TAG */
		assertThrows(IllegalArgumentException.class, () -> new CVCAFile(new ByteArrayInputStream(bad)));
	}

	@Test
	public void testReadTooLongReference() {
		byte[] bad = new byte[CVCAFile.LENGTH];
		bad[0] = CVCAFile.CAR_TAG;
		bad[1] = 17; /* > 16 */
		assertThrows(IllegalArgumentException.class, () -> new CVCAFile(new ByteArrayInputStream(bad)));
	}

	@Test
	public void testReadBadPadding() throws Exception {
		CVCAFile cvca = new CVCAFile("NLDKK4CVC", null);
		byte[] encoded = cvca.getEncoded();
		encoded[CVCAFile.LENGTH - 1] = 0x01; /* non-zero padding */
		assertThrows(IllegalArgumentException.class, () -> new CVCAFile(new ByteArrayInputStream(encoded)));
	}

	@Test
	public void testReadAltTooLong() {
		/* First ref len 1, second ref claims length 17. */
		byte[] bytes = new byte[CVCAFile.LENGTH];
		bytes[0] = CVCAFile.CAR_TAG;
		bytes[1] = 1;
		bytes[2] = 'A';
		bytes[3] = CVCAFile.CAR_TAG;
		bytes[4] = 17;
		assertThrows(IllegalArgumentException.class, () -> new CVCAFile(new ByteArrayInputStream(bytes)));
	}

	@Test
	public void testEquals() throws Exception {
		CVCAFile a = new CVCAFile("NLDKK4CVC", "NLDKK4CVB");
		CVCAFile b = new CVCAFile("NLDKK4CVC", "NLDKK4CVB");
		CVCAFile c = new CVCAFile("NLDKK4CVC", null);
		CVCAFile d = new CVCAFile("NLDKK4CVX", null);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertNotEquals(c, a);
		assertNotEquals(c, d);
		assertNotEquals(a, null);
		assertNotEquals(a, new Object());
	}

	@Test
	public void testWriteToOutputStream() throws Exception {
		CVCAFile cvca = new CVCAFile("NLDKK4CVC", null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		cvca.writeObject(out);
		out.flush();
		assertArrayEquals(cvca.getEncoded(), out.toByteArray());
	}
}
