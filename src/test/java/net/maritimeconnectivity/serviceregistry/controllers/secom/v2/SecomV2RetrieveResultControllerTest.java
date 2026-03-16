package net.maritimeconnectivity.serviceregistry.controllers.secom.v2;

import net.maritimeconnectivity.serviceregistry.TestingConfiguration;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import org.grad.secomv2.core.models.SearchResult;
import org.grad.secomv2.core.models.ServiceInstanceObject;
import org.junit.jupiter.api.Assertions;
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
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
@Import(TestingConfiguration.class)
public class SecomV2RetrieveResultControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private SearchConsolidationService searchConsolidationService;



    @Test
    void retrieveResultForInvalidTransactionId() {

        String transactionId = "invalidTransactionId";

        //Return null when calling the getresults
        doReturn(null)
                .when(searchConsolidationService)
                .getResults(eq(transactionId));

        webTestClient.get()
                .uri("/api/secom/" + RETREIVE_RESULTS_INTERFACE_PATH + "/" + transactionId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .isEqualTo("Transaction not found: " + transactionId);



        verify(searchConsolidationService).getResults(eq(transactionId));
    }

    @Test
    void retrieveResultForValidTransactionId() {
        List<ServiceInstanceObject> validResults = new ArrayList<>();
        String validTransactionId = "validTransactionId";

        //Setup test variable
        final ServiceInstanceObject resultInstance = new ServiceInstanceObject();
        resultInstance.setTransactionId(validTransactionId);
        resultInstance.setName("testName");
        validResults.add(resultInstance);


        //Return the instance when calling getResults
        doReturn(validResults)
                .when(searchConsolidationService)
                .getResults(eq(validTransactionId));

        webTestClient.get()
                .uri("/api/secom/" + RETREIVE_RESULTS_INTERFACE_PATH + "/" + validTransactionId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SearchResult.class)
                .value(result -> {
                    Assertions.assertEquals(1, result.getServiceInstance().size());
                    Assertions.assertEquals(validTransactionId, result.getServiceInstance().getFirst().getTransactionId());
                });

        verify(searchConsolidationService).getResults(eq(validTransactionId));

    }



}
