package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import net.maritimeconnectivity.serviceregistry.TestingConfiguration;
import net.maritimeconnectivity.serviceregistry.components.SecomV2SignatureProviderImpl;
import net.maritimeconnectivity.serviceregistry.components.SecomV2SigningIdentityProvider;
import net.maritimeconnectivity.serviceregistry.components.SecomV2TrustStoreProviderImpl;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import net.maritimeconnectivity.serviceregistry.services.SecomSearchResultSigningService;
import org.grad.secomv2.core.models.EnvelopeSearchResultObject;
import org.grad.secomv2.core.models.SearchResult;
import org.grad.secomv2.core.models.ServiceInstanceObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;

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
    }

    @Test
    void testRetrieveResultForInvalidTransactionId() {

        String transactionId = "invalidTransactionId";

        //Return null when calling the getresults
        doReturn(null)
                .when(searchConsolidationService)
                .getResults(eq(transactionId));

        webTestClient.get()
                .uri("/api/secom/" + RETREIVE_RESULTS_INTERFACE_PATH + "/" + transactionId)
                .exchange()
                .expectStatus().isNotFound();



        verify(searchConsolidationService).getResults(eq(transactionId));
    }

    @Test
    void testRetrieveResultForValidTransactionId() {
        List<ServiceInstanceObject> validResults = new ArrayList<>();
        String validTransactionId = "validTransactionId";

        //Setup test variable
        final ServiceInstanceObject resultInstance = new ServiceInstanceObject();
        resultInstance.setName("testName");
        validResults.add(resultInstance);


        //Return the instance when calling getResults
        doReturn(validResults)
                .when(searchConsolidationService)
                .getResults(eq(validTransactionId));

        webTestClient.post()
                .uri("/api/secom/" + RETREIVE_RESULTS_INTERFACE_PATH + "/" + validTransactionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SearchResult.class)
                .value(result -> {
                    Assertions.assertEquals(1, result.getEnvelope().getServiceInstance().size());
                    Assertions.assertEquals(validTransactionId, result.getEnvelope().getTransactionId());
                });

        verify(searchConsolidationService).getResults(eq(validTransactionId));

    }

    // Shows that the controller will actually pull new data from the service on each call
    @Test
    void testRetrieveResultsReturnsCurrentServiceResponseOnEachCall () {
        String validTransactionId = "validTransactionId";

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

        //Return the instance when calling getResults
        when(searchConsolidationService.getResults(validTransactionId))
                .thenReturn(validResults) //Call 1
                .thenReturn(validResultsNew) //Call 2
                .thenReturn(emptyResults);


        webTestClient.get()
                .uri("/api/secom/" + RETREIVE_RESULTS_INTERFACE_PATH + "/" + validTransactionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SearchResult.class)
                .value(result -> {
                    Assertions.assertEquals(1, result.getEnvelope().getServiceInstance().size());
                    Assertions.assertEquals(validTransactionId, result.getEnvelope().getTransactionId());
                    Assertions.assertEquals("testName", result.getEnvelope().getServiceInstance().getFirst().getName());
                });

        webTestClient.get()
                .uri("/api/secom/" + RETREIVE_RESULTS_INTERFACE_PATH + "/" + validTransactionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SearchResult.class)
                .value(result -> {
                    Assertions.assertEquals(1, result.getEnvelope().getServiceInstance().size());
                    Assertions.assertEquals(validTransactionId, result.getEnvelope().getTransactionId());
                    Assertions.assertEquals("newTestName", result.getEnvelope().getServiceInstance().getFirst().getName());
                });

        webTestClient.get()
                .uri("/api/secom/" + RETREIVE_RESULTS_INTERFACE_PATH + "/" + validTransactionId)
                .exchange()
                .expectStatus().isOk();

        verify(searchConsolidationService, times(3)).getResults(validTransactionId);



    }

}
