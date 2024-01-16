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
 * The Invalid Request Exception.
 *
 * A generic runtime exception to be thrown whenever the provided request does
 * not appear to be valid.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class InvalidRequestException extends MSRBaseException {

    /**
     * The Exception HTTP Status Code.
     */
    private static HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

    /**
     * Instantiates an empty Data Not Found exception.
     */
    public InvalidRequestException() {
        super("The provided request does not appear to be valid", null, httpStatus);
    }

    /**
     * Instantiates a new Data Not Found exception.
     *
     * @param message the message
     * @param t       the throwable
     */
    public InvalidRequestException(String message, Throwable t) {
        super(message, t, httpStatus);
    }

}
