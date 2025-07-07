package net.maritimeconnectivity.serviceregistry.components.gmsp;


import net.maritimeconnectivity.serviceregistry.components.Gmsp;
import net.maritimeconnectivity.serviceregistry.components.mms.MmsEdgeRouter;
import net.maritimeconnectivity.serviceregistry.components.mms.OutgoingMmtpFactory;
import net.maritimeconnectivity.serviceregistry.components.mms.OutgoingProtobufMmtpFactory;
import net.maritimeconnectivity.serviceregistry.utils.KeyStoreUtil;
import org.grad.secom.core.models.SearchFilterObject;
import org.grad.secom.core.models.SearchParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
class GmspIntegrationTest {

    @Autowired
    private Gmsp gmsp;

    @Test
    void testGlobalSearchNoGeometry() {
        // Arrange
        SearchFilterObject sfo = new SearchFilterObject();
        SearchParameters params = new SearchParameters();
        params.setKeywords("test");
        sfo.setQuery(params);  // Valid query
        sfo.setGeometry(null); // No geometry (this is what we're testing)

        String testEndpoint = "http://example.com/test/endpoint";
        String testMrn = "urn:mrn:mcp:example:test:client";

        assertDoesNotThrow(() -> {
            String gmspRequestUuid = gmsp.globalSearch(testEndpoint, testMrn, sfo);

            //Sleep for 4 seconds
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Assertions.assertTrue(gmsp.isSent(gmspRequestUuid));
        });

    }

}
