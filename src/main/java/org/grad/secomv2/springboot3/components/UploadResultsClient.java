package org.grad.secomv2.springboot3.components;

import org.grad.secomv2.core.models.SearchObjectResult;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

public class UploadResultsClient extends SecomClient {

    public UploadResultsClient(URL url, SecomConfigProperties config) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        super(url, config);
    }


    public HttpStatusCode uploadResults(String endpoint, List<SearchObjectResult> searchResults) {
        try {
            ResponseEntity<Void> entity = this.secomClient
                    .post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(searchResults))
                    .retrieve()
                    .toBodilessEntity()
                    .block(); //Waits for response

            return entity.getStatusCode();
        } catch (WebClientResponseException e) {
            return e.getStatusCode();
        }
    }
}
