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
import org.springframework.cloud.gateway.sample.filter.TimingGatewayFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Spring Cloud Gateway 路由配置
 *
 * <p>
 * 演示功能： - 基础路由配置（Host、Path 谓词） - 请求体读取和修改 - 响应体修改 - 自定义过滤器（限流）
 *
 * @author Spencer Gibb
 */
@Configuration
public class RouteConfiguration {

	@Value("${test.url}")
	private String testUri;

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
			// ============================================================
			// 限流路由：使用自定义 ThrottleGatewayFilter
			// 配置：容量 1，每 10 秒补充 1 个令牌
			// ============================================================
			.route(r -> r.order(-1) // 高优先级
				.path("/api/**")
				.filters(f -> f.filter(new ThrottleGatewayFilter().setCapacity(5)
					.setRefillTokens(1)
					.setRefillPeriod(10)
					.setRefillUnit(TimeUnit.SECONDS)))
				.uri(testUri))

			// ============================================================
			// 外部 API 代理路由：转发到公共测试 API
			// 将 /api/** 转发到 https://jsonplaceholder.typicode.com/**
			// ============================================================
			.route(r -> r.order(300)
				.path("/api/**")
				.filters(f -> f.stripPrefix(1) // 去掉 /api 前缀
					.addRequestHeader("X-Proxy-By", "SpringGateway") // 添加代理标识
				)
				.uri(testUri))

			// 自定义过滤器
			.route(r -> r.path("/timing/**")
				.filters(f -> f.filter(new TimingGatewayFilter()))
				.uri("http://example.com"))

			// ============================================================
			// todo:基础路由：Host 谓词 + Path 谓词 + PrefixPath 过滤器
			// ============================================================

			// ============================================================
			// todo:请求体谓词：读取请求体内容进行匹配
			// ============================================================

			// ============================================================
			// todo:请求体修改：String → JSON
			// ============================================================

			// ============================================================
			// todo:响应体修改：String → String (转大写)
			// ============================================================

			// ============================================================
			// todo:响应体修改：处理空响应体
			// ============================================================

			// ============================================================
			// todo:响应体修改：错误供应商处理
			// ============================================================

			// ============================================================
			// todo:响应体修改：Map → String (提取特定字段)
			// ============================================================

			// ============================================================
			// todo:路径谓词：按路径匹配
			// ============================================================
			.build();
	}

}
