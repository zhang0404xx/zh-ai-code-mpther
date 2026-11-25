# 项目文件与类说明

> 本文件说明项目目录结构、各文件职责、类与 Bean 的职责边界，以及配置项与对外端点。
> **维护约定**：新增/删除/重命名文件、新增类或 Bean、调整配置项与端点时，同步更新本文件，并在 [changelog.md](changelog.md) 记录变更；若有功能 / 链路新增，同步更新 [project-guide.md](project-guide.md)（无新增则不改）。
>
> 最后更新：2026-11-25

## 一、技术栈

| 项        | 值                                                                                     |
|----------|---------------------------------------------------------------------------------------|
| JDK      | 21（`D:\Java\jdk21`）                                                                   |
| 框架       | Spring Boot 4.1.2 / Spring Framework 7                                                |
| Web      | `spring-boot-starter-webmvc`（Boot 4 起不再叫 `-web`，且**内置 validation**）                   |
| 构建       | Maven Wrapper（Maven 3.10.0）                                                           |
| 数据库驱动    | `mysql-connector-j`（`runtime`）                                                        |
| 持久层      | MyBatis-Flex 1.11.8（`mybatis-flex-spring-boot4-starter`），代码生成用 `mybatis-flex-codegen` |
| 连接池      | HikariCP（版本由 Boot 管理）                                                                 |
| 工具库      | Hutool 5.8.25                                                                         |
| 接口文档     | springdoc-openapi 3.2.0 + Knife4j UI 4.5.0                                            |
| AI 框架    | LangChain4j 1.23.0（Spring Boot 4 starter `1.23.0-beta33`，OpenAI 兼容协议 + `@AiService`）     |
| 端口 / 上下文 | `8123` / `/api`                                                                       |

## 二、目录结构

```
zh-ai-code-mpther
├── pom.xml                             # 依赖与构建配置
├── mvnw / mvnw.cmd                     # Maven Wrapper 启动脚本
├── .mvn/wrapper/maven-wrapper.properties # wrapper 版本与 Maven 发行版地址
├── docs/                               # 项目文档（本目录）
│   ├── pitfalls.md                     # 踩坑记录
│   ├── changelog.md                    # 变更记录
│   ├── file-class-reference.md         # 本文件
│   └── project-guide.md                # 项目讲解（功能串成完整链路）
├── sql/
│   └── create_table.sql                # 建表脚本（user / app / chat_history）
├── src/main/java/com/zh/zhaicodempther/
│   ├── ZhAiCodeMptherApplication.java  # 启动类
│   ├── ai/
│   │   ├── AiCodeGeneratorService.java        # AI 代码生成服务接口（LangChain4j AI Service）
│   │   ├── AiCodeGeneratorServiceFactory.java # AI 服务工厂（@Configuration，注册代理 Bean）
│   │   └── model/                             # 结构化输出结果（HtmlCodeResult / MultiFileCodeResult）
│   ├── core/
│   │   ├── AiCodeGeneratorFacade.java  # 代码生成门面（按类型生成 + 落盘，同步 / 流式两个入口）
│   │   ├── handler/                    # 按类型的处理器（同类型的解析 + 保存合在一个类）
│   │   │   └── HtmlCodeHandler.java / MultiFileCodeHandler.java
│   │   ├── parser/                     # 解析器接口 + 解析执行器
│   │   │   ├── CodeParser.java
│   │   │   └── CodeParserExecutor.java         # 按类型选择解析器
│   │   └── saver/                      # 文件保存模板 + 保存执行器
│   │       ├── CodeFileSaverTemplate.java      # 保存模板（校验 → 建唯一目录 → 写文件）
│   │       └── CodeFileSaverExecutor.java      # 按类型选择保存器（tmp/code_output/{bizType}_{雪花ID}）
│   ├── annotation/
│   │   └── AuthCheck.java              # 权限校验注解
│   ├── aop/
│   │   ├── AuthCheckAspect.java        # 权限校验切面（@Order(2)）
│   │   └── ControllerLogAspect.java    # Controller 日志切面（参数/返回/耗时，@Order(1)）
│   ├── common/
│   │   ├── DeleteRequest.java             # 删除请求封装
│   │   ├── PageQuery.java               # 分页请求封装
│   │   ├── PageResult.java              # 分页结果封装
│   │   └── Result.java                 # 统一响应体
│   ├── config/
│   │   ├── CorsConfig.java              # 全局跨域配置
│   │   ├── JsonConfig.java              # 全局 JSON 配置（Long 序列化为字符串）
│   │   └── Knife4jConfig.java          # 接口文档配置
│   ├── controller/
│   │   ├── AppController.java          # 应用业务接口（用户 6 个 + 管理员 4 个，均返回 Result）
│   │   ├── ChatHistoryController.java  # 会话历史 CRUD 接口（生成）
│   │   ├── DemoController.java         # 分页/删除/异常示例接口
│   │   ├── HealthController.java       # 健康检查接口
│   │   ├── UserController.java         # 用户 CRUD 接口（生成，路径 /user）
│   │   └── UsersController.java        # 用户分页示例接口（路径 /users）
│   ├── mapper/
│   │   ├── AppMapper.java              # 应用 Mapper
│   │   ├── ChatHistoryMapper.java      # 会话历史 Mapper
│   │   └── UserMapper.java             # 用户 Mapper
│   ├── service/
│   │   ├── AppService.java             # 应用服务接口
│   │   ├── ChatHistoryService.java     # 会话历史服务接口
│   │   ├── UserService.java            # 用户服务接口
│   │   └── impl/                       # 三个 ServiceImpl（继承 ServiceImpl<Mapper, Entity>）
│   ├── model/
│   │   ├── UserVO.java                 # 用户视图对象（示例）
│   │   ├── dto/                        # 请求对象（含 AppAddRequest / AppUpdateRequest / AppAdminUpdateRequest / AppQueryRequest）
│   │   ├── vo/                         # 视图对象（含 AppVo）
│   │   └── entity/                     # 实体（App / ChatHistory / User）
│   ├── exception/
│   │   ├── ErrorCode.java              # 错误码枚举
│   │   ├── BizException.java           # 业务异常
│   │   └── GlobalExceptionHandler.java # 全局异常处理器
│   └── generator/
│       └── MyBatisFlexGenerator.java   # MyBatis-Flex 代码生成器（main 直接运行）
├── src/main/resources/
│   ├── application.yml                 # 应用配置
│   └── prompt/                         # AI 系统提示词（html / multi-file 两份）
└── src/test/java/com/zh/zhaicodempther/
    ├── ZhAiCodeMptherApplicationTests.java # 上下文加载测试
    ├── core/AiCodeGeneratorFacadeTest.java # 门面端到端测试（同步 / 流式生成 + 落盘，真实调用 DeepSeek）
    ├── core/AiCodeGeneratorFacadeSaveTest.java   # 门面同步单元测试（Mock AI 服务，校验保存执行器落盘 / 空结果 / 空类型）
    ├── core/AiCodeGeneratorFacadeStreamTest.java # 门面流式单元测试（Mock AI 服务，校验透传 / 解析落盘 / 异常）
    └── ai/AiCodeGeneratorServiceTest.java  # AI 生成测试（真实调用 DeepSeek）
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
  - 直接作为接口入参：`GET /api/users/page?pageNum=1&pageSize=20`
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

| 码     | 枚举                       | 含义         |
|-------|--------------------------|------------|
| 0     | `SUCCESS`                | 成功         |
| 40000 | `BAD_REQUEST`            | 请求参数错误     |
| 40100 | `UNAUTHORIZED`           | 未认证或登录已过期  |
| 40300 | `FORBIDDEN`              | 没有操作权限     |
| 40400 | `NOT_FOUND`              | 请求的资源不存在   |
| 40500 | `METHOD_NOT_ALLOWED`     | 请求方法不支持    |
| 41500 | `UNSUPPORTED_MEDIA_TYPE` | 不支持的媒体类型   |
| 50000 | `INTERNAL_SERVER_ERROR`  | 系统内部异常     |
| 50300 | `SERVICE_UNAVAILABLE`    | 服务暂不可用     |
| 10000 | `BUSINESS_ERROR`         | 业务处理失败     |
| 10001 | `DATA_NOT_FOUND`         | 数据不存在      |
| 10002 | `DATA_DUPLICATE`         | 数据已存在      |
| 10003 | `OPERATION_NOT_ALLOWED`  | 当前状态不允许该操作 |
| 11001 | `USER_NAME_DUPLICATE`    | 用户名已存在     |
| 11002 | `USER_NAME_NOT_FOUND`    | 用户名不存在     |
| 11003 | `PASSWORD_ERROR`         | 密码错误       |

- **扩展方式**：按业务域新增段位（`10xxx` 通用业务、`11xxx` 用户域；如 20001~29999 表示订单域），并在本表登记
- **注意**：新增常量须写在最后一个常量的 `;` 之前（前一项结尾改为 `,`），且 `code` 不得与已有值重复

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

### 3.10 `UserController`（生成的 CRUD 控制器，`@RestController`）
- **路径**：`src/main/java/com/zh/zhaicodempther/controller/UserController.java`
- **职责**：MyBatis-Flex 生成器产出的用户 CRUD 接口，直接调用 `UserService`
- **接口**：`POST /api/user/save`、`DELETE /api/user/remove/{id}`、`PUT /api/user/update`、`GET /api/user/list`、
  `GET /api/user/getInfo/{id}`、`GET /api/user/page`（参数为 `Page<User>`）
- **登录相关接口**（返回 `Result`）：`POST /api/user/register`、`POST /api/user/login`（成功后写入 Session
  `USER_LOGIN_STATE`）、`GET /api/user/get/login`（读取 Session 登录态 → 按 id 查库 → 返回脱敏 `LonginUserVo`；未登录返回
  `UNAUTHORIZED(40100)`）、`POST /api/user/logout`（移除 Session 登录态，返回 `Result<Boolean>`；未登录返回
  `OPERATION_NOT_ALLOWED(10003)`）
- **Service 链路**：`UserService#getLoginUser(HttpServletRequest)` 返回 `User` 实体，供其他业务复用；Controller 层再调用
  `loginUserVo` 脱敏
- **权限**：CRUD 接口（`save` / `remove` / `update` / `list` / `getInfo` / `page`）均为 `@AuthCheck(mustRole = ADMIN)`；
  `register` / `login` / `get/login` / `logout` 无注解
- **脱敏查询接口**（`@AuthCheck`，user / admin 均可）：`GET /api/user/get/vo/{id}` → `Result<UserVo>`；
  `GET /api/user/list/page/vo?pageNum=&pageSize=` → `Result<PageResult<UserVo>>`；经 `UserService#getUserVo` /
  `getUserVoList` 转换，不返回密码与逻辑删除字段；分页未使用 `orderBy`
- **注意**：返回实体或 `Page`，未套项目 `Result`；`/user/page` 为 MyBatis-Flex 分页对象语义，与示例接口 `/users/page` 的
  `PageQuery` 语义不同，勿混淆

### 3.10.1 `UsersController`（分页示例控制器，`@RestController`）

- **路径**：`src/main/java/com/zh/zhaicodempther/controller/UsersController.java`
- **职责**：示例接口，演示 `PageQuery` + `PageResult` + `Result` 完整链路，数据来自内存常量 `MOCK_USERS`（30 条）
- **接口**：`GET /api/users/page`，参数 `pageNum` / `pageSize` / `orderBy`（来自 `PageQuery`，用 `@ParameterObject` 展开）+
  `keyword`（可选，用户名模糊匹配）
- **为何是 `/users`**：生成的 `UserController` 已占用 `/user`，两者最初都映射 `/user/page` 导致 `Ambiguous mapping`
  启动失败；示例改到复数路径 `/users` 后冲突消除
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

### 3.16 `MyBatisFlexGenerator`（代码生成器，官方模板写法）

- **路径**：`src/main/java/com/zh/zhaicodempther/generator/MyBatisFlexGenerator.java`
- **职责**：`HikariDataSource` + `GlobalConfig` + `Generator` 三步生成代码，不注册 Spring Bean、不参与应用启动
- **运行方式**：IDEA 里直接运行 `main`；源码按 `src/main/java` 相对路径输出，工作目录需为项目根
- **数据源**：Hutool `YamlUtil.loadByPath("application.yml")` 读取 classpath 的 `spring.datasource.url/username/password`
  ，不硬编码账号口令；yml 改动后需重新构建（读的是 `target/classes` 副本，见 pitfalls P-20）
- **生成范围**：`setGenerateTable("user", "app", "chat_history")`；去掉该行则生成全库表
- **输出包结构**：`basePackage` 为 `com.zh.zhaicodempther.genresult`，其下使用官方默认子包 `entity` / `mapper` /
  `service` / `service.impl` / `controller`（与业务包隔离，不产生重名）
- **模板风格**：Lombok、JDK 21；`setLogicDeleteColumn("isDelete")` 使 `isDelete` 列生成 `@Column(isLogicDelete = true)`
  （列名大小写敏感，见 pitfalls P-19）
- **覆盖策略**：官方默认 `overwriteEnable=false`，同名文件自动跳过；`genresult` 为独立包，不会影响现有
  `controller/UserController.java`
- **注意**：模板生成的 Controller 直接返回实体，不套项目 `Result`；实体也未加 `@Schema`，新表生成后需按项目约定改造

### 3.17 `ControllerLogAspect`（`@Aspect` + `@Component`）

- **路径**：`src/main/java/com/zh/zhaicodempther/aop/ControllerLogAspect.java`
- **职责**：`@Around` 拦截 `com.zh.zhaicodempther.controller..*` 的所有方法，记录请求地址、参数、返回值与耗时
- **日志格式**：
  - 成功（`info`）：`[GET /api/demo/page] DemoController#page | 参数: {...} | 返回: {...} | 耗时: 1 ms`
  - 异常（`warn`）：`... | 异常: BizException: 用户不存在 | 耗时: 261 ms`，随后原样抛出，由 `GlobalExceptionHandler` 生成响应
- **参数处理**：按参数名组装为 JSON；跳过 `ServletRequest/Response`、`HttpSession`、`BindingResult`、`InputStream/OutputStream`；
  `MultipartFile` 只记录文件名与大小
- **安全与体积**：JSON 中 key 含 `password`（忽略大小写）的值替换为 `******`；单段超过 `MAX_LOG_LENGTH`=2000 字符截断
- **序列化**：构造器注入 Jackson 3 `tools.jackson.databind.ObjectMapper`，与接口响应一致；失败时退化为 `toString`（不用 Hutool，见
  pitfalls P-23）
- **注意**：依赖 `spring-boot-starter-aspectj`；`private` 方法不会被拦截（当前 `HealthController#check` 无日志）；返回大列表的接口会
  增加序列化开销，必要时可调低日志级别或缩小切入点
- **顺序**：`@Order(1)`，包在 `AuthCheckAspect` 外层，鉴权失败也会输出 `warn` 日志

### 3.18 `AuthCheck`（注解）与 `AuthCheckAspect`（`@Aspect` + `@Component` + `@Order(2)`）

- **路径**：`annotation/AuthCheck.java`、`aop/AuthCheckAspect.java`
- **用法**：`@AuthCheck` 登录即可；`@AuthCheck(mustRole = UserRoleEnum.ADMIN)` 仅管理员；可标注在方法或类上，方法优先
- **校验流程**：`UserService#getLoginUser` 取登录用户（未登录 → `40100`）→ `UserRoleEnum.getEnumByValue(userRole)` →
  `hasPermission(mustRole)`，不满足或角色未知 → `40300`
- **角色等级**：`UserRoleEnum.level`，`USER=10`、`ADMIN=100`，高等级自动拥有低等级权限；新增角色只需加枚举常量并选定等级
  （如 `VIP("会员", "vip", 50)`），切面无需修改
- **注意**：角色以数据库最新值为准（每次鉴权查一次库），改角色后无需重新登录；`private` 方法上的注解不生效；若将来出现
  非线性权限（如互斥角色），需把 `mustRole` 扩展为数组或改为权限点模型

### 3.19 `AiCodeGeneratorService`（接口）与 `AiCodeGeneratorServiceFactory`（`@Configuration`）

- **路径**：`ai/AiCodeGeneratorService.java`、`ai/AiCodeGeneratorServiceFactory.java`
- **接口方法**：系统提示词均经 `@SystemMessage(fromResource = "prompt/xxx.txt")` 从 classpath 读取

| 方法 | 返回类型 | 调用模型 | 说明 |
|---|---|---|---|
| `generateHtmlCode(String)` | `HtmlCodeResult` | `ChatModel` | 同步，结构化输出（见 3.20） |
| `generateMultiFileCode(String)` | `MultiFileCodeResult` | `ChatModel` | 同步，结构化输出 |
| `generateHtmlCodeStream(String)` | `Flux<String>` | `StreamingChatModel` | 流式，返回模型原始文本片段（Markdown 代码块），不解析 |
| `generateMultiFileCodeStream(String)` | `Flux<String>` | `StreamingChatModel` | 流式，同上 |

- **工厂**：注入 starter 自动装配的 `ChatModel`（`chat-model.*`）与 `StreamingChatModel`（`streaming-chat-model.*`），
  `@Bean aiCodeGeneratorService()` 用 `AiServices.builder(...).chatModel(chatModel).streamingChatModel(streamingChatModel).build()`
  生成接口代理，按方法返回类型选用模型；业务中直接 `@Resource AiCodeGeneratorService` 使用
- **流式前提**：返回 `Flux<String>` 需 `langchain4j-reactor` 依赖；流式方法暂无调用方（仅测试类），结果未落盘
- **注意**：`@SystemMessage` 必须用 `fromResource` 指定文件路径，写成 `value` 会把路径字符串当提示词原文（见 pitfalls P-24）；
  接口未标 `@AiService`，避免与工厂重复注册；未配置 `api-key` 时无 `ChatModel` Bean，工厂会导致启动失败

### 3.20 `HtmlCodeResult` / `MultiFileCodeResult`（结构化输出结果）

- **路径**：`ai/model/HtmlCodeResult.java`、`ai/model/MultiFileCodeResult.java`
- **职责**：作为 `AiCodeGeneratorService` 的返回类型，承载模型按 JSON 返回的代码

| 类 | 字段 | 对应方法 |
|---|---|---|
| `HtmlCodeResult` | `htmlCode`、`description` | `generateHtmlCode` |
| `MultiFileCodeResult` | `htmlCode`、`cssCode`、`jsCode`、`description` | `generateMultiFileCode` |

- **写法**：Lombok `@Data` 普通类；类与字段标注 `dev.langchain4j.model.output.structured.Description`，描述文本会写进
  LangChain4j 追加到 user 消息末尾的 JSON 格式说明（如 `"htmlCode": (HTML代码; type: string)`）
- **机制**：LangChain4j 在 user 消息末尾追加 JSON 格式说明；`chat-model` 配置 `response-format: json_object`，接口层保证返回合法 JSON，
  再由 LangChain4j 解析为 POJO
- **注意**：系统提示词仍要求 Markdown 代码块加解释文字，与 JSON 指令存在语义冲突（见 pitfalls P-25）

### 3.21 代码保存器（`core/saver/CodeFileSaverTemplate` + `*CodeHandler`，模板方法 + 执行器）

- **职责**：把 3.20 的结构化结果写成网页文件，返回保存目录 `File`；均为普通类，不注册 Bean，执行器持有单例保存器

| 类 / 方法 | 作用 |
|---|---|
| `CodeFileSaverTemplate<T>#saveCode(T)`（`public final`） | 模板方法：`validateInput`（默认非空，`BAD_REQUEST`）→ `buildUniqueDir` → `saveFiles` |
| `CodeFileSaverTemplate#buildUniqueDir()`（`protected final`） | 创建 `{根目录}/{bizType}_{雪花ID}`，`bizType` 为 `getCodeType().getValue()` |
| `CodeFileSaverTemplate#writeToFile(dirPath, filename, content)`（`protected final`） | Hutool `FileUtil.writeString`，UTF-8，`null` 内容写空文件 |
| `HtmlCodeHandler#saveFiles` | `HTML`，写 `index.html` |
| `MultiFileCodeHandler#saveFiles` | `MULTI_FILE`，写 `index.html` / `style.css` / `script.js` |
| `CodeFileSaverExecutor.executeSaver(Object, CodeGenTypeEnum)` | 按类型选保存器；类型为 `null` / `VUE_PROJECT` 抛 `BAD_REQUEST`；结果类型与枚举不符时抛 `ClassCastException` |

- **处理器**：`HtmlCodeHandler` / `MultiFileCodeHandler`（位于 `core/handler/`）均为 `extends CodeFileSaverTemplate<T> implements CodeParser<T>`，同一类型的解析与保存在同一文件
- **包划分**：`core/parser/` 放解析接口与解析执行器，`core/saver/` 放保存模板与保存执行器，`core/handler/` 放具体实现；
  执行器字段声明为 `CodeParser<T>` / `CodeFileSaverTemplate<T>`，`execute*` 只依赖接口 / 模板；具体处理器只在字段初始化时 `new`，
  故 `parser` / `saver` 仍在 import 层面引用 `handler`
- **扩展**：新类型 = 新增一个 `XxxCodeHandler`（实现 `parseCode` / `getCodeType` / `saveFiles`），并在两个执行器的 `switch` 加分支
- **根目录**：`AppConstant.CODE_OUTPUT_ROOT_DIR` = `System.getProperty("user.dir") + "/tmp/code_output"`，随启动目录变化；`tmp` 已被
  `.gitignore` 忽略
- **注意**：多文件名需与 `codegen-multi-file-system-prompt.txt` 中 `<link>` / `<script>` 引用一致；调用方为 3.22 门面类，旧目录不会自动清理

### 3.22 `AiCodeGeneratorFacade`（代码生成门面）

- **路径**：`core/AiCodeGeneratorFacade.java`
- **职责**：门面模式，组合 `AiCodeGeneratorService`（3.19，生成）、`CodeParserExecutor`（3.23，解析）与 `CodeFileSaverExecutor`（3.21，落盘），调用方只需传提示词和类型
- **注册方式**：`@Service`，`@Resource` 注入 `AiCodeGeneratorService` 代理 Bean

| 方法 | 可见性 | 作用 |
|---|---|---|
| `generateAndSaveCode(String userMessage, CodeGenTypeEnum)` | `public` | 统一入口，按类型分发，返回保存目录 `File` |
| `generateAndSaveHtmlCode(String)` | `private` | `generateHtmlCode` → `CodeFileSaverExecutor.executeSaver(result, HTML)` |
| `generateAndSaveMultiFileCode(String)` | `private` | `generateMultiFileCode` → `CodeFileSaverExecutor.executeSaver(result, MULTI_FILE)` |
| `generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum)` | `public` | 流式统一入口：按类型选 `generate*CodeStream`（`Supplier`，订阅时才调用），返回 `Flux<String>`（模型原始文本片段） |
| `processCodeStream(Flux<String>, CodeGenTypeEnum)` | `private` | 通用流处理：拼接片段，流结束后 `CodeParserExecutor` → `CodeFileSaverExecutor` |

- **异常**：类型为 `null` → `BizException(BAD_REQUEST, "生成类型为空")`；`VUE_PROJECT` → `BAD_REQUEST`「不支持的生成类型」（同步 / 流式一致，
  流式在调用时直接抛出，不经 `Flux`）
- **流式机制**：`Flux.defer` 内新建 `StringBuilder`，`doOnNext` 拼接片段，`doOnComplete` 解析并落盘；落盘在流结束时触发，不订阅则不调用模型；
  解析 / 保存异常被 `catch` 后记 `error` 日志，不传给订阅方
- **注意**：同步调用阻塞（实测多文件约 10 s）；暂无 HTTP 接口调用，仅测试类使用
- **测试**：`AiCodeGeneratorFacadeTest` 真实调用 DeepSeek；`AiCodeGeneratorFacadeSaveTest` / `AiCodeGeneratorFacadeStreamTest` 用 Mockito 分别模拟同步结构化结果与流式片段，不依赖模型与 api-key

### 3.23 代码解析器（`core/parser/CodeParser` + `*CodeHandler`，策略 + 执行器）

- **职责**：把流式拼接后的 Markdown 文本解析为 3.20 的结构化结果；不注册 Bean，执行器持有单例解析器

| 类 / 方法 | 作用 |
|---|---|
| `CodeParser<T>`（接口） | `parseCode(String)`；共享 `HTML` / `CSS` / `JS` / 任意代码块正则常量 |
| `CodeParser#extractCode(content, pattern)`（`default`） | 返回首个匹配代码块内容（`trim`），无匹配返回 `null` |
| `CodeParser#extractDescription(content)`（`default`） | 剥离全部代码块后的剩余文字作为 `description` |
| `HtmlCodeHandler#parseCode` | 取首个 ```` ```html ```` 块为 `htmlCode`，无该块时整段文本作为 `htmlCode` |
| `MultiFileCodeHandler#parseCode` | 分别取 ```` ```html ```` / ```` ```css ```` / ```` ```js ````（或 `javascript`）块，缺失字段为 `null`（落盘时写空文件） |
| `CodeParserExecutor.executeParser(String, CodeGenTypeEnum)` | 按类型选解析器，返回 `Object`；类型为 `null` / `VUE_PROJECT` 抛 `BAD_REQUEST` |

- **注意**：依赖系统提示词要求的代码块语言标记；提示词改为 JSON 输出或改语言标记时需同步调整正则（见 pitfalls P-25 第 1 点）

### 3.24 `AppController`（`@RestController`）与 `AppService` / `AppServiceImpl`

- **路径**：`controller/AppController.java`、`service/AppService.java`、`service/impl/AppServiceImpl.java`
- **职责**：应用的创建、修改、删除、详情与分页查询；替换了生成器产出的裸 CRUD，均返回 `Result`

| 接口 | 权限 | 入参 | 返回 | 规则 |
|---|---|---|---|---|
| `POST /api/app/add` | `@AuthCheck` | `AppAddRequest`（`initPrompt`） | `Result<Long>` | `initPrompt` 为空 `40000`；`userId` 取当前登录用户 |
| `PUT /api/app/update` | `@AuthCheck` | `AppUpdateRequest`（`id` / `appName`） | `Result<Boolean>` | 只改 `appName`；不存在 `10001`，非本人 `40300` |
| `DELETE /api/app/delete/{id}` | `@AuthCheck` | 路径 `id` | `Result<Boolean>` | 同上归属校验；逻辑删除 |
| `GET /api/app/get/vo/{id}` | `@AuthCheck` | 路径 `id` | `Result<AppVo>` | 不存在 `10001` |
| `GET /api/app/my/list/page/vo` | `@AuthCheck` | `AppQueryRequest`（仅用 `pageNum` / `pageSize` / `appName`） | `Result<PageResult<AppVo>>` | `pageSize > 20` 返回 `40000`；`userId` 固定为当前用户 |
| `GET /api/app/good/list/page/vo` | `@AuthCheck` | 同上 | 同上 | `pageSize > 20` 返回 `40000`；`priority` 固定为 `AppConstant.GOOD_APP_PRIORITY`（99） |
| `DELETE /api/app/admin/delete/{id}` | ADMIN | 路径 `id` | `Result<Boolean>` | 任意应用；不存在 `10001` |
| `PUT /api/app/admin/update` | ADMIN | `AppAdminUpdateRequest`（`id` / `appName` / `cover` / `priority`） | `Result<Boolean>` | 三个字段全为 `null` 返回 `40000`；`null` 字段不更新 |
| `GET /api/app/admin/list/page/vo` | ADMIN | `AppQueryRequest`（除时间外全部字段） | `Result<PageResult<AppVo>>` | 无 20 条限制，受 `PageQuery` 上限 100 约束 |
| `GET /api/app/admin/get/vo/{id}` | ADMIN | 路径 `id` | `Result<AppVo>` | 不存在 `10001` |

| Service 方法 | 作用 |
|---|---|
| `createApp(AppAddRequest, User)` | 校验 `initPrompt`；`appName` 取 prompt 前 `DEFAULT_APP_NAME_LENGTH`（12）字，`codeGenType` 暂固定 `multi_file`，`priority` 默认 `0` |
| `getAppVo(App)` | 复制字段并按 `userId` 查创建者，经 `UserService#getUserVo` 脱敏 |
| `getAppVoList(List<App>)` | `listByIds` 一次查出全部创建者再组装，避免 N+1 |
| `getQueryWrapper(AppQueryRequest)` | `id` / `codeGenType` / `deployKey` / `priority` / `userId` 用 `eq`，`appName` / `cover` / `initPrompt` 用 `like`；值为空不参与；默认 `createTime` 倒序 |

- **DTO / VO**：`AppQueryRequest extends PageQuery`，可直接以 `@ParameterObject` 绑定 Query 参数；`AppVo` 不含 `editTime` / `isDelete`，
  附带 `UserVo user`
- **注意**：条件列名全部使用方法引用（`App::getAppName`），走实体 `@Column` 驼峰映射，避免 P-22；未使用 `PageQuery.orderBy`
- **注意**：用户编辑 / 管理员更新都写入 `editTime = now()`，`updateTime` 由数据库 `on update` 维护

### 3.25 `JsonConfig`（`@Configuration`）

- **路径**：`src/main/java/com/zh/zhaicodempther/config/JsonConfig.java`
- **职责**：注册 `JsonMapperBuilderCustomizer`（`org.springframework.boot.jackson.autoconfigure`），向 Boot 自动配置的 Jackson 3
  `JsonMapper` 添加 `SimpleModule`，`Long` / `long` 使用 `tools.jackson.databind.ser.std.ToStringSerializer`
- **目的**：雪花 ID 超出 JS `Number` 安全整数（`2^53-1`），以数字返回会被前端四舍五入，回传后查不到数据（见 pitfalls P-26）
- **影响范围**：所有接口响应中的 `Long` 字段（`id` / `userId` / 分页 `total` 等）均为字符串；`ControllerLogAspect` 注入同一
  `ObjectMapper`，日志中的 `Long` 也显示为字符串；`Integer` 等其他数字类型不变
- **注意**：反序列化不受影响，请求中字符串或数字形式的 id 均可绑定到 `Long`；前端若需对这些字段做数值运算须自行转换

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
| `langchain4j.open-ai.chat-model.base-url` | `https://api.deepseek.com` | 仅 `application-local.yml`；DeepSeek 兼容 OpenAI 协议 |
| `langchain4j.open-ai.chat-model.api-key` | `${DEEPSEEK_API_KEY:your-deepseek-api-key}` | 仅 `application-local.yml`；配置后才创建 `ChatModel` Bean，占位值需替换为真实 key |
| `langchain4j.open-ai.chat-model.model-name` | `deepseek-flash` | 模型名（DeepSeek-V4.1-Flash，默认开启思考模式；旧名 `deepseek-chat` 已停用） |
| `langchain4j.open-ai.chat-model.max-tokens` | `8192` | 单次输出 token 上限 |
| `langchain4j.open-ai.chat-model.log-requests` / `log-responses` | `true` | 打印模型请求 / 响应（本地调试用） |
| `langchain4j.open-ai.chat-model.response-format` | `json_object` | 请求体带 `response_format: {type: json_object}`，接口层保证输出合法 JSON（结构化输出用） |
| `langchain4j.open-ai.chat-model.strict-json-schema` | `true` | 仅 `response-format: json_schema` 时生效，配合 `json_object` 无效果 |
| `langchain4j.open-ai.streaming-chat-model.*` | 同 `chat-model`（不含 `response-format` / `strict-json-schema`），另有 `max-retries: 5` | 流式模型 `StreamingChatModel`，供 `*Stream` 方法使用；不配 `response-format`，流式输出原始文本 |

**不存在的配置**：`knife4j.*`（当前为纯 UI 模式，写了也不生效，见 pitfalls P-02）

## 五、对外端点

| 用途            | 地址                                                                                   |
|---------------|--------------------------------------------------------------------------------------|
| Knife4j 界面    | `http://localhost:8123/api/doc.html`                                                 |
| 原生 Swagger UI | `http://localhost:8123/api/swagger-ui/index.html`                                    |
| OpenAPI 元数据   | `http://localhost:8123/api/v3/api-docs`                                              |
| 分组配置          | `http://localhost:8123/api/v3/api-docs/swagger-config`                               |
| 分组元数据（中文分组）   | `http://localhost:8123/api/v3/api-docs/全部接口`                                         |
| 健康检查          | `http://localhost:8123/api/health/check`（返回 `ok`）                                    |
| 用户分页示例        | `http://localhost:8123/api/users/page?pageNum=1&pageSize=5&keyword=用户1`              |
| Demo 分页示例     | `http://localhost:8123/api/demo/page?pageNum=1&pageSize=5`                           |
| Demo 删除示例     | `DELETE http://localhost:8123/api/demo/delete`，body `{"ids":[1,2,3]}`                |
| Demo 异常示例     | `http://localhost:8123/api/demo/exception/biz?id=999`、`/wrap`、`/unexpected?index=99` |
| 当前登录用户        | `GET http://localhost:8123/api/user/get/login`（需先调用 `/api/user/login`，携带 Session Cookie） |
| 用户注销          | `POST http://localhost:8123/api/user/logout`（携带 Session Cookie） |
| 脱敏用户查询        | `GET http://localhost:8123/api/user/get/vo/{id}`、`/api/user/list/page/vo?pageNum=1&pageSize=10`（需登录） |
| 应用（用户）        | `POST /api/app/add`、`PUT /api/app/update`、`DELETE /api/app/delete/{id}`、`GET /api/app/get/vo/{id}`、`/api/app/my/list/page/vo`、`/api/app/good/list/page/vo`（需登录，分页每页最多 20） |
| 应用（管理员）       | `DELETE /api/app/admin/delete/{id}`、`PUT /api/app/admin/update`、`GET /api/app/admin/list/page/vo`、`/api/app/admin/get/vo/{id}`（需 admin） |

## 六、依赖清单（pom.xml）

| 依赖                                    | 版本               | scope             | 说明                                                                                    |
|---------------------------------------|------------------|-------------------|---------------------------------------------------------------------------------------|
| `spring-boot-starter-webmvc`          | 4.1.2（parent 管理） | compile           | Web MVC + 内置 validation                                                               |
| `mysql-connector-j`                   | 9.7.0（parent 管理） | runtime           | MySQL 驱动                                                                              |
| `lombok`                              | 1.18.36          | compile（optional） | 注解处理，`@Slf4j` 等                                                                       |
| `spring-boot-starter-webmvc-test`     | 4.1.2            | test              | 测试                                                                                    |
| `hutool-all`                          | 5.8.25           | compile           | 工具集（体积较大，可按需拆 `hutool-core` 等）                                                        |
| `mybatis-flex-spring-boot4-starter`   | 1.11.8           | compile           | MyBatis-Flex 持久层（Spring Boot 4 专用 starter，传递 `mybatis-spring:4.0.0`）                  |
| `mybatis-flex-codegen`                | 1.11.8           | compile           | 代码生成模块，仅 `generator` 包使用                                                              |
| `spring-boot-starter-jdbc`            | 4.1.2（parent 管理） | compile           | 提供 `DataSourceAutoConfiguration` + `spring-jdbc` + `spring-tx`，MyBatis-Flex 自动配置的前置条件 |
| `spring-boot-starter-aspectj`         | 4.1.2（parent 管理） | compile           | AOP 支持（传递 `spring-aop` + `aspectjweaver`），即 Boot 3 的 `spring-boot-starter-aop`              |
| `HikariCP`                            | Boot 管理          | compile           | 数据库连接池（生成器单次元数据连接）                                                                    |
| `springdoc-openapi-starter-webmvc-ui` | 3.2.0            | compile           | OpenAPI 元数据 + 原生 UI                                                                   |
| `knife4j-openapi3-ui`                 | 4.5.0            | compile           | Knife4j 前端静态资源                                                                        |
| `langchain4j-open-ai-spring-boot4-starter` | `${langchain4j.version}` = 1.23.0-beta33 | compile | OpenAI 兼容协议模型自动配置（传递 `langchain4j-open-ai:1.23.0`、`langchain4j-core`、Boot 4 RestClient HTTP 客户端）；未配置 `langchain4j.open-ai.*` 时不创建模型 Bean |
| `langchain4j-spring-boot4-starter`    | `${langchain4j.version}` = 1.23.0-beta33 | compile | 声明式 `@AiService` 自动配置，传递 `langchain4j:1.23.0` 核心                                     |
| `langchain4j-reactor`                 | `${langchain4j.version}` = 1.23.0-beta33 | compile | Reactor 适配，AI Service 方法可返回 `Flux<String>`（流式输出）                                     |

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
- 业务层：生成器已覆盖实体 / Mapper / Service 骨架，Controller 与 DTO/VO 需按 `Result` + `PageQuery` 约定手写（App 已完成，ChatHistory 待做）
- 鉴权方案（若引入 JWT，需在 `Knife4jConfig` 增补 `SecurityScheme`）
