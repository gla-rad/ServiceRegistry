package net.maritimeconnectivity.serviceregistry.services;


import ch.qos.logback.core.net.server.Client;
import ch.qos.logback.core.net.server.ConcurrentServerRunner;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.Gmsp;
import net.maritimeconnectivity.serviceregistry.config.CacheConfig;
import net.maritimeconnectivity.serviceregistry.exceptions.InvalidRequestException;
import net.maritimeconnectivity.serviceregistry.models.domain.ConsolidatedSearchResult;
import org.grad.secom.core.exceptions.SecomNotAuthorisedException;
import org.grad.secomv2.core.models.ServiceInstanceObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Service implementation for consolidating search results obtained from local and global search
 *
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
@Service
@Slf4j
public class SearchConsolidationService {

    private final Cache sessions;

    // cm injected by Spring as it is defined as a Bean in CacheConfig
    public SearchConsolidationService(CacheManager cm) {
        this.sessions = cm.getCache(CacheConfig.CACHE_NAME);
        if (this.sessions == null) {
            throw new IllegalStateException("Cache '" + CacheConfig.CACHE_NAME + "' not found. Check CacheConfig.");
        }

    }

    public void addResults(String transactionId, List<ServiceInstanceObject> results) {
        for (ServiceInstanceObject r : results) {
            log.debug("Added result {}", r.getName());
            addResult(transactionId, r);
        }
    }

    public void createConsolidationEntry(String transactionId, String uid) {
        ConsolidatedSearchResult agg = create(transactionId, uid);
        sessions.put(transactionId, agg);
        log.warn("Created consolidation entry for transaction {}", transactionId);
    }

    /** Add a single result to the transaction’s consolidated set (creates the entry if absent).
     * Do not add duplicate results*/
    public void addResult(String transactionId, ServiceInstanceObject result) {
        ConsolidatedSearchResult agg = get(transactionId);
        String key = getKey(result);            // choose your canonical key; instanceId for now
        if (key != null && !key.isBlank()) {
            agg.addIfNew(key, result);            // dedup happens inside the aggregator
        } else {
            throw new InvalidRequestException("No results found for transaction id " + transactionId);
        }
    }

    /** Read all results currently stored for the transaction (immutable snapshot). */
    public List<ServiceInstanceObject> getResults(String transactionId, String uid) {
        var agg = sessions.get(transactionId, ConsolidatedSearchResult.class);
        if (agg == null) {
            log.debug("No results found for transactionId {}", transactionId);
            return null;
        }

        if (!Objects.equals(agg.getUid(), uid)) {
            log.warn(
                    "Securitu violaten for user on transaction {}. Expected uid {}, got {}",
                    transactionId,
                    agg.getUid(),
                    uid
            );
           throw new SecomNotAuthorisedException("Not authorized to access results for this " +
                   "transactionId");
        }

        // Capture a stable snapshot exactly once
        List<ServiceInstanceObject> snapshot = List.copyOf(agg.snapshot()); // defensive copy

        // Log using the same snapshot
        for (ServiceInstanceObject r : snapshot) {
            log.debug("Retrieved result {}", r.getName());
        }

        log.debug("--NOW RETURNING {} RESULTS--", snapshot.size());
        return snapshot;
    }



    private ConsolidatedSearchResult get(String transactionId) {
        Cache.ValueWrapper value = sessions.get(transactionId);
        return value != null ? (ConsolidatedSearchResult) value.get() : null;
    }

    private ConsolidatedSearchResult create(String transactionId, String uid) {
        return ConsolidatedSearchResult.create(transactionId, uid);
    }

    /** For now: instanceId as the dedup key; adjust if you adopt a different canonical key later. */
    private String getKey(ServiceInstanceObject r) {
        String id = r.getInstanceId();
        return (id == null) ? null : id.trim().toLowerCase();
    }


}
