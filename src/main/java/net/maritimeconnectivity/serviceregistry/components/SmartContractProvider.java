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

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.utils.MsrContract;
import org.iala_aism.g1128.v1_3.servicespecificationschema.ServiceStatus;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.tx.gas.DefaultGasProvider;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The Smart Contract Provider Component.
 *
 * A component implementation that can be used as a wrapper around the MSR
 * global ledger smart contract. On initialization, it will connect to the
 * ledger using smart contract and can then  be used to contact the ledger
 * and search for or persist information.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Component
@SessionScope
@Slf4j
@ConditionalOnProperty(value = "ledger.enabled", matchIfMissing = true)
public class SmartContractProvider {

    @Value("${info.msr.name:Unknown}")
    protected String msrName;

    @Value("${info.msr.url:Unknown}")
    protected String msrUrl;

    @Value("${net.maritimeconnectivity.serviceregistry.ledger.server-url:ws://localhost:8546}")
    protected String ledgerServerUrl;

    @Value("${net.maritimeconnectivity.serviceregistry.ledger.credentials:}")
    protected String ledgerCredentials;

    @Value("${net.maritimeconnectivity.serviceregistry.ledger.contract-address:}")
    protected String ledgerContractAddress;

    // MSR Ledger Smart Contract
    protected Web3j web3j;
    protected MsrContract msrContract;

    /**
     * By default, the SmartContractProvider should try to connect to the MSR
     * global ledger in order to access it for its searching and persistence
     * functionality.
     *
     * @throws ConnectException if errors occur while connecting to the ledger's web-socket
     */
    @PostConstruct
    public void init() throws ConnectException {
        // Connect asynchronously to the ledger through a web-socket
        this.web3j = this.createWeb3j(this.ledgerServerUrl);
        final MsrContract loadedContract = MsrContract.load(this.ledgerContractAddress,
                web3j,
                Credentials.create(this.ledgerCredentials),
                new DefaultGasProvider());

        // Perform a test call
        try {
            final Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
            Optional.of(loadedContract)
                    .ifPresent(contract -> {
                        log.info(String.format("Web3j {}: successfully connected to the ledger", web3ClientVersion.getWeb3ClientVersion()));
                        this.msrContract = contract;
                    });
        } catch (Exception ex) {
            this.log.error(ex.getMessage());
        }
    }

    /**
     * We need to always close the web-socket connection when we are done.
     */
    @PreDestroy
    public void destroy() {
        web3j.shutdown();
    }

    /**
     * Returns the MSR ledger smart contract object.
     *
     * @return the MSR ledger smart contract object
     */
    public MsrContract getMsrContract() {
        return this.msrContract;
    }

    /**
     * Returns whether there is a valid connection to the MSR ledger.
     *
     * @return whether there is a valid connection to the MSR ledger
     */
    public boolean isMsrContractConnected() {
        try {
            return this.msrContract != null && this.msrContract.isValid();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Creates and returns a new ledger service instance based on the
     * definitions of the MSR ledger smart contract.
     *
     * @param instance the instance to generate the ledger MSR service instance entry for
     * @return the new ledger MSR service instance entry
     */
    public MsrContract.ServiceInstance createNewServiceInstance(Instance instance) {
        return new MsrContract.ServiceInstance(instance.getName(),
                instance.getInstanceId(),
                instance.getVersion(),
                Optional.ofNullable(instance.getKeywords()).orElse(Collections.emptyList()).stream().collect(Collectors.joining(",")),
                Optional.ofNullable(instance.getGeometry()).map(Geometry::toString).orElse(null),
                Optional.ofNullable(instance.getStatus()).map(ServiceStatus::ordinal).map(BigInteger::valueOf).orElse(BigInteger.valueOf(-1)),
                "designMrn",
                "designVersion",
                "",
                ""
        );
    }

    /**
     * A helper function that creates a new web3j web-socket connections and
     * allows us to easily unit-test the rest of the component.
     *
     * @return a new web-socket service
     */
    protected Web3j createWeb3j(String url) throws ConnectException {
        final WebSocketService webSocketService =  new WebSocketService(url, true);
        webSocketService.connect();
        return Web3j.build(webSocketService);
    }

}
