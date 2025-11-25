package net.maritimeconnectivity.serviceregistry.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Set;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

@Slf4j
@Component
public class KeyStoreUtil {

    @Value("${info.mms.ownEdgerouter.keyStore.path}")
    private String mmsKeystorePath;
    @Value("${info.mms.ownEdgerouter.keyStore.password}")
    private String mmsKeystorePassword;

    public KeyStore getMmsKeystore() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        return KeyStore.getInstance(new File(mmsKeystorePath), mmsKeystorePassword.toCharArray());
    }

    public char[] getMmsKeystorePassword() {
        return mmsKeystorePassword.toCharArray();
    }

    public String getOwnMrn() throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, InvalidNameException {
        KeyStore keystore = getMmsKeystore();
        String alias = keystore.aliases().nextElement(); // assumes only 1 cert
        Certificate cert = keystore.getCertificate(alias);

        if (cert instanceof X509Certificate) {
            X509Certificate x509 = (X509Certificate) cert;

            // Parse subject DN
            LdapName ldapDN = new LdapName(x509.getSubjectX500Principal().getName());
            for (Rdn rdn : ldapDN.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("UID")) {
                    return rdn.getValue().toString();
                }
            }
        }
        return null;
    }
}