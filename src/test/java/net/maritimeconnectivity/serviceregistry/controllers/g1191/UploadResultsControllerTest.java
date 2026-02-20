package net.maritimeconnectivity.serviceregistry.controllers.g1191;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.maritimeconnectivity.serviceregistry.controllers.InstanceController;
import net.maritimeconnectivity.serviceregistry.controllers.advices.MSRBaseExceptionResolver;
import net.maritimeconnectivity.serviceregistry.controllers.g1191.v2.UploadResultsController;
import net.maritimeconnectivity.serviceregistry.exceptions.InvalidRequestException;
import net.maritimeconnectivity.serviceregistry.models.dto.secom.v2.SearchObjectResultWithCert;
import net.maritimeconnectivity.serviceregistry.services.SearchConsolidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = UploadResultsController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(MSRBaseExceptionResolver.class)
public class UploadResultsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SearchConsolidationService searchConsolidationService;

    // Test variables
    private String validTransactionId;
    private List<SearchObjectResultWithCert> results;


    @BeforeEach
    void setUp() {
        this.validTransactionId = "validTransactionId";

        this.results = new ArrayList<>();
        SearchObjectResultWithCert result1 = new SearchObjectResultWithCert();
        result1.setInstanceId("testInstanceId");
        result1.setName("A test service instance");
        this.results.add(result1);
    }


    /*
        The following parameterized test runs the uploadResultsOnlyInvalidXactIdThrows with different sets
        of arguments. Using a valid xactID should yield a code 200 while invalid xactID should yield 400
        due to the controlleradvice mapping InvalidRequestException to http response code 400
     */
    static Stream<Arguments> txCases() {
        return Stream.of(
                Arguments.of("validTx", 200, false),
                Arguments.of("invalidTx", 400, true)
        );
    }
    @ParameterizedTest
    @MethodSource("txCases")
    void uploadResultsReturnsExpectedStatus(String tx, int expectedStatus, boolean shouldThrow) throws Exception {

        if (shouldThrow) {
            doThrow(new InvalidRequestException("No results found for transaction id " + tx))
                    .when(searchConsolidationService)
                    .addResults(eq(tx), anyList());
        }

        mockMvc.perform(
                    post("/api/g1191/v2/uploadResults/{transactionId}", tx)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(results))
            )
            .andExpect(status().is(expectedStatus));
        verify(searchConsolidationService).addResults(eq(tx), anyList());
    }


    @Test
    void testUploadEmptyResultsToValidTransactionId() throws Exception {

        //Build payload
        List<SearchObjectResultWithCert> searchResults = new ArrayList<>();

        //Act
        MvcResult mvcResult = this.mockMvc.perform(post("/api/g1191/v2/uploadResults/{transactionId}", this.validTransactionId)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(searchResults)))
                .andExpect(status().isBadRequest())
                .andReturn();

    }

}
