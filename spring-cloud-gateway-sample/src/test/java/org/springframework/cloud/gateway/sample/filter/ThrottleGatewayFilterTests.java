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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 单元测试：验证 ThrottleGatewayFilter 限流过滤器功能
 *
 * <p>
 * 测试范围：
 * <ul>
 * <li>令牌桶初始化</li>
 * <li>令牌消费成功场景</li>
 * <li>令牌耗尽时拒绝请求</li>
 * <li>链式调用配置</li>
 * <li>延迟初始化（双重检查锁定）</li>
 * </ul>
 *
 * @author Test Author
 */
class ThrottleGatewayFilterTests {

	private static final Logger log = LoggerFactory.getLogger(ThrottleGatewayFilterTests.class);

	private ThrottleGatewayFilter filter;

	private GatewayFilterChain chain;

	private ServerWebExchange exchange;

	@BeforeEach
	void setUp() {
		log.debug("Setting up test fixtures");
		filter = new ThrottleGatewayFilter();
		chain = mock(GatewayFilterChain.class);
		when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

		exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());
		log.debug("Test fixtures initialized");
	}

	@Test
	void filterIsConfiguredWithDefaultValues() {
		log.info("Test: filterIsConfiguredWithDefaultValues");
		// 验证过滤器可以使用默认值创建
		ThrottleGatewayFilter defaultFilter = new ThrottleGatewayFilter();
		assertThat(defaultFilter).isNotNull();
		log.debug("Filter created with default values successfully");
	}

	@Test
	void filterSupportsChainedConfiguration() {
		log.info("Test: filterSupportsChainedConfiguration");
		// 验证链式调用配置
		ThrottleGatewayFilter configuredFilter = new ThrottleGatewayFilter().setCapacity(10)
			.setRefillTokens(2)
			.setRefillPeriod(5)
			.setRefillUnit(java.util.concurrent.TimeUnit.SECONDS);

		log.debug("Configured filter - capacity: {}, refillTokens: {}, refillPeriod: {}, refillUnit: {}",
				10, 2, 5, java.util.concurrent.TimeUnit.SECONDS);

		assertThat(configuredFilter.getCapacity()).isEqualTo(10);
		assertThat(configuredFilter.getRefillTokens()).isEqualTo(2);
		assertThat(configuredFilter.getRefillPeriod()).isEqualTo(5);
		assertThat(configuredFilter.getRefillUnit()).isEqualTo(java.util.concurrent.TimeUnit.SECONDS);
		log.debug("All configuration values verified successfully");
	}

	@Test
	void requestAllowedWhenTokensAvailable() {
		log.info("Test: requestAllowedWhenTokensAvailable");
		// 配置：容量 5，每次补充 1 个令牌，补充周期 1 秒
		filter.setCapacity(5);
		filter.setRefillTokens(1);
		filter.setRefillPeriod(1);
		filter.setRefillUnit(java.util.concurrent.TimeUnit.SECONDS);
		log.debug("Filter configured - capacity: 5, refillTokens: 1, refillPeriod: 1s");

		// 第一次请求应该成功
		Mono<Void> result = filter.filter(exchange, chain);

		assertThat(result).isNotNull();
		verify(chain).filter(exchange);

		assertThat(exchange.getResponse().getStatusCode()).isNull(); // 未设置状态码表示请求通过
		log.info("Request allowed when tokens are available");
	}

	@Test
	void multipleRequestsAllowedWithinCapacity() {
		log.info("Test: multipleRequestsAllowedWithinCapacity");
		// 配置：容量 3
		filter.setCapacity(3);
		filter.setRefillTokens(1);
		filter.setRefillPeriod(1);
		filter.setRefillUnit(java.util.concurrent.TimeUnit.SECONDS);
		log.debug("Filter configured - capacity: 3, refillTokens: 1, refillPeriod: 1s");

		// 发送 3 个请求，都应该成功
		for (int i = 0; i < 3; i++) {
			log.debug("Sending request {}/{}", i + 1, 3);
			ServerWebExchange newExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());
			filter.filter(newExchange, chain).block();
			assertThat(newExchange.getResponse().getStatusCode()).isNull();
			log.debug("Request {} passed through filter", i + 1);
		}
		log.info("All {} requests allowed within capacity", 3);
	}

	@Test
	void requestDeniedWhenTokensExhausted() {
		log.info("Test: requestDeniedWhenTokensExhausted");
		// 配置：容量 1
		filter.setCapacity(1);
		filter.setRefillTokens(1);
		filter.setRefillPeriod(10); // 10 秒补充周期
		filter.setRefillUnit(java.util.concurrent.TimeUnit.SECONDS);
		log.debug("Filter configured - capacity: 1, refillTokens: 1, refillPeriod: 10s");

		// 第一个请求消耗令牌
		log.debug("Sending first request (should consume the only available token)");
		filter.filter(exchange, chain).block();
		assertThat(exchange.getResponse().getStatusCode()).isNull();
		log.info("First request passed - token consumed");

		// 第二个请求应该被拒绝（令牌已耗尽）
		log.debug("Sending second request (should be denied - no tokens available)");
		ServerWebExchange secondExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());
		filter.filter(secondExchange, chain).block();

		assertThat(secondExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		log.info("Second request correctly denied with status: {}", HttpStatus.TOO_MANY_REQUESTS);
	}

	@Test
	void capacityCanBeZero() {
		log.info("Test: capacityCanBeZero (boundary case)");
		// 边界情况：容量为 0，所有请求都应被拒绝
		filter.setCapacity(0);
		filter.setRefillTokens(1);
		filter.setRefillPeriod(1);
		filter.setRefillUnit(java.util.concurrent.TimeUnit.SECONDS);
		log.debug("Filter configured - capacity: 0 (edge case), refillTokens: 1, refillPeriod: 1s");

		log.debug("Sending request (should be denied - capacity is 0)");
		filter.filter(exchange, chain).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		log.info("Request correctly denied with status: {} (capacity is 0)", HttpStatus.TOO_MANY_REQUESTS);
	}

	@Test
	void refillRateCanBeZero() {
		log.info("Test: refillRateCanBeZero (boundary case)");
		// 边界情况：补充率为 0，令牌不会补充
		filter.setCapacity(2);
		filter.setRefillTokens(0); // 不补充
		filter.setRefillPeriod(1);
		filter.setRefillUnit(java.util.concurrent.TimeUnit.SECONDS);
		log.debug("Filter configured - capacity: 2, refillTokens: 0 (no refill), refillPeriod: 1s");

		// 消耗所有令牌
		log.debug("Consuming all available tokens (capacity: {})", 2);
		for (int i = 0; i < 2; i++) {
			log.debug("Sending request {}/{} to consume tokens", i + 1, 2);
			ServerWebExchange newExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());
			filter.filter(newExchange, chain).block();
		}
		log.info("All tokens consumed");

		// 之后的请求应该被拒绝
		log.debug("Sending additional request (should be denied - tokens exhausted with no refill)");
		ServerWebExchange exhaustedExchange = MockServerWebExchange
			.from(MockServerHttpRequest.get("/api/test").build());
		filter.filter(exhaustedExchange, chain).block();

		assertThat(exhaustedExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		log.info("Request correctly denied - tokens exhausted and refill rate is 0");
	}

}
