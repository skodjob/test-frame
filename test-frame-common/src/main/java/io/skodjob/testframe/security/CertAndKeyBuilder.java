/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.skodjob.testframe.security;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static org.bouncycastle.asn1.x509.Extension.authorityKeyIdentifier;
import static org.bouncycastle.asn1.x509.Extension.basicConstraints;
import static org.bouncycastle.asn1.x509.Extension.extendedKeyUsage;
import static org.bouncycastle.asn1.x509.Extension.keyUsage;
import static org.bouncycastle.asn1.x509.Extension.subjectAlternativeName;
import static org.bouncycastle.asn1.x509.Extension.subjectKeyIdentifier;
import static org.bouncycastle.asn1.x509.GeneralName.dNSName;
import static org.bouncycastle.asn1.x509.KeyPurposeId.id_kp_clientAuth;
import static org.bouncycastle.asn1.x509.KeyPurposeId.id_kp_serverAuth;
import static org.bouncycastle.asn1.x509.KeyUsage.cRLSign;
import static org.bouncycastle.asn1.x509.KeyUsage.digitalSignature;
import static org.bouncycastle.asn1.x509.KeyUsage.keyCertSign;
import static org.bouncycastle.asn1.x509.KeyUsage.keyEncipherment;
import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

/**
 * Builder of certificates using java
 */
public class CertAndKeyBuilder {

    /**
     * Key size
     */
    public static final int KEY_SIZE = 2048;

    /**
     * Key pair algorithm
     */
    public static final String KEY_PAIR_ALGORITHM = "RSA";

    /**
     * Sign algorithm
     */
    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";

    /**
     * Default cert validity period
     */
    public static final Duration CERTIFICATE_VALIDITY_PERIOD = Duration.ofDays(30);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final KeyPair keyPair;
    private final CertAndKey caCert;
    private final List<Extension> extensions;
    private X500Name issuer;
    private X500Name subject;

    private CertAndKeyBuilder(KeyPair keyPair, CertAndKey caCert, List<Extension> extensions) {
        this.keyPair = keyPair;
        this.caCert = caCert;
        if (caCert != null) {
            try {
                this.issuer = new JcaX509CertificateHolder(caCert.getCertificate()).getSubject();
            } catch (CertificateEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        this.extensions = new ArrayList<>(extensions);
    }

    /**
     * Returns builder for root CA
     *
     * @return Returns builder for root CA
     */
    public static CertAndKeyBuilder rootCaCertBuilder() {
        KeyPair keyPair = generateKeyPair();
        return new CertAndKeyBuilder(
            keyPair,
            null,
            asList(
                new Extension(keyUsage, true, keyUsage(keyCertSign | cRLSign)),
                new Extension(basicConstraints, true, ca()),
                new Extension(subjectKeyIdentifier, false, createSubjectKeyIdentifier(keyPair.getPublic()))
            )
        );
    }

    /**
     * Returns builder for intermediate CA
     *
     * @param caCert ca certificate
     * @return Returns builder for intermediate CA
     */
    public static CertAndKeyBuilder intermediateCaCertBuilder(CertAndKey caCert) {
        KeyPair keyPair = generateKeyPair();
        return new CertAndKeyBuilder(
            keyPair,
            caCert,
            asList(
                new Extension(keyUsage, true, keyUsage(keyCertSign)),
                new Extension(basicConstraints, true, ca()),
                new Extension(subjectKeyIdentifier, false, createSubjectKeyIdentifier(keyPair.getPublic())),
                new Extension(authorityKeyIdentifier, false, createAuthorityKeyIdentifier(caCert.getPublicKey()))
            )
        );
    }

    /**
     * Returns builder for application cert
     *
     * @param caCert ca certificate
     * @return Returns builder for application cert
     */
    public static CertAndKeyBuilder appCaCertBuilder(CertAndKey caCert) {
        KeyPair keyPair = generateKeyPair();
        return new CertAndKeyBuilder(
            keyPair,
            caCert,
            asList(
                new Extension(basicConstraints, true, ca()),
                new Extension(subjectKeyIdentifier, false, createSubjectKeyIdentifier(keyPair.getPublic())),
                new Extension(authorityKeyIdentifier, false, createAuthorityKeyIdentifier(caCert.getPublicKey()))
            )
        );
    }

    /**
     * Returns builder for end entity cert
     *
     * @param caCert ca certificate
     * @return Returns builder for end entity cert
     */
    public static CertAndKeyBuilder endEntityCertBuilder(CertAndKey caCert) {
        KeyPair keyPair = generateKeyPair();
        return new CertAndKeyBuilder(
            keyPair,
            caCert,
            asList(
                new Extension(keyUsage, true, keyUsage(digitalSignature | keyEncipherment)),
                new Extension(extendedKeyUsage, false, extendedKeyUsage(id_kp_serverAuth, id_kp_clientAuth)),
                new Extension(basicConstraints, true, notCa()),
                new Extension(subjectKeyIdentifier, false, createSubjectKeyIdentifier(keyPair.getPublic())),
                new Extension(authorityKeyIdentifier, false, createAuthorityKeyIdentifier(caCert.getPublicKey()))
            )
        );
    }

    /**
     * Sets issues DN
     *
     * @param issuerDn issues DN
     * @return builder
     */
    public CertAndKeyBuilder withIssuerDn(String issuerDn) {
        this.issuer = new X500Name(issuerDn);
        return this;
    }

    /**
     * Sets subject DN
     *
     * @param subjectDn subject DN
     * @return builder
     */
    public CertAndKeyBuilder withSubjectDn(String subjectDn) {
        this.subject = new X500Name(subjectDn);
        return this;
    }

    /**
     * Sets san dns
     *
     * @param hostName hostname
     * @return builder
     */
    public CertAndKeyBuilder withSanDnsName(final String hostName) {
        final GeneralName dnsName = new GeneralName(dNSName, hostName);
        final byte[] subjectAltName = encode(GeneralNames.getInstance(new DERSequence(dnsName)));
        extensions.add(new Extension(subjectAlternativeName, false, subjectAltName));
        return this;
    }

    /**
     * Sets multiple san dns names
     *
     * @param sanDnsNames list of san dns names
     * @return builder
     */
    public CertAndKeyBuilder withSanDnsNames(final ASN1Encodable[] sanDnsNames) {
        final DERSequence subjectAlternativeNames = new DERSequence(sanDnsNames);
        final byte[] subjectAltName = encode(GeneralNames.getInstance(subjectAlternativeNames));
        extensions.add(new Extension(subjectAlternativeName, false, subjectAltName));

        return this;
    }

    /**
     * Returns cert and key in memory from builder
     *
     * @return Returns cert and key in memory
     */
    public CertAndKey build() {
        try {
            BigInteger certSerialNumber = BigInteger.valueOf(System.currentTimeMillis());
            ContentSigner contentSigner = createContentSigner();
            Instant startDate = Instant.now().minus(1, DAYS);
            Instant endDate = startDate.plus(CERTIFICATE_VALIDITY_PERIOD);
            JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                certSerialNumber,
                Date.from(startDate),
                Date.from(endDate),
                subject,
                keyPair.getPublic()
            );
            for (Extension extension : extensions) {
                certBuilder.addExtension(extension);
            }
            X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider(PROVIDER_NAME)
                .getCertificate(certBuilder.build(contentSigner));
            return new CertAndKey(certificate, keyPair.getPrivate());
        } catch (CertIOException | CertificateException | OperatorCreationException e) {
            throw new RuntimeException(e);
        }
    }

    private ContentSigner createContentSigner() throws OperatorCreationException {
        JcaContentSignerBuilder builder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM);
        if (caCert == null) {
            return builder.build(keyPair.getPrivate());
        } else {
            return builder.build(caCert.getPrivateKey());
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM, PROVIDER_NAME);
            keyPairGenerator.initialize(KEY_SIZE);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] keyUsage(int usage) {
        return encode(new KeyUsage(usage));
    }

    private static byte[] extendedKeyUsage(KeyPurposeId... usages) {
        return encode(new ExtendedKeyUsage(usages));
    }

    private static byte[] notCa() {
        return encode(new BasicConstraints(false));
    }

    private static byte[] ca() {
        return encode(new BasicConstraints(true));
    }

    private static byte[] createSubjectKeyIdentifier(PublicKey publicKey) {
        JcaX509ExtensionUtils extensionUtils = createExtensionUtils();
        return encode(extensionUtils.createSubjectKeyIdentifier(publicKey));
    }

    private static byte[] createAuthorityKeyIdentifier(PublicKey publicKey) {
        JcaX509ExtensionUtils extensionUtils = createExtensionUtils();
        return encode(extensionUtils.createAuthorityKeyIdentifier(publicKey));
    }

    private static byte[] encode(ASN1Object object) {
        try {
            return object.getEncoded();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static JcaX509ExtensionUtils createExtensionUtils() {
        try {
            return new JcaX509ExtensionUtils();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
