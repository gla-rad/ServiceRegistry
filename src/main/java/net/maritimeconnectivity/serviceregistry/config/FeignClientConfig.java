/*
 * Copyright (c) 2024 GLA Research and Development Directorate
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

package net.maritimeconnectivity.serviceregistry.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * The FeignClientConfig Class.
 * <p>
 * This configuration provides the OAuth2 authorization for the Feign clients.
 * It will inject new authorization tokens into the feign request header through
 * a request interceptor.
 * <p>
 * Note that this configuration is not annotated, but it should be injected
 * directly to the feign requests that do not already have authorization.
 * <p>
 * The best source for this type of an implementation can be found here:
 * <a>
 *     https://stackoverflow.com/questions/55308918/spring-security-5-calling-oauth2-secured-api-in-application-runner-results-in-il
 * </a>
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Configuration
@ConditionalOnProperty(value = "keycloak.enabled", matchIfMissing = true)
public class FeignClientConfig {


    /**
     * The OIDC Registered Client ID
     */
    @Value("${spring.security.oauth2.client.registration.feign.client-id:mcpsvreg}")
    String clientId;

    /**
     * The Feign Logger Level Configuration.
     * @return the feign logger level
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * The OAuth2 Authorized Client Manager bean provider. In the new Spring
     * Security 5 framework, we can use the OAuth2AuthorizedClientService
     * class to authorize our clients, as long as the configuration is found
     * in the application.properties file.
     *
     * @param clientRegistrationRepository the client registration repository
     * @param clientService the OAuth2 authorized client service
     * @return the OAuth2 authorized client manager to authorize the feign requests
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrationRepository,
                                                                 OAuth2AuthorizedClientService clientService) {
        // First create an OAuth2 Authorized Client Provider
        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        // Create a client manage to handle the Feign authorization
        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                clientService
        );

        // Set the client provider in the client
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        // And return
        return authorizedClientManager;
    }

    /**
     * The Feign request interceptor that will inject the new authorization
     * tokens. To generate those token, it will use the OAuth2AuthorizedClient
     * manager defined above.
     *
     * To generate our token, we need a principal but our setup with service
     * accounts in keycloak doesn't care about that. So in here we use an
     * anonymous authentication token.
     *
     * @param manager the OAuth2 authorized client manager to authorize the feign requests
     * @return the Feign request interceptor
     */
    @Bean
    public RequestInterceptor repositoryClientOAuth2Interceptor(OAuth2AuthorizedClientManager manager) {
        return requestTemplate -> {
            OAuth2AuthorizedClient client = manager.authorize(OAuth2AuthorizeRequest
                    .withClientRegistrationId("feign")
                    .principal(new AnonymousAuthenticationToken("name", this.clientId, AuthorityUtils.createAuthorityList("ROLE_SERVICE")))
                    .build());
            String accessToken = client.getAccessToken().getTokenValue();
            requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        };
    }

}
