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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample Backend Server Application
 *
 * <p>
 * This is a backend service for testing Spring Cloud Gateway features.
 * It runs on port 8081 and provides various test endpoints.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Normal responses for basic forwarding tests</li>
 * <li>Slow responses for timeout/circuit breaker tests</li>
 * <li>Error responses for failure handling tests</li>
 * <li>Random success/failure for intermittent failure scenarios</li>
 * </ul>
 *
 * @author Frank
 */
@SpringBootApplication
public class SampleServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleServerApplication.class, args);
	}
}
