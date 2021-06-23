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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import net.maritimeconnectivity.serviceregistry.models.domain.UserToken;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class ClientJwtTokenUtilityTest {

    private  static final String SECRET = "c2VjcmV0";
    private  static final String GENERAL_TOKEN_1 = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICItbzk2UUFFYXlCbDFRTm1lRHlCTVRkSFM1cHlVVVJHbVdhTmRua2syTnMwIn0.eyJleHAiOjE2MjQzNzUxODksImlhdCI6MTYyNDM3NDg4OSwianRpIjoiMzk0Zjc2NWQtY2NiYi00ZTQzLWI5YjYtNTRlNmExYjc0NGQ5IiwiaXNzIjoiaHR0cDovL3BhbGF0aWEuZ3JhZC1ycm5hdi5wdWI6ODA5MC9hdXRoL3JlYWxtcy9NQ1AiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiNDI1N2NlZTQtZTBlZS00YTRkLWJjMjMtM2VkODE2NTQzNGNjIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoic2VydmljZS1yZWdpc3RyeSIsInNlc3Npb25fc3RhdGUiOiJkYjhhYTMxYS0wMGFkLTRhN2UtYmU4NS1hOTg5YTRkYTQyOWYiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbIioiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX0sInNlcnZpY2UtcmVnaXN0cnkiOnsicm9sZXMiOlsiYWRtaW4iXX19LCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm9yZyI6IkdMQSIsIm5hbWUiOiJTeXN0ZW0gQWRtaW4iLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzeXNhZG1pbiIsImdpdmVuX25hbWUiOiJTeXN0ZW0iLCJmYW1pbHlfbmFtZSI6IkFkbWluIiwiZW1haWwiOiJzeXNhZG1pbkBtY3Aub3JnIn0";

    /**
     * The Test token util.
     */
    private ClientJwtTokenUtility clientJwtTokenUtility;

    /**
     * The Expired token.
     */
    private String expiredToken;

    /**
     * Sets the testing configuration.
     */
    @Before
    public void setup() throws Exception {
        clientJwtTokenUtility = new ClientJwtTokenUtility();
        expiredToken = Jwts.builder()
                .setExpiration(new Date(1L))
                .compact();
    }

    /**
     * Test get claims for expired or empty token.
     */
    @Test
    public void testGetTokenFromStringForExpiredOrEmptyToken() {
        //Expired token
        assertNull(clientJwtTokenUtility.getTokenFromString(null));

        //Expired token
        assertNull(clientJwtTokenUtility.getTokenFromString(expiredToken));

        //Empty token
        assertNull(clientJwtTokenUtility.getTokenFromString(""));
    }

    /**
     * Test get token from string.
     */
    @Test
    public void testGetTokenFromString() {
        final UserToken userToken = clientJwtTokenUtility.getTokenFromString(Jwts.builder()
                .claim("test", true)
                .compact());
        assertNotNull(userToken);
    }

    /**
     * Test that the client utility will be able to read the encrypted payload.
     */
    @Test
    public void testGetAllClaimsFromDoesNotFail() {
        //Create a temporary token
        final String token = Jwts.builder()
                .setId(BigInteger.ONE.toString())
                .setSubject("Subject")
                .claim("CLAIM_1", "Claim 1")
                .claim("CLAIM_2", "Claim 2")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(Date.from(LocalDateTime.now().plusHours(1).toInstant(ZoneOffset.UTC)))
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();

        Claims claims = clientJwtTokenUtility.getAllClaimsFromToken(token);

        assertEquals("Claim 1", claims.get("CLAIM_1"));
        assertEquals("Claim 2", claims.get("CLAIM_2"));
    }
}