package net.maritimeconnectivity.serviceregistry.utils;

import lombok.extern.slf4j.Slf4j;
import org.grad.secomv2.core.utils.SecomPemUtils;
import org.springframework.stereotype.Component;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Component
@Slf4j
public class CertificateParsingUtil {


    public String getMrnFromCertificate(String[] certificate) {

        //TODO FIND proper ifx

        //Extract first cert
        String cert = certificate[0];

        try {
            X509Certificate parsedCert = SecomPemUtils.getCertFromPem(cert);

            LdapName ldapDN = new LdapName(parsedCert.getSubjectX500Principal().getName());
            for (Rdn rdn : ldapDN.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("UID")) {
                    return rdn.getValue().toString();
                }
            }
        } catch (CertificateException | InvalidNameException ex) {
            log.error("Unable to parse MRN from certificate", ex);
        }
        return null;

    }


}
