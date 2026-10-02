package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

public class SODFileTest {

	private KeyPair keyPair;
	private X509Certificate cert;

	public SODFileTest() throws Exception {
		keyPair = LDSTestHelper.generateRSAKeyPair();
		cert = LDSTestHelper.createSelfSignedCertificate("C=NL,O=JMRTD,CN=DocSigner", keyPair, "SHA256withRSA");
	}

	private SODFile createSODFile() {
		byte[] bytes = LDSTestHelper.createSODFileBytes(LDSTestHelper.createDataGroupHashes(),
				cert, keyPair.getPrivate(), false, null, null);
		try {
			return new SODFile(new ByteArrayInputStream(bytes));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	public void testRead() throws Exception {
		SODFile sod = createSODFile();
		assertEquals(LDSFile.EF_SOD_TAG, sod.getTag());
		assertTrue(sod.getLength() > 0);

		Map<Integer, byte[]> hashes = sod.getDataGroupHashes();
		assertEquals(2, hashes.size());
		assertArrayEquals(LDSTestHelper.createDataGroupHashes().get(1), hashes.get(1));

		assertEquals("SHA-256", sod.getDigestAlgorithm());
		assertEquals("SHA-256", sod.getSignerInfoDigestAlgorithm());
		assertEquals("SHA256withRSA", sod.getDigestEncryptionAlgorithm());
		assertNull(sod.getLDSVersion());
		assertNull(sod.getUnicodeVersion());
		assertNotNull(sod.getEContent());
		assertNotNull(sod.getEncryptedDigest());
		assertEquals("C=NL,O=JMRTD,CN=DocSigner", sod.getIssuerX500Principal().getName());
		assertEquals(cert.getSerialNumber(), sod.getSerialNumber());
		assertArrayEquals(cert.getEncoded(), sod.getDocSigningCertificate().getEncoded());
		assertTrue(sod.toString().startsWith("SODFile"));
	}

	@Test
	public void testLDSVersionInfo() {
		byte[] bytes = LDSTestHelper.createSODFileBytes(LDSTestHelper.createDataGroupHashes(),
				cert, keyPair.getPrivate(), false, "0108", "040001");
		try {
			SODFile sod = new SODFile(new ByteArrayInputStream(bytes));
			assertEquals("0108", sod.getLDSVersion());
			assertEquals("040001", sod.getUnicodeVersion());
		} catch (Exception e) {
			fail(e);
		}
	}

	@Test
	public void testCheckDocSignature() throws Exception {
		SODFile sod = createSODFile();
		assertTrue(sod.checkDocSignature(cert));

		KeyPair otherKeyPair = LDSTestHelper.generateRSAKeyPair();
		X509Certificate otherCert = LDSTestHelper.createSelfSignedCertificate("C=NL,O=JMRTD,CN=Other", otherKeyPair, "SHA256withRSA");
		assertFalse(sod.checkDocSignature(otherCert));
	}

	@Test
	public void testCheckDocSignatureWithPlainRSAAlgId() throws Exception {
		/* Covers the "RSA" digest-encryption-algorithm branch in checkDocSignature. */
		byte[] bytes = LDSTestHelper.createSODFileBytes(LDSTestHelper.createDataGroupHashes(),
				cert, keyPair.getPrivate(), true, null, null);
		SODFile sod = new SODFile(new ByteArrayInputStream(bytes));
		assertEquals("RSA", sod.getDigestEncryptionAlgorithm());
		assertTrue(sod.checkDocSignature(cert));
	}

	@Test
	public void testRoundTripWrite() throws Exception {
		byte[] bytes = LDSTestHelper.createSODFileBytes(LDSTestHelper.createDataGroupHashes(),
				cert, keyPair.getPrivate(), false, null, null);
		SODFile sod = new SODFile(new ByteArrayInputStream(bytes));
		byte[] encoded = sod.getEncoded();
		SODFile copy = new SODFile(new ByteArrayInputStream(encoded));
		assertEquals(sod, copy);
		assertEquals(sod.hashCode(), copy.hashCode());
	}

	@Test
	public void testEquals() throws java.io.IOException {
		SODFile sod = createSODFile();
		SODFile same = createSODFile();
		assertEquals(sod, sod);
		assertEquals(sod, same);
		assertNotEquals(sod, null);
		assertNotEquals(sod, new Object());

		java.util.Map<Integer, byte[]> hashes = new java.util.TreeMap<Integer, byte[]>();
	hashes.put(Integer.valueOf(1), new byte[32]);
	hashes.put(Integer.valueOf(2), new byte[32]);
	byte[] bytes = LDSTestHelper.createSODFileBytes(hashes, cert, keyPair.getPrivate(), false, null, null);
	SODFile different = new SODFile(new ByteArrayInputStream(bytes));
	assertNotEquals(sod, different);
	}

	@Test
	public void testReadErrors() {
		/* Content is not a sequence. */
		assertThrows(Exception.class, () -> new SODFile(new ByteArrayInputStream(new byte[] { 0x77, 0x03, 0x02, 0x01, 0x00 })));
	}

	@Test
	public void testWrongContentTypeOID() throws Exception {
		org.bouncycastle.asn1.ASN1EncodableVector v = new org.bouncycastle.asn1.ASN1EncodableVector();
		v.add(new org.bouncycastle.asn1.ASN1ObjectIdentifier("1.2.840.113549.1.7.1"));
		v.add(new org.bouncycastle.asn1.DERTaggedObject(0, new org.bouncycastle.asn1.DERSequence()));
		byte[] content = new org.bouncycastle.asn1.DERSequence(v).getEncoded("DER");
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		net.sf.scuba.tlv.TLVOutputStream tlvOut = new net.sf.scuba.tlv.TLVOutputStream(out);
		tlvOut.writeTag(0x77);
		tlvOut.writeValue(content);
		tlvOut.flush();
		assertThrows(Exception.class, () -> new SODFile(new ByteArrayInputStream(out.toByteArray())));
	}

	@Test
	public void testWrongTaggedObjectTagNo() throws Exception {
		org.bouncycastle.asn1.ASN1EncodableVector v = new org.bouncycastle.asn1.ASN1EncodableVector();
		v.add(new org.bouncycastle.asn1.ASN1ObjectIdentifier("1.2.840.113549.1.7.2"));
		v.add(new org.bouncycastle.asn1.DERTaggedObject(1, new org.bouncycastle.asn1.DERSequence()));
		byte[] content = new org.bouncycastle.asn1.DERSequence(v).getEncoded("DER");
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		net.sf.scuba.tlv.TLVOutputStream tlvOut = new net.sf.scuba.tlv.TLVOutputStream(out);
		tlvOut.writeTag(0x77);
		tlvOut.writeValue(content);
		tlvOut.flush();
		assertThrows(Exception.class, () -> new SODFile(new ByteArrayInputStream(out.toByteArray())));
	}

	@Test
	public void testNonTaggedContent() throws Exception {
		org.bouncycastle.asn1.ASN1EncodableVector v = new org.bouncycastle.asn1.ASN1EncodableVector();
		v.add(new org.bouncycastle.asn1.ASN1ObjectIdentifier("1.2.840.113549.1.7.2"));
		v.add(new org.bouncycastle.asn1.DERSequence());
		byte[] content = new org.bouncycastle.asn1.DERSequence(v).getEncoded("DER");
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		net.sf.scuba.tlv.TLVOutputStream tlvOut = new net.sf.scuba.tlv.TLVOutputStream(out);
		tlvOut.writeTag(0x77);
		tlvOut.writeValue(content);
		tlvOut.flush();
		assertThrows(Exception.class, () -> new SODFile(new ByteArrayInputStream(out.toByteArray())));
	}
}
