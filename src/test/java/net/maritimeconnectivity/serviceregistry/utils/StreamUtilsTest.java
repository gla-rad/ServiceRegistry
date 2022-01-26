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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static net.maritimeconnectivity.serviceregistry.utils.StreamUtils.catchExceptionToNull;
import static net.maritimeconnectivity.serviceregistry.utils.StreamUtils.peek;
import static org.junit.jupiter.api.Assertions.*;

class StreamUtilsTest {

    /**
     * Test that we can peek on objects while streaming without changing their
     * actual values.
     */
    @Test
    void testPeekOnStream() {
        List<String> input = Arrays.asList(new String[]{"one", "two", "three"});
        List<String> result = input.stream()
                .map(peek(v -> v = v.toUpperCase()))
                .collect(Collectors.toList());

        assertNotNull(result);
        assertEquals(3, result.size());
        for(int i=0; i<result.size(); i++) {
            assertEquals(input.get(i), result.get(i));
        }
    }

    /**
     * Test that we can peek on objects inside an optional without changing
     * their actual values.
     */
    @Test
    void testPeekOnOptional() {
        String result = Optional.of("object")
                .map(peek(o -> o = o.toUpperCase()))
                .orElse(null);

        assertNotNull(result);
        assertEquals("object", result);
    }

    /**
     * Test that we can catch all exception inside an optional and direct
     * them to the alternative result.
     */
    @Test
    void testCatchExceptionToNull() {
        String result = Optional.of("object")
                .map(catchExceptionToNull(o -> Integer.toString(1 / 0)))
                .orElse("exception caught");

        assertNotNull(result);
        assertEquals("exception caught", result);
    }

}