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

import net.maritimeconnectivity.serviceregistry.models.domain.UserToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UserContextTest {

    /**
     * The User Context.
     */
    @InjectMocks
    UserContext userContext;

    /**
     * The Client JWT Token Utility.
     */
    @Mock
    ClientJwtTokenUtility clientJwtTokenUtility;

    /**
     * The OAuth Authorised Client Service mock.
     */
    @Mock
    OAuth2AuthorizedClientService clientService;

    //Test Variables
    private UserToken userToken;
    private OAuth2AuthenticationToken oAuth2Authentication;
    private OAuth2AuthorizedClient oAuth2AuthorizedClient;
    private OAuth2AccessToken oAuth2AccessToken;
    private JwtAuthenticationToken jwtAuthentication;
    private Jwt jwtAuthenticationToken;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Create a user token
        this.userToken = new UserToken();
        this.userToken.setUsername("username");
    }

    /**
     * Test that we can read the JWT string out of the loaded user context and
     * it's gonna be the same as the one returned from keycloak, when using
     * OAuth2 tokens.
     */
    @Test
    void testGetJwtStringForOauth2() {
        // Now mock the OAuth2 authentication
        this.oAuth2AccessToken = mock(OAuth2AccessToken.class);
        doReturn("dontCareAboutThisNow").when(this.oAuth2AccessToken).getTokenValue();
        this.oAuth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
        doReturn(this.oAuth2AccessToken).when(this.oAuth2AuthorizedClient).getAccessToken();
        this.oAuth2Authentication = mock(OAuth2AuthenticationToken.class);
        doReturn("authorisedClientRegistrationId").when(oAuth2Authentication).getAuthorizedClientRegistrationId();
        doReturn("name").when(oAuth2Authentication).getName();
        doReturn(this.oAuth2AuthorizedClient).when(this.clientService).loadAuthorizedClient(any(), any());

        SecurityContextHolder.getContext().setAuthentication(this.oAuth2Authentication);
        final String jwtString = userContext.getJwtString().orElse(null);
        assertEquals("dontCareAboutThisNow", jwtString);
    }

    /**
     * Test that we can read the JWT string out of the loaded user context and
     * it's gonna be the same as the one returned from keycloak, when using JWT
     * tokens.
     */
    @Test
    void testGetJwtStringForJwt() {
        // Finally mock the JWT authentication
        this.jwtAuthenticationToken = mock(Jwt.class);
        doReturn("dontCareAboutThisNow").when(this.jwtAuthenticationToken).getTokenValue();
        this.jwtAuthentication = mock(JwtAuthenticationToken.class);
        doReturn(this.jwtAuthenticationToken).when(this.jwtAuthentication).getToken();

        SecurityContextHolder.getContext().setAuthentication(this.jwtAuthentication);
        final String jwtString = userContext.getJwtString().orElse(null);
        assertEquals("dontCareAboutThisNow", jwtString);
    }

    /**
     * Test that we will not read a JWT string for the loaded user context if
     * no authentication is provided.
     */
    @Test
    void testGetJwtStringForNoAuth() {
        SecurityContextHolder.getContext().setAuthentication(null);
        assertNull(userContext.getJwtString().orElse(null));
    }

    /**
     * Test that we can read the JWT token out of the loaded user context and
     * it's gonna be the same as the one returned from keycloak, when using
     * OAuth2 tokens.
     */
    @Test
    void testGetJwtTokenForOAuth2() {
        // Now mock the OAuth2 authentication
        this.oAuth2AccessToken = mock(OAuth2AccessToken.class);
        doReturn("dontCareAboutThisNow").when(this.oAuth2AccessToken).getTokenValue();
        this.oAuth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
        doReturn(this.oAuth2AccessToken).when(this.oAuth2AuthorizedClient).getAccessToken();
        oAuth2Authentication = mock(OAuth2AuthenticationToken.class);
        doReturn("authorisedClientRegistrationId").when(oAuth2Authentication).getAuthorizedClientRegistrationId();
        doReturn("name").when(oAuth2Authentication).getName();
        doReturn(this.oAuth2AuthorizedClient).when(this.clientService).loadAuthorizedClient(any(), any());

        SecurityContextHolder.getContext().setAuthentication(this.oAuth2Authentication);
        doReturn(this.userToken).when(clientJwtTokenUtility).getTokenFromString(any());
        final UserToken userToken = userContext.getJwtToken().orElse(null);
        assertNotNull(userToken);
        assertEquals(this.userToken.getName(), userToken.getName());
    }

    /**
     * Test that we can read the JWT token out of the loaded user context and
     * it's gonna be the same as the one returned from keycloak, when using JWT
     * tokens.
     */
    @Test
    void testGetJwtTokenForJwt() {
        // Finally mock the JWT authentication
        this.jwtAuthenticationToken = mock(Jwt.class);
        doReturn("dontCareAboutThisNow").when(this.jwtAuthenticationToken).getTokenValue();
        this.jwtAuthentication = mock(JwtAuthenticationToken.class);
        doReturn(this.jwtAuthenticationToken).when(this.jwtAuthentication).getToken();

        SecurityContextHolder.getContext().setAuthentication(this.jwtAuthentication);
        doReturn(this.userToken).when(clientJwtTokenUtility).getTokenFromString(any());
        final UserToken userToken = userContext.getJwtToken().orElse(null);
        assertNotNull(userToken);
        assertEquals(this.userToken.getName(), userToken.getName());
    }

    /**
     * Test that we will not read the JWT token for the loaded user context if
     * no authentication is provided.
     */
    @Test
    void testGetJwtTokenForNoAuth() {
        SecurityContextHolder.getContext().setAuthentication(null);
        assertNull(userContext.getJwtToken().orElse(null));
    }

}