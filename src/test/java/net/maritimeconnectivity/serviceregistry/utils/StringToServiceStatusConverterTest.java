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

import org.iala_aism.g1128.v1_3.servicespecificationschema.ServiceStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StringToServiceStatusConverterTest {

    /**
     * Test that we can convert a valid string (regardless of the casing) into
     * the service status enum.
     */
    @Test
    void testConvert() {
        StringToServiceStatusConverter converter = new StringToServiceStatusConverter();
        assertEquals(ServiceStatus.RELEASED, converter.convert("released"));
        assertEquals(ServiceStatus.RELEASED, converter.convert("Released"));
        assertEquals(ServiceStatus.RELEASED, converter.convert("RELEASED"));
        assertEquals(ServiceStatus.PROVISIONAL, converter.convert("provisional"));
        assertEquals(ServiceStatus.PROVISIONAL, converter.convert("Provisional"));
        assertEquals(ServiceStatus.PROVISIONAL, converter.convert("PROVISIONAL"));
        assertEquals(ServiceStatus.DEPRECATED, converter.convert("deprecated"));
        assertEquals(ServiceStatus.DEPRECATED, converter.convert("Deprecated"));
        assertEquals(ServiceStatus.DEPRECATED, converter.convert("DEPRECATED"));
        assertEquals(ServiceStatus.DELETED, converter.convert("deleted"));
        assertEquals(ServiceStatus.DELETED, converter.convert("Deleted"));
        assertEquals(ServiceStatus.DELETED, converter.convert("DELETED"));
    }

    /**
     * Test that we for invalid input, the StringToServiceStatusConverter will
     * return null.
     */
    @Test
    void testConvertInvalid() {
        StringToServiceStatusConverter converter = new StringToServiceStatusConverter();
        assertNull(converter.convert(null));
        assertNull(converter.convert(""));
        assertNull(converter.convert("invalid"));
    }

}