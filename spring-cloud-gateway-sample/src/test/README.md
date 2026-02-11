# 测试文档

本目录包含 Spring Cloud Gateway 示例应用的测试用例。

## 测试结构

```
src/test/java/org/springframework/cloud/gateway/sample/
├── BaseGatewayTest.java                    # 基础测试配置类
├── TestConstants.java                      # 测试常量定义
├── GatewaySampleApplicationTests.java     # 应用集成测试
├── actuator/
│   └── GatewayActuatorTests.java          # Actuator 端点测试
├── config/
│   └── RouteConfigurationTests.java       # 路由配置测试
└── filter/
    ├── ThrottleGatewayFilterTests.java   # 限流过滤器单元测试
    └── TimingGatewayFilterTests.java     # 计时过滤器单元测试
```

## 测试类型

### 1. 单元测试

直接测试类的功能，使用 Mockito 模拟依赖：

- `ThrottleGatewayFilterTests` - 测试限流过滤器
  - 令牌桶初始化
  - 令牌消费和补充
  - 边界情况（容量为0等）

- `TimingGatewayFilterTests` - 测试计时过滤器
  - 请求计时功能
  - 日志记录
  - 不同 HTTP 方法的处理

### 2. 集成测试

在完整的 Spring Context 中测试多个组件的交互：

- `GatewaySampleApplicationTests` - 应用主集成测试
  - 上下文加载
  - API 代理路由
  - 全局过滤器
  - 响应头验证

- `GatewayActuatorTests` - Actuator 端点测试
  - 健康检查
  - 指标端点
  - 端点列表

- `RouteConfigurationTests` - 路由配置测试
  - 路由定位器配置
  - 路由定义验证

## 运行测试

### 运行所有测试

```bash
cd spring-cloud-gateway-sample
../mvnw test
```

### 运行特定测试类

```bash
../mvnw test -Dtest=GatewaySampleApplicationTests
```

### 运行特定测试方法

```bash
../mvnw test -Dtest=ThrottleGatewayFilterTests#filterSupportsChainedConfiguration
```

### 跳过测试

```bash
../mvnw install -DskipTests
```

## 测试配置

### Test Profile

测试使用 `test` profile，配置文件位于：
- `src/test/resources/application-test.yml`
- `src/test/resources/logback-test.xml`

### 测试配置要点

1. **禁用 JMX** - 避免与 Netty 冲突
2. **降低日志级别** - 保持测试输出简洁
3. **暴露健康检查端点** - 方便验证
4. **使用测试服务** - JSONPlaceholder 作为后端

## 测试覆盖

### 已覆盖功能

- ✅ 应用启动和上下文加载
- ✅ API 代理路由
- ✅ 路径重写
- ✅ 全局响应头
- ✅ 限流过滤器（令牌桶算法）
- ✅ 计时过滤器
- ✅ 健康检查端点
- ✅ Actuator 端点

### 待添加测试

- ⏳ 请求体修改过滤器
- ⏳ 响应体修改过滤器
- ⏳ WebSocket 路由
- ⏳ 熔断器集成
- ⏳ 服务发现集成

## 编写新测试

### 单元测试模板

```java
@SpringBootTest
class YourFeatureTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testSomething() {
        // Arrange
        String url = "/api/test";

        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

### 使用测试常量

```java
import static org.springframework.cloud.gateway.sample.TestConstants.*;

@Test
void testWithConstants() {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getFirst(Headers.X_GATEWAY))
        .isEqualTo(Headers.X_GATEWAY_VALUE);
}
```

## 最佳实践

1. **使用描述性的测试方法名**
   - ✅ `requestAllowedWhenTokensAvailable`
   - ❌ `test1`

2. **遵循 AAA 模式**
   - Arrange（准备）- 设置测试数据
   - Act（执行）- 调用被测方法
   - Assert（断言）- 验证结果

3. **使用 @BeforeEach 而不是字段初始化**
   - 避免测试间的状态共享

4. **使用 AssertJ 断言**
   - 更易读的断言语法
   - 更好的错误消息

5. **适当使用 @Disabled**
   - 标记暂时跳过的测试
   - 记录跳过原因

## 故障排除

### 测试失败：端口被占用

```
Port 8080 was already in use
```

**解决方案**：测试使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 自动分配随机端口。

### 测试失败：连接超时

```
ConnectTimeoutException
```

**原因**：依赖外部服务（JSONPlaceholder）

**解决方案**：
1. 检查网络连接
2. 使用 MockRestServiceServer 模拟外部服务

### 测试失败：日志过多

**解决方案**：
- `logback-test.xml` 中降低日志级别
- 使用 `@ActiveProfiles("test")` 激活测试配置

## 贡献指南

添加新测试时：

1. 遵循现有命名约定
2. 添加 Javadoc 注释
3. 更新本 README
4. 确保所有测试通过

---

**作者注**: 测试套件持续完善中，欢迎贡献更多测试用例。
