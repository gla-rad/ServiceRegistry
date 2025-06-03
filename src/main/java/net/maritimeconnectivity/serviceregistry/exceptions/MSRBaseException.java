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

import net.maritimeconnectivity.serviceregistry.models.JsonSerializable;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

/**
 * The Service Registry Base Exception Class
 * <p>
 * This is an abstract class implementation so that all defined MSR exceptions
 * that are thrown during runtime can extend it and be handled by a common
 * exception resolver.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public abstract class MSRBaseException extends RuntimeException implements Serializable, JsonSerializable {

    private static final long serialVersionUID = -1219501175564027979L;

    /**
     * The HTTP Status of the exception.
     */
    protected HttpStatus httpStatus;

    /**
     * Instantiates a new MSRBaseException exception.
     *
     * @param message    the message
     * @param t          the t
     * @param httpStatus the http status
     */
    public MSRBaseException(String message, Throwable t, HttpStatus httpStatus) {
        super(message, t);
        this.httpStatus = httpStatus;
    }

    /**
     * Gets http status.
     *
     * @return the http status
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Sets http status.
     *
     * @param httpStatus the http status
     */
    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

}
