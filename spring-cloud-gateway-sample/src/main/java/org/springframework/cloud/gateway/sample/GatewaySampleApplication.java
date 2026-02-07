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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
/**
 * Spring Cloud Gateway 示例应用
 *
 * <p>演示功能：
 * - 基础路由配置（Host、Path 谓词）
 * - 请求体读取和修改
 * - 响应体修改
 * - 自定义过滤器（限流）
 * - RouterFunction 端点
 *
 * @author Spencer Gibb
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class GatewaySampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewaySampleApplication.class, args);
	}

}
