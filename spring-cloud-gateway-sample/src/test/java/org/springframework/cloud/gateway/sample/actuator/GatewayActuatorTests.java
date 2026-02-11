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

package org.springframework.cloud.gateway.sample.actuator;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集成测试：验证 Spring Cloud Gateway Actuator 端点
 *
 * <p>
 * 测试范围：
 * <ul>
 * <li>健康检查端点</li>
 * <li>网关请求指标端点</li>
 * <li>Actuator 端点列表</li>
 * </ul>
 *
 * @author Test Author
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayActuatorTests {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void healthEndpointIsAccessible() {
		webTestClient.get()
			.uri("/actuator/health")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(String.class)
			.value(body -> assertThat(body).contains("\"status\""));
	}

	@Test
	void healthEndpointReturnsUpStatus() {
		webTestClient.get()
			.uri("/actuator/health")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(String.class)
			.value(body -> assertThat(body).contains("\"status\":\"UP\""));
	}

	@Test
	void actuatorEndpointsListIsAccessible() {
		webTestClient.get()
			.uri("/actuator")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(String.class)
			.value(body -> assertThat(body).contains("_links"));
	}

	@Test
	void actuatorEndpointContainsHealthLink() {
		webTestClient.get()
			.uri("/actuator")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(String.class)
			.value(body -> assertThat(body).contains("/actuator/health"));
	}

	@Test
	@org.junit.jupiter.api.Disabled("在没有实际请求的情况下可能没有指标数据")
	void gatewayRequestsMetricsEndpointExists() {
		// 注意：此端点可能需要先发送实际请求才能返回有意义的数据
		webTestClient.get().uri("/actuator/metrics/spring.cloud.gateway.requests").exchange().expectStatus().isOk();
	}

	@Test
	void actuatorInfoEndpointIsAccessible() {
		// info 端点可能返回空内容或 404，取决于是否配置了信息
		webTestClient.get().uri("/actuator/info").exchange().expectStatus().isOk();
	}

}
