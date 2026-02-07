/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gateway.sample.filter;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.isomorphism.util.TokenBucket;
import org.isomorphism.util.TokenBuckets;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting Filter - Token Bucket Algorithm
 *
 * <p>Functionality:
 * Implements request rate limiting using the token bucket algorithm to prevent system overload.
 *
 * <p>Token Bucket Algorithm:
 * <ul>
 *   <li>Bucket has a fixed capacity of tokens</li>
 *   <li>Tokens are refilled at a fixed rate</li>
 *   <li>When a request arrives, it tries to consume a token</li>
 *   <li>Token available: request is allowed</li>
 *   <li>No token available: request is rejected with 429 error</li>
 * </ul>
 *
 * <p>Usage Example:
 * <pre>{@code
 * new ThrottleGatewayFilter()
 *     .setCapacity(10)              // Bucket capacity: 10 tokens
 *     .setRefillTokens(1)           // Tokens per refill: 1
 *     .setRefillPeriod(1)           // Refill period: 1
 *     .setRefillUnit(TimeUnit.SECONDS)  // Time unit: seconds
 * }</pre>
 *
 * <p>Reference: https://github.com/bbeck/token-bucket
 *
 * @author Spencer Gibb
 */
@Slf4j
@Data
@Accessors(chain = true)
public class ThrottleGatewayFilter implements GatewayFilter {

	/** Token bucket instance (volatile ensures visibility across threads) */
	@Getter(value = AccessLevel.PRIVATE)
	private volatile TokenBucket tokenBucket;

	/** Bucket capacity - maximum number of tokens */
	private int capacity;

	/** Number of tokens to refill each time */
	private int refillTokens;

	/** Refill period time value */
	private int refillPeriod;

	/** Refill period time unit */
	private TimeUnit refillUnit;

	/**
	 * Get token bucket instance (double-checked locking)
	 *
	 * <p>Implementation Details:
	 * <ul>
	 *   <li>First check: lock-free, fast return for existing instance</li>
	 *   <li>Lock & create: ensures thread safety</li>
	 *   <li>Second check: prevents multiple creation (other thread may have created while waiting)</li>
	 * </ul>
	 *
	 * @return TokenBucket instance
	 */
	private TokenBucket getTokenBucket() {
		// First check: if already exists, return directly (avoid unnecessary locking)
		if (tokenBucket != null) {
			return tokenBucket;
		}

		// Synchronize to ensure thread safety
		synchronized (this) {
			// Second check: prevent creating another instance while waiting for lock
			if (tokenBucket == null) {
				tokenBucket = TokenBuckets.builder()
						.withCapacity(capacity)  // Set bucket capacity
						.withFixedIntervalRefillStrategy(
								refillTokens,    // Tokens to add per refill
								refillPeriod,    // Refill interval
								refillUnit       // Time unit
						)
						.build();
			}
		}
		return tokenBucket;
	}

	/**
	 * Filter request - implements rate limiting logic
	 *
	 * <p>Processing Flow:
	 * <ol>
	 *   <li>Get token bucket instance</li>
	 *   <li>Try to consume a token (tryConsume)</li>
	 *   <li>Success: continue processing request</li>
	 *   <li>Failure: return 429 TOO_MANY_REQUESTS</li>
	 * </ol>
	 *
	 * @param exchange current request exchange object
	 * @param chain filter chain
	 * @return async completion signal
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// Get token bucket instance
		TokenBucket tokenBucket = getTokenBucket();

		// TODO: Could be extended to use different buckets per key
		// For example: by IP, user ID, API Key, etc. for granular rate limiting
		// TokenBucket tokenBucket = getTokenBucketForKey(exchange.getRequest().getRemoteAddress());

		if (log.isDebugEnabled()) {
			log.debug("TokenBucket capacity: " + tokenBucket.getCapacity());
			log.debug("TokenBucket available tokens: " + tokenBucket.getNumTokens());
		}

		// Try to consume a token (non-blocking operation)
		boolean consumed = tokenBucket.tryConsume();

		if (consumed) {
			// Successfully got a token, continue processing request
			if (log.isDebugEnabled()) {
				log.debug("Request allowed - token consumed successfully");
			}
			return chain.filter(exchange);
		}

		// No available token, reject request
		if (log.isDebugEnabled()) {
			log.debug("Request throttled - no tokens available");
		}

		// Set HTTP status code: 429 Too Many Requests
		exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
		return exchange.getResponse().setComplete();
	}

}
