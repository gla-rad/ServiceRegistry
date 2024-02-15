/*
 * Copyright (c) 2021 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.maritimeconnectivity.serviceregistry.models.dto.mcp;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * The MCP Service DTO Class.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class McpServiceDto extends McpEntityBase {

    // Class Variables
    @NotNull
    private String name;
    private String permissions;
    private String oidcAccessType;
    private String oidcClientId;
    private String oidcClientSecret;
    private String oidcRedirectUri;
    private String certDomainName;
    private String instanceVersion;

    /**
     * Instantiates a new Mcp service dto.
     */
    public McpServiceDto() {
    }

    /**
     * Instantiates a new Mcp service dto.
     *
     * @param mrn  the mrn
     * @param name the name
     */
    public McpServiceDto(String mrn, String name) {
        super(mrn);
        this.name = name;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets permissions.
     *
     * @return the permissions
     */
    public String getPermissions() {
        return permissions;
    }

    /**
     * Sets permissions.
     *
     * @param permissions the permissions
     */
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    /**
     * Gets oidc access type.
     *
     * @return the oidc access type
     */
    public String getOidcAccessType() {
        return oidcAccessType;
    }

    /**
     * Sets oidc access type.
     *
     * @param oidcAccessType the oidc access type
     */
    public void setOidcAccessType(String oidcAccessType) {
        this.oidcAccessType = oidcAccessType;
    }

    /**
     * Gets oidc client id.
     *
     * @return the oidc client id
     */
    public String getOidcClientId() {
        return oidcClientId;
    }

    /**
     * Sets oidc client id.
     *
     * @param oidcClientId the oidc client id
     */
    public void setOidcClientId(String oidcClientId) {
        this.oidcClientId = oidcClientId;
    }

    /**
     * Gets oidc client secret.
     *
     * @return the oidc client secret
     */
    public String getOidcClientSecret() {
        return oidcClientSecret;
    }

    /**
     * Sets oidc client secret.
     *
     * @param oidcClientSecret the oidc client secret
     */
    public void setOidcClientSecret(String oidcClientSecret) {
        this.oidcClientSecret = oidcClientSecret;
    }

    /**
     * Gets oidc redirect uri.
     *
     * @return the oidc redirect uri
     */
    public String getOidcRedirectUri() {
        return oidcRedirectUri;
    }

    /**
     * Sets oidc redirect uri.
     *
     * @param oidcRedirectUri the oidc redirect uri
     */
    public void setOidcRedirectUri(String oidcRedirectUri) {
        this.oidcRedirectUri = oidcRedirectUri;
    }

    /**
     * Gets cert domain name.
     *
     * @return the cert domain name
     */
    public String getCertDomainName() {
        return certDomainName;
    }

    /**
     * Sets cert domain name.
     *
     * @param certDomainName the cert domain name
     */
    public void setCertDomainName(String certDomainName) {
        this.certDomainName = certDomainName;
    }

    /**
     * Gets instance version.
     *
     * @return the instance version
     */
    public String getInstanceVersion() {
        return instanceVersion;
    }

    /**
     * Sets instance version.
     *
     * @param instanceVersion the instance version
     */
    public void setInstanceVersion(String instanceVersion) {
        this.instanceVersion = instanceVersion;
    }

    /**
     * Overrides the equality operator of the class.
     *
     * @param o the object to check the equality
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof McpServiceDto)) return false;
        if (!super.equals(o)) return false;
        McpServiceDto that = (McpServiceDto) o;
        return Objects.equals(name, that.name);
    }

    /**
     * Overrides the hashcode generation of the object.
     *
     * @return the generated hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }
}
