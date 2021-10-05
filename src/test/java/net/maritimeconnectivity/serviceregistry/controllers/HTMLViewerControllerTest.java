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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = HTMLViewerController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class HTMLViewerControllerTest {

    /**
     * The Mock MVC.
     */
    @Autowired
    MockMvc mockMvc;

    /**
     * Test that we can access the main index HTML page.
     */
    @Test
    void testGetIndex() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(get("/index.html")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    /**
     * Test that we can access the instances HTML page.
     */
    @Test
    void testGetInstances() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(get("/instances.html")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    /**
     * Test that we can access the about HTML page.
     */
    @Test
    void testGetAbout() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(get("/about.html")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

}