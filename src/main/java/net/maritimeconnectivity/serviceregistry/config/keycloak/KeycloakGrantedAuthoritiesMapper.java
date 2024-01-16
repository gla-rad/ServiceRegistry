/*
 * Copyright (c) 2024 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.serviceregistry.config.keycloak;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Keycloak Granted Authorities Mapper Class
 *
 * This class is used to convert the Keycloak roles into a format understood
 * by Springboot i.e. add the ROLE_ prefix.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class KeycloakGrantedAuthoritiesMapper implements GrantedAuthoritiesMapper {

    // Class Variables
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_CLAIM = "roles";

    /**
     * The specified Keycloak Resource ID.
     */
    private final String resourceId;

    /**
     * The Class Constructor.
     *
     * @param resourceId the name of the keycloak resource ID
     */
    public KeycloakGrantedAuthoritiesMapper(String resourceId)
    {
        this.resourceId = resourceId;
    }

    /**
     * The implementation of the interface function that performs the authority
     * mapping operation. It is able to handle both OIDC and OAuth2 inputs
     * and provides the mapped collection of GrantedAuthority objects.
     *
     * @param authorities   the collection of authorities
     * @return the populated collection of GrantedAuthority objects
     */
    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        final Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
        final GrantedAuthority authority = authorities.iterator().next();
        final boolean isOidc = authority instanceof OidcUserAuthority;

        if (isOidc) {
            var oidcUserAuthority = (OidcUserAuthority) authority;
            var userInfo = oidcUserAuthority.getUserInfo();

            if (userInfo.hasClaim(RESOURCE_ACCESS_CLAIM)) {
                final Map<String, Object> realmAccess = (Map<String, Object>) userInfo.getClaimAsMap(RESOURCE_ACCESS_CLAIM);
                final Map<String, Object> clientAccess = (Map<String, Object>) realmAccess.get(this.resourceId);
                final Collection<String> roles = (Collection<String>) clientAccess.get(ROLES_CLAIM);
                mappedAuthorities.addAll(this.generateAuthoritiesFromClaim(roles));
            }
        } else {
            var oauth2UserAuthority = (OAuth2UserAuthority) authority;
            Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();

            if (userAttributes.containsKey(RESOURCE_ACCESS_CLAIM)) {
                final Map<String, Object> realmAccess = (Map<String, Object>) userAttributes.get(RESOURCE_ACCESS_CLAIM);
                final Map<String, Object> clientAccess = (Map<String, Object>) realmAccess.get(this.resourceId);
                final Collection<String> roles = (Collection<String>) clientAccess.get(ROLES_CLAIM);
                mappedAuthorities.addAll(this.generateAuthoritiesFromClaim(roles));
            }
        }
        return mappedAuthorities;
    }

    /**
     * This helper function gets the provided collection of roles and translates
     * them into a collection of GrantedAuthority objects, adding the "ROLE_"
     * prefix so that they can be understood by Springboot.
     *
     * @param roles the collection of roles
     * @return the respective collection of GrantedAuthority objects
     */
    private Collection<GrantedAuthority> generateAuthoritiesFromClaim(Collection<String> roles) {
        return roles.stream().map(x -> new SimpleGrantedAuthority("ROLE_" + x.toUpperCase())).collect(Collectors.toList());
    }
}
