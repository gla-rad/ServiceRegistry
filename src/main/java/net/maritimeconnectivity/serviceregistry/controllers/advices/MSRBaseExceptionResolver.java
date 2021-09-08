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

package net.maritimeconnectivity.serviceregistry.controllers.advices;

import net.maritimeconnectivity.serviceregistry.exceptions.MSRBaseException;
import net.maritimeconnectivity.serviceregistry.utils.HeaderUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The MSR Base Exception Resolver Class.
 *
 * This class implements a global controller advice in order to capture and
 * map the runtime exceptions thrown as the correct HTTP status responses.
 * This advice only handles exceptions that extend the MSRBaseException class.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@ControllerAdvice
public class MSRBaseExceptionResolver extends ResponseEntityExceptionHandler {

    /**
     * Implementation of the confict handling.
     *
     * @param ex        The exception that took place
     * @param request   The request that causes the exception
     * @return The response entity with the appropriate headers and status code
     */
    @ExceptionHandler(MSRBaseException.class)
    protected ResponseEntity<Object> handleConflict(MSRBaseException ex, WebRequest request) {
        return handleExceptionInternal(ex, ex,
                HeaderUtil.createFailureAlert("entity", "error", ex.getMessage()),
                ex.getHttpStatus(),
                request);
    }

}
