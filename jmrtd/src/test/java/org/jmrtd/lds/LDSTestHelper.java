package org.jmrtd.lds;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import net.sf.scuba.data.Gender;

/**
 * Shared helper methods for LDS file tests: self-signed certificates and
 * sample biometric structures. bcpkix is not on the test classpath, hence
 * the manual ASN.1 certificate assembly.
 */
final class LDSTestHelper {

	private LDSTestHelper() {
	}

	static KeyPair generateRSAKeyPair() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(1024);
		return generator.generateKeyPair();
	}

	/**
	 * Creates a minimal self-signed X.509 certificate using
	 * BouncyCastle ASN.1 types directly.
	 */
	static X509Certificate createSelfSignedCertificate(String dn, KeyPair keyPair, String sigAlgName) throws Exception {
		String oid = "SHA256withRSA".equals(sigAlgName) ? "1.2.840.113549.1.1.11" : "1.2.840.113549.1.1.5";
		AlgorithmIdentifier sigAlgId = new AlgorithmIdentifier(new ASN1ObjectIdentifier(oid));

		Date notBefore = new Date(System.currentTimeMillis() - 5000L);
		Date notAfter = new Date(System.currentTimeMillis() + 5L * 365 * 24 * 3600 * 1000L);

		ASN1EncodableVector validityVector = new ASN1EncodableVector();
		validityVector.add(new DERUTCTime(notBefore));
		validityVector.add(new DERUTCTime(notAfter));

		X500Name name = new X500Name(dn);

		SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo(
				ASN1Sequence.getInstance(ASN1Primitive.fromByteArray(keyPair.getPublic().getEncoded())));

		ASN1EncodableVector tbs = new ASN1EncodableVector();
		tbs.add(new DERTaggedObject(true, 0, new ASN1Integer(2)));
		tbs.add(new ASN1Integer(BigInteger.valueOf(1)));
		tbs.add(sigAlgId);
		tbs.add(name);
		tbs.add(new DERSequence(validityVector));
		tbs.add(name);
		tbs.add(subjectPublicKeyInfo);
		DERSequence tbsSequence = new DERSequence(tbs);

		Signature signature = Signature.getInstance(sigAlgName);
		signature.initSign(keyPair.getPrivate());
		signature.update(tbsSequence.getEncoded("DER"));
		byte[] signatureBytes = signature.sign();

		ASN1EncodableVector certVector = new ASN1EncodableVector();
		certVector.add(tbsSequence);
		certVector.add(sigAlgId);
		certVector.add(new DERBitString(signatureBytes));
		DERSequence certSequence = new DERSequence(certVector);

		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certSequence.getEncoded("DER")));
	}

	static MRZInfo createTestMRZInfo() {
		return new MRZInfo("P", "UTO", "ERIKSSON", "ANNA MARIA",
				"L898902C3", "UTO", "740812", Gender.FEMALE, "120415", "ZE184226B");
	}

	static Map<Integer, byte[]> createDataGroupHashes() {
		Map<Integer, byte[]> hashes = new TreeMap<Integer, byte[]>();
		byte[] hash = new byte[32];
		for (int i = 0; i < hash.length; i++) { hash[i] = (byte) i; }
		hashes.put(Integer.valueOf(1), hash);
		hashes.put(Integer.valueOf(2), hash.clone());
		return hashes;
	}

	static FaceInfo createTestFaceInfo() {
		try {
			byte[] imageBytes = new byte[] { 1, 2, 3, 4, 5 };
			FaceImageInfo faceImageInfo = new FaceImageInfo(
					Gender.FEMALE, FaceImageInfo.EyeColor.BLUE,
					0, 0, 0,
					new int[] { 0, 0, 0 }, new int[] { 0, 0, 0 },
					FaceImageInfo.FACE_IMAGE_TYPE_BASIC,
					FaceImageInfo.IMAGE_COLOR_SPACE_RGB24,
					FaceImageInfo.SOURCE_TYPE_STATIC_PHOTO_SCANNER,
					0, 100,
					null, 800, 600,
					new ByteArrayInputStream(imageBytes), imageBytes.length,
					FaceImageInfo.IMAGE_DATA_TYPE_JPEG);
			List<FaceImageInfo> faceImageInfos = new ArrayList<FaceImageInfo>();
			faceImageInfos.add(faceImageInfo);
			return new FaceInfo(faceImageInfos);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	static FingerInfo createTestFingerInfo() {
		try {
			byte[] imageBytes = new byte[] { 1, 2, 3, 4, 5 };
			FingerImageInfo fingerImageInfo = new FingerImageInfo(
					FingerImageInfo.POSITION_RIGHT_THUMB,
					1, 0, 100, 0,
					800, 600,
					new ByteArrayInputStream(imageBytes), imageBytes.length,
					FingerInfo.COMPRESSION_JPEG);
			List<FingerImageInfo> fingerImageInfos = new ArrayList<FingerImageInfo>();
			fingerImageInfos.add(fingerImageInfo);
			return new FingerInfo(0, 20, FingerInfo.SCALE_UNITS_PPI,
					500, 500, 500, 500, 8, FingerInfo.COMPRESSION_JPEG,
					fingerImageInfos);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	static IrisInfo createTestIrisInfo() {
		byte[] deviceUniqueId = new byte[16];
		List<IrisBiometricSubtypeInfo> subtypes = new ArrayList<IrisBiometricSubtypeInfo>();
		return new IrisInfo(IrisInfo.CAPTURE_DEVICE_UNDEF,
				IrisInfo.ORIENTATION_UNDEF, IrisInfo.ORIENTATION_UNDEF,
				IrisInfo.SCAN_TYPE_UNDEF,
				IrisInfo.IROCC_UNDEF, IrisInfo.IROCC_ZEROFILL,
				IrisInfo.IRBNDY_UNDEF, 160,
				IrisInfo.IMAGEFORMAT_MONO_JPEG,
				100, 2, IrisInfo.INTENSITY_DEPTH_UNDEF,
				IrisInfo.TRANS_UNDEF,
				deviceUniqueId, subtypes);
	}

	static DG11File createTestDG11File() {
		try {
			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd");
			List<String> otherNames = new ArrayList<String>();
			otherNames.add("JOHNNY");
			otherNames.add("JOHN");
			List<String> placeOfBirth = new ArrayList<String>();
			placeOfBirth.add("AMSTERDAM");
			placeOfBirth.add("NETHERLANDS");
			List<String> permanentAddress = new ArrayList<String>();
			permanentAddress.add("SESAMSTRAAT 1");
			permanentAddress.add("1000 AA AMSTERDAM");
			List<String> otherValidTDNumbers = new ArrayList<String>();
			otherValidTDNumbers.add("123456789");
			otherValidTDNumbers.add("987654321");
			return new DG11File("ERIKSSON<<ANNA<MARIA",
					otherNames, "9990009908",
					sdf.parse("19740812"), placeOfBirth, permanentAddress,
					"+31 20 123 4567", "SOFTWARE ENGINEER", "MRS",
					"A PERSONAL SUMMARY", new byte[] { 1, 2, 3, 4 },
					otherValidTDNumbers, "NONE");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	static DG12File createTestDG12File() {
		try {
			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd");
			java.text.SimpleDateFormat sdtf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
			return new DG12File("ISSUING AUTHORITY", sdf.parse("20120415"),
					null, "NONE", "PAID",
					new byte[] { 1, 2, 3 }, new byte[] { 4, 5, 6 },
					sdtf.parse("20120415120000"), "SERIAL123");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	static DG12File createTestDG12FileWithNames() {
		try {
			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd");
			List<String> namesOfOtherPersons = new ArrayList<String>();
			namesOfOtherPersons.add("OTHER PERSON 1");
			namesOfOtherPersons.add("OTHER PERSON 2");
			return new DG12File(null, sdf.parse("20120415"), namesOfOtherPersons,
					null, null, null, null, null, null);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Builds EF.SOD file bytes (tag 0x77 + CMS SignedData containing an
	 * LDSSecurityObject) bypassing the SODFile constructors, whose internal
	 * reliance on legacy BouncyCastle API (AlgorithmIdentifier.getInstance(String))
	 * does not work with the BC version on the classpath.
	 */
	static byte[] createSODFileBytes(java.util.Map<Integer, byte[]> dataGroupHashes,
			X509Certificate cert, java.security.PrivateKey privateKey,
			boolean usePlainRSAEncryptionAlgId,
			String ldsVersion, String unicodeVersion) {
		try {
			String DIGEST_OID = "2.16.840.1.101.3.4.2.1"; /* SHA-256 */
			String ICAO_LDS_SOD_OID = "2.23.136.1.1.1";
			String RFC_3369_SIGNED_DATA_OID = "1.2.840.113549.1.7.2";
			String RFC_3369_CONTENT_TYPE_OID = "1.2.840.113549.1.9.3";
			String RFC_3369_MESSAGE_DIGEST_OID = "1.2.840.113549.1.9.4";
			String SIGNATURE_OID = usePlainRSAEncryptionAlgId ? "1.2.840.113549.1.1.1" : "1.2.840.113549.1.1.11";

			AlgorithmIdentifier digestAlgId = new AlgorithmIdentifier(new ASN1ObjectIdentifier(DIGEST_OID));

			org.bouncycastle.asn1.icao.DataGroupHash[] hashArray =
					new org.bouncycastle.asn1.icao.DataGroupHash[dataGroupHashes.size()];
			int i = 0;
			for (java.util.Map.Entry<Integer, byte[]> entry: dataGroupHashes.entrySet()) {
				hashArray[i++] = new org.bouncycastle.asn1.icao.DataGroupHash(
						entry.getKey().intValue(), new org.bouncycastle.asn1.DEROctetString(entry.getValue()));
			}
			org.bouncycastle.asn1.icao.LDSSecurityObject securityObject;
			if (ldsVersion == null) {
				securityObject = new org.bouncycastle.asn1.icao.LDSSecurityObject(digestAlgId, hashArray);
			} else {
				securityObject = new org.bouncycastle.asn1.icao.LDSSecurityObject(digestAlgId, hashArray,
						new org.bouncycastle.asn1.icao.LDSVersionInfo(ldsVersion, unicodeVersion));
			}

			org.bouncycastle.asn1.cms.ContentInfo contentInfo = new org.bouncycastle.asn1.cms.ContentInfo(
					new ASN1ObjectIdentifier(ICAO_LDS_SOD_OID),
					new org.bouncycastle.asn1.DEROctetString(securityObject));
			byte[] contentBytes = ((org.bouncycastle.asn1.DEROctetString) contentInfo.getContent()).getOctets();

			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
			byte[] digestedContent = digest.digest(contentBytes);
			org.bouncycastle.asn1.cms.Attribute contentTypeAttribute = new org.bouncycastle.asn1.cms.Attribute(
					new ASN1ObjectIdentifier(RFC_3369_CONTENT_TYPE_OID),
					new org.bouncycastle.asn1.DLSet(new org.bouncycastle.asn1.ASN1Encodable[] { new ASN1ObjectIdentifier(ICAO_LDS_SOD_OID) }));
			org.bouncycastle.asn1.cms.Attribute messageDigestAttribute = new org.bouncycastle.asn1.cms.Attribute(
					new ASN1ObjectIdentifier(RFC_3369_MESSAGE_DIGEST_OID),
					new org.bouncycastle.asn1.DLSet(new org.bouncycastle.asn1.ASN1Encodable[] { new org.bouncycastle.asn1.DEROctetString(digestedContent) }));
			org.bouncycastle.asn1.ASN1Set signedAttributes = new org.bouncycastle.asn1.DLSet(
					new org.bouncycastle.asn1.ASN1Encodable[] {
							contentTypeAttribute.toASN1Primitive(), messageDigestAttribute.toASN1Primitive() });

			byte[] dataToBeSigned = signedAttributes.getEncoded("DER");
			Signature signer = Signature.getInstance("SHA256withRSA");
			signer.initSign(privateKey);
			signer.update(dataToBeSigned);
			byte[] signatureBytes = signer.sign();

			javax.security.auth.x500.X500Principal issuerPrincipal = cert.getIssuerX500Principal();
			org.bouncycastle.asn1.x500.X500Name issuerName = new X500Name(issuerPrincipal.getName(javax.security.auth.x500.X500Principal.RFC2253));
			org.bouncycastle.asn1.cms.SignerIdentifier sid = new org.bouncycastle.asn1.cms.SignerIdentifier(
					new org.bouncycastle.asn1.cms.IssuerAndSerialNumber(issuerName, cert.getSerialNumber()));
			org.bouncycastle.asn1.cms.SignerInfo signerInfo = new org.bouncycastle.asn1.cms.SignerInfo(
					sid, digestAlgId, signedAttributes,
					new AlgorithmIdentifier(new ASN1ObjectIdentifier(SIGNATURE_OID)),
					new org.bouncycastle.asn1.DEROctetString(signatureBytes),
					null);

			ASN1Sequence certSequence = ASN1Sequence.getInstance(ASN1Primitive.fromByteArray(cert.getEncoded()));
			org.bouncycastle.asn1.ASN1Set certificates = new org.bouncycastle.asn1.DLSet(
					new org.bouncycastle.asn1.ASN1Encodable[] { certSequence });

			org.bouncycastle.asn1.cms.SignedData signedData = new org.bouncycastle.asn1.cms.SignedData(
					new org.bouncycastle.asn1.DLSet(new org.bouncycastle.asn1.ASN1Encodable[] {
							new org.bouncycastle.asn1.DERSequence(new ASN1ObjectIdentifier(DIGEST_OID)) }),
					contentInfo,
					certificates,
					null,
					new org.bouncycastle.asn1.DLSet(new org.bouncycastle.asn1.ASN1Encodable[] { signerInfo.toASN1Primitive() }));

			org.bouncycastle.asn1.ASN1EncodableVector v = new org.bouncycastle.asn1.ASN1EncodableVector();
			v.add(new ASN1ObjectIdentifier(RFC_3369_SIGNED_DATA_OID));
			v.add(new DERTaggedObject(0, signedData));
			byte[] fileContents = new DERSequence(v).getEncoded("DER");

			java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			net.sf.scuba.tlv.TLVOutputStream tlvOut = new net.sf.scuba.tlv.TLVOutputStream(out);
			tlvOut.writeTag(0x77);
			tlvOut.writeValue(fileContents);
			tlvOut.flush();
			return out.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
