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

package org.springframework.cloud.gateway.sample;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成测试：验证 Spring Cloud Gateway 示例应用的基本功能
 *
 * <p>
 * 测试范围：
 * <ul>
 * <li>应用上下文加载</li>
 * <li>API 代理路由功能</li>
 * <li>健康检查端点</li>
 * <li>响应头验证</li>
 * </ul>
 *
 * @author Test Author
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewaySampleApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		// 验证 Spring 上下文能够成功加载
		assertThat(applicationContext).isNotNull();
		assertThat(webTestClient).isNotNull();
	}

	@Test
	void healthEndpointReturnsUp() {
		// 测试健康检查端点
		webTestClient.get()
			.uri("/actuator/health")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(String.class)
			.value(body -> assertThat(body).contains("UP"));
	}

	@Test
	void apiProxyRouteWorks() {
		// 测试 API 代理路由：/api/posts/1 -> JSONPlaceholder /posts/1
		webTestClient.get()
			.uri("/api/posts/1")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(String.class)
			.value(body -> {
				assertThat(body).contains("userId");
				assertThat(body).contains("id");
				assertThat(body).contains("title");
			});
	}

	@Test
	void gatewayResponseHeaderIsAdded() {
		// 验证全局响应头 X-Gateway 被正确添加
		webTestClient.get()
			.uri("/api/posts/1")
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.valueEquals("X-Gateway", "SpringCloudGateway");
	}

	@Test
	void stripPrefixFilterRemovesApiPrefix() {
		// 验证 StripPrefix(1) 过滤器去除了 /api 前缀
		// 请求 /api/posts 应该被转发到 /posts
		webTestClient.get()
			.uri("/api/posts")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(String.class)
			.value(body -> assertThat(body).contains("["));
	}

	@Test
	void actuatorEndpointsAreAccessible() {
		// 验证 Actuator 端点可访问
		webTestClient.get()
			.uri("/actuator")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(String.class)
			.value(body -> assertThat(body).contains("_links"));
	}

	@Test
	void actuatorEndpointsContainsHealthLink() {
		// 验证 Actuator 端点包含健康检查链接
		webTestClient.get().uri("/actuator").exchange().expectStatus().isOk().expectBody(String.class).value(body -> {
			assertThat(body).contains("/actuator/health");
			assertThat(body).contains("href");
		});
	}

}
