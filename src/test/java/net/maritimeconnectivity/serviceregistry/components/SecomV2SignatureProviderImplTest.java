package net.maritimeconnectivity.serviceregistry.components;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

import static org.junit.Assert.*;

public class SecomV2SignatureProviderImplTest {

    private SecomV2SignatureProviderImpl provider;
    private KeyPair keyPair;
    private X509Certificate certificate;
    private String pemCertificate;

    @Before
    public void setUp() throws Exception {
        provider = new SecomV2SignatureProviderImpl();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(384);
        keyPair = kpg.generateKeyPair();

        certificate = createSelfSignedCertificate(keyPair);
        pemCertificate = toMinifiedCert(certificate);

        Field field = SecomV2SignatureProviderImpl.class.getDeclaredField("privateKey");
        field.setAccessible(true);
        field.set(provider, keyPair.getPrivate());
    }

    @Test
    public void generatedValidSignatureShouldPassValidation() {
        byte[] payload = "hello secom".getBytes(StandardCharsets.UTF_8);

        byte[] signature = provider.generateSignature(
                null,
                provider.getSignatureAlgorithm(),
                payload
        );


        assertNotNull(signature);

        assertTrue(provider.validateSignature(
                new String[]{pemCertificate},
                provider.getSignatureAlgorithm(),
                signature,
                payload
        ));
    }

    @Test
    public void alteredSignatureShouldFail() {
        byte[] payload = "hello secom".getBytes(StandardCharsets.UTF_8);

        byte[] signature = provider.generateSignature(
                null,
                provider.getSignatureAlgorithm(),
                payload
        );

        // alter a single byte in the signature to simulate tampering
        signature[1] = (byte) 'A';

        assertNotNull(signature);

        assertFalse(provider.validateSignature(
                new String[]{pemCertificate},
                provider.getSignatureAlgorithm(),
                signature,
                payload
        ));
    }

    private static X509Certificate createSelfSignedCertificate(KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - 60_000);
        Date notAfter = new Date(now + 86400000L);

        X500Name dn = new X500Name("CN=Test");
        BigInteger serial = BigInteger.valueOf(now);

        SubjectPublicKeyInfo subjectPublicKeyInfo =
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                dn, serial, notBefore, notAfter, dn, subjectPublicKeyInfo
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA384withECDSA")
                .build(keyPair.getPrivate());

        X509CertificateHolder holder = certBuilder.build(signer);

        return new JcaX509CertificateConverter()
                .getCertificate(holder);
    }

    private static String toMinifiedCert(X509Certificate certificate) throws Exception {
        return Base64.getEncoder().encodeToString(certificate.getEncoded());
    }
}