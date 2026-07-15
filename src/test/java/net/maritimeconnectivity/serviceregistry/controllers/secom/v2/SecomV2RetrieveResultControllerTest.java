package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import net.maritimeconnectivity.serviceregistry.TestingConfiguration;
import net.maritimeconnectivity.serviceregistry.components.SecomV2SignatureProviderImpl;
import net.maritimeconnectivity.serviceregistry.components.SecomV2SigningIdentityProvider;
import net.maritimeconnectivity.serviceregistry.components.SecomV2TrustStoreProviderImpl;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import org.grad.secomv2.core.components.SecomSignatureAdvice;
import org.grad.secomv2.core.models.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.grad.secomv2.core.interfaces.RetrieveResultServiceInterface.RETRIEVE_RESULT_INTERFACE_PATH;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
@AutoConfigureWebTestClient
@Import(TestingConfiguration.class)
public class SecomV2RetrieveResultControllerTest {

    /**
     * The Web Test Client
     */
    @Autowired
    private WebTestClient webTestClient;

    /**
     * A Mockito Spy of the tested Controller.
     */
    @MockitoSpyBean
    private RetrieveResultController retrieveResultController;

    /**
     * The Search Consolidation Service.
     */
    @MockitoBean
    private SearchConsolidationService searchConsolidationService;

    /**
     * Mock the SECOM signature advise
     */
    @MockitoBean
    private SecomSignatureAdvice secomSignatureAdvice;

    // Test Variables
    private RetrieveResultObject retrieveResultObject;
    private String mrn;

    @BeforeEach
    void setUp() {
        // Setup a search results envelope object
        final EnvelopeRetrieveResultObject envelopeSearchResultObject = new EnvelopeRetrieveResultObject();
        this.retrieveResultObject = new RetrieveResultObject();
        this.retrieveResultObject.setEnvelope(envelopeSearchResultObject);
        this.retrieveResultObject.setEnvelopeSignature("TEST_SIGNATURE");

        // Fix an MRN
        this.mrn = "TESTMRN";

        // For all calls mock the MRN retrieval on the controller
        doReturn(this.mrn).when(this.retrieveResultController).getRetrieveResultsEnvelopeMrn(any());
    }

    @Test
    void testRetrieveResultForInvalidTransactionId() {
        // Create an invalid transaction ID
        final String transactionId = "invalidTransactionId";
        // And set it to the envelope
        retrieveResultObject.getEnvelope().setTransactionId(transactionId);

        // Now perform the endpoint call
        webTestClient.post()
                .uri("/api/secom" + RETRIEVE_RESULT_INTERFACE_PATH)
                .bodyValue(retrieveResultObject)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testRetrieveResultForNonExistentTransactionId() {
        // Create a valid transaction ID
        final String transactionId = String.valueOf(UUID.randomUUID());
        // And set it to the envelope
        retrieveResultObject.getEnvelope().setTransactionId(transactionId);

        // Return no results from the consolidation service
        doReturn(null)
                .when(searchConsolidationService)
                .getResults(eq(transactionId), eq(this.mrn));

        // Now perform the endpoint call
        webTestClient.post()
                .uri("/api/secom" + RETRIEVE_RESULT_INTERFACE_PATH)
                .bodyValue(retrieveResultObject)
                .exchange()
                .expectStatus()
                .isNotFound();

        // Make sure we tried to read the results at lead once
        verify(searchConsolidationService).getResults(eq(transactionId), any());
    }

    @Test
    void testRetrieveResultForValidTransactionId() {
        // Create a valid transaction ID
        final String validTransactionId = UUID.randomUUID().toString();
        // And set it to the envelope
        this.retrieveResultObject.getEnvelope().setTransactionId(validTransactionId);

        // Now create a set of test results
        final List<ServiceInstanceObject> validResults = new ArrayList<>();
        final ServiceInstanceObject resultInstance = new ServiceInstanceObject();
        resultInstance.setName("testName");
        validResults.add(resultInstance);

        // Return the results from the consolidation service
        doReturn(validResults)
                .when(searchConsolidationService)
                .getResults(eq(validTransactionId), eq(this.mrn));

        // Now perform the endpoint call
        webTestClient.post()
                .uri("/api/secom" + RETRIEVE_RESULT_INTERFACE_PATH)
                .bodyValue(retrieveResultObject)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SearchResult.class)
                .value(result -> {
                    Assertions.assertEquals(1, result.getEnvelope().getServiceInstance().size());
                    Assertions.assertEquals(validTransactionId,
                            result.getEnvelope().getTransactionId().toString());
                });

        // Make sure we tried to read the results at lead once
        verify(searchConsolidationService).getResults(eq(validTransactionId), eq(this.mrn));
    }

    // Shows that the controller will actually pull new data from the service on each call
    @Test
    void testRetrieveResultsReturnsCurrentServiceResponseOnEachCall () {
        // Create a valid transaction ID
        final String validTransactionId = UUID.randomUUID().toString();
        // And set it to the envelope
        this.retrieveResultObject.getEnvelope().setTransactionId(validTransactionId);

        // Now create a set of empty test results
        final List<ServiceInstanceObject> emptyResults = new ArrayList<>();

        // And create a set of test results
        final List<ServiceInstanceObject> validResults = new ArrayList<>();
        final ServiceInstanceObject resultInstance = new ServiceInstanceObject();
        resultInstance.setName("testName");
        validResults.add(resultInstance);

        // And create another set of test results
        final List<ServiceInstanceObject> validResultsNew = new ArrayList<>();
        final ServiceInstanceObject newResultInstance = new ServiceInstanceObject();
        newResultInstance.setName("newTestName");
        validResultsNew.add(newResultInstance);

        // Return the instance when calling getResults
        when(searchConsolidationService.getResults(eq(validTransactionId), eq(this.mrn)))
                .thenReturn(validResults)       // Call 1
                .thenReturn(validResultsNew)    // Call 2
                .thenReturn(emptyResults);      // Call 3

        // Now perform the first endpoint call
        webTestClient.post()
                .uri("/api/secom" + RETRIEVE_RESULT_INTERFACE_PATH)
                .bodyValue(retrieveResultObject)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SearchResult.class)
                .value(result -> {
                    Assertions.assertEquals(1, result.getEnvelope().getServiceInstance().size());
                    Assertions.assertEquals(validTransactionId, result.getEnvelope().getTransactionId().toString());
                    Assertions.assertEquals("testName", result.getEnvelope().getServiceInstance().getFirst().getName());
                });

        // Now perform the second endpoint call
        webTestClient.post()
                .uri("/api/secom" + RETRIEVE_RESULT_INTERFACE_PATH)
                .bodyValue(retrieveResultObject)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SearchResult.class)
                .value(result -> {
                    Assertions.assertEquals(1, result.getEnvelope().getServiceInstance().size());
                    Assertions.assertEquals(validTransactionId, result.getEnvelope().getTransactionId().toString());
                    Assertions.assertEquals("newTestName", result.getEnvelope().getServiceInstance().getFirst().getName());
                });

        // Now perform the final second endpoint call
        webTestClient.post()
                .uri("/api/secom" + RETRIEVE_RESULT_INTERFACE_PATH)
                .bodyValue(retrieveResultObject)
                .exchange()
                .expectStatus().isOk();

        // Make sure we tried to read the results 3 times
        verify(searchConsolidationService, times(3)).getResults(eq(validTransactionId), eq(this.mrn));
    }

}
