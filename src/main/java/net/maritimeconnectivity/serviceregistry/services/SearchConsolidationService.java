package net.maritimeconnectivity.serviceregistry.services;


import ch.qos.logback.core.net.server.Client;
import ch.qos.logback.core.net.server.ConcurrentServerRunner;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.components.Gmsp;
import net.maritimeconnectivity.serviceregistry.config.CacheConfig;
import net.maritimeconnectivity.serviceregistry.exceptions.InvalidRequestException;
import net.maritimeconnectivity.serviceregistry.models.domain.ConsolidatedSearchResult;
import org.grad.secomv2.core.models.SearchObjectResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public void addResults(String transactionId, List<SearchObjectResult> results) {
        for (SearchObjectResult r : results) {
            log.debug("Added result {}", r.getName());
            addResult(transactionId, r);
        }
    }

    public void createConsolidationEntry(String transactionId) {
        ConsolidatedSearchResult agg = getOrCreate(transactionId);
        log.debug("Created consolidation entry for transactionId {}", transactionId);
    }

    /** Add a single result to the transaction’s consolidated set (creates the entry if absent).
     * Do not add duplicate results*/
    public void addResult(String transactionId, SearchObjectResult result) {
        ConsolidatedSearchResult agg = getOrCreate(transactionId);
        String key = getKey(result);            // choose your canonical key; instanceId for now
        if (key != null && !key.isBlank()) {
            agg.addIfNew(key, result);            // dedup happens inside the aggregator
        } else {
            throw new InvalidRequestException("No results found for transaction id " + transactionId);
        }
    }

    /** Read all results currently stored for the transaction (immutable snapshot). */
    public List<SearchObjectResult> getResults(String transactionId) {
        var agg = sessions.get(transactionId, ConsolidatedSearchResult.class);
        if (agg == null) {
            log.debug("No results found for transactionId {}", transactionId);
            return null;
        }

        // Capture a stable snapshot exactly once
        List<SearchObjectResult> snapshot = List.copyOf(agg.snapshot()); // defensive copy

        // Log using the same snapshot
        for (SearchObjectResult r : snapshot) {
            log.debug("Retrieved result {}", r.getName());
        }

        log.debug("--NOW RETURNING {} RESULTS--", snapshot.size());
        return snapshot;
    }



    private ConsolidatedSearchResult getOrCreate(String transactionId) {
        return sessions.get(transactionId, () -> ConsolidatedSearchResult.create(transactionId));
    }

    /** For now: instanceId as the dedup key; adjust if you adopt a different canonical key later. */
    private String getKey(SearchObjectResult r) {
        String id = r.getInstanceId();
        return (id == null) ? null : id.trim().toLowerCase();
    }


}
