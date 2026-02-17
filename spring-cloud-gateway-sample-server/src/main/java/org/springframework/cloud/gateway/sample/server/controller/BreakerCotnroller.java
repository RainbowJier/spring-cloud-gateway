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
 * Circuit Breaker Test Controller
 *
 * <p>
 * This controller provides endpoints to test circuit breaker functionality:
 * <ul>
 * <li>Slow responses - triggers timeout-based circuit breaking</li>
 * <li>Errors - triggers failure-rate-based circuit breaking</li>
 * <li>Random failures - simulates intermittent service issues</li>
 * <li>Timeout - simulates unresponsive backend</li>
 * </ul>
 *
 * <p>
 * Usage Examples: <pre>{@code
 * # Test slow response (triggers circuit breaker after multiple requests)
 * curl http://localhost:8080/gateway/cb/slow?delay=5000
 *
 * # Test error response (triggers circuit breaker after 50% failure rate)
 * curl http://localhost:8080/gateway/cb/error
 *
 * # Test random failures (50% success rate)
 * curl http://localhost:8080/gateway/cb/random
 *
 * # Test timeout (never responds)
 * curl http://localhost:8080/gateway/cb/timeout
 * }</pre>
 *
 * @author Frank
 */
@Slf4j
@RestController
@RequestMapping("/breaker")
public class BreakerCotnroller {

	private final AtomicInteger requestCounter = new AtomicInteger(0);

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
		log.info("[CB-Request #{}] Slow request received, delaying {}ms", requestId, delay);

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
		log.warn("[CB-Request #{}] Error request received, returning 500", requestId);

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
			log.warn("[CB-Request #{}] Random request FAILED", requestId);
			return Mono.error(new RuntimeException("Random failure occurred!"));
		}

		log.info("[CB-Request #{}] Random request SUCCEEDED", requestId);
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
	 * This endpoint will wait indefinitely.
	 *
	 * @return Never returns (causes timeout)
	 */
	@GetMapping("/timeout")
	public Mono<Map<String, Object>> timeout() {
		int requestId = requestCounter.incrementAndGet();
		log.warn("[CB-Request #{}] Timeout request received, will hang forever", requestId);

		// This Mono never completes, causing a timeout
		return Mono.never();
	}

}
