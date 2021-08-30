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

package net.maritimeconnectivity.serviceregistry.services;

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.exceptions.McpBasicRestException;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import net.maritimeconnectivity.serviceregistry.models.domain.LedgerRequest;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;
import net.maritimeconnectivity.serviceregistry.utils.MsrContract;
import net.maritimeconnectivity.serviceregistry.utils.MsrErrorConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.MethodNotAllowedException;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.tx.gas.DefaultGasProvider;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * MSR ledger service implementation for interaction between MSR and the MSR ledger
 *
 * @author Jinki Jung (email: jinki@dmc.international)
 */
@Service
@Slf4j
@Transactional
@ConditionalOnProperty(value = "ledger.enabled", matchIfMissing = true)
public class LedgerRequestService {

    @Value("${info.msr.name:Unknown}")
    private String msrName;

    @Value("${info.msr.url:Unknown}")
    private String msrUrl;

    private Web3j web3j;

    private MsrContract msrContract;

    private boolean isLedgerConnected = false;

    @Autowired
    private LedgerRequestDBService ledgerRequestDBService;

    @PostConstruct
    public void init() throws Exception {
        WebSocketService webSocketService = new WebSocketService("ws://localhost:8546", true);
        webSocketService.connect();
        web3j = Web3j.build(webSocketService);
        Credentials credentials = Credentials.create("c87509a1c067bbde78beb793e6fa76530b6382a4c0241e5e4a9ec0a0f44dc0d3");
        msrContract = MsrContract.load("0x345cA3e014Aaf5dcA488057592ee47305D9B3e10", web3j, credentials, new DefaultGasProvider());

        Web3ClientVersion web3ClientVersion = null;
        try {
            web3ClientVersion = web3j.web3ClientVersion().send();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String clientVersion = web3ClientVersion.getWeb3ClientVersion();
        if (msrContract != null) {
            log.info("Web3j "+ clientVersion + ": successfully connected to the ledger");
            isLedgerConnected = true;
        }
    }

    @PreDestroy
    public void shutdown() {
        web3j.shutdown();
    }

    public Page<LedgerRequest> findAll(Pageable pageable) {
        log.debug("Request to get all requests");

        return ledgerRequestDBService.findAll(pageable);
    }

    public LedgerRequest registerInstanceToLedger(Long id) throws DataNotFoundException, MethodNotAllowedException, McpBasicRestException {
        LedgerRequest ledgerRequest = ledgerRequestDBService.findOne(id);

        if (!isLedgerConnected) {
            throw new McpBasicRestException(HttpStatus.NOT_FOUND, MsrErrorConstant.LEDGER_NOT_CONNECTED, null);
        }

        if (ledgerRequest == null) {
            throw new McpBasicRestException(HttpStatus.NOT_FOUND, MsrErrorConstant.LEDGER_REQUEST_NOT_FOUND + " - given request ID: " + id.toString(), null);
        }

        // Try to find the instance if an ID of instance is provided
        // TODO: validation of instance should be in place here
        Instance instance = ledgerRequest.getServiceInstance();
        if(instance == null) {
            throw new McpBasicRestException(HttpStatus.NOT_FOUND, MsrErrorConstant.LEDGER_REQUEST_INSTANCE_NOT_FOUND + " - given request ID: " + id.toString(), null);
        }

        if(ledgerRequest.getStatus() != LedgerRequestStatus.VETTED){
            throw new McpBasicRestException(HttpStatus.UNPROCESSABLE_ENTITY, MsrErrorConstant.LEDGER_REQUEST_STATUS_NOT_FULFILLED + "- current status: " + ledgerRequest.getStatus(), null);
        }

        ledgerRequestDBService.updateStatus(id, LedgerRequestStatus.REQUESTING);

        Thread t = new Thread(() -> {
            try {
                MsrContract.ServiceInstance serviceInstance = new MsrContract.ServiceInstance(instance.getName(), instance.getInstanceId(), instance.getVersion(), instance.getKeywords(), instance.getGeometry().toString(), "designMrn", "designVersion", new MsrContract.Msr(msrName, msrUrl));

                var receipt = msrContract.registerServiceInstance(serviceInstance, Arrays.asList(instance.getKeywords().split(" "))).send();

                if (receipt.getStatus().equals("0x1")){
                    log.info("Instance is successfully registered to the ledger - instance name: " + instance.getName());
                    ledgerRequestDBService.updateStatus(id, LedgerRequestStatus.SUCCEEDED);
                }
                else {
                    log.error(MsrErrorConstant.LEDGER_REGISTRATION_FAILED + " - instance name: " + instance.getName());
                    ledgerRequestDBService.updateStatus(id, LedgerRequestStatus.FAILED);
                }

            } catch (Exception e) {
                log.error(MsrErrorConstant.LEDGER_REGISTRATION_FAILED + " - ", e.getMessage(), e);
                try {
                    ledgerRequestDBService.updateStatus(id, LedgerRequestStatus.FAILED);
                } catch (DataNotFoundException dataNotFoundException) {
                    dataNotFoundException.printStackTrace();
                }
            }
        });
        t.start();
        return ledgerRequest;
    }

    /**
     * Save a LedgerRequest.
     *
     * @param request the entity to save
     * @return the persisted entity
     */
    @Transactional
    public LedgerRequest save(LedgerRequest request) throws DataNotFoundException {
        log.debug("Request to save LedgerRequest : {}", request);

        return ledgerRequestDBService.save(request);
    }

    /**
     * Get one LedgerRequest by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    public LedgerRequest findOne(Long id) throws DataNotFoundException {
        log.debug("Request to get a LedgerRequest : {}", id);

        return ledgerRequestDBService.findOne(id);
    }

    /**
     * Delete the  instance by id.
     *
     * @param id the id of the entity
     */
    @Transactional(propagation = Propagation.NESTED)
    public void delete(Long id) throws DataNotFoundException {
        log.debug("Request to delete Instance : {}", id);
        this.ledgerRequestDBService.delete(id);
    }
}
