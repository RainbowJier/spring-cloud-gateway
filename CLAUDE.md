# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码仓库中工作时提供指导。

## 项目概述

Spring Cloud Gateway 是一个基于 Spring Framework 6、Spring Boot 3 和 Java 17 构建的 API 网关。它提供动态路由、请求/响应过滤和熔断能力，有两种实现方式：WebFlux（响应式，默认）和 WebMVC（基于 Servlet）。

## 构建和测试命令

### Maven 构建
```bash
# 标准构建（需要 Maven wrapper 或 Maven 3.3.3+）
./mvnw install

# 使用 'spring' profile 构建（如果不使用 Maven wrapper 则必需）
./mvnw install -P spring

# 构建并生成文档
./mvnw install -Pdocs

# 构建时跳过测试
./mvnw install -DskipTests

# 跳过依赖 Docker 的测试（Redis 等）
./mvnw install -PwithoutDockerTests
```

### 运行测试
```bash
# 运行所有测试
./mvnw test

# 运行指定测试类
./mvnw test -Dtest=GatewayAutoConfigurationTests

# 跳过依赖 Docker 的测试
./mvnw test -PwithoutDockerTests
```

### 重要构建说明
- 需要 **Java 17**
- 部分集成测试需要本地运行 Docker（用于 Redis 等）
- 如果不使用 Maven wrapper，需要使用 `-P spring` profile 来访问 Spring snapshot/milestone 仓库
- 可能需要增加 Maven 内存：`MAVEN_OPTS=-Xmx512m -XX:MaxPermSize=128m`

## 架构和代码结构

### 模块组织

**核心服务器模块：**
- `spring-cloud-gateway-server-webflux` - 主要的响应式（WebFlux）网关实现
- `spring-cloud-gateway-server-webmvc` - 基于 Servlet（MVC）的网关实现
- `spring-cloud-gateway-proxyexchange-webflux/webmvc` - 两种模式的代理交换支持
- `spring-cloud-starter-gateway-server-webflux/webmvc` - 带自动配置的启动器模块

**支持模块：**
- `spring-cloud-gateway-dependencies` - 依赖管理 BOM
- `spring-cloud-gateway-sample` - 演示用法的示例应用
- `spring-cloud-gateway-integration-tests` - 集成测试（部分需要 Docker）

### 核心架构组件

**路由解析：**
- `RouteLocator`（接口）- 发现路由（实现类：`RouteDefinitionRouteLocator`、`CachingRouteLocator`、`CompositeRouteLocator`）
- `RouteDefinition` - 定义路由，包含谓词、过滤器和目标 URI
- 路由通过 `GatewayProperties` 配置，前缀为 `spring.cloud.gateway.server.webflux`

**路由谓词**（基于条件的路由）：
- 位于 `org.springframework.cloud.gateway.handler.predicate`
- 工厂类命名为 `*RoutePredicateFactory`（如 `PathRoutePredicateFactory`、`HostRoutePredicateFactory`、`MethodRoutePredicateFactory`）
- 支持：Path、Host、Method、Header、Cookie、Query、Time（After/Before/Between）、RemoteAddr、Weight

**网关过滤器**（请求/响应修改）：
- 位于 `org.springframework.cloud.gateway.filter.factory`
- 工厂类命名为 `*GatewayFilterFactory`（如 `AddRequestHeaderGatewayFilterFactory`、`RewritePathGatewayFilterFactory`、`ModifyRequestBodyGatewayFilterFactory`）
- 特殊过滤器：Circuit Breaker（Resilience4J）、Rate Limiter（Bucket4j）、Retry

**全局过滤器**（应用于所有路由）：
- 位于 `org.springframework.cloud.gateway.filter`
- 核心实现：`NettyRoutingFilter`（核心路由）、`RouteToRequestUrlFilter`（URI 解析）、`ForwardRoutingFilter`、`WebsocketRoutingFilter`

**自动配置：**
- `GatewayAutoConfiguration` - 核心自动配置
- `GatewayReactiveLoadBalancerClientAutoConfiguration` - Spring Cloud LoadBalancer 集成
- `GatewayRedisAutoConfiguration` - 基于 Redis 的限流
- `GatewayResilience4JCircuitBreakerAutoConfiguration` - 熔断器集成
- `GatewayDiscoveryClientAutoConfiguration` - 服务发现集成

### 配置属性

**主要属性（前缀：`spring.cloud.gateway.server.webflux`）：**
- `routes[]` - 路由定义，包含谓词和过滤器
- `defaultFilters[]` - 应用于所有路由的过滤器
- `metrics` - 指标配置
- `httpclient` - HTTP 客户端设置（连接超时、响应超时等）

**路由配置示例：**
```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: myroute
              predicates:
                - name: Path
                  args:
                    pattern: /api/**
              filters:
                - name: AddRequestHeader
                  args:
                    name: X-Request-Id
                    value: ABC-123
              uri: http://localhost:8081
```

### 设计模式

- **工厂模式** - 所有谓词和过滤器都使用工厂模式，类名为 `*Factory`
- **责任链模式** - 过滤器按顺序应用于请求/响应
- **响应式编程** - 基于 Project Reactor（WebFlux），使用 Mono/Flux 类型
- **条件配置** - 广泛使用 `@ConditionalOn*` 注解进行自动配置
- **配置属性** - 通过 `@ConfigurationProperties` 实现强类型配置

### 代码规范

**贡献代码必需：**
- 所有 `.java` 文件必须包含带 `@author` 标签的 Javadoc
- 所有新的 Java 文件必须包含 Apache 2.0 许可证头（从现有文件复制）
- 遵循 Spring Framework 代码格式规范
- 使用 Spring Cloud Build 项目中的 `eclipse-code-formatter.xml`
- 提交消息应遵循 conventional commit 格式，包含 `Fixes gh-XXXX` 引用问题
- 所有提交必须包含 `Signed-off-by` 尾声（开发者原产地证书）

**可选但推荐：**
- 在你实质性修改的文件中将自己添加为 `@author`
- 为贡献包含单元测试

### 测试注意事项

- 单元测试在各模块的 `src/test` 中
- 集成测试在 `spring-cloud-gateway-integration-tests` 模块中
- 集成测试可能需要 Docker 运行中间件（Redis 等）
- 开发期间使用 `-PwithoutDockerTests` 跳过依赖 Docker 的测试
- 使用 JaCoCo 进行测试覆盖率统计

### IDE 设置

**IntelliJ IDEA：**
- 从 Spring Cloud Build 导入代码样式：`Intellij_Spring_Boot_Java_Conventions.xml`
- 导入检查配置：`Intellij_Project_Defaults.xml`
- 安装 Checkstyle 插件并配置 Spring Cloud Build 项目的规则
- 在 IDE 中激活 `spring` Maven profile 以访问仓库

**Eclipse：**
- 使用 Spring Cloud Build 的 `eclipse-code-formatter.xml`
- 推荐使用 m2eclipse 插件进行 Maven 支持
- 运行 `./mvnw eclipse:eclipse` 生成项目元数据