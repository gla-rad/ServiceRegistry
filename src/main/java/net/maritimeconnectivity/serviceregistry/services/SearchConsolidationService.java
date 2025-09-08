package net.maritimeconnectivity.serviceregistry.services;


import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.config.CacheConfig;
import net.maritimeconnectivity.serviceregistry.models.domain.ConsolidatedSearchResult;
import org.grad.secomv2.core.models.SearchObjectResult;
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
            addResult(transactionId, r);
        }
    }

    /** Add a single result to the transaction’s consolidated set (creates the entry if absent).
     * Do not add duplicate results*/
    public void addResult(String transactionId, SearchObjectResult result) {
        ConsolidatedSearchResult agg = getOrCreate(transactionId);
        String key = getKey(result);            // choose your canonical key; instanceId for now
        if (key != null && !key.isBlank()) {
            agg.addIfNew(key, result);            // dedup happens inside the aggregator
        }
    }

    /** Read all results currently stored for the transaction (immutable snapshot). */
    public List<SearchObjectResult> getResults(String transactionId) {
        ConsolidatedSearchResult agg = sessions.get(transactionId, ConsolidatedSearchResult.class);
        return (agg == null) ? List.of() : List.copyOf(agg.snapshot());
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
