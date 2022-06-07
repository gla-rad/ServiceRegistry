/*
 * Copyright (c) 2021 Maritime Connectivity Platform Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.serviceregistry.models.dto;

import net.maritimeconnectivity.serviceregistry.models.JsonSerializable;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * The Ledger Request DTO Class.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class LedgerRequestDto implements Serializable, JsonSerializable {

    // Class Variables
    private Long id;
    @NotNull
    private Long serviceInstanceId;
    private LedgerRequestStatus status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;

    /**
     * Instantiates a new Ledger request dto.
     */
    public LedgerRequestDto() {

    }

    /**
     * Gets id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets service instance id.
     *
     * @return the service instance id
     */
    public Long getServiceInstanceId() {
        return serviceInstanceId;
    }

    /**
     * Sets service instance id.
     *
     * @param serviceInstanceId the service instance id
     */
    public void setServiceInstanceId(Long serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public LedgerRequestStatus getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(LedgerRequestStatus status) {
        this.status = status;
    }

    /**
     * Gets reason.
     *
     * @return the reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets reason.
     *
     * @param reason the reason
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Gets created at.
     *
     * @return the created at
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets created at.
     *
     * @param createdAt the created at
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets last updated at.
     *
     * @return the last updated at
     */
    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    /**
     * Sets last updated at.
     *
     * @param lastUpdatedAt the last updated at
     */
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

}
