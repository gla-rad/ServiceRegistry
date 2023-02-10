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

package net.maritimeconnectivity.serviceregistry.models.domain;

import net.maritimeconnectivity.serviceregistry.models.domain.enums.LedgerRequestStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * The type for request to ledger.
 * <p>
 * An service instance can make a request to the MSR ledger by a service provider.
 * The type takes place to describe a status in such process of interaction with
 * the ledger, e.g., registration. It is assumed that there exists a vetting
 * procedure for instance registration to the ledger.
 * </p>
 * @author Jinki Jung (email: jinki@dmc.international)
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ledgerrequest")
public class LedgerRequest implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(30) default 'created'")
    private LedgerRequestStatus status = LedgerRequestStatus.CREATED;

    @Column(name = "reason", nullable = true)
    private String reason;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = true)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_updated_at", nullable = true)
    private LocalDateTime lastUpdatedAt;

    @NotNull
    @OneToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "instance_id")
    private Instance serviceInstance;

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
     * Gets service instance.
     *
     * @return the service instance
     */
    public Instance getServiceInstance() {
        return serviceInstance;
    }

    /**
     * Sets service instance.
     *
     * @param serviceInstance the service instance
     */
    public void setServiceInstance(Instance serviceInstance) {
        this.serviceInstance = serviceInstance;
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
     * @param submittedAt the submitted at
     */
    public void setCreatedAt(LocalDateTime submittedAt) {
        this.createdAt = submittedAt;
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

    /**
     * Overrides the equality operator of the class.
     *
     * @param o the object to check the equality
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LedgerRequest)) return false;
        LedgerRequest that = (LedgerRequest) o;
        return Objects.equals(id, that.id) && serviceInstance.equals(that.serviceInstance);
    }

    /**
     * Overrides the hashcode generation of the object.
     *
     * @return the generated hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, serviceInstance);
    }

}
