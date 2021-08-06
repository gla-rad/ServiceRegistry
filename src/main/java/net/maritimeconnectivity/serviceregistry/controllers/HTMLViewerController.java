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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * The HTML Viewer Controller.
 *
 * This is the home controller that allows user to view the main options.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Controller
public class HTMLViewerController {

    @Value("${info.msr.name:Unknown}")
    private String msrName;

    @Value("${info.msr.mrn:Unknown}")
    private String msrMrn;

    @Value("${info.msr.url:Unknown}")
    private String msrUrl;

    @Value("${info.msr.operatorName:Unknown}")
    private String operatorName;

    @Value("${info.msr.operatorMrn:Unknown}")
    private String operatorMrn;

    @Value("${info.msr.operatorContact:Unknown}")
    private String operatorContact;

    @Value("${info.msr.operatorUrl:}")
    private String operatorUrl;

    @Value("${info.msr.copyright:}")
    private String copyright;

    @Value("${info.msr.profile:}")
    private String profile;

    @Value("${info.msr.projectLocation:}")
    private String projectLocation;

    /**
     * The management page of MSR.
     *
     * @param model The application UI model
     * @return The index page
     */
    @GetMapping("/index.html")
    public String index(Model model) {
        model.addAttribute("operatorUrl", this.operatorUrl);
        model.addAttribute("copyright", this.copyright);
        model.addAttribute("profile", this.profile);
        model.addAttribute("projectLocation", this.projectLocation);
        return "index";
    }

    /**
     * The instance page
     *
     * @param model The application UI model
     * @return The index page
     */
    @GetMapping("/instances")
    public String instances(Model model) {
        model.addAttribute("operatorUrl", this.operatorUrl);
        model.addAttribute("copyright", this.copyright);
        model.addAttribute("profile", this.profile);
        return "instances";
    }

    /**
     * The instance page
     *
     * @param model The application UI model
     * @return The index page
     */
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("MSRName", msrName);
        model.addAttribute("MSRMRN", msrMrn);
        model.addAttribute("MSRURL", msrUrl);
        model.addAttribute("operatorName", operatorName);
        model.addAttribute("operatorMRN", operatorMrn);
        model.addAttribute("contact", operatorContact);
        model.addAttribute("operatorUrl", this.operatorUrl);
        model.addAttribute("copyright", this.copyright);
        model.addAttribute("profile", this.profile);
        return "about";
    }

    /**
     * Logs the user in an authenticated session and redirect to the home page.
     *
     * @param request The logout request
     * @return The home page
     */
    @GetMapping(path = "/login")
    public ModelAndView login(HttpServletRequest request) {
        return new ModelAndView("redirect:" + "/");
    }

    /**
     * Logs the user out of the authenticated session.
     *
     * @param request The logout request
     * @return The home page
     * @throws ServletException Servlet Exception during the logout
     */
    @GetMapping(path = "/logout")
    public ModelAndView logout(HttpServletRequest request) throws ServletException {
        request.logout();
        return new ModelAndView("redirect:" + "/");
    }

}
