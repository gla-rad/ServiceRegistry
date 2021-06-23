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
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class UserContextTest {

    /**
     * The User Context.
     */
    @InjectMocks
    UserContext userContext;

    /**
     * They Keycloak Security Context.
     */
    @Mock
    KeycloakSecurityContext context;

    /**
     * The Client JWT Token Utility.
     */
    @Mock
    ClientJwtTokenUtility clientJwtTokenUtility;

    //Test Variables
    private UserToken userToken;
    private KeycloakAuthenticationToken keyCloakToken;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        userToken = new UserToken();
        userToken.setUsername("username");

        final KeycloakAccount keycloakAccount = new OidcKeycloakAccount() {

            @Override
            public Principal getPrincipal() {
                return new BasicUserPrincipal("username");
            }

            @Override
            public Set<String> getRoles() {
                return Collections.emptySet();
            }

            @Override
            public KeycloakSecurityContext getKeycloakSecurityContext() {
                return context;
            }
        };

        keyCloakToken = new KeycloakAuthenticationToken(keycloakAccount, false);
        doReturn("dontCareAboutThisNow").when(context).getTokenString();
    }

    /**
     * Test that we can read the JWT string out of the loaded user context and
     * it's gonna be the same as the one returned from keycloak.
     */
    @Test
    void testGetJwtString() {
        SecurityContextHolder.getContext().setAuthentication(keyCloakToken);
        final String jwtString = userContext.getJwtString().get();
        Assert.assertEquals("dontCareAboutThisNow", jwtString);
    }

    /**
     * Test that we can read the JWT token out of the loaded user context and
     * it's gonna be the same as the one returned from keycloak.
     */
    @Test
    void testGetJwtToken() {
        SecurityContextHolder.getContext().setAuthentication(keyCloakToken);
        doReturn(this.userToken).when(clientJwtTokenUtility).getTokenFromString(any());
        final UserToken userToken = userContext.getJwtToken().get();
        assertNotNull(userToken);
        assertEquals(this.userToken.getName(), userToken.getName());
    }

}