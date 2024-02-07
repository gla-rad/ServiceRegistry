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

package net.maritimeconnectivity.serviceregistry.exceptions;

import org.springframework.http.HttpStatus;

/**
 * The Ledger Registration Exception.
 *
 * An exception designed to be thrown whenever requests to the ledger
 * registration service fail for any reason.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class LedgerRegistrationError extends MSRBaseException {

    private static final long serialVersionUID = 6333861764563739849L;

    /**
     * The Exception HTTP Status Code.
     */
    private static HttpStatus httpStatus = HttpStatus.BAD_GATEWAY;

    /**
     * Instantiates an empty Ledger Registration exception.
     */
    public LedgerRegistrationError() {
        super("Error while registering instance to the ledger", null, httpStatus);
    }

    /**
     * Instantiates a new Ledger Registration exception.
     *
     * @param message    the message
     * @param t          the t
     */
    public LedgerRegistrationError(String message, Throwable t) {
        super(message, t, httpStatus);
    }

}
