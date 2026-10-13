/*
 * Copyright (c) 2026 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.serviceregistry.components;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * The SECOM v2 Test Certificates Utility.
 * <p/>
 * This utility class generates a small test PKI, consisting of a self-signed
 * EC root CA certificate and an EC leaf certificate issued by it, and is able
 * to persist those as PKCS12 key and trust stores, so that the SECOM v2
 * provider components can be tested against real store files.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
final class SecomV2TestCertificates {

    /**
     * The certificate signature algorithm used throughout the test PKI.
     */
    static final String SIGNATURE_ALGORITHM = "SHA384withECDSA";

    /**
     * The password used for all the generated test stores.
     */
    static final String STORE_PASSWORD = "password";

    /**
     * The alias of the root CA certificate in the generated trust stores.
     */
    static final String ROOT_ALIAS = "root";

    /**
     * The alias of the signing identity in the generated key stores.
     */
    static final String SIGNING_ALIAS = "signing";

    /**
     * Utility class - no instantiation.
     */
    private SecomV2TestCertificates() {
    }

    /**
     * Generates a new EC key pair on the P-384 curve.
     *
     * @return the generated key pair
     * @throws Exception if the key pair generation fails
     */
    static KeyPair generateKeyPair() throws Exception {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(384);
        return kpg.generateKeyPair();
    }

    /**
     * Generates a self-signed certificate for the provided key pair.
     *
     * @param keyPair   the key pair of the certificate
     * @param cn        the common name of the certificate subject
     * @return the generated self-signed certificate
     * @throws Exception if the certificate generation fails
     */
    static X509Certificate createSelfSignedCertificate(KeyPair keyPair, String cn) throws Exception {
        return createCertificate(keyPair, cn, keyPair.getPrivate(), cn);
    }

    /**
     * Generates a certificate for the provided key pair, issued and signed by
     * the provided issuer.
     *
     * @param keyPair       the key pair of the certificate
     * @param subjectCn     the common name of the certificate subject
     * @param issuerKey     the private key of the issuer
     * @param issuerCn      the common name of the issuer
     * @return the generated certificate
     * @throws Exception if the certificate generation fails
     */
    static X509Certificate createCertificate(KeyPair keyPair,
                                             String subjectCn,
                                             PrivateKey issuerKey,
                                             String issuerCn) throws Exception {
        final long now = System.currentTimeMillis();
        final X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                new X500Name("CN=" + issuerCn),
                BigInteger.valueOf(now),
                new Date(now - 60_000),
                new Date(now + 86_400_000L),
                new X500Name("CN=" + subjectCn),
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        );
        final ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(issuerKey);
        return new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));
    }

    /**
     * Writes a PKCS12 key store containing a single private key entry, along
     * with its certificate chain, under the {@link #SIGNING_ALIAS} alias.
     *
     * @param path          the path to write the key store to
     * @param privateKey    the private key of the entry
     * @param chain         the certificate chain of the entry
     * @return the path the key store was written to
     * @throws Exception if the key store generation fails
     */
    static Path writeKeyStore(Path path, PrivateKey privateKey, Certificate... chain) throws Exception {
        final KeyStore ks = emptyStore();
        ks.setKeyEntry(SIGNING_ALIAS, privateKey, STORE_PASSWORD.toCharArray(), chain);
        return store(ks, path);
    }

    /**
     * Writes a PKCS12 trust store containing a single trusted certificate
     * entry under the provided alias.
     *
     * @param path          the path to write the trust store to
     * @param alias         the alias of the trusted certificate
     * @param certificate   the trusted certificate
     * @return the path the trust store was written to
     * @throws Exception if the trust store generation fails
     */
    static Path writeTrustStore(Path path, String alias, Certificate certificate) throws Exception {
        final KeyStore ks = emptyStore();
        ks.setCertificateEntry(alias, certificate);
        return store(ks, path);
    }

    /**
     * Creates a new empty PKCS12 store.
     *
     * @return the empty store
     * @throws Exception if the store cannot be initialised
     */
    private static KeyStore emptyStore() throws Exception {
        final KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        return ks;
    }

    /**
     * Persists the provided store to the provided path.
     *
     * @param ks    the store to be persisted
     * @param path  the path to persist the store to
     * @return the path the store was written to
     * @throws Exception if the store cannot be written
     */
    private static Path store(KeyStore ks, Path path) throws Exception {
        try (OutputStream os = Files.newOutputStream(path)) {
            ks.store(os, STORE_PASSWORD.toCharArray());
        }
        return path;
    }

}
