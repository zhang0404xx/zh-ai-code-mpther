# 项目文件与类说明

> 本文件说明项目目录结构、各文件职责、类与 Bean 的职责边界，以及配置项与对外端点。
> **维护约定**：新增/删除/重命名文件、新增类或 Bean、调整配置项与端点时，同步更新本文件，并在 [changelog.md](changelog.md) 记录变更。
>
> 最后更新：2026-11-08

## 一、技术栈

| 项 | 值 |
|---|---|
| JDK | 21（`D:\Java\jdk21`） |
| 框架 | Spring Boot 4.1.2 / Spring Framework 7 |
| Web | `spring-boot-starter-webmvc`（Boot 4 起不再叫 `-web`，且**内置 validation**） |
| 构建 | Maven Wrapper（Maven 3.10.0） |
| 数据库驱动 | `mysql-connector-j`（`runtime`） |
| 工具库 | Hutool 5.8.25 |
| 接口文档 | springdoc-openapi 3.2.0 + Knife4j UI 4.5.0 |
| 端口 / 上下文 | `8123` / `/api` |

## 二、目录结构

```
zh-ai-code-mpther
├── pom.xml                             # 依赖与构建配置
├── mvnw / mvnw.cmd                     # Maven Wrapper 启动脚本
├── .mvn/wrapper/maven-wrapper.properties # wrapper 版本与 Maven 发行版地址
├── docs/                               # 项目文档（本目录）
│   ├── pitfalls.md                     # 踩坑记录
│   ├── changelog.md                    # 变更记录
│   └── file-class-reference.md         # 本文件
├── src/main/java/com/zh/zhaicodempther/
│   ├── ZhAiCodeMptherApplication.java  # 启动类
│   ├── common/
│   │   ├── DeleteRequest.java             # 删除请求封装
│   │   ├── PageQuery.java               # 分页请求封装
│   │   ├── PageResult.java              # 分页结果封装
│   │   └── Result.java                 # 统一响应体
│   ├── config/
│   │   ├── CorsConfig.java              # 全局跨域配置
│   │   └── Knife4jConfig.java          # 接口文档配置
│   ├── controller/
│   │   ├── DemoController.java         # 分页/删除/异常示例接口
│   │   ├── HealthController.java       # 健康检查接口
│   │   └── UserController.java         # 用户分页示例接口
│   ├── model/
│   │   └── UserVO.java                 # 用户视图对象（示例）
│   └── exception/
│       ├── ErrorCode.java              # 错误码枚举
│       ├── BizException.java           # 业务异常
│       └── GlobalExceptionHandler.java # 全局异常处理器
├── src/main/resources/
│   └── application.yml                 # 应用配置
└── src/test/java/com/zh/zhaicodempther/
    └── ZhAiCodeMptherApplicationTests.java # 上下文加载测试
```

## 三、类与 Bean 清单

### 3.1 `ZhAiCodeMptherApplication`
- **路径**：`src/main/java/com/zh/zhaicodempther/ZhAiCodeMptherApplication.java`
- **职责**：`@SpringBootApplication` 启动类，无业务逻辑
- **注意**：所有业务包必须位于 `com.zh.zhaicodempther` 之下，否则扫描不到

### 3.2 `Knife4jConfig`（`@Configuration`）
- **路径**：`src/main/java/com/zh/zhaicodempther/config/Knife4jConfig.java`
- **职责**：提供 OpenAPI 文档元信息与分组

| Bean | 类型 | 作用 |
|---|---|---|
| `customOpenAPI()` | `OpenAPI` | 设置文档标题/描述/版本/联系人，值来自 `zh.api-doc.*`，**会替换 springdoc 默认的 OpenAPI Bean** |
| `allApi()` | `GroupedOpenApi` | 定义分组「全部接口」，扫描 `/**`；与 `springdoc.group-configs` 叠加而非覆盖 |

- **注入方式**：构造器 `@Value("${zh.api-doc.xxx:默认值}")`，字段 `final`；未配置时用默认值，保证开箱可启动
- **扩展点**：多分组按模块加 `GroupedOpenApi`；JWT 鉴权按钮在此加 `Components.addSecuritySchemes(...)`

### 3.3 `Result<T>`（record）
- **路径**：`src/main/java/com/zh/zhaicodempther/common/Result.java`
- **职责**：统一响应体，`code` / `message` / `data` 三字段
- **工厂方法**：`ok()`、`ok(T)`、`fail(ErrorCode)`、`fail(ErrorCode, String)`、`fail(int, String)`
- **注意**：`isSuccess()` 会被 Jackson 序列化成 `success` 字段（见 pitfalls P-10）

### 3.4 `PageQuery`
- **路径**：`src/main/java/com/zh/zhaicodempther/common/PageQuery.java`
- **职责**：分页请求参数封装（`pageNum` / `pageSize` / `orderBy`）
- **用法**：
  - 直接作为接口入参：`GET /api/user/page?pageNum=1&pageSize=20`
  - 作为请求体父类（`class UserQuery extends PageQuery`）或字段
- **默认值与约束**：`pageNum` 默认 1（小于 1 时纠正）、`pageSize` 默认 10（范围 1~`MAX_PAGE_SIZE`=100）、`orderBy` 字符级过滤后返回
- **便捷方法**：`offset()`（`(pageNum-1)*pageSize`，供 SQL `limit offset` 用）、`limit()`（等价 `getPageSize()`）
- **注意**：`offset()` / `limit()` 故意不命名为 `getXxx()`，避免被 Jackson 当作属性序列化；`orderBy` 仅做字符过滤，拼接 SQL 前仍需按实体字段做白名单校验

### 3.5 `PageResult<T>`（record）
- **路径**：`src/main/java/com/zh/zhaicodempther/common/PageResult.java`
- **职责**：分页结果封装，与 `PageQuery` 配套，字段 `total` / `pageNum` / `pageSize` / `list`
- **工厂方法**：
  - `of(list, total, PageQuery)`：页码与页大小取自请求参数，保证响应与请求一致（**推荐**）
  - `of(list, total, pageNum, pageSize)`：后端强制改写分页参数时使用
  - `empty(PageQuery)`：空结果，保留请求分页参数便于前端渲染分页条
- **便捷方法**：`totalPages()`（总页数，向上取整；不命名为 `getXxx`，不会被序列化）
- **防御措施**：紧凑构造器中 `null` 统一转为空集合、非 `null` 做 `List.copyOf` 快照，避免调用方后续修改集合影响响应

### 3.6 `ErrorCode`（enum）
- **路径**：`src/main/java/com/zh/zhaicodempther/exception/ErrorCode.java`
- **职责**：错误码唯一来源，字段 `code` / `message`
- **编码规则（当前实现）**：
  - `0`：成功
  - `40000`~`50300`：HTTP 错误段，按「HTTP 状态 × 100 + 序号」编码（`40000` → 400、`50000` → 500）
  - `10000+`：业务错误码，语义上统一映射为 HTTP 400
- **注意**：`GlobalExceptionHandler` 用 `HttpStatus.resolve(code)` 反查状态，5 位码返回 `null` 而全部回落 400（见 pitfalls P-16）

| 码 | 枚举 | 含义 |
|---|---|---|
| 0 | `SUCCESS` | 成功 |
| 40000 | `BAD_REQUEST` | 请求参数错误 |
| 40100 | `UNAUTHORIZED` | 未认证或登录已过期 |
| 40300 | `FORBIDDEN` | 没有操作权限 |
| 40400 | `NOT_FOUND` | 请求的资源不存在 |
| 40500 | `METHOD_NOT_ALLOWED` | 请求方法不支持 |
| 41500 | `UNSUPPORTED_MEDIA_TYPE` | 不支持的媒体类型 |
| 50000 | `INTERNAL_SERVER_ERROR` | 系统内部异常 |
| 50300 | `SERVICE_UNAVAILABLE` | 服务暂不可用 |
| 10000 | `BUSINESS_ERROR` | 业务处理失败 |
| 10001 | `DATA_NOT_FOUND` | 数据不存在 |
| 10002 | `DATA_DUPLICATE` | 数据已存在 |
| 10003 | `OPERATION_NOT_ALLOWED` | 当前状态不允许该操作 |

- **扩展方式**：按业务域新增段位（如 20001~29999 表示订单域），并在本表登记

### 3.7 `BizException`（`extends RuntimeException`）
- **路径**：`src/main/java/com/zh/zhaicodempther/exception/BizException.java`
- **职责**：业务代码主动抛出的异常，携带 `ErrorCode`
- **构造方式**：`(ErrorCode)`、`(ErrorCode, String)`（自定义文案覆盖默认）、`(ErrorCode, String, Throwable)`
- **约定**：业务分支不要 `return Result.fail(...)`，统一 `throw new BizException(...)`，保证日志与状态码集中处理

### 3.8 `GlobalExceptionHandler`（`@RestControllerAdvice`）
- **路径**：`src/main/java/com/zh/zhaicodempther/exception/GlobalExceptionHandler.java`
- **职责**：把异常统一转换为 `Result`；HTTP 状态由 `HttpStatus.resolve(code)` 推导，因当前为 5 位码，**所有分支实际返回 HTTP 400**（见 pitfalls P-16）

| 异常 | 返回码 | 日志 |
|---|---|---|
| `BizException` | 其 `ErrorCode` | `warn`（不打堆栈） |
| `MethodArgumentNotValidException`（`@RequestBody` + `@Valid`） | 40000 + `字段: 提示` | 无 |
| `BindException`（表单/Query 对象绑定） | 40000 | 无 |
| `ConstraintViolationException`（`@RequestParam`/`@PathVariable` 校验） | 40000 | 无 |
| `MissingServletRequestParameterException` / `MethodArgumentTypeMismatchException` / `HttpMessageNotReadableException` | 40000 | `warn` |
| `HttpRequestMethodNotSupportedException` | 40500 | `warn` |
| `HttpMediaTypeNotSupportedException` | 41500 | `warn` |
| `NoResourceFoundException` | 40400 | `warn` |
| `Exception`（兜底） | 50000，堆栈只进日志 | `error` |

- **注意**：表内为业务码；受 P-16 影响，当前所有分支的 HTTP 状态均为 400
- **注意**：兜底分支对外只返回通用文案，不泄露堆栈与内部实现细节

### 3.9 `HealthController`（`@RestController`）
- **路径**：`src/main/java/com/zh/zhaicodempther/controller/HealthController.java`
- **职责**：健康检查，验证服务是否存活
- **接口**：`GET /api/health/check` → 返回纯文本 `ok`
- **文档表现**：在 OpenAPI 中归类为 tag `health-controller`，operationId `check`，响应 `*/*` → `string`
- **待改进**：处理方法是 `private` 的。Spring 通过反射 `setAccessible` 仍能映射并调用（已实测 200），但不符合 MVC 规范，且与 AOP 代理、AOT/原生镜像等场景存在失效风险，建议改为 `public`
- **扩展建议**：返回 `Result.ok("ok")` 以统一响应格式；如需接入监控探针，可再加 `GET /api/health/live`、`/api/health/ready`

### 3.10 `UserController`（`@RestController`）
- **路径**：`src/main/java/com/zh/zhaicodempther/controller/UserController.java`
- **职责**：示例接口，演示 `PageQuery` + `PageResult` + `Result` 完整链路，数据来自内存常量 `MOCK_USERS`（30 条）
- **接口**：`GET /api/user/page`，参数 `pageNum` / `pageSize` / `orderBy`（来自 `PageQuery`，用 `@ParameterObject` 展开）+ `keyword`（可选，用户名模糊匹配）
- **实现要点**：
  - 分页通过 `subList(from, to)` 切分，`from`/`to` 均以 `total` 封顶，超出末页返回空集合而非报错
  - 页码/页大小纠正由 `PageQuery` 负责，控制器不重复判断
- **Swagger 注解**：`@Tag(name = "用户管理")`、`@Operation(summary/description)`、字段级 `@Schema`
- **替换为真实实现时**：下沉到 Service，把 `subList` 换成 SQL `LIMIT/OFFSET`，`orderBy` 需按实体字段白名单校验

### 3.11 `UserVO`（record）
- **路径**：`src/main/java/com/zh/zhaicodempther/model/UserVO.java`
- **职责**：用户视图对象，字段 `id` / `name` / `email` / `status` / `createdAt`，仅用于响应，不承担请求校验
- **注意**：时间字段为 `LocalDateTime`，默认序列化为 ISO-8601（`2026-01-07T00:00:00`），如需 `yyyy-MM-dd HH:mm:ss` 需在 yml 配置 `spring.jackson.date-format` 或加 `@JsonFormat`

### 3.12 `ZhAiCodeMptherApplicationTests`（`@SpringBootTest`）
- **路径**：`src/test/java/com/zh/zhaicodempther/ZhAiCodeMptherApplicationTests.java`
- **职责**：`contextLoads()` 验证 Spring 上下文可加载（新增 Bean 后应保持该测试通过）

### 3.13 `DeleteRequest`（record）
- **路径**：`src/main/java/com/zh/zhaicodempther/common/DeleteRequest.java`
- **职责**：删除请求统一封装，用 `ids` 列表同时覆盖单条与批量删除
- **字段**：`ids`（`List<Long>`，必填）、`reason`（删除原因，可选，≤200 字）
- **校验（Bean Validation）**：`@NotEmpty` 列表非空、`@NotNull` 元素非 null、`@Positive` 元素为正整数、`@Size(max=100)` 单次上限；失败由 `GlobalExceptionHandler` 转为 400 + `ids[1]: ID 必须为正整数`
- **紧凑构造器**：`ids.stream().distinct().toList()` 去重并转为不可变列表；`ids == null` 保持 null 交给 `@NotEmpty` 判定（**不能用 `List.copyOf`**，遇到 null 元素会在校验前抛 NPE，变成 500）
- **方法**：`of(Long...)`（后端内部构造）、`isBatch()`（去重后 >1 条）、`firstId()`（单条 ID，空列表返回 null）
- **用法**：`POST/DELETE` 接口用 `@RequestBody @Valid DeleteRequest` 接参；批量上限常量 `MAX_BATCH_SIZE = 100`
- **注意**：配 `@DeleteMapping` + body 时存在客户端兼容风险，见 pitfalls P-17

### 3.14 `DemoController`（`@RestController`）
- **路径**：`src/main/java/com/zh/zhaicodempther/controller/DemoController.java`
- **职责**：异常捕获与分页请求的参考实现，三段代码可直接复制到业务模块；数据来自 `MOCK_DATA`（12 条内存数据）
- **接口**：

| 接口 | 用途 | 实测结果 |
|---|---|---|
| `GET /api/demo/page` | 分页三段式（`PageQuery` → `offset()/limit()` → `PageResult.of`） | 200，`total:12` |
| `DELETE /api/demo/delete` | `DeleteRequest` + `@Valid`，演示删除与校验失败 | 200 / 400 |
| `GET /api/demo/exception/biz?id=999` | 直接 `throw new BizException(DATA_NOT_FOUND)` | 400 + `10001` |
| `GET /api/demo/exception/wrap` | `catch` 底层异常后转 `BizException`，保留 cause | 400 + `50300` |
| `GET /api/demo/exception/unexpected?index=99` | 不 `catch`，交给兜底 handler | 400 + `50000`，堆栈只进日志 |

- **异常处理约定**：可预期业务错误直接抛 `BizException`；第三方异常 `catch` 后必须把原异常作为 cause 传入；未预期异常一律不 `catch`
- **Swagger 表现**：`@Tag(name = "Demo 示例")`；`/demo/page` 参数展开为 `pageNum` / `pageSize` / `orderBy`；`/demo/delete` 请求体指向 `#/components/schemas/DeleteRequest`

### 3.15 `CorsConfig`（`@Configuration`）
- **路径**：`src/main/java/com/zh/zhaicodempther/config/CorsConfig.java`
- **职责**：全局跨域，注册一个 `FilterRegistrationBean<CorsFilter>`（`order = Ordered.HIGHEST_PRECEDENCE`，`urlPattern = /**`）
- **为何不用 `WebMvcConfigurer#addCorsMappings`**：CORS 处理在 `DispatcherServlet` 内部，404 / 全局异常等未匹配 handler 的响应拿不到 CORS 头，浏览器会把 404 显示成跨域错误（见 pitfalls P-18）
- **为何用 `setAllowedOriginPatterns`**：`setAllowedOrigins("*")` 与 `allowCredentials(true)` 同时打开会被 Spring 直接拒绝；`originPatterns` 支持 `*` / `https://*.example.com` 且能与凭证共存
- **配置项**：`zh.cors.allowed-origin-patterns`（默认 `*`）、`allowed-methods`（默认 7 个方法）、`allowed-headers`（默认 `*`，回显请求头）、`exposed-headers`（默认空）、`allow-credentials`（默认 false）、`max-age`（默认 3600 秒）
- **注意**：`UrlBasedCorsConfigurationSource` 匹配 servletPath，**不含** `context-path=/api`，所以注册 `/**`；将来引入 Spring Security 时需确保 CORS 在其之前（或 `http.cors()` 复用同一 `CorsConfigurationSource`）

## 四、application.yml 配置项

| 配置项 | 值 | 作用 |
|---|---|---|
| `spring.application.name` | `zh-ai-code-mpther` | 应用名 |
| `spring.jackson.date-format` | `yyyy-MM-dd HH:mm:ss` | 日期序列化格式 |
| `spring.jackson.time-zone` | `Asia/Shanghai` | 日期序列化时区 |
| `server.port` | `8123` | 服务端口 |
| `server.servlet.context-path` | `/api` | **所有路径前缀，文档地址也因此带 `/api`** |
| `springdoc.group-configs[0]` | `group=default`、`paths-to-match=/**`、`packages-to-scan=...controller` | yml 中声明的分组（与 `Knife4jConfig.allApi()` 的「全部接口」分组共存，文档里会看到两个分组） |
| `zh.api-doc.title` | `zh-ai-code-mpther 接口文档` | 文档标题 |
| `zh.api-doc.description` | `基于 Spring Boot + Knife4j 的在线接口文档` | 文档描述 |
| `zh.api-doc.version` | `v1.0.0` | 文档版本 |
| `zh.api-doc.contact-name` | `zh` | 联系人 |
| `zh.cors.allowed-origin-patterns` | `*` | 允许的来源（支持通配；生产环境应收窄） |
| `zh.cors.allowed-methods` | `GET,POST,PUT,DELETE,PATCH,OPTIONS,HEAD` | 允许的方法 |
| `zh.cors.allowed-headers` | `*` | 允许的请求头（`*` = 回显预检声明的头） |
| `zh.cors.exposed-headers` | 空 | 前端可读的自定义响应头 |
| `zh.cors.allow-credentials` | `false` | 是否允许携带 Cookie |
| `zh.cors.max-age` | `3600` | 预检结果缓存（秒） |

**不存在的配置**：`knife4j.*`（当前为纯 UI 模式，写了也不生效，见 pitfalls P-02）

## 五、对外端点

| 用途 | 地址 |
|---|---|
| Knife4j 界面 | `http://localhost:8123/api/doc.html` |
| 原生 Swagger UI | `http://localhost:8123/api/swagger-ui/index.html` |
| OpenAPI 元数据 | `http://localhost:8123/api/v3/api-docs` |
| 分组配置 | `http://localhost:8123/api/v3/api-docs/swagger-config` |
| 分组元数据（中文分组） | `http://localhost:8123/api/v3/api-docs/全部接口` |
| 健康检查 | `http://localhost:8123/api/health/check`（返回 `ok`） |
| 用户分页示例 | `http://localhost:8123/api/user/page?pageNum=1&pageSize=5&keyword=用户1` |
| Demo 分页示例 | `http://localhost:8123/api/demo/page?pageNum=1&pageSize=5` |
| Demo 删除示例 | `DELETE http://localhost:8123/api/demo/delete`，body `{"ids":[1,2,3]}` |
| Demo 异常示例 | `http://localhost:8123/api/demo/exception/biz?id=999`、`/wrap`、`/unexpected?index=99` |

## 六、依赖清单（pom.xml）

| 依赖 | 版本 | scope | 说明 |
|---|---|---|---|
| `spring-boot-starter-webmvc` | 4.1.2（parent 管理） | compile | Web MVC + 内置 validation |
| `mysql-connector-j` | 9.7.0（parent 管理） | runtime | MySQL 驱动 |
| `lombok` | 1.18.36 | compile（optional） | 注解处理，`@Slf4j` 等 |
| `spring-boot-starter-webmvc-test` | 4.1.2 | test | 测试 |
| `hutool-all` | 5.8.25 | compile | 工具集（体积较大，可按需拆 `hutool-core` 等） |
| `springdoc-openapi-starter-webmvc-ui` | 3.2.0 | compile | OpenAPI 元数据 + 原生 UI |
| `knife4j-openapi3-ui` | 4.5.0 | compile | Knife4j 前端静态资源 |

## 七、构建环境约定（本机）

| 项 | 值 |
|---|---|
| Maven 用户级配置 | `C:\Users\Administrator\.m2\settings.xml`（阿里云镜像 + D 盘仓库） |
| 本地仓库 | `D:\java\Maven\repository` |
| IDEA Maven home | `D:\java\maven\apache-maven-3.9.12` |
| 命令行构建前置 | `$env:JAVA_HOME='D:\Java\jdk21'`（系统默认是 JDK 11） |
| 归档仓库 | `C:\Users\Administrator\.m2\repo.bak`（原 C 盘仓库，观察期后清理） |

常用命令：
```powershell
$env:JAVA_HOME='D:\Java\jdk21'
.\mvnw.cmd -B -DskipTests compile        # 编译
.\mvnw.cmd -B test                       # 测试
.\mvnw.cmd -B spring-boot:run            # 启动（会触发 test-compile）
```

## 八、待补充

- `HealthController.check()` 改为 `public` 并返回 `Result.ok("ok")`
- 确定错误码与 HTTP 状态的映射方案（当前全部 400，见 pitfalls P-16）
- 持久层（MyBatis / JPA）与数据源配置
- 鉴权方案（若引入 JWT，需在 `Knife4jConfig` 增补 `SecurityScheme`）
