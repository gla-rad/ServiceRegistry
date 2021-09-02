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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * The type for request to ledger.
 * <p>
 * An service instance can make a request to the MSR ledger by a service provider.
 * The type takes place to describe a status in such process of interaction with the ledger, e.g., registration.
 * It is assumed that there exists a vetting procedure for instance registration to the ledger.
 *
 * @author Jinki Jung (email: jinki@dmc.international)
 */

@Entity
@Table(name = "ledgerrequest")
public class LedgerRequest {

    @NotNull
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn(unique = true)
    private Instance serviceInstance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(30) default 'created'")
    private LedgerRequestStatus status = LedgerRequestStatus.CREATED;

    @Column(name = "reason", nullable = true)
    private String reason;

    @Column(name = "created_at", updatable = false, nullable = true)
    private String createdAt;

    @Column(name = "last_updated_at", nullable = true)
    private String lastUpdatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instance getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(Instance serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public LedgerRequestStatus getStatus() {
        return status;
    }

    public void setStatus(LedgerRequestStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String submittedAt) {
        this.createdAt = submittedAt;
    }

    public String getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(String lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
