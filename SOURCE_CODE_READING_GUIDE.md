# Spring Cloud Gateway 源码阅读指南

> 本文档为 Spring Cloud Gateway 项目源码阅读提供详细的模块架构、依赖关系和阅读路径指导。

---

## 目录

- [模块架构总览](#模块架构总览)
- [模块依赖关系](#模块依赖关系)
- [源码阅读路线](#源码阅读路线)
  - [阶段 1：从示例入手](#阶段-1从示例入手)
  - [阶段 2：核心模块架构](#阶段-2核心模块架构)
  - [阶段 3：高级特性](#阶段-3高级特性)
  - [阶段 4：WebMVC 实现](#阶段-4webmvc-实现)
  - [阶段 5：ProxyExchange 模块](#阶段-5proxyexchange-模块)
- [阅读技巧](#阅读技巧)
- [推荐阅读顺序总结](#推荐阅读顺序总结)
- [快速开始命令](#快速开始命令)

---

## 模块架构总览

```
spring-cloud-gateway (父项目)
│
├── 📚 spring-cloud-gateway-dependencies          [依赖管理 BOM]
│   └── 统一管理所有依赖版本
│
├── 🎯 核心实现模块
│   ├── spring-cloud-gateway-server-webflux      [核心 ★★★★★] - 响应式网关实现
│   ├── spring-cloud-gateway-server-webmvc       [可选] - Servlet 网关实现
│   ├── spring-cloud-gateway-proxyexchange-webflux - Proxy Exchange 支持
│   └── spring-cloud-gateway-proxyexchange-webmvc   - Proxy Exchange 支持
│
├── 🚀 Starter 模块（用户使用）
│   ├── spring-cloud-starter-gateway-server-webflux [★ 默认推荐]
│   └── spring-cloud-starter-gateway-server-webmvc
│
├── 📖 示例与测试
│   ├── spring-cloud-gateway-sample               [示例应用]
│   └── spring-cloud-gateway-integration-tests    [集成测试]
│       ├── grpc/          - gRPC 测试
│       ├── http2/         - HTTP/2 测试
│       ├── httpclient/    - HTTP 客户端测试
│       └── mvc-failure-analyzer/ - MVC 失败分析
│
└── 📚 docs                                       [文档]
```

### 模块说明

| 模块 | 重要程度 | 说明 |
|------|---------|------|
| `spring-cloud-gateway-dependencies` | ★★★☆☆ | 依赖管理 BOM，统一管理版本 |
| `spring-cloud-gateway-server-webflux` | ★★★★★ | 核心实现，基于 WebFlux 的响应式网关 |
| `spring-cloud-gateway-server-webmvc` | ★★★☆☆ | 基于 Servlet 的网关实现（可选） |
| `spring-cloud-starter-gateway-server-webflux` | ★★★★★ | 用户使用的 Starter，默认推荐 |
| `spring-cloud-gateway-sample` | ★★★★☆ | 示例应用，学习使用方式 |
| `spring-cloud-gateway-integration-tests` | ★★☆☆☆ | 集成测试，部分需要 Docker |

---

## 模块依赖关系

### 用户视角的依赖链

```
用户应用
   ↓ 依赖
starter-gateway-server-webflux (Starter)
   ↓ 依赖
├── spring-cloud-starter (Spring Cloud 基础)
├── spring-boot-starter-webflux (WebFlux)
└── gateway-server-webflux (核心实现) ★★★★★
       ↓ 依赖 (都是 optional)
       ├── spring-boot-starter-webflux (响应式 Web)
       ├── spring-cloud-loadbalancer (负载均衡)
       ├── spring-boot-starter-data-redis (限流)
       ├── resilience4j (熔断器)
       ├── spring-boot-starter-actuator (监控)
       └── micrometer-tracing (链路追踪)
```

### Starter 模块依赖详情

**spring-cloud-starter-gateway-server-webflux** 依赖：
- `spring-cloud-starter` - Spring Cloud 基础
- `spring-cloud-gateway-server-webflux` - 核心实现
- `spring-boot-starter-webflux` - WebFlux 支持

**spring-cloud-gateway-server-webflux** 核心依赖（optional）：
- `spring-boot-starter-webflux` - 响应式 Web 框架
- `spring-boot-starter-validation` - 配置验证
- `spring-cloud-loadbalancer` - 负载均衡
- `spring-cloud-function-context` - 函数式编程支持
- `spring-cloud-stream` - 流处理支持
- `spring-boot-starter-data-redis` - Redis 限流
- `spring-cloud-starter-circuitbreaker-reactor-resilience4j` - 熔断器
- `com.bucket4j:bucket4j_jdk17-core` - 限流算法
- `io.micrometer:micrometer-tracing` - 链路追踪
- `io.grpc:*` - gRPC 支持
- `com.fasterxml.jackson.dataformat:jackson-dataformat-protobuf` - Protobuf 支持

---

## 源码阅读路线

### 阶段 1：从示例入手

**预计时间：** 1-2 小时

**目的：** 快速理解网关的使用方式和功能

#### 推荐阅读文件

```
📁 spring-cloud-gateway-sample/
├── src/main/resources/application.yml    ← 看配置示例
└── src/main/java/.../                   ← 看如何配置路由
```

#### 阅读重点

1. **application.yml** - 了解路由、谓词、过滤器的配置格式
2. **配置类** - 看如何通过代码定义路由
3. **启动类** - 看如何启用网关功能

---

### 阶段 2：核心模块架构

**预计时间：** 3-5 天

**主战场：** `spring-cloud-gateway-server-webflux`

#### 2.1 自动配置入口（必读）

```
📁 spring-cloud-gateway-server-webflux/src/main/java/org/springframework/cloud/gateway/config/
├── GatewayAutoConfiguration.java              [★★★★★] 核心自动配置
├── GatewayProperties.java                     [★★★★☆] 配置属性绑定
├── GatewayReactiveLoadBalancerClientAutoConfiguration.java
├── GatewayRedisAutoConfiguration.java
├── GatewayResilience4JCircuitBreakerAutoConfiguration.java
├── GatewayMetricsAutoConfiguration.java
├── GatewayNoLoadBalancerClientAutoConfiguration.java
├── GatewayTracingAutoConfiguration.java
├── GatewayStreamAutoConfiguration.java
├── GatewayFunctionAutoConfiguration.java
├── LocalResponseCacheAutoConfiguration.java
└── HttpClientProperties.java                  [★★★☆☆] HTTP 客户端配置
```

##### GatewayAutoConfiguration 阅读重点

这是整个网关的核心配置类，需要理解：

1. **核心 Bean 定义：**
   - `RouteLocator` - 路由定位器
   - `RouteDefinitionLocator` - 路由定义定位器
   - `FilteringWebHandler` - 过滤器处理器
   - `RoutePredicateHandlerMapping` - 路由匹配器
   - `HttpClient` - HTTP 客户端

2. **条件装配：**
   - 理解 `@ConditionalOn*` 注解的使用
   - 理解什么时候启用哪些功能

##### GatewayProperties 阅读重点

1. **配置结构：**
   ```java
   @ConfigurationProperties(prefix = "spring.cloud.gateway.server.webflux")
   public class GatewayProperties {
       private List<RouteDefinition> routes = new ArrayList<>();
       private List<FilterDefinition> defaultFilters = new ArrayList<>();
       private Metrics metrics = new Metrics();
       // ...
   }
   ```

2. **如何映射 YAML 配置：**
   ```yaml
   spring:
     cloud:
       gateway:
         server:
           webflux:
             routes:
               - id: route1
                 uri: http://example.org
                 predicates:
                   - Path=/api/**
                 filters:
                   - AddRequestHeader=X-Request-Id, 123
   ```

---

#### 2.2 路由系统（核心核心核心！）

```
📁 spring-cloud-gateway-server-webflux/src/main/java/org/springframework/cloud/gateway/route/
├── Route.java                                [★★★★★] 路由实体模型
├── RouteDefinition.java                      [★★★★☆] 路由定义
├── RouteLocator.java                         [★★★★★] 路由定位器接口
├── RouteDefinitionLocator.java               [★★★★☆] 路由定义定位器
├── RouteDefinitionRouteLocator.java          [★★★★★] 将定义转为路由
├── CachingRouteLocator.java                  [★★★★☆] 带缓存的路由定位器
├── CompositeRouteLocator.java                [★★★☆☆] 组合路由定位器
├── CachingRouteDefinitionLocator.java        [★★★☆☆] 带缓存的定义定位器
├── CompositeRouteDefinitionLocator.java      [★★★☆☆] 组合定义定位器
├── InMemoryRouteDefinitionRepository.java    [★★★☆☆] 内存路由存储
├── RedisRouteDefinitionRepository.java       [★★☆☆☆] Redis 路由存储
├── RouteDefinitionWriter.java                [★★☆☆☆] 路由定义写入接口
├── RouteDefinitionRepository.java            [★★☆☆☆] 路由定义仓库
├── RouteDefinitionMetrics.java               [★★☆☆☆] 路由指标
└── RouteRefreshListener.java                 [★★★☆☆] 路由刷新监听器
```

##### 调用链路

```
配置文件 (application.yml)
    ↓
GatewayProperties (配置绑定)
    ↓
PropertiesRouteDefinitionLocator (读取配置)
    ↓
RouteDefinition (路由定义)
    ↓
RouteDefinitionRouteLocator (转换为路由)
    ↓
Route (最终路由对象)
    ↓
CachingRouteLocator (缓存)
```

##### 核心类说明

**Route.java** - 路由实体
```java
public class Route {
    private String id;                    // 路由 ID
    private URI uri;                      // 目标 URI
    private int order;                    // 排序
    private AsyncPredicate<ServerWebExchange> predicate;  // 谓词
    private List<GatewayFilter> filters;  // 过滤器
}
```

**RouteDefinition.java** - 路由定义
```java
public class RouteDefinition {
    private String id;                       // 路由 ID
    private List<PredicateDefinition> predicates = new ArrayList<>();
    private List<FilterDefinition> filters = new ArrayList<>();
    private URI uri;                         // 目标 URI
    private int order;                       // 排序
}
```

**RouteDefinitionRouteLocator** - 核心转换逻辑
- 将 `RouteDefinition` 转换为 `Route`
- 解析谓词定义并创建 `AsyncPredicate`
- 解析过滤器定义并创建 `GatewayFilter`

---

#### 2.3 过滤器系统（理解请求处理流程）

```
📁 spring-cloud-gateway-server-webflux/src/main/java/org/springframework/cloud/gateway/filter/
├── GlobalFilter.java                         [接口] 全局过滤器
├── GatewayFilter.java                        [接口] 网关过滤器
├── GatewayFilterChain.java                   [接口] 过滤器链
├── OrderedGatewayFilter.java                 [包装类] 带排序的过滤器
├── NettyRoutingFilter.java                   [★★★★★] 核心：实际 HTTP 代理
├── NettyWriteResponseFilter.java             [★★★★☆] 写回响应
├── RouteToRequestUrlFilter.java              [★★★★☆] 解析目标 URL
├── ForwardRoutingFilter.java                 [★★★☆☆] 转发路由
├── WebsocketRoutingFilter.java               [★★★☆☆] WebSocket 支持
├── FunctionRoutingFilter.java                [★★★☆☆] 函数式路由
├── StreamRoutingFilter.java                  [★★★☆☆] 流处理路由
├── ReactiveLoadBalancerClientFilter.java     [★★★★☆] 负载均衡
├── LoadBalancerServiceInstanceCookieFilter.java [★★★☆☆] 负载均衡 Cookie
├── AdaptCachedBodyGlobalFilter.java         [★★★☆☆] 缓存 Body
├── RemoveCachedBodyFilter.java              [★★★☆☆] 清除缓存 Body
├── GatewayMetricsFilter.java                [★★☆☆☆] 指标收集
├── ForwardPathFilter.java                   [★★☆☆☆] 转发路径处理
├── FilterDefinition.java                    [★★★☆☆] 过滤器定义
├── factory/                                  [★★★★☆] 各种过滤器工厂
│   ├── AddRequestHeaderGatewayFilterFactory.java
│   ├── AddRequestParameterGatewayFilterFactory.java
│   ├── AddResponseHeaderGatewayFilterFactory.java
│   ├── RewritePathGatewayFilterFactory.java
│   ├── RewriteResponseHeaderGatewayFilterFactory.java
│   ├── SetPathGatewayFilterFactory.java
│   ├── SetRequestHeaderGatewayFilterFactory.java
│   ├── SetResponseHeaderGatewayFilterFactory.java
│   ├── PrefixPathGatewayFilterFactory.java
│   ├── StripPrefixGatewayFilterFactory.java
│   ├── RedirectToGatewayFilterFactory.java
│   ├── RemoveRequestHeaderGatewayFilterFactory.java
│   ├── RemoveRequestParameterGatewayFilterFactory.java
│   ├── RemoveResponseHeaderGatewayFilterFactory.java
│   ├── RequestRateLimiterGatewayFilterFactory.java
│   ├── RetryGatewayFilterFactory.java
│   ├── SetStatusGatewayFilterFactory.java
│   ├── SaveSessionGatewayFilterFactory.java
│   ├── SecureHeadersGatewayFilterFactory.java
│   ├── RequestHeaderSizeGatewayFilterFactory.java
│   ├── RequestHeaderToRequestUriGatewayFilterFactory.java
│   ├── SpringCloudCircuitBreakerFilterFactory.java
│   ├── CacheRequestBodyGatewayFilterFactory.java
│   └── ...
├── cors/                                     [★★★☆☆] CORS 支持
│   └── CorsGatewayFilterFactory.java
├── ratelimit/                                [★★★☆☆] 限流实现
│   ├── PrincipalNameKeyResolver.java
│   └── RateLimiter.java
└── headers/                                  [★★★☆☆] 请求头处理
    ├── ForwardedHeadersFilter.java
    ├── RemoveHopByHopHeadersFilter.java
    └── XForwardedHeadersFilter.java
```

##### 请求处理流程

```
HTTP 请求进入
    ↓
RoutePredicateHandlerMapping.getHandler()     [匹配路由]
    ↓
找到匹配的 Route
    ↓
FilteringWebHandler.handle()                  [处理请求]
    ↓
获取过滤器链 (GlobalFilter + GatewayFilter)
    ↓
按顺序执行过滤器:
    1. AdaptCachedBodyGlobalFilter            [缓存 Body]
    2. RouteToRequestUrlFilter                [解析 URL]
    3. ReactiveLoadBalancerClientFilter       [负载均衡选择实例]
    4. NettyRoutingFilter                     [★ 发送 HTTP 请求]
    5. [路由级 GatewayFilter 序列]
    6. NettyWriteResponseFilter               [写回响应]
    ↓
HTTP 响应返回
```

##### 核心 GlobalFilter 执行顺序

| 过滤器 | 顺序 | 作用 |
|-------|------|------|
| `RemoveCachedBodyFilter` | -HIGHEST_PRECEDENCE (最先) | 清除缓存 |
| `AdaptCachedBodyGlobalFilter` | -HIGHEST_PRECEDENCE + 1000 | 缓存 Body |
| `NettyWriteResponseFilter` | -HIGHEST_PRECEDENCE + 100 | 写响应 |
| `ForwardPathFilter` | 0 | 转发路径 |
| `RouteToRequestUrlFilter` | 10000 | 解析 URL |
| `LoadBalancerClientFilter` | 10100 | 负载均衡 |
| `WebsocketRoutingFilter` | 10150 | WebSocket |
| `NettyRoutingFilter` | 10300 | ★ 实际代理 ★ |
| `GatewayMetricsFilter` | -100 | 指标收集 |

---

#### 2.4 谓词工厂（路由匹配条件）

```
📁 spring-cloud-gateway-server-webflux/src/main/java/org/springframework/cloud/gateway/handler/predicate/
├── RoutePredicateFactory.java                [接口] 谓词工厂
├── PathRoutePredicateFactory.java            [★★★★★] 路径匹配
├── MethodRoutePredicateFactory.java          [★★★★☆] HTTP 方法匹配
├── HostRoutePredicateFactory.java            [★★★★☆] Host 匹配
├── HeaderRoutePredicateFactory.java          [★★★☆☆] Header 匹配
├── CookieRoutePredicateFactory.java          [★★★☆☆] Cookie 匹配
├── QueryRoutePredicateFactory.java           [★★★☆☆] Query 参数匹配
├── RemoteAddrRoutePredicateFactory.java      [★★★☆☆] IP 匹配
├── WeightRoutePredicateFactory.java          [★★★☆☆] 权重路由
├── BetweenRoutePredicateFactory.java         [★★★☆☆] 时间区间
├── BeforeRoutePredicateFactory.java          [★★★☆☆] 时间之前
├── AfterRoutePredicateFactory.java           [★★★☆☆] 时间之后
├── CloudFoundryRouteServicePredicateFactory.java
├── ReadBodyRoutePredicateFactoryFactory.java  [★★☆☆☆] 读取 Body
├── RoutePredicateFactory.java                [基类]
└── predicate/
    └── GatewayPredicate.java                 [包装类]
```

##### 常用谓词示例

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: path-route
          uri: http://example.org
          predicates:
            - Path=/api/**                    # 路径匹配

        - id: method-route
          uri: http://example.org
          predicates:
            - Method=GET,POST                 # 方法匹配

        - id: header-route
          uri: http://example.org
          predicates:
            - Header=X-Request-Id, \d+        # Header 匹配

        - id: cookie-route
          uri: http://example.org
          predicates:
            - Cookie=chocolate, ch.p           # Cookie 匹配

        - id: weight-route
          uri: http://example.org
          predicates:
            - Weight=group1, 8                 # 80% 流量
```

---

#### 2.5 请求处理器

```
📁 spring-cloud-gateway-server-webflux/src/main/java/org/springframework/cloud/gateway/handler/
├── FilteringWebHandler.java                  [★★★★★] 过滤器链执行器
└── RoutePredicateHandlerMapping.java         [★★★★★] 路由匹配器
```

##### FilteringWebHandler

核心职责：
1. 接收匹配的路由
2. 组装过滤器链（GlobalFilter + GatewayFilter）
3. 按顺序执行过滤器
4. 返回响应

##### RoutePredicateHandlerMapping

核心职责：
1. 继承 `AbstractHandlerMapping`
2. 使用 `RouteLocator` 获取所有路由
3. 遍历路由，使用谓词匹配
4. 返回匹配的路由

---

### 阶段 3：高级特性

**预计时间：** 2-3 天（按需阅读）

#### 3.1 熔断器

```
📁 相关文件：
├── config/GatewayResilience4JCircuitBreakerAutoConfiguration.java
├── filter/factory/SpringCloudCircuitBreakerFilterFactory.java
└── filter/factory/RetryGatewayFilterFactory.java
```

**功能：**
- 集成 Resilience4J
- 熔断、降级、限流
- 自动重试

**配置示例：**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: circuitbreaker-route
          uri: http://example.org
          predicates:
            - Path=/api/**
          filters:
            - CircuitBreaker=myCircuitBreaker
```

---

#### 3.2 限流

```
📁 相关文件：
├── config/GatewayRedisAutoConfiguration.java
├── filter/factory/RequestRateLimiterGatewayFilterFactory.java
├── filter/ratelimit/
│   ├── PrincipalNameKeyResolver.java
│   ├── RateLimiter.java
│   └── ...
```

**限流实现：**
- 基于 Redis 的令牌桶算法
- 基于 Bucket4j 的本地限流
- 支持自定义 Key 解析器

**配置示例：**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: rate-limiter-route
          uri: http://example.org
          predicates:
            - Path=/api/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

---

#### 3.3 负载均衡

```
📁 相关文件：
├── config/GatewayReactiveLoadBalancerClientAutoConfiguration.java
├── filter/ReactiveLoadBalancerClientFilter.java
├── filter/LoadBalancerServiceInstanceCookieFilter.java
└── config/GatewayLoadBalancerProperties.java
```

**功能：**
- 集成 Spring Cloud LoadBalancer
- 支持服务发现
- 客户端负载均衡

**配置示例：**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: lb-route
          uri: lb://service-name              # 使用负载均衡
          predicates:
            - Path=/api/**
```

---

#### 3.4 服务发现

```
📁 discovery/
├── GatewayDiscoveryClientAutoConfiguration.java
└── ...
```

**功能：**
- 自动从注册中心发现服务
- 自动创建路由

---

#### 3.5 监控与追踪

```
📁 相关文件：
├── config/GatewayMetricsAutoConfiguration.java
├── config/GatewayTracingAutoConfiguration.java
├── filter/GatewayMetricsFilter.java
└── actuate/
    └── GatewayControllerEndpoint.java        [Actuator 端点]
```

**功能：**
- Micrometer 指标收集
- 分布式链路追踪
- Actuator 健康检查

---

### 阶段 4：WebMVC 实现

**预计时间：** 1-2 天（可选）

**目的：** 对比学习响应式和 Servlet 两种实现

```
📁 spring-cloud-gateway-server-webmvc/
├── config/GatewayAutoConfiguration.java
├── route/                                    [对应 WebFlux 的 route 包]
├── filter/                                   [对应 WebFlux 的 filter 包]
├── handler/                                  [对应 WebFlux 的 handler 包]
└── handler/predicate/                        [对应 WebFlux 的 predicate 包]
```

**对比学习点：**
- WebFlux vs MVC 的实现差异
- 响应式 `Mono/Flux` vs Servlet 阻塞模型
- `DispatcherHandler` vs `DispatcherServlet`
- `WebFilter` vs `Filter`

---

### 阶段 5：ProxyExchange 模块

**预计时间：** 半天

**目的：** 了解编程式代理用法

```
📁 spring-cloud-gateway-proxyexchange-webflux/
└── src/main/java/org/springframework/cloud/gateway/exchange/
    └── ProxyExchange.java                    [编程式代理工具类]
```

**使用场景：**
- 在 Controller 中手动代理请求
- 需要更细粒度控制的场景

**示例：**
```java
@RestController
public class ProxyController {

    @GetMapping("/proxy/**")
    public ResponseEntity<?> proxy(
        ProxyExchange<byte[]> proxy) throws Exception {
        return proxy.uri("http://example.org")
                    .get();
    }
}
```

---

## 阅读技巧

### 1. 带着问题读源码

在阅读时时刻思考这些问题：

- **一个 HTTP 请求如何被路由到后端服务？**
  - 请求如何进入网关？
  - 如何匹配到正确的路由？
  - 如何选择后端实例？

- **过滤器是如何按顺序执行的？**
  - GlobalFilter 和 GatewayFilter 有什么区别？
  - 过滤器的顺序是如何确定的？
  - 如何自定义过滤器？

- **路由配置是如何加载的？**
  - YAML 配置如何映射为对象？
  - RouteDefinition 如何转换为 Route？
  - 路由如何动态刷新？

---

### 2. 用调试辅助理解

```bash
# 1. 构建项目
./mvnw clean install -DskipTests

# 2. 启动示例项目
cd spring-cloud-gateway-sample
../mvnw spring-boot:run

# 3. 发送测试请求
curl http://localhost:8080/api/get
```

**关键断点位置：**

```java
// 1. 路由匹配
// 文件：RoutePredicateHandlerMapping.java
// 方法：getHandler(ServerWebExchange exchange)
// 作用：匹配路由，返回 FilteringWebHandler

// 2. 过滤器链执行
// 文件：FilteringWebHandler.java
// 方法：handle(ServerWebExchange exchange)
// 作用：组装并执行过滤器链

// 3. 实际代理请求
// 文件：NettyRoutingFilter.java
// 方法：filter(ServerWebExchange exchange, GatewayFilterChain chain)
// 作用：发送 HTTP 请求到后端服务

// 4. 路由定义转换
// 文件：RouteDefinitionRouteLocator.java
// 方法：getRoutes()
// 作用：将 RouteDefinition 转换为 Route
```

---

### 3. 理解响应式编程

**核心概念：**
- **Mono<T>** - 0 或 1 个元素的异步序列
- **Flux<T>** - 0 到 N 个元素的异步序列
- **背压（Backpressure）** - 生产者-消费者流量控制
- **调度器（Scheduler）** - 线程切换

**常用操作符：**
```java
// 创建
Mono.just(value)
Flux.fromIterable(list)

// 转换
.map(x -> transform(x))
.flatMap(x -> asyncOperation(x))

// 组合
Mono.zip(mono1, mono2)
Flux.merge(flux1, flux2)

// 条件
.filter(x -> condition(x)
.switchIfEmpty(fallback)
.onErrorResume(ex -> recovery)

// 终端
.subscribe(value -> handle(value))
.block()  // 阻塞等待
```

---

### 4. 画图辅助理解

**推荐的图示：**

1. **模块依赖图** - 理解模块间关系
2. **请求处理流程图** - 理解请求如何流转
3. **类关系图** - 理解核心类的继承和组合关系
4. **时序图** - 理解组件间的调用顺序

示例：请求处理时序图

```
客户端
   │
   │ 1. HTTP 请求
   ↓
RoutePredicateHandlerMapping
   │
   │ 2. 遍历 Route，使用 Predicate 匹配
   ↓
找到匹配的 Route
   │
   │ 3. 传递给 FilteringWebHandler
   ↓
FilteringWebHandler
   │
   │ 4. 组装过滤器链
   ↓
GlobalFilter 链
   │  • AdaptCachedBodyGlobalFilter
   │  • RouteToRequestUrlFilter
   │  • ReactiveLoadBalancerClientFilter
   ↓
NettyRoutingFilter
   │
   │ 5. 发送 HTTP 请求到后端
   ↓
后端服务
   │
   │ 6. 返回响应
   ↓
NettyWriteResponseFilter
   │
   │ 7. 写回响应给客户端
   ↓
客户端
```

---

## 推荐阅读顺序总结

### 第 1 天：环境准备和示例体验

| 时间 | 任务 | 文件 |
|------|------|------|
| 上午 | 构建项目，运行示例 | `spring-cloud-gateway-sample/` |
| 下午 | 阅读配置文件和配置类 | `application.yml`, `GatewayProperties.java` |

**学习目标：**
- 能够运行示例项目
- 理解基本配置格式
- 理解配置属性绑定机制

---

### 第 2 天：自动配置和核心模型

| 时间 | 任务 | 文件 |
|------|------|------|
| 上午 | 理解自动配置 | `GatewayAutoConfiguration.java` |
| 下午 | 理解路由模型 | `Route.java`, `RouteDefinition.java` |

**学习目标：**
- 理解 Spring Boot 自动配置机制
- 理解核心 Bean 的定义和作用
- 理解路由的数据模型

---

### 第 3 天：路由定位器链

| 时间 | 任务 | 文件 |
|------|------|------|
| 上午 | 路由定义定位 | `RouteDefinitionLocator.java`, `PropertiesRouteDefinitionLocator.java` |
| 下午 | 路由转换和缓存 | `RouteDefinitionRouteLocator.java`, `CachingRouteLocator.java` |

**学习目标：**
- 理解路由如何从配置加载
- 理解 RouteDefinition 如何转换为 Route
- 理解缓存机制

---

### 第 4 天：请求处理流程

| 时间 | 任务 | 文件 |
|------|------|------|
| 上午 | 路由匹配 | `RoutePredicateHandlerMapping.java` |
| 下午 | 过滤器链执行 | `FilteringWebHandler.java` |

**学习目标：**
- 理解请求如何匹配到路由
- 理解过滤器链的组装和执行
- 理解责任链模式

---

### 第 5 天：核心过滤器

| 时间 | 任务 | 文件 |
|------|------|------|
| 上午 | 实际代理请求 | `NettyRoutingFilter.java` |
| 下午 | 常用过滤器工厂 | `filter/factory/*.java` |

**学习目标：**
- 理解实际 HTTP 代理的实现
- 理解 Netty 的使用
- 理解常用过滤器的工作原理

---

### 第 6 天：谓词工厂

| 时间 | 任务 | 文件 |
|------|------|------|
| 上午 | 核心谓词工厂 | `PathRoutePredicateFactory.java`, `MethodRoutePredicateFactory.java` |
| 下午 | 其他谓词工厂 | `handler/predicate/*.java` |

**学习目标：**
- 理解谓词工厂的设计模式
- 理解各种匹配条件的实现
- 学会自定义谓词

---

### 第 7+ 天：高级特性（按需学习）

| 特性 | 文件 |
|------|------|
| 熔断器 | `GatewayResilience4JCircuitBreakerAutoConfiguration.java` |
| 限流 | `GatewayRedisAutoConfiguration.java`, `filter/ratelimit/` |
| 负载均衡 | `GatewayReactiveLoadBalancerClientAutoConfiguration.java`, `ReactiveLoadBalancerClientFilter.java` |
| 监控 | `GatewayMetricsAutoConfiguration.java`, `GatewayMetricsFilter.java` |
| 追踪 | `GatewayTracingAutoConfiguration.java` |
| 服务发现 | `discovery/` |

---

## 快速开始命令

### 构建项目

```bash
# 标准构建（需要 Maven 3.3.3+ 和 Java 17）
./mvnw clean install

# 跳过测试
./mvnw clean install -DskipTests

# 跳过 Docker 相关测试
./mvnw clean install -PwithoutDockerTests

# 使用 'spring' profile（如果不使用 Maven wrapper）
mvn clean install -P spring
```

### 运行示例

```bash
cd spring-cloud-gateway-sample

# 运行示例项目
../mvnw spring-boot:run

# 或者使用 Maven
mvn spring-boot:run
```

### 运行测试

```bash
# 运行所有测试
./mvnw test

# 运行指定测试类
./mvnw test -Dtest=GatewayAutoConfigurationTests

# 跳过 Docker 相关测试
./mvnw test -PwithoutDockerTests

# 运行集成测试
cd spring-cloud-gateway-integration-tests
../mvnw test
```

### 调试配置

在 IDE 中配置远程调试：

```bash
# 启动时添加调试参数
mvn spring-boot:run -Drun.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
```

---

## 附录

### 核心配置属性

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          # 路由定义
          routes:
            - id: route1
              uri: http://example.org
              predicates:
                - Path=/api/**
              filters:
                - AddRequestHeader=X-Request-Id, 123

          # 默认过滤器（应用于所有路由）
          defaultFilters:
            - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin

          # 指标配置
          metrics:
            enabled: true
            tags:
              key: value

          # HTTP 客户端配置
          httpclient:
            connect-timeout: 1000
            response-timeout: 5s
            pool:
              max-connections: 500
              max-idle-time: 20s
              max-life-time: 60s

          # 前缀（用于负载均衡）
          loadbalancer:
            use404: true
```

### 常用端点

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 网关路由
curl http://localhost:8080/actuator/gateway/routes

# 全局过滤器
curl http://localhost:8080/actuator/gateway/globalfilters

# 刷新路由
curl -X POST http://localhost:8080/actuator/gateway/refresh
```

### 参考资源

- [官方文档](https://spring.io/projects/spring-cloud-gateway)
- [GitHub 仓库](https://github.com/spring-cloud/spring-cloud-gateway)
- [Spring Cloud Gateway 参考手册](https://docs.spring.io/spring-cloud-gateway/reference/)
- [Project Reactor 文档](https://projectreactor.io/docs)

---

**文档版本：** 1.0
**最后更新：** 2025-02-06
**适用于：** Spring Cloud Gateway 5.0.2-SNAPSHOT