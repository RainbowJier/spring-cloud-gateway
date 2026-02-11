# Spring Cloud Gateway Sample 模块学习指南

本指南将帮助你逐步深入理解 Spring Cloud Gateway 的示例模块。

> **最新更新**: 本指南已根据代码重构后的最新状态更新（2026-02）

---

## 目录

1. [模块概述](#1-模块概述)
2. [环境准备](#2-环境准备)
3. [项目结构](#3-项目结构)
4. [快速启动](#4-快速启动)
5. [核心概念讲解](#5-核心概念讲解)
6. [代码深入分析](#6-代码深入分析)
7. [实践练习](#7-实践练习)
8. [扩展学习](#8-扩展学习)

---

## 1. 模块概述

### 1.1 这个模块是什么？

`spring-cloud-gateway-sample` 是 Spring Cloud Gateway 的**官方示例应用**，展示了：

- Java DSL 路由定义方式
- 自定义过滤器的实现（限流、计时）
- 路由配置与主应用类的分离
- 请求路径重写和头部修改
- 响应式编程模式

### 1.2 技术栈

| 技术 | 版本 | 用途 |
|-----|------|-----|
| Java | 17+ | 编程语言 |
| Spring Boot | 3.x | 应用框架 |
| Spring Cloud Gateway | 5.x | API 网关核心 |
| WebFlux | - | 响应式 Web 框架 |
| Lombok | - | 简化 Java 代码（生成 getter/setter/logger） |
| token-bucket | 1.7 | 限流算法库 |
| jsonplaceholder.typicode.com | - | 测试目标服务 |

---

## 2. 环境准备

### 2.1 必需环境

```bash
# 检查 Java 版本（需要 17+）
java -version

# 检查 Maven 版本（需要 3.3.3+）
mvn -version
```

### 2.2 可选环境

```bash
# 安装 curl 用于 HTTP 测试
# Windows: 使用 PowerShell 自带的 Invoke-WebRequest 或安装 Git Bash
```

---

## 3. 项目结构

### 3.1 目录树

```
spring-cloud-gateway-sample/
├── pom.xml                                          # Maven 配置
├── src/
│   ├── main/
│   │   ├── java/org/springframework/cloud/gateway/sample/
│   │   │   ├── GatewaySampleApplication.java       # 主应用类 ⭐
│   │   │   ├── config/
│   │   │   │   └── RouteConfiguration.java         # 路由配置类 ⭐
│   │   │   └── filter/
│   │   │       ├── ThrottleGatewayFilter.java      # 自定义限流过滤器 ⭐
│   │   │       └── TimingGatewayFilter.java        # 请求计时过滤器 ⭐
│   │   └── resources/
│   │       └── application.yml                       # 主配置文件 ⭐
│   └── test/
│       └── java/org/springframework/cloud/gateway/sample/
│           ├── GatewaySampleApplicationTests.java
│           └── GatewaySampleApplicationWithoutMetricsTests.java
```

### 3.2 依赖关系

```xml
<!-- 核心依赖 -->
spring-cloud-starter-gateway-server-webflux    # Gateway 核心
spring-boot-starter-webflux                   # 响应式 Web
spring-boot-starter-actuator                  # 监控端点
lombok                                        # 简化 Java 代码（可选）
token-bucket:1.7                              # 限流算法库
```

**Lombok 配置** (pom.xml):

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Maven 编译器配置 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## 4. 快速启动

### Step 1: 编译项目

```bash
cd spring-cloud-gateway-sample
../mvnw clean install
```

### Step 2: 运行应用

```bash
# 方式 1: Maven 插件
../mvnw spring-boot:run

# 方式 2: 直接运行 JAR
java -jar target/spring-cloud-gateway-sample-5.0.2-SNAPSHOT.jar
```

### Step 3: 验证启动

应用默认运行在 **8080 端口**，验证启动成功：

```bash
# 查看健康状态
curl http://localhost:8080/actuator/health

# 查看网关请求指标（确认路由已加载）
curl http://localhost:8080/actuator/metrics/spring.cloud.gateway.requests

# 测试 API 代理路由
curl http://localhost:8080/api/posts/1
```

预期输出：
- 健康检查返回 `{"status":"UP"}`
- `/api/posts/1` 返回 JSONPlaceholder 的测试数据
- 响应头包含 `X-Gateway: SpringCloudGateway`

> **注意**: Spring Cloud Gateway 5.x 版本使用配置前缀 `spring.cloud.gateway.server.webflux` 而非旧版的 `spring.cloud.gateway`。

---

## 5. 核心概念讲解

### 5.1 三大核心组件

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Route     │  匹配   │   Predicate  │  决定   │   Filter    │
│   (路由)    │──────→  │   (谓词)     │──────→  │   (过滤器)  │
└─────────────┘         └──────────────┘         └─────────────┘
      │                        │                        │
      │                        │                        │
      ↓                        ↓                        ↓
  "目标地址"              "匹配条件"              "请求/响应修改"
  (URI: httpbin)         (Path, Host, Method)    (AddHeader, Rewrite)
```

### 5.2 路由的定义方式

当前示例应用主要使用 **Java DSL** 方式定义路由，这是最灵活且推荐的方式。

```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route(r -> r.path("/api/**")
            .filters(f -> f.stripPrefix(1))
            .uri("http://backend.com"))
        .build();
}
```

**其他方式（可选）**:

| 方式 | 优点 | 缺点 |
|-----|------|-----|
| Java DSL | 类型安全、IDE 友好、调试方便 | 需要重新编译 |
| YAML 配置 | 配置即服务、易于动态刷新 | 缺少编译时检查 |
| Kotlin DSL | 简洁优雅（示例中已移除） | 需要 Kotlin 环境 |

### 5.3 常用谓词（Predicates）

| 谓词 | 配置示例 | 说明 |
|-----|---------|-----|
| Path | `- Path=/api/**` | 路径匹配 |
| Host | `- Host=**.abc.org` | Host 匹配 |
| Method | `- Method=GET,POST` | HTTP 方法 |
| Header | `- Header=X-Request-Id, \d+` | 请求头 |
| Query | `- Query=baz` | 查询参数 |
| Cookie | `- Cookie=chocolate, ch.p` | Cookie |
| Before | `- Before=2027-01-01T17:42:47.789-07:00` | 时间之前 |
| After | `- After=2023-01-01T17:42:47.789-07:00` | 时间之后 |

### 5.4 常用过滤器（Filters）

| 过滤器 | 配置示例 | 说明 |
|-------|---------|-----|
| AddRequestHeader | `AddRequestHeader=X-Request-Id, 123` | 添加请求头 |
| AddResponseHeader | `AddResponseHeader=X-Response-Id, 456` | 添加响应头 |
| PrefixPath | `PrefixPath=/httpbin` | 路径前缀 |
| StripPrefix | `StripPrefix=2` | 去除路径前 N 段 |
| RewritePath | `RewritePath=/foo/(?<segment>.*), /$\{segment}` | 路径重写 |
| SetPath | `SetPath=/something` | 设置路径 |

---

## 6. 代码深入分析

### 6.1 主应用类 (GatewaySampleApplication.java)

**文件位置**: `src/main/java/org/springframework/cloud/gateway/sample/GatewaySampleApplication.java`

#### 关键注解

```java
@SpringBootConfiguration    // 标识为 Spring Boot 配置类
@EnableAutoConfiguration    // 启用自动配置
```

**说明**: 主应用类已简化为仅包含启动代码，路由配置已移至独立的 `RouteConfiguration` 类。这种分离符合关注点分离原则，使代码更易于维护。

---

### 6.2 路由配置类 (RouteConfiguration.java)

**文件位置**: `src/main/java/org/springframework/cloud/gateway/sample/config/RouteConfiguration.java`

#### 类结构

```java
@Configuration  // 标识为配置类
public class RouteConfiguration {

    @Value("${test.url}")
    private String testUri;  // 从配置文件注入目标 URI

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // 路由定义...
            .build();
    }
}
```

#### 当前配置的路由

**路由 1: 限流路由（行 50-61）**

```java
.route(r -> r
    .order(-1)  // 高优先级
    .path("/api/**")
    .filters(f -> f.filter(
        new ThrottleGatewayFilter()
            .setCapacity(5)
            .setRefillTokens(1)
            .setRefillPeriod(10)
            .setRefillUnit(TimeUnit.SECONDS)))
    .uri(testUri)
)
```

**限流参数**:
- `capacity`: 5（桶容量）
- `refillTokens`: 1（每次补充 1 个令牌）
- `refillPeriod`: 10（每 10 秒）
- `refillUnit`: SECONDS（秒）

**效果**: 每 10 秒允许最多 5 个请求。

**路由 2: 外部 API 代理路由（行 67-75）**

```java
.route(r -> r
    .order(300)
    .path("/api/**")
    .filters(f -> f
        .stripPrefix(1)  // 去掉 /api 前缀
        .addRequestHeader("X-Proxy-By", "SpringGateway"))
    .uri(testUri)
)
```

**工作流程**:

```
请求: http://localhost:8080/api/posts/1
  ↓
1. Path 谓词匹配: /api/** ✓
  ↓
2. 应用过滤器:
   - stripPrefix(1) → /api/posts/1 变为 /posts/1
   - addRequestHeader → 添加 X-Proxy-By: SpringGateway
  ↓
3. 转发到: https://jsonplaceholder.typicode.com/posts/1
```

**路由 3: 计时过滤器路由（行 78-81）**

```java
.route(r -> r.path("/timing/**")
    .filters(f -> f.filter(new TimingGatewayFilter()))
    .uri("http://example.com")
)
```

**功能**: 记录请求处理时间，用于性能监控。

#### 待实现的路由（TODO 注释）

代码中包含多个 `// todo:` 注释，标记了待实现的功能：
- 基础路由：Host 谓词 + Path 谓词 + PrefixPath 过滤器
- 请求体谓词：读取请求体内容进行匹配
- 请求体修改：String → JSON
- 响应体修改：String → String (转大写)
- 响应体修改：处理空响应体
- 响应体修改：错误供应商处理
- 响应体修改：Map → String (提取特定字段)
- 路径谓词：按路径匹配

这些可以作为学习练习来实现。

---

### 6.3 自定义限流过滤器 (ThrottleGatewayFilter.java)

**文件位置**: `src/main/java/org/springframework/cloud/gateway/sample/ThrottleGatewayFilter.java`

#### 类结构概览

```java
@Slf4j                                  // Lombok: 自动生成 logger
@Data                                  // Lombok: 自动生成 getter/setter
@Accessors(chain = true)               // Lombok: 启用链式调用
public class ThrottleGatewayFilter implements GatewayFilter {
    @Getter(value = AccessLevel.PRIVATE)  // 私有 getter（自定义实现）
    private volatile TokenBucket tokenBucket;

    private int capacity;                  // 其他字段使用 @Data 生成 getter/setter
    private int refillTokens;
    private int refillPeriod;
    private TimeUnit refillUnit;

    // 核心过滤逻辑
    @Override
    public Mono<Void> filter(...) { ... }

    // 自定义 getTokenBucket() 方法（双重检查锁定）
    private TokenBucket getTokenBucket() { ... }
}
```

#### 核心过滤逻辑

```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // 获取令牌桶实例
    TokenBucket tokenBucket = getTokenBucket();

    // 尝试消费令牌（非阻塞操作）
    boolean consumed = tokenBucket.tryConsume();

    if (consumed) {
        // 成功获取令牌，继续处理请求
        log.debug("Request allowed - token consumed successfully");
        return chain.filter(exchange);
    }

    // 无可用令牌，拒绝请求
    log.debug("Request throttled - no tokens available");
    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    return exchange.getResponse().setComplete();  // 返回 429
}
```

#### 工作原理

```
┌──────────────────────────────────────────────────────┐
│                 Token Bucket (容量: 1)                 │
├──────────────────────────────────────────────────────┤
│  初始状态: [•]  (1 个令牌)                             │
│                                                       │
│  请求 1 到达: tryConsume() → 成功 ✓                   │
│           状态: [ ]  (0 个令牌)                       │
│                                                       │
│  请求 2 到达: tryConsume() → 失败 ✗ → 429 错误       │
│                                                       │
│  10 秒后:   [+1] 补充令牌                             │
│           状态: [•]  (1 个令牌)                       │
│                                                       │
│  请求 3 到达: tryConsume() → 成功 ✓                   │
└──────────────────────────────────────────────────────┘
```

#### 关键设计模式

1. **使用 Lombok 简化代码**

```java
@Slf4j                    // 自动生成: private static final Log log = LogFactory.getLog(...);
@Data                     // 自动生成所有字段的 getter/setter
@Accessors(chain = true)  // setter 方法返回 this，支持链式调用
public class ThrottleGatewayFilter implements GatewayFilter {
    // 不需要手动编写 getter/setter 方法
}
```

**Lombok 注解说明**:

| 注解 | 功能 | 替代的代码 |
|-----|------|-----------|
| `@Slf4j` | 自动生成 logger | `private static final Log log = LogFactory.getLog(...)` |
| `@Data` | 生成 getter/setter | ~40 行样板代码 |
| `@Accessors(chain = true)` | setter 返回 this | 实现链式调用 |
| `@Getter(value = AccessLevel.PRIVATE)` | 私有 getter | 保持自定义方法可见性 |

2. **延迟初始化** (Double-Check Locking)

```java
private TokenBucket getTokenBucket() {
    if (tokenBucket != null) {
        return tokenBucket;
    }
    synchronized (this) {
        if (tokenBucket == null) {
            tokenBucket = TokenBuckets.builder()
                .withCapacity(capacity)
                .withFixedIntervalRefillStrategy(refillTokens, refillPeriod, refillUnit)
                .build();
        }
    }
    return tokenBucket;
}
```

2. **链式调用**

```java
new ThrottleGatewayFilter()
    .setCapacity(1)
    .setRefillTokens(1)
    .setRefillPeriod(10)
    .setRefillUnit(TimeUnit.SECONDS)
```

---

### 6.4 请求计时过滤器 (TimingGatewayFilter.java)

**文件位置**: `src/main/java/org/springframework/cloud/gateway/sample/filter/TimingGatewayFilter.java`

#### 完整代码

```java
@Slf4j  // Lombok: 自动生成 logger
public class TimingGatewayFilter implements GatewayFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();

        return chain.filter(exchange).doFinally(signalType -> {
            long duration = System.currentTimeMillis() - start;
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();

            log.info(String.format("[%s] %s completed in %dms", method, path, duration));
        });
    }
}
```

#### 工作原理

```
┌──────────────────────────────────────────────────────────┐
│  请求进入过滤器 → 记录开始时间 (start)                     │
├──────────────────────────────────────────────────────────┤
│  继续过滤器链 (chain.filter)                              │
├──────────────────────────────────────────────────────────┤
│  响应返回 → doFinally 触发 → 计算耗时并记录日志            │
└──────────────────────────────────────────────────────────┘
```

#### 关键点

1. **使用 `doFinally`**: 无论请求成功或失败，都会执行耗时计算
2. **响应式编程**: 使用 `Mono<Void>` 返回类型，符合 WebFlux 规范
3. **日志记录**: 输出 HTTP 方法、请求路径和处理耗时

#### 日志示例

```
INFO: [GET] /api/posts/1 completed in 245ms
INFO: [POST] /api/users completed in 512ms
```

---

### 6.5 配置文件分析

**文件位置**: `src/main/resources/application.yml`

#### 当前配置

```yaml
# 测试 URI 配置（用于 RouteConfiguration.java）
test:
  uri: https://jsonplaceholder.typicode.com

spring:
  jmx:
    enabled: false  # 禁用 JMX，避免与 Netty 冲突

  cloud:
    gateway.server.webflux:
      # 默认过滤器：应用于所有路由
      default-filters:
        - AddResponseHeader=X-Gateway, SpringCloudGateway
        - AddRequestHeader=X-Gateway-Request-Timestamp, ${java.time.Instant.now()}

# ============================================================================
# 日志配置：开发环境使用 DEBUG/TRACE 级别
# ============================================================================
logging:
  level:
    org.springframework.cloud.gateway: TRACE
    org.springframework.http.server.reactive: DEBUG
    org.springframework.web.reactive: DEBUG
    reactor.netty: DEBUG

# ============================================================================
# Actuator 管理端点配置
# 允许访问所有管理端点（生产环境应限制）
# ============================================================================
management.endpoints.web.exposure.include: '*'
```

#### 配置要点

1. **test.uri**: 目标服务地址，当前使用 JSONPlaceholder 测试 API
2. **default-filters**: 应用于所有路由的全局过滤器
   - 添加响应头 `X-Gateway: SpringCloudGateway`
   - 添加请求时间戳头
3. **JMX 禁用**: 避免与 Netty 的冲突
4. **日志级别**: 开发环境使用 TRACE/DEBUG 级别
5. **Actuator**: 暴露所有管理端点（生产环境应限制）

#### 配置前缀变化

Spring Cloud Gateway 5.x 使用新的配置前缀：
- **旧版**: `spring.cloud.gateway`
- **新版**: `spring.cloud.gateway.server.webflux`

这是为了支持 WebMVC 和 WebFlux 两种实现方式的区分。

---

## 7. 实践练习

### 练习 1: API 代理路由测试

**目标**: 理解路径重写和代理功能

```bash
# 测试 1: StripPrefix 过滤器
curl http://localhost:8080/api/posts/1

# 预期结果:
# - 请求被转发到 https://jsonplaceholder.typicode.com/posts/1
# - /api 前缀被去除
# - 响应头包含 X-Gateway: SpringCloudGateway
```

**工作流程**:

```
请求: http://localhost:8080/api/posts/1
  ↓
1. Path 谓词匹配: /api/** ✓
  ↓
2. 全局过滤器 (default-filters):
   - 添加 X-Gateway 响应头
   - 添加 X-Gateway-Request-Timestamp 请求头
  ↓
3. 路由过滤器:
   - stripPrefix(1) → /api/posts/1 变为 /posts/1
   - addRequestHeader(X-Proxy-By: SpringGateway)
  ↓
4. 转发到: https://jsonplaceholder.typicode.com/posts/1
```

---

### 练习 2: 限流过滤器测试

**目标**: 理解令牌桶限流算法

```bash
# 测试 2: 快速发送多个请求
for i in {1..6}; do
  echo "Request $i:"
  curl -w "\nHTTP Status: %{http_code}\n" http://localhost:8080/api/posts/1
  sleep 0.5
done

# 预期结果:
# - 前 5 个请求返回 200 OK
# - 第 6 个请求返回 429 Too Many Requests
```

**令牌消耗图**:

```
令牌数
  5 │  ●●●●●─────●●●●●─────
  4 │  ●●●●●─────●●●●●─────
  3 │  ●●●●●─────●●●●●─────
  2 │  ●●●●●─────●●●●●─────
  1 │  ●●●●●─────●●●●●─────
  0 │  └─●─┬─●─┬─●─┬─●─┬─●─┬
    0s  1s  2s  3s  4s  5s  10s
    ↑   ↑   ↑   ↑   ↑
   请求 请求 请求 请求 请求
   1-5  全部  消耗  令牌
   ✓   (耗尽)      (补充后恢复)
```

---

### 练习 3: 计时过滤器测试

**目标**: 观察请求计时日志

```bash
# 测试 3: 发送请求并查看日志
curl http://localhost:8080/timing/test

# 在应用日志中查找:
# INFO: [GET] /timing/test completed in XXXms
```

---

### 练习 4: 查看网关指标

**目标**: 理解路由优先级和监控指标

```bash
# 查看网关请求指标
curl http://localhost:8080/actuator/metrics/spring.cloud.gateway.requests | jq

# 查看健康状态
curl http://localhost:8080/actuator/health | jq

# 查看所有可用的 actuator 端点
curl http://localhost:8080/actuator | jq
```

**观察要点**:
- order=-1 的限流路由会优先执行
- order=300 的 API 代理路由处理正常请求
- order 越小，优先级越高

---

### 练习 5: 实现自定义路由（TODO 练习）

**目标**: 根据代码中的 TODO 注释实现新的路由

参考 `RouteConfiguration.java` 中的 TODO 列表：

1. **基础路由**: Host 谓词 + Path 谓词 + PrefixPath 过滤器
2. **请求体谓词**: 读取请求体内容进行匹配
3. **请求体修改**: String → JSON 对象转换
4. **响应体修改**: String → String (转大写)
5. **响应体修改**: 处理空响应体场景

**实现提示**:
- 使用 `RouteLocatorBuilder` 构建路由
- 参考 `ThrottleGatewayFilter` 和 `TimingGatewayFilter` 实现自定义过滤器
- 使用 `@Slf4j` 和 Lombok 注解简化代码

---

## 8. 扩展学习

### 8.1 实现自己的过滤器

**任务**: 创建一个记录请求耗时的过滤器

#### 方式 1: 传统实现（不使用 Lombok）

```java
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TimingGatewayFilter implements GatewayFilter {
    private static final Log log = LogFactory.getLog(TimingGatewayFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - start;
            log.info("Request to {} took {} ms",
                exchange.getRequest().getPath(), duration);
        }));
    }
}
```

#### 方式 2: 使用 Lombok 简化（推荐）

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j  // 自动生成 logger
public class TimingGatewayFilter implements GatewayFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - start;
            log.info("Request to {} took {} ms",
                exchange.getRequest().getPath(), duration);
        }));
    }
}
```

**应用方式**:

```java
.route(r -> r.path("/timing/**")
    .filters(f -> f.filter(new TimingGatewayFilter()))
    .uri("http://example.com"))
```

---

### 8.2 熔断器集成

添加 Resilience4J 熔断器：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

配置：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: circuitbreaker_route
          uri: http://example.com
          predicates:
            - Path=/cb/**
          filters:
            - name: CircuitBreaker
              args:
                name: myCircuitBreaker
                fallbackUri: forward:/fallback
```

---

### 8.3 限流器集成

使用 Redis 实现分布式限流：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

配置：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: rate_limiter
          uri: http://example.com
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

---

### 8.4 服务发现集成

使用 Eureka 或 Nacos 进行动态路由：

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
```

这样可以直接用 `http://gateway/{service-id}/path` 访问服务。

---

### 8.5 监控和指标

查看网关指标：

```bash
# 查看网关请求统计
curl http://localhost:8080/actuator/metrics/spring.cloud.gateway.requests

# 查看所有可用的 metrics
curl http://localhost:8080/actuator/metrics
```

**可用指标示例**:
- `spring.cloud.gateway.requests` - 请求总数和响应时间
- 按标签过滤：`?tag=routeId:xyz` 或 `?tag=status:200`

集成 Prometheus + Grafana 实现可视化监控。

---

## 总结

通过学习这个示例模块，你应该掌握：

✅ **基础概念**:
- Route（路由）
- Predicate（谓词/匹配条件）
- Filter（过滤器）

✅ **路由定义方式**:
- Java DSL（主要方式）
- YAML 配置（可选）

✅ **核心过滤器**:
- 路径修改（StripPrefix、RewritePath）
- 头部操作（AddRequestHeader、AddResponseHeader）

✅ **自定义过滤器**:
- 实现 `GatewayFilter` 接口
- 返回 `Mono<Void>`
- 响应式编程模式
- 使用 Lombok 简化代码（@Slf4j、@Data、@Accessors）

✅ **当前示例功能**:
- 限流过滤器（ThrottleGatewayFilter）- 令牌桶算法
- 计时过滤器（TimingGatewayFilter）- 请求耗时记录
- API 代理路由 - 路径重写和头部修改
- 配置与代码分离设计

✅ **开发工具**:
- Lombok 注解减少样板代码
- Maven 注解处理器配置
- Actuator 监控端点

✅ **架构变化**（5.x 版本）:
- 配置前缀从 `spring.cloud.gateway` 改为 `spring.cloud.gateway.server.webflux`
- 主应用类简化，路由配置独立到 `RouteConfiguration`
- 移除 Kotlin DSL 示例
- 使用 JSONPlaceholder 替代 httpbin.org 作为测试服务

---

## 下一步学习建议

1. **阅读源码**: 深入研究 `spring-cloud-gateway-server-webflux` 模块
2. **实战项目**: 构建一个包含认证、限流、熔断的完整网关
3. **性能优化**: 学习 Netty 配置、连接池调优
4. **生产实践**: 了解灰度发布、蓝绿部署、监控告警

---

## 参考资料

- [Spring Cloud Gateway 官方文档](https://spring.io/projects/spring-cloud-gateway)
- [Spring Cloud Gateway 源码](https://github.com/spring-cloud/spring-cloud-gateway)
- [Project Reactor 文档](https://projectreactor.io/docs)
- [httpbin.org 在线测试](https://httpbin.org)

---

**作者注**: 本指南基于 Spring Cloud Gateway 5.0.2-SNAPSHOT 版本编写。