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

package net.maritimeconnectivity.serviceregistry.exceptions;

import org.springframework.http.HttpStatus;

/**
 * The Ledger Connection Exception.
 *
 * An exception that is designed to be thrown whenever the connection to the
 * ledger fails.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class LedgerConnectionException extends MSRBaseException {

    private static final long serialVersionUID = -4001242965130326029L;

    /**
     * The Exception HTTP Status Code.
     */
    private static HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;

    /**
     * Instantiates an empty Ledger Connection exception.
     */
    public LedgerConnectionException() {
        super("Error while connecting to the MSR ledger", null, httpStatus);
    }

    /**
     * Instantiates a new Ledger Connection exception.
     *
     * @param message the message
     * @param t       the t
     */
    public LedgerConnectionException(String message, Throwable t) {
        super(message, t, httpStatus);
    }

}
