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

package net.maritimeconnectivity.serviceregistry.exceptions;

import org.springframework.http.HttpStatus;

/**
 * The Duplicate Data Exception.
 * <p>
 * A generic runtime exception to be thrown whenever important data is about
 * to be duplicated.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class DuplicateDataException extends MSRBaseException {

    private static final long serialVersionUID = -3189233115083685487L;

    /**
     * The Exception HTTP Status Code.
     */
    private static HttpStatus httpStatus = HttpStatus.CONFLICT;

    /**
     * Instantiates an empty Duplicate Data exception.
     */
    public DuplicateDataException() {
        super("Duplicate data detected", null, httpStatus);
    }

    /**
     * Instantiates a new Duplicate Data exception.
     *
     * @param message the message
     * @param t       the throwable
     */
    public DuplicateDataException(String message, Throwable t) {
        super(message, t, httpStatus);
    }

}

