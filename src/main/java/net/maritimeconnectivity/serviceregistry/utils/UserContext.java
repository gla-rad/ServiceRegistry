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

import net.maritimeconnectivity.serviceregistry.models.domain.UserToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * The type User context.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserContext {

	/**
	 * The JTW Token Utility
	 */
	@Autowired
	private ClientJwtTokenUtility jwtTokenUtility;

	/**
	 * The OAuth Authorised Client Service.
	 */
	@Autowired
	OAuth2AuthorizedClientService clientService;

	/**
	 * Gets JWT string if it exists.
	 *
	 * @return the JWT string if it exists
	 */
	public  Optional<String> getJwtString() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return Optional.ofNullable(authentication)
				.filter(OAuth2AuthenticationToken.class::isInstance)
				.map(OAuth2AuthenticationToken.class::cast)
				.map(t -> clientService.loadAuthorizedClient(t.getAuthorizedClientRegistrationId(), t.getName()))
				.map(OAuth2AuthorizedClient.class::cast)
				.map(OAuth2AuthorizedClient::getAccessToken)
				.map(OAuth2AccessToken::getTokenValue);
	}

	/**
	 * Gets JWT token if it exists.
	 *
	 * @return the JWT token if it exists
	 */
	public Optional<UserToken> getJwtToken() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return Optional.ofNullable(authentication)
				.filter(OAuth2AuthenticationToken.class::isInstance)
				.map(OAuth2AuthenticationToken.class::cast)
				.map(t -> clientService.loadAuthorizedClient(t.getAuthorizedClientRegistrationId(), t.getName()))
				.map(OAuth2AuthorizedClient.class::cast)
				.map(OAuth2AuthorizedClient::getAccessToken)
				.map(OAuth2AccessToken::getTokenValue)
				.map(this.jwtTokenUtility::getTokenFromString);
	}

}
