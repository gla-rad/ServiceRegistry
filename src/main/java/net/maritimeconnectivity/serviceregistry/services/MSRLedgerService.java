package net.maritimeconnectivity.serviceregistry.services;

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.exceptions.DataNotFoundException;
import net.maritimeconnectivity.serviceregistry.models.domain.Instance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;

/**
 * MSR ledger client Implementation for MSR-ledger integration
 *
 * @author Jinki Jung (email: jinki@dmc.international)
 */
@Service
@Slf4j
@Transactional
public class MSRLedgerService {
    String msrContractAddress = "0x345cA3e014Aaf5dcA488057592ee47305D9B3e10";

    /**
     *
     */
    public void init() {
        //*
        Web3j web3j = Web3j.build(new HttpService("http://localhost:8545"));
        Web3ClientVersion web3ClientVersion = null;
        try {
            web3ClientVersion = web3j.web3ClientVersion().send();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String web3ClientVersionString = web3ClientVersion.getWeb3ClientVersion();
        System.out.println("Web 3 client version: " + web3ClientVersionString);
        /*
        const web3 = new Web3j(Web3j.givenProvider);
        web3j.eth.requestAccounts().then(as => {
            setAccounts(as);
            const msrContract = new web3.eth.Contract(msrABI.abi as AbiItem[], msrContractAddress);
            setContract(msrContract);
        });
         */
    }

}
