/*
 * Copyright (c) 2025 Maritime Connectivity Platform Consortium
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.grad.secom.core.base.DateTimeDeSerializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * The LocalDateDeserializer Class.
 * <p/>
 * This deserializer is used to easily decode the timestamp format of the
 * timestamp fields coming from the MIR. We should probably keep them into
 * LocalDateTime format in our end so using this utility makes this quite easy.
 * <p/>
 * Note that for serializing back these objects we should use the standard
 * SECOM method, since it's included in the SECOM responses.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */

public class LocalDateTimeDeserializer extends DateTimeDeSerializer {

    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        try {
            long timestamp = jsonParser.getLongValue();
            return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (IOException ex) {
            // In case of errors, try the standard SECOM approach
            return super.deserialize(jsonParser, deserializationContext);
        }
    }

}
