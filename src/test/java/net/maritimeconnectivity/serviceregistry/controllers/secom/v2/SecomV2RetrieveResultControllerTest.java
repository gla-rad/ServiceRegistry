package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import net.maritimeconnectivity.serviceregistry.TestingConfiguration;
import net.maritimeconnectivity.serviceregistry.components.SecomV2SignatureProviderImpl;
import net.maritimeconnectivity.serviceregistry.components.SecomV2SigningIdentityProvider;
import net.maritimeconnectivity.serviceregistry.components.SecomV2TrustStoreProviderImpl;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import net.maritimeconnectivity.serviceregistry.services.SecomSearchResultSigningService;
import net.maritimeconnectivity.serviceregistry.utils.CertificateParsingUtil;
import org.grad.secomv2.core.models.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.maritimeconnectivity.serviceregistry.controllers.secom.v2.RetrieveResultController.RETREIVE_RESULTS_INTERFACE_PATH;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
@Import(TestingConfiguration.class)
public class SecomV2RetrieveResultControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private SearchConsolidationService searchConsolidationService;

    @MockitoBean
    private SecomV2SignatureProviderImpl secomV2SignatureProvider;

    @MockitoBean
    private SecomV2SigningIdentityProvider secomV2SigningIdentityProvider;

    @MockitoBean
    private SecomV2TrustStoreProviderImpl secomV2TrustStoreProvider;

    @MockitoBean
    private org.grad.secomv2.core.components.SecomSignatureFilter secomSignatureFilter;

    @MockitoBean
    private SecomSearchResultSigningService secomSearchResultSigningService;

    @MockitoBean
    CertificateParsingUtil certificateParsingUtil;

    private RetrieveResultObject retrieveResultObject;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            EnvelopeSearchResultObject envelope = invocation.getArgument(0, EnvelopeSearchResultObject.class);

            SearchResult result = new SearchResult();
            result.setEnvelope(envelope);
            result.setEnvelopeSignature("TEST_SIGNATURE");

            return result;
        }).when(secomSearchResultSigningService)
                .signSearchResult(any(EnvelopeSearchResultObject.class));

        EnvelopeRetrieveResultObject envelopeSearchResultObject = new EnvelopeRetrieveResultObject();
        retrieveResultObject = new RetrieveResultObject();
        retrieveResultObject.setEnvelope(envelopeSearchResultObject);
        retrieveResultObject.setEnvelopeSignature("TEST_SIGNATURE");

    }

    @Test
    void testRetrieveResultForInvalidTransactionId() {
        String transactionId = "invalidTransactionId";
        String uid = "TESTMRN2";

        retrieveResultObject.getEnvelope().setTransactionId(transactionId);

        doReturn(uid)
                .when(certificateParsingUtil)
                .getMrnFromCertificate(any());

        doReturn(null)
                .when(searchConsolidationService)
                .getResults(eq(transactionId), eq(uid));

        webTestClient.post()
                .uri("/api/secom/" + RETREIVE_RESULTS_INTERFACE_PATH)
                .bodyValue(retrieveResultObject)
                .exchange()
                .expectStatus().isNotFound();

        verify(certificateParsingUtil)
                .getMrnFromCertificate(any());

        verify(searchConsolidationService)
                .getResults(eq(transactionId), eq(uid));
    }

    @Test
    void testRetrieveResultForValidTransactionId() {
        List<ServiceInstanceObject> validResults = new ArrayList<>();
        String validTransactionId = UUID.randomUUID().toString();
        String uid = "TESTMRN2";

        doReturn(uid)
                .when(certificateParsingUtil)
                .getMrnFromCertificate(any());


        //Setup test variable
        final ServiceInstanceObject resultInstance = new ServiceInstanceObject();
        resultInstance.setName("testName");
        validResults.add(resultInstance);

        this.retrieveResultObject.getEnvelope().setTransactionId(validTransactionId);

        // Mock the signature validation
        doReturn(true).when(this.secomV2SignatureProvider).validateSignature(any(),any(),any(),any());

        //Return the instance when calling getResults
        doReturn(validResults)
                .when(searchConsolidationService)
                .getResults(eq(validTransactionId), any());



        webTestClient.post()
                .uri("/api/secom/" + RETREIVE_RESULTS_INTERFACE_PATH)
                .bodyValue(retrieveResultObject)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SearchResult.class)
                .value(result -> {
                    Assertions.assertEquals(1, result.getEnvelope().getServiceInstance().size());
                    Assertions.assertEquals(validTransactionId,
                            result.getEnvelope().getTransactionId().toString());
                });

        verify(secomSearchResultSigningService).signSearchResult(any());
        verify(searchConsolidationService).getResults(eq(validTransactionId), any());

    }

    // Shows that the controller will actually pull new data from the service on each call
    @Test
    void testRetrieveResultsReturnsCurrentServiceResponseOnEachCall () {
        String validTransactionId = UUID.randomUUID().toString();
        String uid = "TESTMRN2";

        List<ServiceInstanceObject> emptyResults = new ArrayList<>();

        //Setup test variable
        List<ServiceInstanceObject> validResults = new ArrayList<>();
        final ServiceInstanceObject resultInstance = new ServiceInstanceObject();
        resultInstance.setName("testName");
        validResults.add(resultInstance);

        List<ServiceInstanceObject> validResultsNew = new ArrayList<>();
        final ServiceInstanceObject newResultInstance = new ServiceInstanceObject();
        newResultInstance.setName("newTestName");
        validResultsNew.add(newResultInstance);

        this.retrieveResultObject.getEnvelope().setTransactionId(validTransactionId);

        doReturn(uid)
                .when(certificateParsingUtil)
                .getMrnFromCertificate(any());

        //Return the instance when calling getResults
        when(searchConsolidationService.getResults(validTransactionId, uid))
                .thenReturn(validResults) //Call 1
                .thenReturn(validResultsNew) //Call 2
                .thenReturn(emptyResults);


        webTestClient.post()
                .uri("/api/secom/" + RETREIVE_RESULTS_INTERFACE_PATH)
                .bodyValue(retrieveResultObject)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SearchResult.class)
                .value(result -> {
                    Assertions.assertEquals(1, result.getEnvelope().getServiceInstance().size());
                    Assertions.assertEquals(validTransactionId,
                            result.getEnvelope().getTransactionId().toString());
                    Assertions.assertEquals("testName", result.getEnvelope().getServiceInstance().getFirst().getName());
                });

        webTestClient.post()
                .uri("/api/secom/" + RETREIVE_RESULTS_INTERFACE_PATH)
                .bodyValue(retrieveResultObject)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SearchResult.class)
                .value(result -> {
                    Assertions.assertEquals(1, result.getEnvelope().getServiceInstance().size());
                    Assertions.assertEquals(validTransactionId,
                            result.getEnvelope().getTransactionId().toString());
                    Assertions.assertEquals("newTestName", result.getEnvelope().getServiceInstance().getFirst().getName());
                });

        webTestClient.post()
                .uri("/api/secom/" + RETREIVE_RESULTS_INTERFACE_PATH)
                .bodyValue(retrieveResultObject)
                .exchange()
                .expectStatus().isOk();

        verify(searchConsolidationService, times(3)).getResults(validTransactionId, uid);



    }

}
