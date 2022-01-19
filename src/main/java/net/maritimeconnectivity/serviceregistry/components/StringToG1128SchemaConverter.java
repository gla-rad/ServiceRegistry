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

import net.maritimeconnectivity.serviceregistry.models.domain.enums.G1128Schemas;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * The Sting to G1128 Schemas Converter.
 *
 * This utility class can convert strings to G1128 Schemas enumeration entries.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Component
public class StringToG1128SchemaConverter implements Converter<String, G1128Schemas> {

    @Override
    public G1128Schemas convert(String source) {
        for (G1128Schemas s : G1128Schemas.values()) {
            if (s.getName().equalsIgnoreCase(source)) {
                return s;
            }
        }
        return null;
    }

}
