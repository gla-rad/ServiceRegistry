
//This is just a nasty namespace hack to access the WebClient secomClient in the derived class
package org.grad.secomv2.springboot3.components;

import lombok.extern.slf4j.Slf4j;
import org.grad.secomv2.core.models.ServiceInstanceObject;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;

@Slf4j
public class UploadResultsClient extends SecomClient {

    public UploadResultsClient(URL url, SecomConfigProperties config) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        super(url, config);
        log.info("Initializing UploadResultsClient with URL: {}", url);
    }

    //
    public HttpStatusCode uploadResults(List<ServiceInstanceObject> searchResults) {
        ResponseEntity<Void> entity = this.secomClient
                .post()
                .uri("")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(searchResults)
                .exchangeToMono(response -> response.toBodilessEntity())
                .block();

        assert entity != null;
        return entity.getStatusCode();
    }

}
