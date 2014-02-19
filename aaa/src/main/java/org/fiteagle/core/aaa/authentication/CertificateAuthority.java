package org.fiteagle.core.aaa.authentication;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertificateAuthority {

	private static CertificateAuthority CA = null;

	public static CertificateAuthority getInstance() {
		if (CA == null)
			CA = new CertificateAuthority();
		return CA;
	}

	private CertificateAuthority() {
	}

	private KeyStoreManagement keyStoreManagement = KeyStoreManagement
			.getInstance();

//	public X509Certificate createCertificate(User newUser, PublicKey publicKey)
//			throws Exception {
//		X509Certificate caCert = keyStoreManagement.getCACert();
//		X500Name issuer = new JcaX509CertificateHolder(caCert).getSubject();
//		PrivateKey caPrivateKey = keyStoreManagement.getCAPrivateKey();
//		ContentSigner contentsigner = new JcaContentSignerBuilder(
//				"SHA1WithRSAEncryption").build(caPrivateKey);
//
//		X500Name subject = createX500Name(newUser);
//		SubjectPublicKeyInfo subjectsPublicKeyInfo = getPublicKey(publicKey);
//		X509v3CertificateBuilder ca_gen = new X509v3CertificateBuilder(issuer,
//				new BigInteger(new SecureRandom().generateSeed(256)),
//				new Date(),
//				new Date(System.currentTimeMillis() + 31500000000L), subject,
//				subjectsPublicKeyInfo);
//		BasicConstraints ca_constraint = new BasicConstraints(false);
//		ca_gen.addExtension(X509Extension.basicConstraints, true, ca_constraint);
//		GeneralNames subjectAltName = new GeneralNames(new GeneralName(
//				GeneralName.uniformResourceIdentifier, getURN(newUser)));
//
//		X509Extension extension = new X509Extension(false, new DEROctetString(
//				subjectAltName));
//		ca_gen.addExtension(X509Extension.subjectAlternativeName, false,
//				extension.getParsedValue());
//		X509CertificateHolder holder = (X509CertificateHolder) ca_gen
//				.build(contentsigner);
//		CertificateFactory cf = CertificateFactory.getInstance("X.509");
//		return (X509Certificate) cf
//				.generateCertificate(new ByteArrayInputStream(holder
//						.getEncoded()));
//	}
//
//	private String getURN(User newUser) {
//		InterfaceConfiguration config = InterfaceConfiguration.getInstance();
//		return URN.getURNFromUser(newUser).toString();
//	}

	public X509Certificate getSliceAuthorityCertificate() {
		try {
			return keyStoreManagement.getSliceAuthorityCert();
		} catch (KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException e) {
			throw new CertificateNotFoundException();
		}
	}

//	private SubjectPublicKeyInfo getPublicKey(PublicKey key) throws Exception {
//
//		SubjectPublicKeyInfo subPubInfo = new SubjectPublicKeyInfo(
//				(ASN1Sequence) ASN1Sequence.fromByteArray(key.getEncoded()));
//		return subPubInfo;
//	}

//	private X500Name createX500Name(User newUser) {
//		X500Principal prince = new X500Principal("CN=" + newUser.getUsername());
//		X500Name x500Name = new X500Name(prince.getName());
//		return x500Name;
//	}


	
	public class EncodeCertificateException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	public class CertificateNotFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

//	public X509Certificate createCertificate(Group g) throws Exception {
//	
//	
//		KeyManagement keyManager = KeyManagement.getInstance();
//		KeyPair keypair = keyManager.generateKeyPair();
//		X509Certificate caCert = keyStoreManagement.getCACert();
//		X500Name issuer = new JcaX509CertificateHolder(caCert).getSubject();
//		PrivateKey caPrivateKey = keyStoreManagement.getCAPrivateKey();
//		ContentSigner contentsigner = new JcaContentSignerBuilder(
//				"SHA1WithRSAEncryption").build(caPrivateKey);
//
//		X500Name subject = createX500Name(g);
//		SubjectPublicKeyInfo subjectsPublicKeyInfo = getPublicKey(keypair.getPublic());
//		X509v3CertificateBuilder ca_gen = new X509v3CertificateBuilder(issuer,
//				new BigInteger(new SecureRandom().generateSeed(256)),
//				new Date(),
//				new Date(System.currentTimeMillis() + 31500000000L), subject,
//				subjectsPublicKeyInfo);
//		BasicConstraints ca_constraint = new BasicConstraints(false);
//		ca_gen.addExtension(X509Extension.basicConstraints, true, ca_constraint);
//		GeneralNames subjectAltName = new GeneralNames(new GeneralName(
//				GeneralName.uniformResourceIdentifier, getURN(g)));
//
//		X509Extension extension = new X509Extension(false, new DEROctetString(
//				subjectAltName));
//		ca_gen.addExtension(X509Extension.subjectAlternativeName, false,
//				extension.getParsedValue());
//		X509CertificateHolder holder = (X509CertificateHolder) ca_gen
//				.build(contentsigner);
//		CertificateFactory cf = CertificateFactory.getInstance("X.509");
//		return (X509Certificate) cf
//				.generateCertificate(new ByteArrayInputStream(holder
//						.getEncoded()));
//	}
//
//	private String getURN(Group g) {
//		
//		return URN.getURNFromGroup(g).toString();
//	}
//
//	private X500Name createX500Name(Group g) {
//		// TODO Auto-generated method stub
//		return new X500Name("cn="+ g.getGroupId());
//	}

}
