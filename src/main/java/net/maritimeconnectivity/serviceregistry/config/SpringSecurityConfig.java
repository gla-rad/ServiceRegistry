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

package net.maritimeconnectivity.serviceregistry.config;

import jakarta.servlet.DispatcherType;
import net.maritimeconnectivity.serviceregistry.config.keycloak.KeycloakGrantedAuthoritiesMapper;
import net.maritimeconnectivity.serviceregistry.config.keycloak.KeycloakJwtAuthenticationConverter;
import net.maritimeconnectivity.serviceregistry.config.keycloak.KeycloakLogoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.Arrays;

/**
 * The Spring Security Configuration.
 *
 * This is the security definition for the security configuration and the filter
 * chains the service.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Configuration
@EnableWebSecurity
@EnableReactiveMethodSecurity
@ConditionalOnProperty(value = "keycloak.enabled", matchIfMissing = true)
class SpringSecurityConfig {

    /**
     * The default application name.
     */
    @Value("${keycloak.clientId:service-registry}")
    private String clientId;

    /**
     * The default application name.
     */
    @Value("${gla.rad.aton-service.resources.open:/,/index,/webjars/**,/static/src/**,/static/css/**,/static/images/**,/api/xmls/schemas/**,/api/secom/**}")
    private String[] openResources;

    /**
     * The REST Template.
     *
     * @return the REST template
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Define a slightly more flexible HTTP Firewall configuration that allows
     * characters like semicolons, slashes and percentages.
     */
    @Bean
    protected HttpFirewall securityHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowSemicolon(true);
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowUrlEncodedPercent(true);
        return firewall;
    }

    /**
     * Forwarded header filter registration bean.
     * <p>
     * This corrects the urls produced by the microservice when accessed from a proxy server.
     * E.g. Api gateway:
     * my-service.com/style.css -> api-gateway.com/my-service/style.css
     * <p>
     * The proxy server should be sending the forwarded header address as a header
     * which this filter will pick up and resolve for us.
     *
     * @return the filter registration bean
     */
    @Bean
    protected FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        final FilterRegistrationBean<ForwardedHeaderFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new ForwardedHeaderFilter());
        filterRegistrationBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterRegistrationBean;
    }

    /**
     * Specify a converter for the Keycloak authority claims.
     *
     * @return The Keycloak JWT Authentication Converter
     */
    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> keycloakJwtAuthenticationConverter() {
        return new KeycloakJwtAuthenticationConverter(this.clientId);
    }

    /**
     * Specify a mapper for the keycloak authority claims.
     *
     * @return the Keycloak Granted Authority Mapper
     */
    @Bean
    protected GrantedAuthoritiesMapper keycloakGrantedAuthoritiesMapper() {
        return new KeycloakGrantedAuthoritiesMapper(this.clientId);
    }

    /**
     * Define a logout handler for handling Keycloak logouts.
     *
     * @param restTemplate the REST template
     * @return the Keycloak logout handler
     */
    @Bean
    protected KeycloakLogoutHandler keycloakLogoutHandler(RestTemplate restTemplate) {
        return new KeycloakLogoutHandler(restTemplate);
    }

    /**
     * Define the session authentication strategy which uses a simple session
     * registry to store our current sessions.
     *
     * @return the session authentication strategy
     */
    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    /**
     * Defines the security web-filter chains.
     *
     * Allows open access to the health and info actuator endpoints.
     * All other actuator endpoints are only available for the actuator role.
     * Finally, all other exchanges need to be authenticated.
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ClientRegistrationRepository clientRegistrationRepository,
                                           RestTemplate restTemplate) throws Exception {
        // Register the CORS preflight filter
        http.addFilterBefore(new SimpleCorsFilter(), ChannelProcessingFilter.class);
        // Authenticate through configured OpenID Provider
        http.oauth2Login(login -> login
                .loginPage("/oauth2/authorization/keycloak")
//                .authorizationEndpoint().baseUri("/oauth2/authorization/keycloak")
//                .authorizationRequestRepository(new HttpSessionOAuth2AuthorizationRequestRepository());
        );
        // Also, logout at the OpenID Connect provider
        http.logout(logout -> logout
                .deleteCookies("JSESSIONID")
                .addLogoutHandler(keycloakLogoutHandler(restTemplate))
                .logoutSuccessUrl("/")
//                .logoutSuccessHandler(new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository)));
        );
        // Require authentication for all requests
        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(EndpointRequest.to(
                                InfoEndpoint.class,     //info endpoints
                                HealthEndpoint.class    //health endpoints
                        )).permitAll()
                        .requestMatchers(EndpointRequest.toAnyEndpoint())
                        .hasRole("ACTUATOR")
                        .requestMatchers(new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.name()))
                        .permitAll() // Allow preflight requests
                        .requestMatchers(new AntPathRequestMatcher("/v3/api-docs", HttpMethod.GET.name()))
                        .permitAll() // Allow request to Swagger file
                        .requestMatchers(Arrays.stream(this.openResources)
                                .map(AntPathRequestMatcher::new)
                                .toArray(AntPathRequestMatcher[]::new))
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(keycloakJwtAuthenticationConverter())
                        )
                );
        // Disable the CSRF
        http.csrf(AbstractHttpConfigurer::disable);

        // Build and return the chain
        return http.build();
    }

}
