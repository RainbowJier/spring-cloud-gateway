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

package org.springframework.cloud.gateway.sample.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.sample.filter.ThrottleGatewayFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Spring Cloud Gateway 路由配置
 *
 * <p>演示功能：
 * - 基础路由配置（Host、Path 谓词）
 * - 请求体读取和修改
 * - 响应体修改
 * - 自定义过滤器（限流）
 *
 * @author Spencer Gibb
 */
@Configuration
public class RouteConfiguration {

	@Value("${test.uri:http://httpbin.org:80}")
	String uri;

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		// @formatter:off
		// 可以使用以下 URI 进行测试：
		// String uri = "http://httpbin.org:80";  // 在线测试服务
		// String uri = "http://localhost:9080";   // 本地测试服务

		return builder.routes()
			// ============================================================
			// 基础路由：Host 谓词 + Path 谓词 + PrefixPath 过滤器
			// ============================================================
			.route(r -> r
				.host("**.abc.org")
				.and()
				.path("/anything/png")
				.filters(f -> f
					.prefixPath("/httpbin")
					.addResponseHeader("X-TestHeader", "foobar")
				)
				.uri(uri)
			)

			// ============================================================
			// 请求体谓词：读取请求体内容进行匹配
			// ============================================================
			.route("read_body_pred", r -> r
				.host("*.readbody.org")
				.and()
				.readBody(String.class, s -> s.trim().equalsIgnoreCase("hi"))
				.filters(f -> f
					.prefixPath("/httpbin")
					.addResponseHeader("X-TestHeader", "read_body_pred")
				)
				.uri(uri)
			)

			// ============================================================
			// 请求体修改：String → Hello 对象 → JSON
			// ============================================================
			.route("rewrite_request_obj", r -> r
				.host("*.rewriterequestobj.org")
				.filters(f -> f
					.prefixPath("/httpbin")
					.addResponseHeader("X-TestHeader", "rewrite_request")
					.modifyRequestBody(
						String.class,
						Hello.class,
						MediaType.APPLICATION_JSON_VALUE,
						(exchange, s) -> Mono.just(new Hello(s.toUpperCase(Locale.ROOT)))
					)
				)
				.uri(uri)
			)

			// ============================================================
			// 请求体修改：String → String (转大写并重复)
			// ============================================================
			.route("rewrite_request_upper", r -> r
				.host("*.rewriterequestupper.org")
				.filters(f -> f
					.prefixPath("/httpbin")
					.addResponseHeader("X-TestHeader", "rewrite_request_upper")
					.modifyRequestBody(
						String.class,
						String.class,
						(exchange, s) -> Mono.just(
							s.toUpperCase(Locale.ROOT) + s.toUpperCase(Locale.ROOT)
						)
					)
				)
				.uri(uri)
			)

			// ============================================================
			// 响应体修改：String → String (转大写)
			// ============================================================
			.route("rewrite_response_upper", r -> r
				.host("*.rewriteresponseupper.org")
				.filters(f -> f
					.prefixPath("/httpbin")
					.addResponseHeader("X-TestHeader", "rewrite_response_upper")
					.modifyResponseBody(
						String.class,
						String.class,
						(exchange, s) -> Mono.just(s.toUpperCase(Locale.ROOT))
					)
				)
				.uri(uri)
			)

			// ============================================================
			// 响应体修改：处理空响应体
			// ============================================================
			.route("rewrite_empty_response", r -> r
				.host("*.rewriteemptyresponse.org")
				.filters(f -> f
					.prefixPath("/httpbin")
					.addResponseHeader("X-TestHeader", "rewrite_empty_response")
					.modifyResponseBody(
						String.class,
						String.class,
						(exchange, s) -> {
							return Mono.just(s.toUpperCase(Locale.ROOT));
						}
					)
				)
				.uri(uri)
			)

			// ============================================================
			// 响应体修改：错误供应商处理
			// ============================================================
			.route("rewrite_response_fail_supplier", r -> r
				.host("*.rewriteresponsewithfailsupplier.org")
				.filters(f -> f
					.prefixPath("/httpbin")
					.addResponseHeader("X-TestHeader", "rewrite_response_fail_supplier")
					.modifyResponseBody(
						String.class,
						String.class,
						(exchange, s) -> {
							if (s == null) {
								return Mono.error(new IllegalArgumentException("this should not happen"));
							}
							return Mono.just(s.toUpperCase(Locale.ROOT));
						}
					)
				)
				.uri(uri)
			)

			// ============================================================
			// 响应体修改：Map → String (提取特定字段)
			// ============================================================
			.route("rewrite_response_obj", r -> r
				.host("*.rewriteresponseobj.org")
				.filters(f -> f
					.prefixPath("/httpbin")
					.addResponseHeader("X-TestHeader", "rewrite_response_obj")
					.modifyResponseBody(
						Map.class,
						String.class,
						MediaType.TEXT_PLAIN_VALUE,
						(exchange, map) -> Mono.just(map.get("data").toString())
					)
					.setResponseHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE)
				)
				.uri(uri)
			)

			// ============================================================
			// 路径谓词：按路径匹配
			// ============================================================
			.route(r -> r
				.path("/image/webp")
				.filters(f -> f
					.prefixPath("/httpbin")
					.addResponseHeader("X-AnotherHeader", "baz")
				)
				.uri(uri)
			)

			// ============================================================
			// 限流路由：使用自定义 ThrottleGatewayFilter
			// 配置：容量 1，每 10 秒补充 1 个令牌
			// ============================================================
			.route(r -> r
				.order(-1)  // 高优先级
				.host("**.throttle.org")
				.and()
				.path("/get")
				.filters(f -> f
					.prefixPath("/httpbin")
					.filter(new ThrottleGatewayFilter()
						.setCapacity(1)
						.setRefillTokens(1)
						.setRefillPeriod(10)
						.setRefillUnit(TimeUnit.SECONDS)
					)
				)
				.uri(uri)
			)
			.build();
		// @formatter:on
	}

	public class Hello {

		String message;

		public Hello() {
		}

		public Hello(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

	}


}
