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

package org.springframework.cloud.gateway.sample.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Rate Limiter Configuration
 *
 * <p>
 * Provides KeyResolver beans for the Redis-based rate limiter.
 *
 * <p>
 * The KeyResolver determines the key used for rate limiting.
 * Multiple KeyResolver beans can be defined and selected in the configuration.
 *
 * @author Frank
 */
@Configuration
public class RateLimiterConfig {

	private static final String UNKNOWN_IP = "0.0.0.0";

	/**
	 * Extract client IP address from the exchange.
	 *
	 * @param exchange the server web exchange
	 * @return client IP address, or "0.0.0.0" if not available
	 */
	private static String getClientIp(ServerWebExchange exchange) {
		return exchange.getRequest().getRemoteAddress() != null
				? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
				: UNKNOWN_IP;
	}

	/**
	 * IP Address Key Resolver
	 *
	 * <p>
	 * Rate limits based on the client's IP address.
	 *
	 * <p>
	 * Usage in YAML: <pre>{@code
	 * default-filters:
	 *   - name: RequestRateLimiter
	 *     args:
	 *       key-resolver: "#{@ipKeyResolver}"
	 *       redis-rate-limiter:
	 *         replenish-rate: 10
	 *         burst-capacity: 20
	 * }</pre>
	 *
	 * @return KeyResolver that uses IP address as the key
	 */
	@Bean
	public KeyResolver ipKeyResolver() {
		return exchange -> Mono.just(getClientIp(exchange));
	}

	/**
	 * Path Key Resolver
	 *
	 * <p>
	 * Rate limits based on the request path.
	 * Useful for limiting specific endpoints.
	 *
	 * @return KeyResolver that uses request path as the key
	 */
	@Bean
	public KeyResolver pathKeyResolver() {
		return exchange -> Mono.just(exchange.getRequest().getPath().value());
	}

	/**
	 * Smart Key Resolver - Combines IP + Path Segment
	 *
	 * <p>
	 * Creates rate limit keys based on IP address + first path segment under /rate/.
	 * This allows different rate limits for different API groups while using a single route config.
	 *
	 * <p>
	 * Example key patterns:
	 * <ul>
	 * <li>Request to /gateway/rate/api/users → key: "192.168.1.1:api"</li>
	 * <li>Request to /gateway/rate/admin/users → key: "192.168.1.1:admin"</li>
	 * <li>Request to /gateway/rate/public/data → key: "192.168.1.1:public"</li>
	 * </ul>
	 *
	 * <p>
	 * Combined with multiple Redis rate limiters, you can set different limits per API group.
	 *
	 * @return KeyResolver that combines IP + path segment for granular rate limiting
	 */
	@Bean(name = "smartKeyResolver")
	@Primary
	public KeyResolver smartKeyResolver() {
		return exchange -> {
			String ip = getClientIp(exchange);
			String path = exchange.getRequest().getPath().value();
			String segment = "default";

			// Pattern: /gateway/rate/{segment}/...
			String[] parts = path.split("/");
			if (parts.length > 3 && "rate".equals(parts[2])) {
				segment = parts[3];
			}

			return Mono.just(ip + ":" + segment);
		};
	}

}
