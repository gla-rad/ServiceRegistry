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

package net.maritimeconnectivity.serviceregistry.utils;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The Stream Utils Class.
 *
 * A set of useful tools when trying to write a nice set of streams or
 * optional mapping operations.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class StreamUtils {

    /**
     * Allows peeking into the handled objects without manipulating the
     * object itself. This way we can actually use the objects setter
     * or getters and produce visually appealing code.
     *
     * @param c     The consumer the performs the peek
     * @param <T>   The return type of the operation
     * @return A unary operator function that performs the peek
     */
    public static <T> UnaryOperator<T> peek(Consumer<T> c) {
        Objects.requireNonNull(c);
        return x -> {
            c.accept(x);
            return x;
        };
    }

    /**
     * Catches all thrown exceptions and returns a null result. This could be
     * quite useful while handling objects inside an optional stream where
     * thrown exceptions could be grouped regardless and pointed to a common
     * error.
     *
     * @param f     The function to monitor for exceptions
     * @param <T>   The input type of the function
     * @param <U>   The return type of the function
     * @return An encompassing function that handles all exceptions
     */
    public static <T, U> Function<T, U> catchExceptionToNull(Function<? super T, ? extends U> f) {
        Objects.requireNonNull(f);
        return x -> {
            try {
                return f.apply(x);
            } catch(Exception ex) {
                return null;
            }
        };
    }

}
