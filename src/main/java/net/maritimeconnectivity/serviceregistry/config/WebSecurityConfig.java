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

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakSecurityComponents;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.ForwardedHeaderFilter;

import javax.servlet.DispatcherType;
import java.security.Principal;

/**
 * The Web Security Configuration.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Configuration
@EnableWebSecurity
@ComponentScan(basePackageClasses = KeycloakSecurityComponents.class)
@ConditionalOnProperty(value = "keycloak.enabled", matchIfMissing = true)
class WebSecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

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
     * Forwarded header filter filter registration bean.
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
     * Defines the session authentication strategy.
     */
    @Bean
    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    /**
     * Rewiring the security adapter to use the KeycloakAuthenticationProvider
     * in order to perform the authentication.
     *
     * @param auth The authentication manager builder
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        SimpleAuthorityMapper grantedAuthorityMapper = new SimpleAuthorityMapper();
        grantedAuthorityMapper.setPrefix("ROLE_");
        grantedAuthorityMapper.setConvertToUpperCase(true);

        KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
        keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(grantedAuthorityMapper);
        auth.authenticationProvider(keycloakAuthenticationProvider);
    }

    /**
     * Override this method to configure {@link WebSecurity} so that we ignore
     * certain requests like swagger, css etc.
     *
     * @param webSecurity The web security
     * @throws Exception Exception thrown while configuring the security
     */
    @Override
    public void configure(WebSecurity webSecurity) throws Exception {
        super.configure(webSecurity);
        webSecurity
                // Set some alternative firewall rules to allow extra characters
                .httpFirewall(securityHttpFirewall())
                //This will not attempt to authenticate these end points.
                //Saves on validation requests.
                .ignoring()
                .antMatchers(
                    "/webjars/**",  //bootstrap
                    "/static/src/**",            //js files
                    "/static/css/**",           //css files
                    "/static/images/**",        //the images
                    "/api/xmls/schemas/*"       //the G1128 schemas
                );
    }

    /**
     * The HTTP security configuration.
     * <p>
     * For now this will allow all requests to the micro-service web, health
     * and login endpoints without any authorisation requirements.
     *
     * @param httpSecurity The HTTP security
     * @throws Exception thrown while configuring the security
     */
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        super.configure(httpSecurity);
        httpSecurity
                .addFilterBefore(new SimpleCorsFilter(), ChannelProcessingFilter.class)
                .csrf().disable()
                .authorizeRequests()
                .antMatchers(
                        "/webjars/**",   //bootstrap
                        "/static/src/**", 		     //js files
                        "/static/css/**", 			 //css files
                        "/static/images/**",         //the images
                        "/" , "index.html"           //the home page
                ).permitAll()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Allow preflight requests
                .antMatchers(HttpMethod.GET, "/v3/api-docs").permitAll() // Allow request to Swagger file
                .requestMatchers(EndpointRequest.to(
                        InfoEndpoint.class,         //info endpoints
                        HealthEndpoint.class        //health endpoints
                )).permitAll()
                .requestMatchers(EndpointRequest.toAnyEndpoint()) //
                .hasRole("ACTUATOR")
                .anyRequest()
                .authenticated();
    }

    /**
     * Allows to inject requests scoped wrapper for {@link KeycloakSecurityContext}.
     *
     * Returns the {@link KeycloakSecurityContext} from the Spring
     * {@link ServletRequestAttributes}'s {@link Principal}.
     * <p>
     * The principal must support retrieval of the KeycloakSecurityContext, so at
     * this point, only {@link KeycloakPrincipal} values and
     * {@link KeycloakAuthenticationToken} are supported.
     *
     * @return the current <code>KeycloakSecurityContext</code>
     */
    @Bean
    @Scope(scopeName = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public KeycloakSecurityContext provideKeycloakSecurityContext() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        Principal principal = attributes.getRequest().getUserPrincipal();
        if (principal == null) {
            return null;
        }

        // Check for authenticated users
        if (principal instanceof KeycloakAuthenticationToken) {
            principal = Principal.class.cast(KeycloakAuthenticationToken.class.cast(principal).getPrincipal());
        }

        // Check for tokens based on context - i.e. other services
        if (principal instanceof KeycloakPrincipal) {
            return KeycloakPrincipal.class.cast(principal).getKeycloakSecurityContext();
        }

        return null;
    }

}
