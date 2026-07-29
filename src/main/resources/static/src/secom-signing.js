/******************************************************************************
 *                        SECOM V2 ENVELOPE SIGNING                            *
 *                                                                            *
 * SECOM v2 requires the request bodies of the service interfaces to be       *
 * wrapped into an envelope, which is signed by the requesting party. This     *
 * facility allows the front-end to load a PKCS#12 keystore, and use the       *
 * contained identity to sign the SECOM envelopes before they are submitted.   *
 *                                                                            *
 * The PKCS#12 parsing is performed using node-forge, while the actual signing *
 * is delegated to the browser's native Web Crypto API.                        *
 ******************************************************************************/

const SecomSigning = (function () {

    /**
     * The currently loaded signing identity, or null if no keystore has been
     * loaded yet. It contains the certificate chain (as minified PEMs), the
     * imported private key and the signature algorithm parameters.
     */
    let identity = null;

    /**
     * The X.509 signature algorithm OIDs, mapped onto the Web Crypto
     * parameters that produce an equivalent signature.
     */
    const SIGNATURE_ALGORITHM_OIDS = {
        '1.2.840.10045.4.3.2': {name: 'ECDSA', hash: 'SHA-256'},
        '1.2.840.10045.4.3.3': {name: 'ECDSA', hash: 'SHA-384'},
        '1.2.840.10045.4.3.4': {name: 'ECDSA', hash: 'SHA-512'},
        '1.2.840.113549.1.1.11': {name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256'},
        '1.2.840.113549.1.1.12': {name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-384'},
        '1.2.840.113549.1.1.13': {name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-512'}
    };

    /**
     * The elliptic curve OIDs supported by the Web Crypto API.
     */
    const EC_CURVE_OIDS = {
        '1.2.840.10045.3.1.7': 'P-256',
        '1.3.132.0.34': 'P-384',
        '1.3.132.0.35': 'P-521'
    };

    const EC_KEY_OID = '1.2.840.10045.2.1';

    /**
     * The order in which the SECOM SearchParameters attributes are combined
     * into the signature CSV string. This has to match the attribute array of
     * the SearchParameters class of the SECOM library exactly, otherwise the
     * generated signature will not be verifiable by the server.
     */
    const SEARCH_PARAMETER_ORDER = ['name', 'status', 'version', 'keywords',
        'description', 'dataProductType', 'specificationId', 'designId',
        'instanceId', 'mmsi', 'imo', 'serviceType', 'unlocode', 'endpointUri'];

    /**
     * Converts a forge ASN.1 object into its DER binary string representation.
     *
     * @param {Object} asn1Object       The forge ASN.1 object to be converted
     * @return {string} the DER binary string representation
     */
    function toDerBytes(asn1Object) {
        return forge.asn1.toDer(asn1Object).getBytes();
    }

    /**
     * SECOM requires the certificates to be minified, i.e. stripped of their
     * PEM headers and line-feeds, which leaves just the Base64 encoding of the
     * DER representation of the certificate.
     *
     * @param {Object} certAsn1         The forge ASN.1 object of the certificate
     * @return {string} the minified PEM representation of the certificate
     */
    function toMinifiedPem(certAsn1) {
        return forge.util.encode64(toDerBytes(certAsn1));
    }

    /**
     * Generates the SHA-256 thumbprint of the provided certificate. Note that
     * the SECOM library generates the thumbprints in lower case hex, and
     * compares them verbatim, so the case here is important.
     *
     * @param {Object} certAsn1         The forge ASN.1 object of the certificate
     * @return {string} the lower case hex SHA-256 thumbprint of the certificate
     */
    function getCertThumbprint(certAsn1) {
        const md = forge.md.sha384.create();
        md.update(toDerBytes(certAsn1));
        return md.digest().toHex();
    }

    /**
     * Returns the TBSCertificate of a certificate, alongside the offset that
     * its fields are found at. The offset depends on whether the optional and
     * explicitly tagged version field is present or not.
     *
     * @param {Object} certAsn1         The forge ASN.1 object of the certificate
     * @return {Object} the TBS certificate and the offset of its fields
     */
    function getTbsCertificate(certAsn1) {
        const tbs = certAsn1.value[0];
        const versioned = tbs.value[0].tagClass === forge.asn1.Class.CONTEXT_SPECIFIC;
        return {tbs: tbs, offset: versioned ? 1 : 0};
    }

    /**
     * Returns the DER representation of the issuer and the subject names of the
     * provided certificate, so that the certificates of a chain can be linked
     * with each other.
     *
     * @param {Object} certAsn1         The forge ASN.1 object of the certificate
     * @return {Object} the DER encoded issuer and subject names
     */
    function getCertNames(certAsn1) {
        const tbsInfo = getTbsCertificate(certAsn1);
        return {
            issuer: toDerBytes(tbsInfo.tbs.value[tbsInfo.offset + 2]),
            subject: toDerBytes(tbsInfo.tbs.value[tbsInfo.offset + 4])
        };
    }

    /**
     * Returns the signature algorithm OID of the provided certificate, i.e. the
     * algorithm that the issuing CA used to sign it.
     *
     * @param {Object} certAsn1         The forge ASN.1 object of the certificate
     * @return {string} the signature algorithm OID of the certificate
     */
    function getCertSignatureAlgorithmOid(certAsn1) {
        return forge.asn1.derToOid(certAsn1.value[1].value[0].value);
    }

    /**
     * Extracts the MRN of the identity from a certificate, which in the MCP is
     * carried by the UID attribute of the certificate subject.
     *
     * @param {Object} certAsn1         The forge ASN.1 object of the certificate
     * @return {string} the MRN of the certificate subject, if it can be found
     */
    function getCertMrn(certAsn1) {
        const tbsInfo = getTbsCertificate(certAsn1);
        const subject = tbsInfo.tbs.value[tbsInfo.offset + 4];
        for (const rdn of subject.value) {
            for (const attribute of rdn.value) {
                if (forge.asn1.derToOid(attribute.value[0].value) === '0.9.2342.19200300.100.1.1') {
                    return attribute.value[1].value;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the ASN.1 object of a certificate bag. Note that node-forge is
     * only able to parse RSA certificates, so for the elliptic curve ones that
     * the MCP is using, it falls back onto providing the raw ASN.1 object.
     *
     * @param {Object} certBag          The PKCS#12 certificate bag
     * @return {Object} the forge ASN.1 object of the contained certificate
     */
    function getCertBagAsn1(certBag) {
        return certBag.asn1 ? certBag.asn1 : forge.pki.certificateToAsn1(certBag.cert);
    }

    /**
     * Retrieves the DER representation of the PKCS#8 private key info of a key
     * bag. For the elliptic curve keys node-forge cannot parse the key, but it
     * does provide the decrypted ASN.1 object, which is what we are after here,
     * since the Web Crypto API can import it directly.
     *
     * @param {Object} keyBag           The PKCS#12 key bag
     * @return {Uint8Array} the DER encoded PKCS#8 private key info
     */
    function getPrivateKeyInfo(keyBag) {
        const keyInfoAsn1 = keyBag.asn1 ? keyBag.asn1
            : forge.pki.wrapRsaPrivateKey(forge.pki.privateKeyToAsn1(keyBag.key));
        const der = toDerBytes(keyInfoAsn1);
        const bytes = new Uint8Array(der.length);
        for (let i = 0; i < der.length; i++) {
            bytes[i] = der.charCodeAt(i);
        }
        return {der: bytes, asn1: keyInfoAsn1};
    }

    /**
     * Imports the PKCS#8 private key of the keystore into the Web Crypto API,
     * using the parameters dictated by the signature algorithm of the loaded
     * certificate.
     *
     * @param {Object} keyInfo          The DER and ASN.1 of the PKCS#8 private key info
     * @param {Object} algorithm        The Web Crypto signature algorithm parameters
     * @return {Promise<CryptoKey>} the imported private key
     */
    function importPrivateKey(keyInfo, algorithm) {
        // The key algorithm identifier tells us what we are dealing with
        const keyAlgorithm = keyInfo.asn1.value[1];
        const keyOid = forge.asn1.derToOid(keyAlgorithm.value[0].value);

        // For the elliptic curve keys we also need to pick up the used curve
        if (keyOid === EC_KEY_OID) {
            const curveOid = forge.asn1.derToOid(keyAlgorithm.value[1].value);
            const namedCurve = EC_CURVE_OIDS[curveOid];
            if (!namedCurve) {
                return Promise.reject(new Error(`The elliptic curve ${curveOid} of the provided keystore is not supported`));
            }
            if (algorithm.name !== 'ECDSA') {
                return Promise.reject(new Error('The provided keystore contains an elliptic curve key, but its certificate was signed using RSA'));
            }
            return crypto.subtle.importKey('pkcs8', keyInfo.der,
                {name: 'ECDSA', namedCurve: namedCurve}, false, ['sign']);
        }

        // Otherwise fall back onto RSA
        if (algorithm.name !== 'RSASSA-PKCS1-v1_5') {
            return Promise.reject(new Error('The provided keystore contains an RSA key, but its certificate was signed using an elliptic curve algorithm'));
        }
        return crypto.subtle.importKey('pkcs8', keyInfo.der,
            {name: 'RSASSA-PKCS1-v1_5', hash: algorithm.hash}, false, ['sign']);
    }

    /**
     * The Web Crypto API generates the ECDSA signatures in the raw IEEE P1363
     * format, i.e. the concatenation of the R and S values. Java however
     * expects them to be encoded as an ASN.1 DER sequence, so a conversion is
     * required here.
     *
     * @param {Uint8Array} raw          The raw IEEE P1363 ECDSA signature
     * @return {Uint8Array} the DER encoded ECDSA signature
     */
    function ecdsaRawSignatureToDer(raw) {
        const half = raw.length / 2;
        const asn1 = forge.asn1.create(forge.asn1.Class.UNIVERSAL, forge.asn1.Type.SEQUENCE, true, [
            toAsn1Integer(raw.subarray(0, half)),
            toAsn1Integer(raw.subarray(half))
        ]);
        const der = toDerBytes(asn1);
        const bytes = new Uint8Array(der.length);
        for (let i = 0; i < der.length; i++) {
            bytes[i] = der.charCodeAt(i);
        }
        return bytes;
    }

    /**
     * Encodes an unsigned big-endian value as an ASN.1 INTEGER. Any leading
     * zeros are dropped, while a single zero byte is prepended if required, so
     * that the value is not misinterpreted as a negative number.
     *
     * @param {Uint8Array} value        The unsigned big-endian value
     * @return {Object} the forge ASN.1 INTEGER object
     */
    function toAsn1Integer(value) {
        let start = 0;
        while (start < value.length - 1 && value[start] === 0) {
            start++;
        }
        let bytes = Array.from(value.subarray(start));
        if (bytes[0] & 0x80) {
            bytes.unshift(0);
        }
        return forge.asn1.create(forge.asn1.Class.UNIVERSAL, forge.asn1.Type.INTEGER, false,
            String.fromCharCode.apply(null, bytes));
    }

    /**
     * Converts a byte array into its upper case hex string representation, as
     * expected by the SECOM envelope signature field.
     *
     * @param {Uint8Array} bytes        The bytes to be converted
     * @return {string} the upper case hex representation of the bytes
     */
    function toHex(bytes) {
        return Array.from(bytes)
            .map(b => b.toString(16).padStart(2, '0'))
            .join('')
            .toUpperCase();
    }

    /**
     * Orders the certificates of the keystore into a chain, starting with the
     * provided leaf certificate and following the issuer links. Note that the
     * self-signed root certificate is deliberately left out of the chain, since
     * the Java PKIX validation of the server expects it to be provided as a
     * trust anchor, and not as part of the certificate path.
     *
     * @param {Object} leaf             The leaf certificate entry
     * @param {Array} certificates      All the certificate entries of the keystore
     * @return {Object} the ordered certificate chain and the root certificate
     */
    function buildCertificateChain(leaf, certificates) {
        const chain = [];
        const root = certificates.find(c => c.names.issuer === c.names.subject) || null;
        let current = leaf;

        // Follow the issuer links to build up the chain
        while (current && !chain.includes(current)) {
            // The self-signed root is a trust anchor, not part of the chain
            if (current.names.issuer === current.names.subject) {
                break;
            }
            chain.push(current);
            current = certificates.find(c => c !== current && c.names.subject === current.names.issuer);
        }

        return {chain: chain, root: root};
    }

    /**
     * Loads a PKCS#12 keystore and prepares the contained identity, so that it
     * can be used to sign the SECOM envelopes.
     *
     * @param {File} file               The PKCS#12 keystore file
     * @param {string} password         The password of the keystore
     * @return {Promise<Object>} the details of the loaded signing identity
     */
    async function loadKeystore(file, password) {
        // Parse the keystore into a forge PKCS#12 object. Note that forge
        // operates on binary strings, so the raw bytes have to be encoded
        // before they can be handed over to it.
        const buffer = await file.arrayBuffer();
        const binary = forge.util.createBuffer(forge.util.binary.raw.encode(new Uint8Array(buffer)));
        const p12Asn1 = forge.asn1.fromDer(binary);
        const p12 = forge.pkcs12.pkcs12FromAsn1(p12Asn1, password);

        // Collect the private key of the keystore
        const keyBags = []
            .concat(p12.getBags({bagType: forge.pki.oids.pkcs8ShroudedKeyBag})[forge.pki.oids.pkcs8ShroudedKeyBag] || [])
            .concat(p12.getBags({bagType: forge.pki.oids.keyBag})[forge.pki.oids.keyBag] || []);
        if (keyBags.length === 0) {
            throw new Error('The provided keystore does not contain a private key');
        }
        const keyBag = keyBags[0];

        // And now collect all the certificates it contains
        const certBags = p12.getBags({bagType: forge.pki.oids.certBag})[forge.pki.oids.certBag] || [];
        if (certBags.length === 0) {
            throw new Error('The provided keystore does not contain any certificates');
        }
        const certificates = certBags.map(bag => {
            const asn1 = getCertBagAsn1(bag);
            return {
                asn1: asn1,
                names: getCertNames(asn1),
                localKeyId: bag.attributes && bag.attributes.localKeyId ? bag.attributes.localKeyId[0] : null
            };
        });

        // The certificate of the private key is the leaf of the chain. It is
        // linked to the key through the local key ID, but if that is not
        // available, fall back onto the first certificate that is not a CA.
        const keyLocalId = keyBag.attributes && keyBag.attributes.localKeyId ? keyBag.attributes.localKeyId[0] : null;
        const leaf = (keyLocalId ? certificates.find(c => c.localKeyId === keyLocalId) : null)
            || certificates.find(c => c.names.issuer !== c.names.subject)
            || certificates[0];

        // Build the certificate chain and pick up the root certificate
        const chainInfo = buildCertificateChain(leaf, certificates);

        // The server verifies the envelope signature using the algorithm that
        // the leaf certificate itself was signed with, so we have to match it.
        const signatureAlgorithmOid = getCertSignatureAlgorithmOid(leaf.asn1);
        const algorithm = SIGNATURE_ALGORITHM_OIDS[signatureAlgorithmOid];
        if (!algorithm) {
            throw new Error(`The signature algorithm ${signatureAlgorithmOid} of the provided certificate is not supported`);
        }

        // Finally import the private key so that we can sign with it
        const privateKey = await importPrivateKey(getPrivateKeyInfo(keyBag), algorithm);

        // And keep hold of the prepared identity
        identity = {
            privateKey: privateKey,
            algorithm: algorithm,
            certificates: chainInfo.chain.map(c => toMinifiedPem(c.asn1)),
            rootThumbprint: chainInfo.root ? getCertThumbprint(chainInfo.root.asn1) : null,
            mrn: getCertMrn(leaf.asn1)
        };

        return {
            mrn: identity.mrn,
            chainLength: identity.certificates.length,
            rootThumbprint: identity.rootThumbprint,
            algorithm: `${algorithm.hash} with ${algorithm.name}`
        };
    }

    /**
     * Clears out any previously loaded signing identity.
     */
    function clearKeystore() {
        identity = null;
    }

    /**
     * Whether a signing identity has been loaded and envelopes can be signed.
     *
     * @return {boolean} whether a signing identity is available
     */
    function isLoaded() {
        return identity !== null;
    }

    /**
     * Converts a single envelope attribute onto its string representation, as
     * described in section 7.3.4 of the SECOM standard.
     *
     * @param {*} attribute             The attribute to be converted
     * @return {string} the string representation of the attribute
     */
    function attributeConversion(attribute) {
        if (attribute === null || attribute === undefined) {
            return '';
        } else if (Array.isArray(attribute)) {
            return `[${attribute.join(', ')}]`;
        }
        return String(attribute);
    }

    /**
     * Generates the CSV string representation of the SECOM search parameters,
     * which is used as part of the envelope signature payload.
     *
     * @param {Object} query            The SECOM search parameters
     * @return {string} the CSV string representation of the search parameters
     */
    function getSearchParametersCsvString(query) {
        return SEARCH_PARAMETER_ORDER
            .map(field => attributeConversion(query[field]))
            .join('.');
    }

    /**
     * Generates the CSV string representation of the signature attributes that
     * are shared by all the SECOM envelopes. These always come last in the
     * attribute array of the envelopes.
     *
     * @param {Object} envelope         The SECOM envelope
     * @param {number} signatureTime    The signature time in epoch seconds
     * @return {Array} the CSV attributes shared by all the SECOM envelopes
     */
    function getCommonCsvAttributes(envelope, signatureTime) {
        return [
            attributeConversion(envelope.envelopeSignatureCertificate),
            attributeConversion(envelope.envelopeRootCertificateThumbprint),
            attributeConversion(signatureTime)
        ];
    }

    /**
     * Generates the CSV string representation of the SECOM search filter
     * envelope, which constitutes the payload of the envelope signature. The
     * attribute order has to match the attribute array of the
     * EnvelopeSearchFilterObject class of the SECOM library exactly.
     *
     * @param {Object} envelope         The SECOM search filter envelope
     * @param {number} signatureTime    The signature time in epoch seconds
     * @return {string} the CSV string representation of the envelope
     */
    function getSearchFilterEnvelopeCsvString(envelope, signatureTime) {
        return [
            envelope.query ? getSearchParametersCsvString(envelope.query) : '',
            attributeConversion(envelope.geometry),
            attributeConversion(envelope.localOnly)
        ].concat(getCommonCsvAttributes(envelope, signatureTime)).join('.');
    }

    /**
     * Generates the CSV string representation of the SECOM retrieve result
     * envelope, which constitutes the payload of the envelope signature. The
     * attribute order has to match the attribute array of the
     * EnvelopeRetrieveResultObject class of the SECOM library exactly.
     *
     * @param {Object} envelope         The SECOM retrieve result envelope
     * @param {number} signatureTime    The signature time in epoch seconds
     * @return {string} the CSV string representation of the envelope
     */
    function getRetrieveResultEnvelopeCsvString(envelope, signatureTime) {
        return [attributeConversion(envelope.transactionId)]
            .concat(getCommonCsvAttributes(envelope, signatureTime)).join('.');
    }

    /**
     * Populates the signature fields of the provided SECOM envelope using the
     * loaded signing identity, and signs it. The result is a complete SECOM
     * envelope signature bearer object, that can be submitted to the
     * respective SECOM v2 service interface.
     *
     * @param {Object} envelope         The SECOM envelope to be signed
     * @param {Function} csvGenerator   The CSV string generator of the envelope
     * @return {Promise<Object>} the signed SECOM envelope signature bearer object
     */
    async function signEnvelope(envelope, csvGenerator) {
        if (!isLoaded()) {
            throw new Error('No signing keystore has been loaded');
        }
        if (!identity.rootThumbprint) {
            throw new Error('The root certificate could not be determined from the provided keystore');
        }

        // The signature time is only accurate to the second, since that is how
        // it gets encoded into the signature payload
        const signatureTime = Math.floor(Date.now() / 1000);

        // Complete the envelope with the signature information
        const signedEnvelope = Object.assign({}, envelope, {
            envelopeSignatureCertificate: identity.certificates,
            envelopeRootCertificateThumbprint: identity.rootThumbprint,
            envelopeSignatureTime: new Date(signatureTime * 1000).toISOString().replace(/\.\d+Z$/, 'Z')
        });

        // Generate the signature over the CSV representation of the envelope
        const payload = new TextEncoder().encode(csvGenerator(signedEnvelope, signatureTime));
        const signature = new Uint8Array(await crypto.subtle.sign(
            identity.algorithm.name === 'ECDSA'
                ? {name: 'ECDSA', hash: identity.algorithm.hash}
                : {name: 'RSASSA-PKCS1-v1_5'},
            identity.privateKey,
            payload));

        // And build up the signed SECOM envelope signature bearer object
        return {
            envelope: signedEnvelope,
            envelopeSignature: toHex(identity.algorithm.name === 'ECDSA'
                ? ecdsaRawSignatureToDer(signature)
                : signature)
        };
    }

    /**
     * Signs a SECOM search filter envelope, so that it can be submitted to the
     * SECOM v2 searchService interface.
     *
     * @param {Object} envelope         The SECOM search filter envelope to be signed
     * @return {Promise<Object>} the signed SECOM search filter object
     */
    function signSearchFilterObject(envelope) {
        return signEnvelope(envelope, getSearchFilterEnvelopeCsvString);
    }

    /**
     * Signs a SECOM retrieve result envelope for the provided transaction, so
     * that it can be submitted to the SECOM v2 retrieveResult interface.
     *
     * @param {string} transactionId    The transaction to retrieve the results of
     * @return {Promise<Object>} the signed SECOM retrieve result object
     */
    function signRetrieveResultObject(transactionId) {
        return signEnvelope({transactionId: transactionId}, getRetrieveResultEnvelopeCsvString);
    }

    // Expose the public facility functions
    return {
        loadKeystore: loadKeystore,
        clearKeystore: clearKeystore,
        isLoaded: isLoaded,
        signSearchFilterObject: signSearchFilterObject,
        signRetrieveResultObject: signRetrieveResultObject
    };
})();
/******************************************************************************/
