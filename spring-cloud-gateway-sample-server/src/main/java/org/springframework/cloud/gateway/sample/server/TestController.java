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

package org.springframework.cloud.gateway.sample.server;

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
public class TestController {

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
	 * Slow response test - simulates slow backend service
	 *
	 * <p>
	 * Use this to test circuit breaker timeout functionality.
	 * When delay exceeds the configured timeout, circuit breaker should trigger.
	 *
	 * @param delay delay in milliseconds (default: 100ms)
	 * @return JSON response after specified delay
	 */
	@GetMapping("/slow")
	public Mono<Map<String, Object>> slow(@RequestParam(defaultValue = "100") long delay) {
		int requestId = requestCounter.incrementAndGet();
		log.info("[Request #{}] Slow request received, delay: {}ms", requestId, delay);

		return Mono.delay(Duration.ofMillis(delay))
				.then(Mono.fromSupplier(() -> {
					Map<String, Object> response = new HashMap<>();
					response.put("status", "success");
					response.put("message", "Slow response completed");
					response.put("requestId", requestId);
					response.put("delay", delay);
					response.put("timestamp", System.currentTimeMillis());
					return response;
				}));
	}

	/**
	 * Error response test - simulates failing backend service
	 *
	 * <p>
	 * Use this to test circuit breaker error handling.
	 * Multiple consecutive errors should trigger the circuit breaker.
	 *
	 * @return JSON error response
	 */
	@GetMapping("/error")
	public Mono<Map<String, Object>> error() {
		int requestId = requestCounter.incrementAndGet();
		log.error("[Request #{}] Error request received - simulating failure", requestId);

		Map<String, Object> response = new HashMap<>();
		response.put("status", "error");
		response.put("message", "Simulated backend service failure!");
		response.put("requestId", requestId);
		response.put("timestamp", System.currentTimeMillis());

		return Mono.just(response);
	}

	/**
	 * Random error test - randomly returns success or error
	 *
	 * <p>
	 * Use this to test circuit breaker with intermittent failures.
	 * The failure rate is approximately 50%.
	 *
	 * @return JSON response (success or error)
	 */
	@GetMapping("/random")
	public Mono<Map<String, Object>> random() {
		int requestId = requestCounter.incrementAndGet();
		boolean shouldFail = Math.random() < 0.5;

		if (shouldFail) {
			log.warn("[Request #{}] Random request - FAILING", requestId);
			Map<String, Object> response = new HashMap<>();
			response.put("status", "error");
			response.put("message", "Random failure occurred");
			response.put("requestId", requestId);
			response.put("timestamp", System.currentTimeMillis());
			return Mono.just(response);
		} else {
			log.info("[Request #{}] Random request - SUCCESS", requestId);
			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "Random success");
			response.put("requestId", requestId);
			response.put("timestamp", System.currentTimeMillis());
			return Mono.just(response);
		}
	}

	/**
	 * Timeout test - simulates backend service that times out
	 *
	 * <p>
	 * Use this to test circuit breaker timeout behavior.
	 * Default delay is 5 seconds, which exceeds typical timeout configurations.
	 *
	 * @param delay delay in milliseconds (default: 5000ms)
	 * @return JSON response after delay (or timeout)
	 */
	@GetMapping("/timeout")
	public Mono<Map<String, Object>> timeout(@RequestParam(defaultValue = "5000") long delay) {
		int requestId = requestCounter.incrementAndGet();
		log.warn("[Request #{}] Timeout test initiated, delay: {}ms", requestId, delay);

		return Mono.delay(Duration.ofMillis(delay))
				.then(Mono.fromSupplier(() -> {
					Map<String, Object> response = new HashMap<>();
					response.put("status", "success");
					response.put("message", "Request completed (but likely timed out)");
					response.put("requestId", requestId);
					response.put("delay", delay);
					response.put("timestamp", System.currentTimeMillis());
					return response;
				}));
	}




}
