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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.sample.filter.ThrottleGatewayFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Spring Cloud Gateway 自定义过滤器配置
 *
 * <p>
 * 配置自定义过滤器的 Bean 定义，以便在 YAML 路由配置中使用。
 *
 * <p>
 * 路由配置已移至 application.yml，本类仅保留必要的 Bean 定义。
 *
 * @author Spencer Gibb
 */
@Configuration
public class RouteConfiguration {

	@Value("${sample-server.uri}")
	private String testUri;

	/**
	 * 自定义限流路由配置
	 *
	 * <p>
	 * 注意：此路由使用自定义的 ThrottleGatewayFilter，无法在 YAML 中配置，
	 * 因此保留在 Java 代码中。其他路由已移至 application.yml。
	 *
	 * @return RouteLocator 路由定位器
	 */
	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
			// ============================================================
			// 限流路由：使用自定义 ThrottleGatewayFilter
			// 配置：容量 5，每 10 秒补充 1 个令牌
			// ============================================================
			.route("throttle-route", r -> r.order(-1) // 高优先级
				.path("/api/**")
				.filters(f -> f.filter(new ThrottleGatewayFilter().setCapacity(5)
					.setRefillTokens(1)
					.setRefillPeriod(10)
					.setRefillUnit(TimeUnit.SECONDS)))
				.uri(testUri))
			.build();
	}

}
