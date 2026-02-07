# Spring Cloud Gateway Sample 模块学习指南

本指南将帮助你逐步深入理解 Spring Cloud Gateway 的示例模块。

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

- 路由的多种定义方式（Java DSL、Kotlin DSL、YAML 配置）
- 各种网关过滤器的使用
- 自定义过滤器的实现
- 请求/响应体的修改
- 限流功能实现

### 1.2 技术栈

| 技术 | 版本 | 用途 |
|-----|------|-----|
| Java | 17+ | 编程语言 |
| Kotlin | - | 支持 Kotlin DSL 路由定义 |
| Spring Boot | 3.x | 应用框架 |
| Spring Cloud Gateway | 5.x | API 网关核心 |
| WebFlux | - | 响应式 Web 框架 |
| Lombok | - | 简化 Java 代码（生成 getter/setter/logger） |
| token-bucket | 1.7 | 限流算法库 |
| httpbin.org | - | 测试目标服务 |

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
# 安装 wscat 用于 WebSocket 测试
npm install -g wscat

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
│   │   │   └── ThrottleGatewayFilter.java          # 自定义限流过滤器 ⭐
│   │   ├── kotlin/org/springframework/cloud/gateway/sample/
│   │   │   └── AdditionalRoutes.kt                 # Kotlin 路由定义 ⭐
│   │   └── resources/
│   │       ├── application.yml                       # 主配置文件 ⭐
│   │       └── application-secureheaders.yml         # 安全头配置
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

# 测试简单路由
curl http://localhost:8080/testfun
```

预期输出：
- 健康检查返回 `{"status":"UP"}`
- 请求指标显示已配置的路由（如 `routeId: default_path_to_httpbin`）
- `/testfun` 返回 "hello"

> **注意**: Spring Cloud Gateway 5.x 版本中，`/actuator/gateway/routes` 端点可能不再可用或需要额外配置。请使用 metrics 端点或实际路由测试来验证。

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

### 5.2 路由的三种定义方式

#### 方式 1: Java DSL (推荐)

```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route(r -> r.host("**.abc.org")
            .and().path("/anything/png")
            .filters(f -> f.prefixPath("/httpbin"))
            .uri("http://httpbin.org:80"))
        .build();
}
```

#### 方式 2: Kotlin DSL (更简洁)

```kotlin
@Bean
fun additionalRouteLocator(builder: RouteLocatorBuilder) = builder.routes {
    route(id = "test-kotlin") {
        host("kotlin.abc.org")
        filters { prefixPath("/httpbin") }
        uri(uri)
    }
}
```

#### 方式 3: YAML 配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: my_route
          uri: http://httpbin.org:80
          predicates:
            - Path=/api/**
          filters:
            - PrefixPath=/httpbin
```

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
@Import(AdditionalRoutes.class)  // 导入 Kotlin 路由配置
```

#### 路由示例分析

**示例 1: 基础路由（行 63-68）**

```java
.route(r -> r.host("**.abc.org").and().path("/anything/png")
    .filters(f -> f.prefixPath("/httpbin")
                    .addResponseHeader("X-TestHeader", "foobar"))
    .uri(uri))
```

**工作流程**:

```
请求: http://abc.org/anything/png
  ↓
1. Host 谓词匹配: **.abc.org ✓
  ↓
2. Path 谓词匹配: /anything/png ✓
  ↓
3. 应用过滤器:
   - prefixPath(/httpbin) → 路径变为 /httpbin/anything/png
   - addResponseHeader → 添加 X-TestHeader: foobar
  ↓
4. 转发到: http://httpbin.org:80/httpbin/anything/png
```

**示例 2: 请求体谓词（行 69-75）**

```java
.route("read_body_pred", r -> r.host("*.readbody.org")
    .and().readBody(String.class, s -> s.trim().equalsIgnoreCase("hi"))
    .filters(f -> f.prefixPath("/httpbin")
                    .addResponseHeader("X-TestHeader", "read_body_pred"))
    .uri(uri))
```

**特点**: 读取请求体进行判断，只有内容为 "hi"（忽略大小写）才匹配。

**示例 3: 请求体修改（行 76-84）**

```java
.route("rewrite_request_obj", r -> r.host("*.rewriterequestobj.org")
    .filters(f -> f.prefixPath("/httpbin")
                    .addResponseHeader("X-TestHeader", "rewrite_request")
                    .modifyRequestBody(String.class, Hello.class,
                        MediaType.APPLICATION_JSON_VALUE,
                        (exchange, s) -> {
                            return Mono.just(new Hello(s.toUpperCase()));
                        }))
    .uri(uri))
```

**转换流程**:

```
String 请求体 → 转大写 → Hello 对象 → JSON
"hello"  →  "HELLO"  →  Hello("HELLO")  →  {"message":"HELLO"}
```

**示例 4: 响应体修改（行 94-101）**

```java
.route("rewrite_response_upper", r -> r.host("*.rewriteresponseupper.org")
    .filters(f -> f.prefixPath("/httpbin")
                    .addResponseHeader("X-TestHeader", "rewrite_response_upper")
                    .modifyResponseBody(String.class, String.class,
                        (exchange, s) -> {
                            return Mono.just(s.toUpperCase());
                        }))
    .uri(uri))
```

**转换流程**:

```
上游响应 "hello world" → 转大写 → 返回给客户端 "HELLO WORLD"
```

**示例 5: 自定义限流过滤器（行 145-154）**

```java
.route(r -> r.order(-1)  // 高优先级
    .host("**.throttle.org").and().path("/get")
    .filters(f -> f.prefixPath("/httpbin")
                    .filter(new ThrottleGatewayFilter()
                        .setCapacity(1)
                        .setRefillTokens(1)
                        .setRefillPeriod(10)
                        .setRefillUnit(TimeUnit.SECONDS)))
    .uri(uri))
```

**限流参数**:
- `capacity`: 1（桶容量）
- `refillTokens`: 1（每次补充 1 个令牌）
- `refillPeriod`: 10（每 10 秒）
- `refillUnit`: SECONDS（秒）

**效果**: 每 10 秒只允许 1 个请求。

---

### 6.2 自定义过滤器 (ThrottleGatewayFilter.java)

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

### 6.3 Kotlin DSL 路由 (AdditionalRoutes.kt)

**文件位置**: `src/main/kotlin/org/springframework/cloud/gateway/sample/AdditionalRoutes.kt`

#### 完整代码

```kotlin
@Configuration(proxyBeanMethods = false)
open class AdditionalRoutes {
    @Value("\${test.uri:http://httpbin.org:80}")
    var uri: String = ""

    @Bean
    open fun additionalRouteLocator(builder: RouteLocatorBuilder) = builder.routes {
        route(id = "test-kotlin") {
            host("kotlin.abc.org") and path("/anything/kotlinroute")
            filters {
                prefixPath("/httpbin")
                addResponseHeader("X-TestHeader", "foobar")
            }
            uri(uri)
        }
    }
}
```

#### Kotlin DSL 优势

| Java DSL | Kotlin DSL |
|---------|-----------|
| `r.host("**.abc.org").and().path("/png")` | `host("**.abc.org") and path("/png")` |
| `.filters(f -> f.prefixPath("/http"))` | `filters { prefixPath("/http") }` |
| 需要更多语法 | 更接近自然语言 |

---

### 6.4 配置文件分析

#### application.yml

**文件位置**: `src/main/resources/application.yml`

```yaml
test:
  uri: lb://httpbin  # 使用负载均衡

spring:
  cloud:
    gateway.server.webflux:
      default-filters:      # 全局过滤器（应用于所有路由）
      - PrefixPath=/httpbin
      - AddResponseHeader=X-Response-Default-Foo, Default-Bar

      routes:
      # WebSocket 路由
      - id: websocket_test
        uri: ws://localhost:9000
        order: 9000
        predicates:
        - Path=/echo

      # 默认路由（最低优先级）
      - id: default_path_to_httpbin
        uri: ${test.uri}
        order: 10000
        predicates:
        - Path=/**
```

**配置要点**:

1. **default-filters**: 应用于所有路由的全局过滤器
2. **order**: 数字越小优先级越高（-1 最高）
3. **lb://**: 表示使用 Spring Cloud LoadBalancer

#### application-secureheaders.yml

展示了 **SecureHeaders** 过滤器的配置：

```yaml
filters:
  - name: SecureHeaders
    args:
      disable: x-frame-options       # 禁用某个头
      enable: permissions-policy      # 启用某个头
      permissions-policy: geolocation=("https://example.net")
```

---

## 7. 实践练习

### 练习 1: 基础路由测试

**目标**: 理解 Host 谓词和路径前缀

```bash
# 测试 1: Host 匹配 + PrefixPath
curl -H "Host: abc.org" http://localhost:8080/anything/png

# 预期结果:
# - 请求被转发到 http://httpbin.org/httpbin/anything/png
# - 响应头包含 X-TestHeader: foobar
```

**流程图**:

```
┌─────────────────────────────────────────────────────────┐
│  curl -H "Host: abc.org" http://localhost:8080/.../png  │
└─────────────────────────────────────────────────────────┘
                         │
                         ↓
         ┌───────────────────────────────┐
         │  Route: **.abc.org 匹配 ✓    │
         │  Path: /anything/png 匹配 ✓   │
         └───────────────────────────────┘
                         │
                         ↓
         ┌───────────────────────────────┐
         │  Filters:                     │
         │  1. prefixPath(/httpbin)      │
         │     /anything/png             │
         │     → /httpbin/anything/png   │
         │  2. addResponseHeader         │
         └───────────────────────────────┘
                         │
                         ↓
         ┌───────────────────────────────┐
         │  Forward to:                  │
         │  http://httpbin.org:80        │
         │  /httpbin/anything/png        │
         └───────────────────────────────┘
```

---

### 练习 2: 请求体修改测试

**目标**: 理解请求体类型转换

```bash
# 测试 2: String → JSON 对象
curl -X POST -H "Host: rewriterequestobj.org" \
  -H "Content-Type: text/plain" \
  -d "hello world" \
  http://localhost:8080/anything

# 预期结果:
# - 请求体 "hello world" 被转换为 {"message":"HELLO WORLD"}
# - Content-Type 变为 application/json
```

---

### 练习 3: 限流过滤器测试

**目标**: 理解令牌桶算法

```bash
# 测试 3: 限流功能
curl -H "Host: throttle.org" http://localhost:8080/get
# 第 1 次: 200 OK ✓

curl -H "Host: throttle.org" http://localhost:8080/get
# 第 2 次（立即）: 429 Too Many Requests ✗

# 等待 10 秒后重试
sleep 10
curl -H "Host: throttle.org" http://localhost:8080/get
# 第 3 次: 200 OK ✓
```

**令牌消耗图**:

```
令牌数
  1 │  ●─────●─────●
  0 │  └─●─┬─●─┬─●─┬─●
    0s   5s    10s   15s    20s
    ↑    ↑     ↑     ↑
   请求 请求  等待  请求
   1    2    10秒   3
   ✓   ✗         ✓
```

---

### 练习 4: WebSocket 路由

**目标**: 理解 WebSocket 代理

```bash
# 终端 1: 启动 WebSocket 服务器
wscat --listen 9000

# 终端 2: 通过网关连接
wscat --connect ws://localhost:8080/echo

# 现在终端 2 发送的消息会通过网关转发到终端 1
```

---

### 练习 5: 路由优先级和查看指标

**目标**: 理解 order 参数并查看路由信息

```bash
# 查看网关请求指标（包含路由 ID 信息）
curl http://localhost:8080/actuator/metrics/spring.cloud.gateway.requests | jq

# 查看所有可用的 actuator 端点
curl http://localhost:8080/actuator | jq
```

**观察要点**:
- order=-1 的路由优先级最高（限流路由）
- order=10000 的路由优先级最低（默认路由）
- 指标中包含 `routeId` 标签，显示哪些路由被访问

**路由优先级参考**:
- 限流路由: `order=-1` (最高优先级)
- 默认路由: `order=10000` (最低优先级)

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

✅ **三种路由定义方式**:
- Java DSL
- Kotlin DSL
- YAML 配置

✅ **核心过滤器**:
- 路径修改（PrefixPath、StripPrefix、RewritePath）
- 头部操作（AddRequestHeader、AddResponseHeader）
- 请求/响应体修改（modifyRequestBody、modifyResponseBody）

✅ **自定义过滤器**:
- 实现 `GatewayFilter` 接口
- 返回 `Mono<Void>`
- 响应式编程模式
- 使用 Lombok 简化代码（@Slf4j、@Data、@Accessors）

✅ **高级功能**:
- 限流（令牌桶算法）
- 熔断（Resilience4J）
- 服务发现
- WebSocket 代理

✅ **开发工具**:
- Lombok 注解减少样板代码
- Maven 注解处理器配置

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