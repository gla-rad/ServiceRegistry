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
 * The type Xml validation exception.
 */
public class XMLValidationException extends Exception {

    private static final long serialVersionUID = -1801881599895016351L;

    /**
     * Instantiates a new Xml validation exception.
     *
     * @param message the message
     * @param t       the t
     */
    public XMLValidationException(String message, Throwable t) {
        super(message, t);
    }

}
