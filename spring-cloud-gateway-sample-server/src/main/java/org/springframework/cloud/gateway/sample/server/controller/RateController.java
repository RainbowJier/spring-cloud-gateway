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

package org.springframework.cloud.gateway.sample.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test Controller for Gateway Testing
 *
 * <p>
 * This controller simulates a backend service to test various gateway features:
 * <ul>
 * <li>Normal request forwarding</li>
 * <li>Rate limiting</li>
 * <li>Circuit breaker (slow responses, errors)</li>
 * </ul>
 *
 * <p>
 * Usage Examples: <pre>{@code
 * # Test normal forwarding
 * curl http://localhost:8081/test/normal
 *
 * # Test circuit breaker with slow response
 * curl http://localhost:8081/test/slow?delay=3000
 *
 * # Test circuit breaker with error
 * curl http://localhost:8081/test/error
 * }</pre>
 *
 * @author Frank
 */
@Slf4j
@RestController
@RequestMapping("/test")
public class RateController {

	private final AtomicInteger requestCounter = new AtomicInteger(0);

	/**
	 * Normal response test - simulates healthy backend service
	 *
	 * <p>
	 * Use this to test basic gateway forwarding functionality.
	 *
	 * @return JSON response with success message
	 */
	@GetMapping("/normal")
	public Mono<Map<String, Object>> normal() {
		int requestId = requestCounter.incrementAndGet();
		log.info("[Request #{}] Normal request received", requestId);

		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "Backend service is healthy!");
		response.put("requestId", requestId);
		response.put("timestamp", System.currentTimeMillis());

		return Mono.just(response);
	}


	/**
	 * Slow response test - simulates a slow backend service
	 *
	 * <p>
	 * Use this to test circuit breaker timeout functionality.
	 * Rapid requests will cause the circuit breaker to trip.
	 *
	 * @param delay delay in milliseconds (default: 3000)
	 * @return JSON response after delay
	 */
	@GetMapping("/slow")
	public Mono<Map<String, Object>> slow(@RequestParam(defaultValue = "3000") long delay) {
		int requestId = requestCounter.incrementAndGet();
		log.info("[Request #{}] Slow request received, delaying {}ms", requestId, delay);

		return Mono.delay(Duration.ofMillis(delay))
			.then(Mono.fromSupplier(() -> {
				Map<String, Object> response = new HashMap<>();
				response.put("status", "success");
				response.put("message", "Slow response completed!");
				response.put("requestId", requestId);
				response.put("delay", delay);
				response.put("timestamp", System.currentTimeMillis());
				return response;
			}));
	}

	/**
	 * Error response test - simulates a failing backend service
	 *
	 * <p>
	 * Use this to test circuit breaker error handling.
	 * Multiple failed requests will trip the circuit breaker.
	 *
	 * @return Always returns 500 error
	 */
	@GetMapping("/error")
	public Mono<Map<String, Object>> error() {
		int requestId = requestCounter.incrementAndGet();
		log.warn("[Request #{}] Error request received, returning 500", requestId);

		return Mono.error(new RuntimeException("Simulated backend service error!"));
	}

	/**
	 * Random error test - simulates intermittent failures
	 *
	 * <p>
	 * Use this to test circuit breaker behavior with unstable services.
	 * 50% chance of success, 50% chance of failure.
	 *
	 * @return Random success or error response
	 */
	@GetMapping("/random")
	public Mono<Map<String, Object>> random() {
		int requestId = requestCounter.incrementAndGet();
		boolean shouldFail = Math.random() < 0.5;

		if (shouldFail) {
			log.warn("[Request #{}] Random request FAILED", requestId);
			return Mono.error(new RuntimeException("Random failure occurred!"));
		}

		log.info("[Request #{}] Random request SUCCEEDED", requestId);
		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "Random request succeeded!");
		response.put("requestId", requestId);
		response.put("timestamp", System.currentTimeMillis());

		return Mono.just(response);
	}

	/**
	 * Timeout test - simulates a backend that never responds
	 *
	 * <p>
	 * Use this to test gateway timeout handling.
	 * This endpoint will wait indefinitely (or very long time).
	 *
	 * @return Never returns (causes timeout)
	 */
	@GetMapping("/timeout")
	public Mono<Map<String, Object>> timeout() {
		int requestId = requestCounter.incrementAndGet();
		log.warn("[Request #{}] Timeout request received, will hang forever", requestId);

		// This Mono never completes, causing a timeout
		return Mono.never();
	}

}
