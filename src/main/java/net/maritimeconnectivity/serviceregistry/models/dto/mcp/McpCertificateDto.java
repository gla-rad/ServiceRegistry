/*
 * Copyright (c) 2025 Maritime Connectivity Platform Consortium
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

package net.maritimeconnectivity.serviceregistry.models.dto.mcp;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.maritimeconnectivity.serviceregistry.utils.LocalDateTimeDeserializer;
import org.grad.secom.core.base.DateTimeSerializer;

import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * The MCP Certificate DTO Class.
 * <p/>
 * This class is used to load the represent the certificate entries as they
 * come directly from the MCP Identity Registry.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class McpCertificateDto {

    private BigInteger id;
    private String certificate;
    @JsonSerialize(using = DateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime start;
    @JsonSerialize(using = DateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime end;
    private String serialNumber;
    private boolean revoked;
    @JsonSerialize(using = DateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime revokedAt;
    private String revokeReason;

    /**
     * Gets id.
     *
     * @return the id
     */
    public BigInteger getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(BigInteger id) {
        this.id = id;
    }

    /**
     * Gets certificate.
     *
     * @return the certificate
     */
    public String getCertificate() {
        return certificate;
    }

    /**
     * Sets certificate.
     *
     * @param certificate the certificate
     */
    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    /**
     * Gets start.
     *
     * @return the start
     */
    public LocalDateTime getStart() {
        return start;
    }

    /**
     * Sets start.
     *
     * @param start the start
     */
    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    /**
     * Gets end.
     *
     * @return the end
     */
    public LocalDateTime getEnd() {
        return end;
    }

    /**
     * Sets end.
     *
     * @param end the end
     */
    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    /**
     * Gets serial number.
     *
     * @return the serial number
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * Sets serial number.
     *
     * @param serialNumber the serial number
     */
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    /**
     * Is revoked boolean.
     *
     * @return the boolean
     */
    public boolean isRevoked() {
        return revoked;
    }

    /**
     * Sets revoked.
     *
     * @param revoked the revoked
     */
    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    /**
     * Gets revoked at.
     *
     * @return the revoked at
     */
    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    /**
     * Sets revoked at.
     *
     * @param revokedAt the revoked at
     */
    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    /**
     * Gets revoke reason.
     *
     * @return the revoke reason
     */
    public String getRevokeReason() {
        return revokeReason;
    }

    /**
     * Sets revoke reason.
     *
     * @param revokeReason the revoke reason
     */
    public void setRevokeReason(String revokeReason) {
        this.revokeReason = revokeReason;
    }

}
