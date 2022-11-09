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

package net.maritimeconnectivity.serviceregistry.config;

import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak Configuration.
 *
 * Spring Boot 2.6.x seems to have introduced some change causing the
 * previously-working integration with Keycloak to have a circular reference,
 * preventing application start; it works and starts fine with the current
 * 2.5.x release.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Configuration
public class KeycloakConfiguration {

    /**
     * On multi-tenant scenarios, Keycloak will defer the resolution of a
     * KeycloakDeployment to the target application at the request-phase.
     *
     * A Request object is passed to the resolver and callers expect a complete
     * KeycloakDeployment. Based on this KeycloakDeployment, Keycloak will
     * resume authenticating and authorizing the request.
     */
    @Bean
    public KeycloakSpringBootConfigResolver KeycloakConfigResolver() {
        return new KeycloakSpringBootConfigResolver();
    }

}
