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

package net.maritimeconnectivity.serviceregistry.components;

import net.maritimeconnectivity.serviceregistry.components.StringToG1128SchemaConverter;
import net.maritimeconnectivity.serviceregistry.models.domain.enums.G1128Schemas;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StringToG1128SchemaConverterTest {

    /**
     * Test that we can convert a valid string (regardless of the casing) into
     * the G1128 schemas enum.
     */
    @Test
    void testConvert() {
        StringToG1128SchemaConverter converter = new StringToG1128SchemaConverter();
        assertEquals(G1128Schemas.BASE, converter.convert("base"));
        assertEquals(G1128Schemas.BASE, converter.convert("Base"));
        assertEquals(G1128Schemas.BASE, converter.convert("BASE"));
        assertEquals(G1128Schemas.DESIGN, converter.convert("design"));
        assertEquals(G1128Schemas.DESIGN, converter.convert("Design"));
        assertEquals(G1128Schemas.DESIGN, converter.convert("DESIGN"));
        assertEquals(G1128Schemas.SERVICE, converter.convert("service"));
        assertEquals(G1128Schemas.SERVICE, converter.convert("Service"));
        assertEquals(G1128Schemas.SERVICE, converter.convert("SERVICE"));
        assertEquals(G1128Schemas.INSTANCE, converter.convert("instance"));
        assertEquals(G1128Schemas.INSTANCE, converter.convert("Instance"));
        assertEquals(G1128Schemas.INSTANCE, converter.convert("INSTANCE"));
    }

    /**
     * Test that for invalid inputs, the StringToG1128SchemaConverter will
     * return null.
     */
    @Test
    void testConvertInvalid() {
        StringToG1128SchemaConverter converter = new StringToG1128SchemaConverter();
        assertNull(converter.convert(null));
        assertNull(converter.convert(""));
        assertNull(converter.convert("invalid"));
    }
}