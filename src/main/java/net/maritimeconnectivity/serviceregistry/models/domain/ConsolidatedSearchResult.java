package net.maritimeconnectivity.serviceregistry.models.domain;

import lombok.Getter;
import org.grad.secomv2.core.models.SearchObjectResult;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all consolidated results for a single search transaction.
 *
 * Keyed by a deduplication key (TBD but for now use instanceId),
 * values are full SearchObjectResult objects.
 */
public class ConsolidatedSearchResult {

    @Getter
    private final String transactionId;
    private final Map<String, SearchObjectResult> results;

    private ConsolidatedSearchResult(String transactionId) {
        this.transactionId = transactionId;
        this.results = new ConcurrentHashMap<>();
    }

    public static ConsolidatedSearchResult create(String transactionId) {
        return new ConsolidatedSearchResult(transactionId);
    }

    /**Dont do thjousan
     * Add a result if not already present (deduplication).
     *
     * @param key a stable deduplication key (TBD but for now use instanceId)
     * @param result the result to add
     * @return true if the result was added, false if it was a duplicate
     */
    public boolean addIfNew(String key, SearchObjectResult result) {
        if (key == null || key.isBlank()) {
            return false; // skip invalid or empty keys
        }
        // Normalize key to avoid duplicates with different casing/whitespace
        String normalizedKey = key.trim().toLowerCase();
        return results.putIfAbsent(normalizedKey, result) == null;
    }


    /**
     * @return a read-only snapshot of all consolidated results.
     * Avoids concurrent modification issues, as more results may be added by GMSP
     */
    public Collection<SearchObjectResult> snapshot() {
        return java.util.List.copyOf(results.values());
    }

}
