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

}