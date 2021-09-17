/*
 * Copyright (c) 2021 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.serviceregistry.components;

import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.utils.MsrContract;
import org.efficiensea2.maritime_cloud.service_registry.v1.servicespecificationschema.ServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.tx.gas.ContractGasProvider;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartContractProviderTest {

    /**
     * The Tested Component.
     */
    @InjectMocks
    @Spy
    private SmartContractProvider smartContractProvider;

    // Component Variables
    private MsrContract msrContract;
    private Web3j web3j;
    private Request<?, Web3ClientVersion> web3jVersionRequest;
    private Web3ClientVersion web3ClientVersion;
    private Instance instance;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Populate the component parameters
        this.smartContractProvider.msrName = "MSR Name";
        this.smartContractProvider.msrUrl = "http://google.com";
        this.smartContractProvider.ledgerServerUrl = "ws://localhost:12345";
        this.smartContractProvider.ledgerCredentials = "c12345";
        this.smartContractProvider.ledgerContractAddress = "ContactAddress";

        // Mock the MSR Contract
        this.msrContract = mock(MsrContract.class);

        // Mock the web3j ethereum version request and response
        this.web3j = mock(Web3j.class);
        this.web3jVersionRequest = mock(Request.class);
        this.web3ClientVersion = mock(Web3ClientVersion.class);

        // Create a temp geometry factory to get some shapes
        final GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        final Point point = factory.createPoint(new Coordinate(52.001, 1.002));

        // Create a new instance
        this.instance = new Instance();
        this.instance.setInstanceId(String.format("net.maritimeconnectivity.service-registry.instance.%d", 100L));
        this.instance.setName("Instance Name");
        this.instance.setVersion("1.0.0");
        this.instance.setComment("No comment");
        this.instance.setStatus(ServiceStatus.RELEASED);
        this.instance.setGeometry(point);
    }

    /**
     * Test that we can initialise successfully the smart contract provider
     * and that the Web3j web-socket gets connected correctly.
     */
    @Test
    void testInit() throws IOException {
        doReturn(this.web3j).when(smartContractProvider).createWeb3j(this.smartContractProvider.ledgerServerUrl);
        doReturn(this.web3jVersionRequest).when(this.web3j).web3ClientVersion();
        doReturn(this.web3ClientVersion).when(this.web3jVersionRequest).send();
        doReturn("Version 0").when(this.web3ClientVersion).getWeb3ClientVersion();
        doReturn(this.smartContractProvider.ledgerContractAddress).when(this.msrContract).getContractAddress();

        // Mock the static MsrContract.load function and perform the component call
        try (MockedStatic<MsrContract> msrContract = Mockito.mockStatic(MsrContract.class)) {
            msrContract.when((MockedStatic.Verification)MsrContract.load(any(String.class), any(Web3j.class), any(Credentials.class), any(ContractGasProvider.class))).thenReturn(this.msrContract);
            this.smartContractProvider.init();
            assertEquals(this.smartContractProvider.ledgerContractAddress, this.smartContractProvider.msrContract.getContractAddress());
        }
    }

    /**
     * Test that when the smart contract provider component gets destroyed,
     * the Web3j web-socket connection gets cleared out.
     */
    @Test
    void testDestroy() {
        this.smartContractProvider.web3j = this.web3j;

        // Perform the component call
        this.smartContractProvider.destroy();

        verify(web3j, times(1)).shutdown();
    }

    /**
     * Test that we can retrieve the loaded MSR Contract correctly.
     */
    @Test
    void testGetMsrContract() {
        this.smartContractProvider.msrContract = this.msrContract;

        // Perform the service call
        MsrContract result = this.smartContractProvider.getMsrContract();

        assertNotNull(result);
        assertEquals(this.smartContractProvider.msrContract, result);
    }

    /**
     * Test that we can correctly detect is the MSR contract is loaded and
     * properly connected to the Web3j web-socket connection.
     */
    @Test
    void testIsMsrContractConnected() throws IOException {
        doReturn(Boolean.TRUE).when(this.msrContract).isValid();
        this.smartContractProvider.msrContract = this.msrContract;

        assertTrue(this.smartContractProvider.isMsrContractConnected());
    }

    /**
     * Test that we can correctly detect is the MSR contract is not loaded or
     * not properly connected to the Web3j web-socket connection.
     */
    @Test
    void testIsMsrContractConnectedFalse() {
        // Test for no connection
        assertFalse(this.smartContractProvider.isMsrContractConnected());

        // Test for invalid connection
        this.smartContractProvider.msrContract = this.msrContract;
        assertFalse(this.smartContractProvider.isMsrContractConnected());
    }

    /**
     * Test that we can create new MSR ledger service instance objects based
     * on the local instance implementation.
     */
    @Test
    void testCreateNewServiceInstance() {
        // Perform the component call
        MsrContract.ServiceInstance result = this.smartContractProvider.createNewServiceInstance(this.instance);

        assertNotNull(result);
        assertEquals(this.instance.getName(), result.name);
        assertEquals(this.instance.getInstanceId(), result.mrn);
        assertEquals(this.instance.getVersion(), result.version);
        assertEquals(this.instance.getGeometry().toString(), result.coverageArea);
        assertEquals("designMrn", result.implementsDesignMRN);
        assertEquals("designVersion", result.implementsDesignVersion);
    }

}