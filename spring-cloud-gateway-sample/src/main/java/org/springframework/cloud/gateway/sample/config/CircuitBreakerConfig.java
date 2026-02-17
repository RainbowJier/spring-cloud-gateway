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

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Circuit Breaker Fallback Handler
 *
 * <p>
 * Functionality: Provides fallback responses when the circuit breaker is triggered due to
 * downstream service failures.
 *
 * <p>
 * Circuit Breaker States:
 * <ul>
 * <li>CLOSED: Normal operation, requests pass through</li>
 * <li>OPEN: Circuit is tripped, fallback is triggered immediately</li>
 * <li>HALF_OPEN: Testing if downstream service has recovered</li>
 * </ul>
 *
 * <p>
 * Usage Example: <pre>{@code
 * // In route configuration:
 * .filters(f -> f.circuitBreaker("myCircuitBreaker")
 *     .fallbackUri("forward:/fallback"))
 * }</pre>
 *
 * @author Frank
 */
@Slf4j
@RestController
public class CircuitBreakerConfig {

	/**
	 * Fallback endpoint for circuit breaker
	 *
	 * <p>
	 * Called when downstream service is unavailable or circuit breaker is open.
	 *
	 * <p>
	 * Circuit Breaker States:
	 * <ul>
	 * <li>CLOSED: Normal operation, requests pass through</li>
	 * <li>OPEN: Circuit is tripped, fallback is triggered immediately</li>
	 * <li>HALF_OPEN: Testing if downstream service has recovered</li>
	 * </ul>
	 *
	 * <p>
	 * Usage Example: <pre>{@code
	 * # In route configuration:
	 * filters:
	 *   - name: CircuitBreaker
	 *     args:
	 *       name: myCircuitBreaker
	 *       fallbackUri: forward:/fallback
	 * }</pre>
	 *
	 * @return JSON response with fallback information
	 */
	@RequestMapping("/fallback")
	public Mono<Map<String, Object>> fallback() {
		log.warn("Circuit breaker triggered! Returning fallback response.");

		Map<String, Object> result = new HashMap<>();
		result.put("status", "error");
		result.put("message", "Service temporarily unavailable. Please try again later.");
		result.put("timestamp", System.currentTimeMillis());
		result.put("code", 503);

		return Mono.just(result);
	}

}
