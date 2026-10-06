package net.maritimeconnectivity.serviceregistry.utils;

import org.grad.secomv2.core.models.ServiceInstanceObject;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Converts a list of {@link ServiceInstanceObject} into a deterministic,
 * field-value-based byte representation suitable for inclusion in a SECOM
 * envelope's signature CSV attribute array.
 * <p>
 * {@link ServiceInstanceObject} does not override {@code toString()} or
 * implement the SECOM {@code CsvStringGenerator} interface, so passing it
 * (or a list of it) directly into an envelope's attribute array would fall
 * back to identity-based {@code Object.toString()}, which differs between
 * the objects signed on the sender side and the objects deserialized on the
 * receiver side even when all field values are equal. Serializing to
 * canonical JSON bytes instead ties the signed content to actual field
 * values, and routes through the existing byte[] (Base64) handling in
 * {@code CsvStringGenerator.attributeConversion}.
 */
public final class ServiceInstanceCsvUtil {

    // Dedicated, self-contained mapper - deliberately not the Spring-managed
    // ObjectMapper bean, so its configuration can never drift for unrelated
    // web-serialization reasons and silently break signature compatibility
    // between peer nodes. Alphabetical property sorting makes the JSON
    // output independent of JVM/library reflection ordering.
    private static final JsonMapper CANONICAL_MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();

    private ServiceInstanceCsvUtil() {
    }

    public static byte[] toCanonicalBytes(List<ServiceInstanceObject> serviceInstances) {
        return CANONICAL_MAPPER.writeValueAsBytes(serviceInstances == null ? List.of() : serviceInstances);
    }
}
