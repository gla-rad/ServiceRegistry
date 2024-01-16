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

package net.maritimeconnectivity.serviceregistry.controllers.advices;

import jakarta.servlet.http.HttpServletRequest;
import net.maritimeconnectivity.serviceregistry.exceptions.MSRBaseException;
import net.maritimeconnectivity.serviceregistry.utils.HeaderUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * Implementation of the conflict handling.
     *
     * @param ex        The exception that took place
     * @param request   The request that causes the exception
     * @return The response entity with the appropriate headers and status code
     */
    @ExceptionHandler(MSRBaseException.class)
    protected ResponseEntity<Object> handleConflict(MSRBaseException ex, WebRequest request) {
        final String entityName = Optional.of(request)
                .filter(ServletWebRequest.class::isInstance)
                .map(ServletWebRequest.class::cast)
                .map(ServletWebRequest::getRequest)
                .map(HttpServletRequest::getRequestURI)
                .map(uri -> uri.split("/"))
                .filter(array -> array.length > 2)
                .map(array -> array[2])
                .map(s -> s.replaceAll("s$", ""))
                .orElse("general-entity");
        final String requestBody = Optional.of(request)
                .filter(ServletWebRequest.class::isInstance)
                .map(ServletWebRequest.class::cast)
                .map(ServletWebRequest::getRequest)
                .map(r -> { try { return r.getReader(); } catch (IOException e) { return null; } })
                .map(BufferedReader::lines)
                .orElseGet(Stream::empty)
                .collect(Collectors.joining(System.lineSeparator()));
        return handleExceptionInternal(ex,
                requestBody,
                HeaderUtil.createFailureAlert(entityName, ex.getMessage(), ex.toString()),
                ex.getHttpStatus(),
                request);
    }

}
