/*
 * Copyright (c) 2023 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.serviceregistry.config.keycloak;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Keycloak JTW Authentication Converter Class
 *
 * This class is used to convert the Keycloak roles into a format understood
 * by Springboot i.e. add the ROLE_ prefix.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken>  {

    /**
     * The JWT Default JWT Authorities Converter.
     */
    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    /**
     * The specified Keycloak Resource ID.
     */
    private final String resourceId;

    /**
     * The Class Constructor.
     *
     * @param resourceId the name of the keycloak resource ID
     */
    public KeycloakJwtAuthenticationConverter(String resourceId)
    {
        this.resourceId = resourceId;
    }

    /**
     * Defines the conversion operation in which the roles will be adjusted with
     * the correct prefix.
     *
     * @param jwt the keycloak-generated JWT token
     * @return the adjusted authentication token
     */
    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                        defaultGrantedAuthoritiesConverter.convert(jwt).stream(),
                        extractResourceRoles(jwt, resourceId).stream())
                .collect(Collectors.toSet());
        return new JwtAuthenticationToken(jwt, authorities);
    }

    /**
     * This helper function performs the actual conversion and pickes up all the
     * roles defined for the specified resource ID, in order to add the ROLE_
     * prefix.
     *
     * @param jwt the keycloak-generated JWT token
     * @param resourceId the resource ID to pick up the roles for
     * @return the adjusted authentication token
     */
    private static Collection<? extends GrantedAuthority> extractResourceRoles(final Jwt jwt, final String resourceId)
    {
        // Parse the incoming JWT token
        final Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        final Collection<String> resourceRoles= Optional.ofNullable(resourceAccess)
                .map(map -> map.get(resourceId))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(map -> map.get("roles"))
                .filter(Collection.class::isInstance)
                .map(Collection.class::cast)
                .map(roles -> new ArrayList<String>(roles))
                .orElse(null);
        // Map the roles with a ROLE_ prefix
        return Optional.ofNullable(resourceRoles)
                .orElse(Collections.emptySet())
                .stream()
                .map(x -> new SimpleGrantedAuthority("ROLE_" + x.toUpperCase()))
                .collect(Collectors.toList());
    }

}
