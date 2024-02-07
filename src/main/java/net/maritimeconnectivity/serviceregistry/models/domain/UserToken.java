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

package net.maritimeconnectivity.serviceregistry.models.domain;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.AuthenticatedPrincipal;

import java.io.Serializable;
import java.util.*;


/**
 * The type User token.
 *
 * A useful class to retrieve and store the authentication token information.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class UserToken implements AuthenticatedPrincipal, Serializable {

    protected static final String TK_PREFERRED_USERNAME = "preferred_username";
    protected static final String TK_SCOPE = "scope";
    protected static final String TK_ORGANISATION = "org";
    protected static final String TK_CLIENT = "azp";
    protected static final String TK_RESOURCE_ACCESS = "resource_access";
    protected static final String TK_ROLES = "roles";

    /**
     * The Id of the token.
     *
     * Should be unique to the token.
     */
    private String id;

    /**
     * The Username.
     */
    private String username;

    /**
     * The Company.
     */
    private String organisation;

    /**
     * The Roles.
     */
    private List<String> roles;

    /**
     * The Client
     */
    private String client;

    /**
     * The expiration date
     */
    private Date expirationDate;

    /**
     * Empty Constructor.
     */
    public UserToken() {

    }

    /**
     * Instantiates a new User token.
     *
     * @param claims the claims
     */
    public UserToken(final Claims claims) {
        setUsername(claims.get(TK_PREFERRED_USERNAME, String.class));
        setId(claims.getId());
        setOrganisation(claims.get(TK_ORGANISATION, String.class));
        setClient(claims.get(TK_CLIENT, String.class));

        // Read through the client roles
        Optional.of(claims)
                .map(c -> c.get(TK_RESOURCE_ACCESS, LinkedHashMap.class))
                .map(c -> c.get(this.getClient()))
                .filter(LinkedHashMap.class::isInstance)
                .map(LinkedHashMap.class::cast)
                .map(c -> c.get(TK_CLIENT))
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .ifPresent(this::setRoles);

        // No setter for the expiration data -- just save it manually
        this.expirationDate = claims.getExpiration();
    }

    /**
     * Gets id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }


    /**
     * Gets username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets username.
     *
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets organisation.
     *
     * @return the organisation
     */
    public String getOrganisation() {
        return organisation;
    }

    /**
     * Sets organisation.
     *
     * @param organisation the organisation
     */
    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    /**
     * Gets roles.
     *
     * @return the roles
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * Sets roles.
     *
     * @param roles the roles
     */
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    /**
     * Gets client.
     *
     * @return the client
     */
    public String getClient() {
        return client;
    }

    /**
     * Sets client.
     *
     * @param client the client
     */
    public void setClient(String client) {
        this.client = client;
    }

    /**
     * Gets expiration date.
     *
     * @return the expiration date
     */
    public Date getExpirationDate() {
        return expirationDate;
    }

    /**
     * Returns the name of the authenticated <code>Principal</code>. Never <code>null</code>.
     *
     * @return the name of the authenticated <code>Principal</code>
     */
    @Override
    public String getName() {
        return getUsername();
    }

}
