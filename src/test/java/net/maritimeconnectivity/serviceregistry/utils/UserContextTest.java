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
import org.junit.Assert;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private OAuth2AuthenticationToken authentication;
    private OAuth2AuthorizedClient oAuth2AuthorizedClient;
    private OAuth2AccessToken oAuth2AccessToken;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Create a user token
        this.userToken = new UserToken();
        this.userToken.setUsername("username");

        // Now mock the authentication
        this.oAuth2AccessToken = mock(OAuth2AccessToken.class);
        doReturn("dontCareAboutThisNow").when(this.oAuth2AccessToken).getTokenValue();
        this.oAuth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
        doReturn(this.oAuth2AccessToken).when(this.oAuth2AuthorizedClient).getAccessToken();
        authentication = mock(OAuth2AuthenticationToken.class);
        doReturn("authorisedClientRegistrationId").when(authentication).getAuthorizedClientRegistrationId();
        doReturn("name").when(authentication).getName();
        doReturn(this.oAuth2AuthorizedClient).when(this.clientService).loadAuthorizedClient(any(), any());
    }

    /**
     * Test that we can read the JWT string out of the loaded user context and
     * it's gonna be the same as the one returned from keycloak.
     */
    @Test
    void testGetJwtString() {
        SecurityContextHolder.getContext().setAuthentication(this.authentication);
        final String jwtString = userContext.getJwtString().get();
        Assert.assertEquals("dontCareAboutThisNow", jwtString);
    }

    /**
     * Test that we can read the JWT token out of the loaded user context and
     * it's gonna be the same as the one returned from keycloak.
     */
    @Test
    void testGetJwtToken() {
        SecurityContextHolder.getContext().setAuthentication(this.authentication);
        doReturn(this.userToken).when(clientJwtTokenUtility).getTokenFromString(any());
        final UserToken userToken = userContext.getJwtToken().get();
        assertNotNull(userToken);
        assertEquals(this.userToken.getName(), userToken.getName());
    }

}