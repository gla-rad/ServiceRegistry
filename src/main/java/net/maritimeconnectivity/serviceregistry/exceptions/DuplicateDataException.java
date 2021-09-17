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

/**
 * The Duplicate Data Exception.
 * <p>
 * A generic runtime exception to be thrown whenever important data is about
 * to be duplicated.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class DuplicateDataException extends RuntimeException {

    private static final long serialVersionUID = -2171229941490315104L;

    /**
     * Instantiates a new Duplicate data exception.
     *
     * @param message the message
     * @param t       the t
     */
    public DuplicateDataException(String message, Throwable t) {
        super(message, t);
    }

}

