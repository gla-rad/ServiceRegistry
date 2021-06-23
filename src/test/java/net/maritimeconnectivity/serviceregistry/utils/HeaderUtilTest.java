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

package net.maritimeconnectivity.serviceregistry.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.*;

class HeaderUtilTest {

    /**
     * Test that we can create the alert headers with both the "X-mcsrApp-alert"
     * and "X-mcsrApp-params" fields.
     */
    @Test
    void testCreateAlert() {
        HttpHeaders headers = HeaderUtil.createAlert("message", "param");
        assertNotNull(headers);
        assertTrue(headers.containsKey("X-mcsrApp-alert"));
        assertEquals("[message]", headers.get("X-mcsrApp-alert").toString());
        assertTrue(headers.containsKey("X-mcsrApp-params"));
        assertEquals("[param]", headers.get("X-mcsrApp-params").toString());
    }

    /**
     * Test that we can create the creation alert headers.
     */
    @Test
    void testCreateEntityCreationAlert() {
        HttpHeaders headers = HeaderUtil.createEntityCreationAlert("entity", "param");
        assertNotNull(headers);
        assertTrue(headers.containsKey("X-mcsrApp-alert"));
        assertTrue(headers.containsKey("X-mcsrApp-params"));
        assertEquals("[param]", headers.get("X-mcsrApp-params").toString());
    }

    /**
     * Test that we can create the update alert headers.
     */
    @Test
    void testCreateEntityUpdateAlert() {
        HttpHeaders headers = HeaderUtil.createEntityUpdateAlert("entity", "param");
        assertNotNull(headers);
        assertTrue(headers.containsKey("X-mcsrApp-alert"));
        assertTrue(headers.containsKey("X-mcsrApp-params"));
        assertEquals("[param]", headers.get("X-mcsrApp-params").toString());
    }

    /**
     * Test that we can create the delete alert headers.
     */
    @Test
    void testCreateEntityDeletionAlert() {
        HttpHeaders headers = HeaderUtil.createEntityDeletionAlert("entity", "param");
        assertNotNull(headers);
        assertTrue(headers.containsKey("X-mcsrApp-alert"));
        assertTrue(headers.containsKey("X-mcsrApp-params"));
        assertEquals("[param]", headers.get("X-mcsrApp-params").toString());
    }

    /**
     * Test that we can create the status update alert headers.
     */
    @Test
    void testCreateEntityStatusUpdateAlert() {
        HttpHeaders headers = HeaderUtil.createEntityStatusUpdateAlert("entity", "param");
        assertNotNull(headers);
        assertTrue(headers.containsKey("X-mcsrApp-alert"));
        assertTrue(headers.containsKey("X-mcsrApp-params"));
        assertEquals("[param]", headers.get("X-mcsrApp-params").toString());
    }

    /**
     * Test that we can create the creation failure alert headers.
     */
    @Test
    void testCreateFailureAlert() {
        HttpHeaders headers = HeaderUtil.createFailureAlert("entity", "key", "message");
        assertNotNull(headers);
        assertTrue(headers.containsKey("X-mcsrApp-error"));
        assertEquals("[error.key]", headers.get("X-mcsrApp-error").toString());
        assertTrue(headers.containsKey("X-mcsrApp-params"));
        assertEquals("[entity]", headers.get("X-mcsrApp-params").toString());
    }
}