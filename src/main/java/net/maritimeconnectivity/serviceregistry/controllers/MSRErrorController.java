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

package net.maritimeconnectivity.serviceregistry.controllers;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * The MSR Error Controller
 *
 * This is kind o deprecated but it's still saves us a lot of trouble when
 * keycloak is complaining and sending us to the error page.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Controller
@RequestMapping("/error")
public class MSRErrorController implements ErrorController {

    /**
     * Redirect all errors to the main page.
     *
     * @return the main page redirect
     */
    @RequestMapping
    public ModelAndView handleError() {
        return new ModelAndView("redirect:" + "/");
    }

    @Override
    public String getErrorPath() {
        return null;
    }

}
