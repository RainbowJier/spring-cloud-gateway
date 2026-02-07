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

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Timing Filter - Records request processing time
 *
 * <p>Functionality:
 * Measures and logs the time taken to process each request through the gateway.
 *
 * <p>Processing Flow:
 * <ol>
 *   <li>Record start time when request enters the filter</li>
 *   <li>Continue processing through the filter chain</li>
 *   <li>Calculate elapsed time when response completes</li>
 *   <li>Log the request path and processing duration</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>{@code
 * RouteLocatorBuilder.Builder routes = builder.routes();
 * routes.route("timing-route",
 *     r -> r.path("/api/**")
 *         .filters(f -> f.filter(new TimingGatewayFilter()))
 *         .uri("http://localhost:8081")
 * );
 * }</pre>
 *
 * @author Frank
 */
@Slf4j
public class TimingGatewayFilter implements GatewayFilter {
	/**
	 * Filters the request and records processing time.
	 *
	 * <p>Available Request Properties from ServerWebExchange:
	 * <ul>
	 *   <li>Request Method: {@code exchange.getRequest().getMethod()}</li>
	 *   <li>Request Path: {@code exchange.getRequest().getPath()}</li>
	 *   <li>Request URI: {@code exchange.getRequest().getURI()}</li>
	 *   <li>Request Headers: {@code exchange.getRequest().getHeaders()}</li>
	 *   <li>Query Parameters: {@code exchange.getRequest().getQueryParams()}</li>
	 *   <li>Cookies: {@code exchange.getRequest().getCookies()}</li>
	 *   <li>Remote Address: {@code exchange.getRequest().getRemoteAddress()}</li>
	 *   <li>Local Address: {@code exchange.getRequest().getLocalAddress()}</li>
	 *   <li>Request Body: {@code exchange.getRequest().getBody()}</li>
	 * </ul>
	 *
	 * <p>Available Response Properties from ServerWebExchange:
	 * <ul>
	 *   <li>Response Status: {@code exchange.getResponse().getStatusCode()}</li>
	 *   <li>Response Headers: {@code exchange.getResponse().getHeaders()}</li>
	 *   <li>Committed Flag: {@code exchange.getResponse().isCommitted()}</li>
	 * </ul>
	 *
	 * @param exchange the current server exchange containing request and response
	 * @param chain provides a way to delegate to the next filter
	 * @return {@code Mono<Void>} to indicate when request processing is complete
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		long start = System.currentTimeMillis();

		return chain.filter(exchange).doFinally(signalType -> {
			long duration = System.currentTimeMillis() - start;
			String path = exchange.getRequest().getPath().value();  // request path
			String method = exchange.getRequest().getMethod().name(); // request method

			log.info(String.format("[%s] %s completed in %dms", method, path, duration));
		});
	}

}
