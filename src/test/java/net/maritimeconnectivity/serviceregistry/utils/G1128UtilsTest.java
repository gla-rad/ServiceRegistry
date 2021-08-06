package net.maritimeconnectivity.serviceregistry.utils;

import org.apache.commons.io.IOUtils;
import org.efficiensea2.maritime_cloud.service_registry.v1.serviceinstanceschema.CoverageInfo;
import org.efficiensea2.maritime_cloud.service_registry.v1.serviceinstanceschema.ServiceInstance;
import org.efficiensea2.maritime_cloud.service_registry.v1.servicespecificationschema.ServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class G1128UtilsTest {

    // Class Variables
    private ServiceInstance serviceInstance;
    private String serviceInstanceXml;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() {
        this.serviceInstanceXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<serviceInstance xmlns=\"http://efficiensea2.org/maritime-cloud/service-registry/v1/ServiceInstanceSchema.xsd\" xmlns:ns2=\"http://efficiensea2.org/maritime-cloud/service-registry/v1/ServiceSpecificationSchema.xsd\">\n" +
                "    <id>org.test.serviceInstance.0.0.1</id>\n" +
                "    <version>0.0.1TEST</version>\n" +
                "    <name>Service Instance Name</name>\n" +
                "    <status>released</status>\n" +
                "    <description>No comment</description>\n" +
                "    <keywords>test,G1128,service,instance</keywords>\n" +
                "    <endpoint>https://test.org/somoething</endpoint>\n" +
                "    <MMSI>123456789</MMSI>\n" +
                "    <IMO>imo</IMO>\n" +
                "    <serviceType>test</serviceType>\n" +
                "    <requiresAuthorization>false</requiresAuthorization>\n" +
                "    <coversAreas>\n" +
                "        <unLoCode>HWC</unLoCode>\n" +
                "    </coversAreas>\n" +
                "</serviceInstance>\n";

        // Create a service instance similar to the static XML defined here
        this.serviceInstance = new ServiceInstance();
        this.serviceInstance.setName("Service Instance Name");
        this.serviceInstance.setVersion("0.0.1TEST");
        this.serviceInstance.setId("org.test.serviceInstance.0.0.1");
        this.serviceInstance.setKeywords("test,G1128,service,instance");
        this.serviceInstance.setStatus(ServiceStatus.RELEASED);
        this.serviceInstance.setDescription("No comment");
        this.serviceInstance.setName(serviceInstance.getName());
        this.serviceInstance.setEndpoint("https://test.org/somoething");
        this.serviceInstance.setMMSI("123456789");
        this.serviceInstance.setIMO("imo");
        this.serviceInstance.setServiceType("test");

        /// Also set the UnLoCode for the coverage info
        CoverageInfo coverageInfo = new CoverageInfo();
        coverageInfo.setUnLoCode("HWC");
        this.serviceInstance.setCoversAreas(coverageInfo);
    }

    /**
     * Test that we can create (marshall) and XML based on a G1128
     * specification object class.
     *
     * @throws JAXBException a JAXB exception thrown during the marshalling operation
     */
    @Test
    void testMarchallG1128() throws JAXBException {
        String xml = new G1128Utils<>(ServiceInstance.class).marshalG1128(this.serviceInstance);
        assertNotNull(xml);
        assertEquals(this.serviceInstanceXml, xml);
    }

    /**
     * Test that we can generate (unmarshall) a G1128 POJO based on a valid
     * XML G1128 specification.
     *
     * @throws IOException any IO exceptions while reading the input XML file
     * @throws JAXBException a JAXB exception thrown during the unmarshalling operation
     */
    @Test
    void testUnmarshallG1128() throws IOException, JAXBException {
        // Read a test service instance specification
        InputStream in = new ClassPathResource("test-instance.xml").getInputStream();
        String xml = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        // Unmarshall it to a G1128 service instance object
        ServiceInstance serviceInstance = new G1128Utils<>(ServiceInstance.class).unmarshallG1128(this.serviceInstanceXml);

        // Assert all information is correct
        assertNotNull(serviceInstance);
        assertEquals(this.serviceInstance.getName(), serviceInstance.getName());
        assertEquals(this.serviceInstance.getVersion(), serviceInstance.getVersion());
        assertEquals(this.serviceInstance.getId(), serviceInstance.getId());
        assertEquals(this.serviceInstance.getKeywords(), serviceInstance.getKeywords());
        assertEquals(this.serviceInstance.getStatus(), serviceInstance.getStatus());
        assertEquals(this.serviceInstance.getDescription(), serviceInstance.getDescription());
        assertEquals(this.serviceInstance.getEndpoint(), serviceInstance.getEndpoint());
        assertEquals(this.serviceInstance.getMMSI(), serviceInstance.getMMSI());
        assertEquals(this.serviceInstance.getIMO(), serviceInstance.getIMO());
        assertEquals(this.serviceInstance.getServiceType(), serviceInstance.getServiceType());
        assertEquals(this.serviceInstance.getCoversAreas().getUnLoCode(), serviceInstance.getCoversAreas().getUnLoCode());
        assertEquals(this.serviceInstance.getProducedBy(), serviceInstance.getProducedBy());
        assertEquals(this.serviceInstance.getProvidedBy(), serviceInstance.getProvidedBy());
    }

}