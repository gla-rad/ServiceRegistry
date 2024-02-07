/*
 * Copyright (c) 2024 Maritime Connectivity Platform Consortium
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

package net.maritimeconnectivity.serviceregistry.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.serviceregistry.models.domain.UserToken;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * The ClientJwtTokenUtility Utility Class
 *
 * This class allow operations on top of the JWT tokens such as the extraction
 * of claim information.
 *
 * @author nva
 */
@Component
@Slf4j
public class ClientJwtTokenUtility {

    /**
     * Generate token user token.
     *
     * @param jwtToken the jwt token
     * @return the user token
     */
    public UserToken getTokenFromString(final String jwtToken) {
        UserToken userToken = null;
        try {
            final Claims tokenClaims = getClaimFromToken(jwtToken, Function.identity());
            userToken = new UserToken(tokenClaims);
        } catch (IllegalArgumentException e) {
            log.info("Unable to get JWT Token");
        } catch (ExpiredJwtException e) {
            log.info("JWT Token has expired");
        } catch (Exception e) {
            log.warn("Unmapped JWT error - {}", e.getMessage());
        }
        return userToken;
    }

    /**
     * Gets all claims from token.
     * <p>
     * Decodes the token string into a Claims object containing the
     * properties of the token.
     *
     * @param token the token
     * @return the all claims from token
     */
    public Claims getAllClaimsFromToken(final String token) {
        // Strips out the signature, so that we can read the payload.
        // As a result, we shouldn't trust this token unless we are sure it ca
        // be validated by <auth server>.
        final String noSignatureToken = token.replaceFirst("[^\\.]*$", "");
        return (Claims) Jwts.parser().parse(noSignatureToken).getBody();
    }

    /**
     * Gets claim from token.
     *
     * @param <T>            the type parameter
     * @param token          the token
     * @param claimsResolver the claims resolver
     * @return the claim from token
     */
    protected <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

}
