package org.jmrtd.cert;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import org.jmrtd.cert.CVCAuthorizationTemplate.Permission;
import org.jmrtd.cert.CVCAuthorizationTemplate.Role;
import org.junit.jupiter.api.Test;

import net.sf.scuba.tlv.TLVOutputStream;

public class CVCertificateFactorySpiTest {

	@Test
	public void testGenerateCertificateRoundTrip() throws Exception {
		CardVerifiableCertificate original = CVCTestUtil.createCertificate(Role.DV_F, Permission.READ_ACCESS_DG3_AND_DG4);
		byte[] encoded = original.getEncoded();

		CVCertificateFactorySpi spi = new CVCertificateFactorySpi();
		Certificate parsed = spi.engineGenerateCertificate(new ByteArrayInputStream(encoded));

		assertNotNull(parsed);
		assertTrue(parsed instanceof CardVerifiableCertificate);
		assertArrayEquals(original.getEncoded(), parsed.getEncoded());
		assertEquals(original.getAuthorityReference(), ((CardVerifiableCertificate) parsed).getAuthorityReference());
		assertEquals(original.getHolderReference(), ((CardVerifiableCertificate) parsed).getHolderReference());
	}

	@Test
	public void testGenerateCertificateWrongTagThrows() {
		byte[] wrongTag = new byte[] { 0x7F, 0x61, 0x02, 0x02, 0x01, 0x00 };
		CVCertificateFactorySpi spi = new CVCertificateFactorySpi();
		assertThrows(CertificateException.class,
				() -> spi.engineGenerateCertificate(new ByteArrayInputStream(wrongTag)));
	}

	@Test
	public void testGenerateCertificateCorruptValueThrows() throws Exception {
		/* Valid 7F21 tag but truncated / invalid body. */
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TLVOutputStream tlvOut = new TLVOutputStream(out);
		byte[] garbage = new byte[64];
		for (int i = 0; i < garbage.length; i++) {
			garbage[i] = (byte) i;
		}
		tlvOut.writeTag(0x7F21);
		tlvOut.writeValue(garbage);
		tlvOut.close();

		CVCertificateFactorySpi spi = new CVCertificateFactorySpi();
		assertThrows(CertificateException.class,
				() -> spi.engineGenerateCertificate(new ByteArrayInputStream(out.toByteArray())));
	}

	@Test
	public void testGenerateCertificateTruncatedValueThrows() {
		/* Valid 7F21 tag declaring 100 bytes, but stream truncated: IOException expected. */
		byte[] truncated = new byte[] { 0x7F, 0x21, (byte) 0x81, 0x64, 0x01, 0x02 };
		CVCertificateFactorySpi spi = new CVCertificateFactorySpi();
		assertThrows(CertificateException.class,
				() -> spi.engineGenerateCertificate(new ByteArrayInputStream(truncated)));
	}

	@Test
	public void testGenerateCertificatesReturnsNull() throws Exception {
		CVCertificateFactorySpi spi = new CVCertificateFactorySpi();
		assertNull(spi.engineGenerateCertificates(new ByteArrayInputStream(new byte[0])));
	}

	@Test
	public void testGenerateCRLReturnsNull() throws Exception {
		CVCertificateFactorySpi spi = new CVCertificateFactorySpi();
		assertNull(spi.engineGenerateCRL(new ByteArrayInputStream(new byte[0])));
	}

	@Test
	public void testGenerateCRLsReturnsNull() throws Exception {
		CVCertificateFactorySpi spi = new CVCertificateFactorySpi();
		assertNull(spi.engineGenerateCRLs(new ByteArrayInputStream(new byte[0])));
	}
}
