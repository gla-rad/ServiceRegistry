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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ClientJwtTokenUtilityTest {

    private  static final String SECRET = "c2VjcmV0";

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
    @BeforeEach
    void setup() throws Exception {
        this.clientJwtTokenUtility = new ClientJwtTokenUtility();
        this.expiredToken = Jwts.builder()
                .setExpiration(new Date(1L))
                .compact();
    }

    /**
     * Test get claims for expired or empty token.
     */
    @Test
    void testGetTokenFromStringForExpiredOrEmptyToken() {
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
    void testGetTokenFromString() {
        final UserToken userToken = clientJwtTokenUtility.getTokenFromString(Jwts.builder()
                .claim("test", true)
                .compact());
        assertNotNull(userToken);
    }

    /**
     * Test that the client utility will be able to read the encrypted payload.
     */
    @Test
    void testGetAllClaimsFromDoesNotFail() {
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