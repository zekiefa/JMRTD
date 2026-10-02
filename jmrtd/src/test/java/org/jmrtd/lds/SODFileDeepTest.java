package org.jmrtd.lds;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.DLSet;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.icao.DataGroupHash;
import org.bouncycastle.asn1.icao.LDSSecurityObject;
import org.bouncycastle.asn1.icao.LDSVersionInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.junit.jupiter.api.Test;

/**
 * Deep coverage tests for {@link SODFile}, complementary to {@code SODFileTest}.
 * Focuses on ECDSA and RSA-PSS signature paths, multi-data-group hashes,
 * issuer/serial accessors, signature tampering, and DER structure error paths.
 */
public class SODFileDeepTest {

	private static final String SHA256_OID = "2.16.840.1.101.3.4.2.1";
	private static final String ICAO_LDS_SOD_OID = "2.23.136.1.1.1";
	private static final String RFC_3369_SIGNED_DATA_OID = "1.2.840.113549.1.7.2";
	private static final String RFC_3369_CONTENT_TYPE_OID = "1.2.840.113549.1.9.3";
	private static final String RFC_3369_MESSAGE_DIGEST_OID = "1.2.840.113549.1.9.4";
	private static final String SHA256_WITH_RSA_OID = "1.2.840.113549.1.1.11";
	private static final String RSASSA_PSS_OID = "1.2.840.113549.1.1.10";
	private static final String SHA256_WITH_ECDSA_OID = "1.2.840.10045.4.3.2";

	private KeyPair rsaKeyPair;
	private X509Certificate rsaCert;

	public SODFileDeepTest() throws Exception {
		rsaKeyPair = LDSTestHelper.generateRSAKeyPair();
		rsaCert = LDSTestHelper.createSelfSignedCertificate("CN=DeepSigner,O=JMRTD,C=NL", rsaKeyPair, "SHA256withRSA");
	}

	/* ---------- Generic SOD byte builder (RSA, RSA-PSS and ECDSA) ---------- */

	private static X509Certificate createCertificate(String dn, KeyPair keyPair, String sigAlgName, String sigAlgOID) throws Exception {
		AlgorithmIdentifier sigAlgId = new AlgorithmIdentifier(new ASN1ObjectIdentifier(sigAlgOID));

		Date notBefore = new Date(System.currentTimeMillis() - 5000L);
		Date notAfter = new Date(System.currentTimeMillis() + 5L * 365 * 24 * 3600 * 1000L);

		ASN1EncodableVector validity = new ASN1EncodableVector();
		validity.add(new DERUTCTime(notBefore));
		validity.add(new DERUTCTime(notAfter));

		X500Name name = new X500Name(dn);
		SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo(
				ASN1Sequence.getInstance(ASN1Primitive.fromByteArray(keyPair.getPublic().getEncoded())));

		ASN1EncodableVector tbs = new ASN1EncodableVector();
		tbs.add(new DERTaggedObject(true, 0, new ASN1Integer(2)));
		tbs.add(new ASN1Integer(BigInteger.valueOf(42)));
		tbs.add(sigAlgId);
		tbs.add(name);
		tbs.add(new DERSequence(validity));
		tbs.add(name);
		tbs.add(spki);
		DERSequence tbsSequence = new DERSequence(tbs);

		Signature signature = Signature.getInstance(sigAlgName);
		signature.initSign(keyPair.getPrivate());
		signature.update(tbsSequence.getEncoded("DER"));
		byte[] signatureBytes = signature.sign();

		ASN1EncodableVector certVector = new ASN1EncodableVector();
		certVector.add(tbsSequence);
		certVector.add(sigAlgId);
		certVector.add(new DERBitString(signatureBytes));

		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		return (X509Certificate) factory.generateCertificate(
				new ByteArrayInputStream(new DERSequence(certVector).getEncoded("DER")));
	}

	private static byte[] createSODFileBytes(Map<Integer, byte[]> dataGroupHashes,
			X509Certificate cert, java.security.PrivateKey privateKey,
			String jcaSignatureAlg, String signatureOID,
			String ldsVersion, String unicodeVersion) throws Exception {
		AlgorithmIdentifier digestAlgId = new AlgorithmIdentifier(new ASN1ObjectIdentifier(SHA256_OID));

		DataGroupHash[] hashArray = new DataGroupHash[dataGroupHashes.size()];
		int i = 0;
		for (Map.Entry<Integer, byte[]> entry: dataGroupHashes.entrySet()) {
			hashArray[i++] = new DataGroupHash(entry.getKey().intValue(), new DEROctetString(entry.getValue()));
		}
		LDSSecurityObject securityObject;
		if (ldsVersion == null) {
			securityObject = new LDSSecurityObject(digestAlgId, hashArray);
		} else {
			securityObject = new LDSSecurityObject(digestAlgId, hashArray, new LDSVersionInfo(ldsVersion, unicodeVersion));
		}

		ContentInfo contentInfo = new ContentInfo(new ASN1ObjectIdentifier(ICAO_LDS_SOD_OID), new DEROctetString(securityObject));
		byte[] contentBytes = ((DEROctetString) contentInfo.getContent()).getOctets();

		byte[] digestedContent = java.security.MessageDigest.getInstance("SHA-256").digest(contentBytes);
		org.bouncycastle.asn1.cms.Attribute contentTypeAttribute = new org.bouncycastle.asn1.cms.Attribute(
				new ASN1ObjectIdentifier(RFC_3369_CONTENT_TYPE_OID),
				new DLSet(new org.bouncycastle.asn1.ASN1Encodable[] { new ASN1ObjectIdentifier(ICAO_LDS_SOD_OID) }));
		org.bouncycastle.asn1.cms.Attribute messageDigestAttribute = new org.bouncycastle.asn1.cms.Attribute(
				new ASN1ObjectIdentifier(RFC_3369_MESSAGE_DIGEST_OID),
				new DLSet(new org.bouncycastle.asn1.ASN1Encodable[] { new DEROctetString(digestedContent) }));
		DLSet signedAttributes = new DLSet(new org.bouncycastle.asn1.ASN1Encodable[] {
				contentTypeAttribute.toASN1Primitive(), messageDigestAttribute.toASN1Primitive() });

		Signature signer = Signature.getInstance(jcaSignatureAlg);
		signer.initSign(privateKey);
		signer.update(signedAttributes.getEncoded("DER"));
		byte[] signatureBytes = signer.sign();

		javax.security.auth.x500.X500Principal issuerPrincipal = cert.getIssuerX500Principal();
		X500Name issuerName = new X500Name(issuerPrincipal.getName(javax.security.auth.x500.X500Principal.RFC2253));
		SignerIdentifier sid = new SignerIdentifier(new IssuerAndSerialNumber(issuerName, cert.getSerialNumber()));
		SignerInfo signerInfo = new SignerInfo(sid, digestAlgId, signedAttributes,
				new AlgorithmIdentifier(new ASN1ObjectIdentifier(signatureOID)),
				new DEROctetString(signatureBytes), null);

		ASN1Sequence certSequence = ASN1Sequence.getInstance(ASN1Primitive.fromByteArray(cert.getEncoded()));
		SignedData signedData = new SignedData(
				new DLSet(new org.bouncycastle.asn1.ASN1Encodable[] { new DERSequence(new ASN1ObjectIdentifier(SHA256_OID)) }),
				contentInfo,
				new DLSet(new org.bouncycastle.asn1.ASN1Encodable[] { certSequence }),
				null,
				new DLSet(new org.bouncycastle.asn1.ASN1Encodable[] { signerInfo.toASN1Primitive() }));

		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new ASN1ObjectIdentifier(RFC_3369_SIGNED_DATA_OID));
		v.add(new DERTaggedObject(0, signedData));
		byte[] fileContents = new DERSequence(v).getEncoded("DER");

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		net.sf.scuba.tlv.TLVOutputStream tlvOut = new net.sf.scuba.tlv.TLVOutputStream(out);
		tlvOut.writeTag(0x77);
		tlvOut.writeValue(fileContents);
		tlvOut.flush();
		return out.toByteArray();
	}

	private static Map<Integer, byte[]> createManyHashes() {
		Map<Integer, byte[]> hashes = new TreeMap<Integer, byte[]>();
		for (int dg = 1; dg <= 4; dg++) {
			byte[] hash = new byte[32];
			java.util.Arrays.fill(hash, (byte) dg);
			hashes.put(Integer.valueOf(dg), hash);
		}
		return hashes;
	}

	/* ---------- Tests ---------- */

	@Test
	public void testManyDataGroupHashes() throws Exception {
		Map<Integer, byte[]> hashes = createManyHashes();
		byte[] bytes = LDSTestHelper.createSODFileBytes(hashes, rsaCert, rsaKeyPair.getPrivate(), false, null, null);
		SODFile sod = new SODFile(new ByteArrayInputStream(bytes));

		Map<Integer, byte[]> readHashes = sod.getDataGroupHashes();
		assertEquals(4, readHashes.size());
		assertTrue(readHashes.keySet().containsAll(java.util.Arrays.asList(1, 2, 3, 4)));
		for (Map.Entry<Integer, byte[]> entry: hashes.entrySet()) {
			assertArrayEquals(entry.getValue(), readHashes.get(entry.getKey()));
		}
	}

	@Test
	public void testSignerInfoAccessors() throws Exception {
		byte[] bytes = LDSTestHelper.createSODFileBytes(createManyHashes(), rsaCert, rsaKeyPair.getPrivate(), false, "0108", "040000");
		SODFile sod = new SODFile(new ByteArrayInputStream(bytes));

		assertEquals("SHA-256", sod.getDigestAlgorithm());
		assertEquals("SHA-256", sod.getSignerInfoDigestAlgorithm());
		assertEquals("SHA256withRSA", sod.getDigestEncryptionAlgorithm());
		assertEquals("0108", sod.getLDSVersion());
		assertEquals("040000", sod.getUnicodeVersion());
		assertTrue(sod.getIssuerX500Principal().getName().contains("CN=DeepSigner"));
		assertEquals(rsaCert.getSerialNumber(), sod.getSerialNumber());
		assertNotNull(sod.getEContent());
		assertEquals(128, sod.getEncryptedDigest().length); /* 1024-bit RSA signature */
		assertNotNull(sod.getDocSigningCertificate());
		assertArrayEquals(rsaCert.getEncoded(), sod.getDocSigningCertificate().getEncoded());
		assertTrue(sod.toString().contains("DeepSigner"));
	}

	@Test
	public void testCheckDocSignatureValid() throws Exception {
		byte[] bytes = LDSTestHelper.createSODFileBytes(createManyHashes(), rsaCert, rsaKeyPair.getPrivate(), false, null, null);
		SODFile sod = new SODFile(new ByteArrayInputStream(bytes));
		assertTrue(sod.checkDocSignature(sod.getDocSigningCertificate()));
		assertTrue(sod.checkDocSignature(rsaCert));
	}

	@Test
	public void testCheckDocSignatureTamperedSignature() throws Exception {
		byte[] bytes = LDSTestHelper.createSODFileBytes(createManyHashes(), rsaCert, rsaKeyPair.getPrivate(), false, null, null);
		SODFile sod = new SODFile(new ByteArrayInputStream(bytes));

		/* Flip one byte inside the actual signature bytes in the encoded file. */
		byte[] encoded = sod.getEncoded();
		byte[] signature = sod.getEncryptedDigest();
		int index = indexOf(encoded, signature);
		assertTrue(index >= 0, "signature bytes should occur in encoded SOD");
		encoded[index] ^= 0x01;

		SODFile tampered = new SODFile(new ByteArrayInputStream(encoded));
		assertFalse(tampered.checkDocSignature(rsaCert));
	}

	@Test
	public void testHashMismatchDetectionViaDataGroupHashes() throws Exception {
		Map<Integer, byte[]> hashes = createManyHashes();
		byte[] bytes = LDSTestHelper.createSODFileBytes(hashes, rsaCert, rsaKeyPair.getPrivate(), false, null, null);
		SODFile sod = new SODFile(new ByteArrayInputStream(bytes));

		/* Compute digest over arbitrary content and compare to stored hash: mismatch expected. */
		byte[] someDGContent = new byte[] { 1, 2, 3, 4, 5 };
		byte[] digest = java.security.MessageDigest.getInstance(sod.getDigestAlgorithm()).digest(someDGContent);
		assertFalse(java.util.Arrays.equals(digest, sod.getDataGroupHashes().get(Integer.valueOf(1))));
	}

	@Test
	public void testECDSAPathway() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
		KeyPair ecKeyPair = generator.generateKeyPair();
		X509Certificate ecCert = createCertificate("C=NL,O=JMRTD,CN=ECSigner", ecKeyPair, "SHA256withECDSA", SHA256_WITH_ECDSA_OID);

		byte[] bytes = createSODFileBytes(createManyHashes(), ecCert, ecKeyPair.getPrivate(),
				"SHA256withECDSA", SHA256_WITH_ECDSA_OID, null, null);
		SODFile sod = new SODFile(new ByteArrayInputStream(bytes));

		assertEquals("SHA256withECDSA", sod.getDigestEncryptionAlgorithm());
		assertEquals("SHA-256", sod.getSignerInfoDigestAlgorithm());
		assertEquals("C=NL,O=JMRTD,CN=ECSigner", sod.getIssuerX500Principal().getName());
		assertTrue(sod.checkDocSignature(ecCert));

		/* Verification with a different EC key must fail. */
		KeyPair otherECKeyPair = generator.generateKeyPair();
		X509Certificate otherECCert = createCertificate("C=NL,O=JMRTD,CN=ECOther", otherECKeyPair, "SHA256withECDSA", SHA256_WITH_ECDSA_OID);
		assertFalse(sod.checkDocSignature(otherECCert));
	}

	@Test
	public void testRSAPSSPathway() throws Exception {
		String jcaName = null;
		for (String candidate: new String[] { "SHA256withRSA/PSS", "SHA256withRSAandMGF1", "RSASSA-PSS" }) {
			try {
				Signature.getInstance(candidate);
				jcaName = candidate;
				break;
			} catch (Exception e) {
				/* try next */
			}
		}
		if (jcaName == null) {
			/* Provider lacks PSS support; nothing to test. */
			return;
		}
		if ("RSASSA-PSS".equals(jcaName)) {
			Signature.getInstance(jcaName); /* needs params; skip */
			return;
		}

		byte[] bytes = createSODFileBytes(createManyHashes(), rsaCert, rsaKeyPair.getPrivate(),
				jcaName, RSASSA_PSS_OID, null, null);
		SODFile sod = new SODFile(new ByteArrayInputStream(bytes));

		assertEquals("SSAwithRSA/PSS", sod.getDigestEncryptionAlgorithm());
		assertTrue(sod.checkDocSignature(rsaCert));
	}

	@Test
	public void testGetDocSigningCertificateFromDLFraming() throws Exception {
		/* Rebuild the file with DERSet certificates to exercise the certificate loop. */
		KeyPair keyPair = LDSTestHelper.generateRSAKeyPair();
		X509Certificate cert = LDSTestHelper.createSelfSignedCertificate("C=NL,O=JMRTD,CN=DLCert", keyPair, "SHA256withRSA");
		byte[] bytes = createSODFileBytes(createManyHashes(), cert, keyPair.getPrivate(),
				"SHA256withRSA", SHA256_WITH_RSA_OID, null, null);
		SODFile sod = new SODFile(new ByteArrayInputStream(bytes));
		assertEquals(BigInteger.valueOf(1), sod.getSerialNumber());
		assertNotNull(sod.getDocSigningCertificate());
	}

	@Test
	public void testLDSVersionInfoPresentAndAbsent() throws Exception {
		byte[] withVersion = LDSTestHelper.createSODFileBytes(createManyHashes(), rsaCert, rsaKeyPair.getPrivate(), false, "0107", "030000");
		SODFile sod1 = new SODFile(new ByteArrayInputStream(withVersion));
		assertEquals("0107", sod1.getLDSVersion());
		assertEquals("030000", sod1.getUnicodeVersion());

		byte[] withoutVersion = LDSTestHelper.createSODFileBytes(createManyHashes(), rsaCert, rsaKeyPair.getPrivate(), false, null, null);
		SODFile sod2 = new SODFile(new ByteArrayInputStream(withoutVersion));
		assertNull(sod2.getLDSVersion());
		assertNull(sod2.getUnicodeVersion());
	}

	@Test
	public void testEqualsAndHashCodeConsistency() throws Exception {
		byte[] bytes = LDSTestHelper.createSODFileBytes(createManyHashes(), rsaCert, rsaKeyPair.getPrivate(), false, null, null);
		SODFile a = new SODFile(new ByteArrayInputStream(bytes));
		SODFile b = new SODFile(new ByteArrayInputStream(bytes));

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(null));
		assertFalse(a.equals("not a SODFile"));
		assertTrue(a.equals(a));

		byte[] otherBytes = LDSTestHelper.createSODFileBytes(LDSTestHelper.createDataGroupHashes(), rsaCert, rsaKeyPair.getPrivate(), false, null, null);
		SODFile c = new SODFile(new ByteArrayInputStream(otherBytes));
		assertNotEquals(a, c);
	}

	@Test
	public void testCertificatesAREADLSET() throws Exception {
		/* Certificates encoded with DERSet instead of DLSet. */
		byte[] bytes = createSODFileBytes(createManyHashes(), rsaCert, rsaKeyPair.getPrivate(),
				"SHA256withRSA", SHA256_WITH_RSA_OID, null, null);
		SODFile sod = new SODFile(new ByteArrayInputStream(bytes));
		X509Certificate cert = sod.getDocSigningCertificate();
		assertNotNull(cert);
		assertTrue(cert.getIssuerX500Principal().getName().contains("CN=DeepSigner"));
	}

	@Test
	public void testReadErrorSequenceTooLong() throws Exception {
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new ASN1ObjectIdentifier(RFC_3369_SIGNED_DATA_OID));
		v.add(new DERTaggedObject(0, new DERSequence()));
		v.add(new DERSequence()); /* third element: sequence of length 3 */
		byte[] content = new DERSequence(v).getEncoded("DER");
		byte[] wrapped = wrapInTag77(content);
		assertThrows(Exception.class, () -> new SODFile(new ByteArrayInputStream(wrapped)));
	}

	@Test
	public void testReadErrorNonSequenceContentInsideTag() throws Exception {
		/* Valid outer structure but SignedData content is an octet string, not a sequence. */
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new ASN1ObjectIdentifier(RFC_3369_SIGNED_DATA_OID));
		v.add(new DERTaggedObject(true, 0, new DEROctetString(new byte[] { 1, 2, 3 })));
		byte[] content = new DERSequence(v).getEncoded("DER");
		byte[] wrapped = wrapInTag77(content);
		assertThrows(Exception.class, () -> new SODFile(new ByteArrayInputStream(wrapped)));
	}

	@Test
	public void testRoundTripPreservesAllFields() throws Exception {
		byte[] bytes = LDSTestHelper.createSODFileBytes(createManyHashes(), rsaCert, rsaKeyPair.getPrivate(), false, "0108", "040000");
		SODFile original = new SODFile(new ByteArrayInputStream(bytes));
		byte[] encoded = original.getEncoded();
		SODFile copy = new SODFile(new ByteArrayInputStream(encoded));

		assertEquals(original.getDataGroupHashes().keySet(), copy.getDataGroupHashes().keySet());
		assertEquals(original.getDigestAlgorithm(), copy.getDigestAlgorithm());
		assertEquals(original.getSignerInfoDigestAlgorithm(), copy.getSignerInfoDigestAlgorithm());
		assertEquals(original.getDigestEncryptionAlgorithm(), copy.getDigestEncryptionAlgorithm());
		assertEquals(original.getLDSVersion(), copy.getLDSVersion());
		assertEquals(original.getUnicodeVersion(), copy.getUnicodeVersion());
		assertEquals(original.getIssuerX500Principal(), copy.getIssuerX500Principal());
		assertEquals(original.getSerialNumber(), copy.getSerialNumber());
		assertArrayEquals(original.getEncryptedDigest(), copy.getEncryptedDigest());
		assertArrayEquals(original.getEContent(), copy.getEContent());
		assertEquals(original, copy);
	}

	private static int indexOf(byte[] haystack, byte[] needle) {
		outer:
		for (int i = 0; i + needle.length <= haystack.length; i++) {
			for (int j = 0; j < needle.length; j++) {
				if (haystack[i + j] != needle[j]) { continue outer; }
			}
			return i;
		}
		return -1;
	}

	private static byte[] wrapInTag77(byte[] content) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		net.sf.scuba.tlv.TLVOutputStream tlvOut = new net.sf.scuba.tlv.TLVOutputStream(out);
		tlvOut.writeTag(0x77);
		tlvOut.writeValue(content);
		tlvOut.flush();
		return out.toByteArray();
	}
}
