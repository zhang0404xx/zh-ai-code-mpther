# 变更记录

> 记录本项目的所有变更（遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 风格，日期格式 `YYYY-MM-DD`）。
> **维护约定**：任何依赖、配置、类结构、接口的增删改，都在本文件对应日期下新增条目，并同步更新 [file-class-reference.md](file-class-reference.md)；若涉及问题排查，另记入 [pitfalls.md](pitfalls.md)。
>
> 最后更新：2026-11-13

## [未发布]

### 新增（Added，2026-11-13）

#### 获取当前登录用户

- `UserService`：新增 `User getLoginUser(HttpServletRequest request)`
- `UserServiceImpl#getLoginUser`：从 Session 读取 `UserConstant.USER_LOGIN_STATE`，为空或无 id 时抛
  `BizException(UNAUTHORIZED, "用户未登录")`；再按 id 查库返回最新数据，查不到同样视为未登录
- `UserController`：新增 `GET /api/user/get/login`，返回 `Result<LonginUserVo>`（经 `loginUserVo` 脱敏）

#### 用户注销

- `UserService`：新增 `boolean userLogout(HttpServletRequest request)`
- `UserServiceImpl#userLogout`：Session 中无 `USER_LOGIN_STATE` 时抛 `BizException(UNAUTHORIZED, "用户未登录")`，否则
  `removeAttribute` 清除登录态并返回 `true`
- `UserController`：新增 `POST /api/user/logout`，返回 `Result<Boolean>`

#### AOP 依赖

- `pom.xml`：新增 `org.springframework.boot:spring-boot-starter-aspectj`（版本由 parent 管理，4.1.2），传递
  `spring-aop:7.0.10` + `aspectjweaver:1.9.25.1`；Boot 4 中原 `spring-boot-starter-aop` 已更名，见 [pitfalls.md](pitfalls.md) P-09

#### Controller 日志切面

- `src/main/java/com/zh/zhaicodempther/aop/ControllerLogAspect.java`：新增 `@Aspect` 环绕通知，切入点
  `execution(* com.zh.zhaicodempther.controller..*(..))`
  - 正常调用 `info`：`[HTTP 方法 URI] 类#方法 | 参数 | 返回 | 耗时`；异常 `warn`：记录异常类型与信息后原样抛出
  - 参数按名称组装，跳过 `ServletRequest/Response`、`HttpSession`、`BindingResult`、流；`MultipartFile` 只记文件名与大小
  - key 含 `password` 的字段脱敏为 `******`；单段超过 2000 字符截断
  - 序列化用容器内 Jackson 3 `ObjectMapper`（Hutool 无法序列化 record，见 [pitfalls.md](pitfalls.md) P-23）
  - `@Order(1)`，位于权限切面外层，鉴权失败也会记录

#### 注解式权限控制

- `annotation/AuthCheck.java`：新增权限注解，`mustRole` 为 `UserRoleEnum`（默认 `USER`，即登录即可），可标注在方法或类上，方法优先
- `aop/AuthCheckAspect.java`：新增权限切面（`@Order(2)`），切入点 `@annotation(AuthCheck) || @within(AuthCheck)`；
  通过 `UserService#getLoginUser` 取当前用户（未登录 `40100`），角色等级不足或未知角色 `40300`
- `UserRoleEnum`：新增 `level` 字段（`USER=10`、`ADMIN=100`）与 `hasPermission(required)`，高等级自动拥有低等级权限；新增角色只需加枚举常量
- `UserController`：生成的 CRUD 接口 `save` / `remove` / `update` / `list` / `getInfo` / `page` 加
  `@AuthCheck(mustRole = UserRoleEnum.ADMIN)`，仅管理员可访问（原先任何人可读写全部用户数据）

#### 脱敏用户查询（user / admin 均可访问）

- `UserService` / `UserServiceImpl`：新增 `getUserVo(User)`、`getUserVoList(List<User>)`，只复制 `UserVo` 字段（不含
  `userPassword` / `isDelete` / `editTime` / `updateTime`）
- `UserController`：新增 `GET /api/user/get/vo/{id}`（`Result<UserVo>`，用户不存在 → `10001`）与
  `GET /api/user/list/page/vo`（`PageQuery` 接参，返回 `Result<PageResult<UserVo>>`），均标注 `@AuthCheck`

### 验证（Verified，2026-11-13）

| 检查项                        | 结果            |
|----------------------------|---------------|
| `mvnw -B -o -DskipTests compile` | BUILD SUCCESS（获取登录用户、注销各编译一次） |
| `mvnw compile dependency:tree`（过滤 aspectj） | BUILD SUCCESS，`spring-boot-starter-aspectj:4.1.2` → `spring-aop` + `aspectjweaver` |
| `GET /api/demo/page?pageNum=1&pageSize=2` | 200，切面日志含参数 `query`、完整 `Result` 返回值与耗时 |
| `POST /api/user/login`（不存在的账号） | 400，切面 `warn` 日志含 `BizException`，`userPassword` 显示为 `******` |
| 验证后服务关闭 | 8123 端口已释放 |
| 未登录 `GET /api/user/list` | 400 + `40100 用户未登录` |
| 普通用户 `authtest01` 登录后 `GET /api/user/list`、`/user/getInfo/1` | 400 + `40300 没有操作权限`，日志切面记录拒绝 |
| 普通用户 `GET /api/user/get/login`（无注解） | 200，正常返回 |
| 管理员放行路径（`authtest01` 的 `userRole` 改为 `admin`） | `GET /api/user/list`、`/user/getInfo/{id}` 均 200 返回数据；注销后再访问 `list` → `40100` |
| 未登录 `GET /api/user/get/vo/{id}` | 400 + `40100` |
| 普通用户 `authtest02` / 管理员 `authtest01` 访问 `get/vo/{id}`、`list/page/vo?pageNum=1&pageSize=2` | 均 200，`total:4`，响应不含 `userPassword` / `isDelete` |
| `GET /api/user/get/vo/1`（不存在） | 400 + `10001 用户不存在` |

### 新增（Added，2026-11-12）

#### 代码生成器

- `src/main/java/com/zh/zhaicodempther/generator/MyBatisFlexGenerator.java`：新增 MyBatis-Flex 代码生成器，采用官方模板写法（
  `HikariDataSource` + `GlobalConfig` + `Generator`），独立 `main`，不注册 Bean、不参与应用启动
  - 数据源用 Hutool `YamlUtil` 读取 classpath 的 `application.yml`（`spring.datasource.url/username/password`），不硬编码账号口令
  - `setGenerateTable("user", "app", "chat_history")` 只生成三张表；去掉该行则生成全库
  - 生成 entity / mapper / service / serviceImpl / controller，`basePackage` 为 `com.zh.zhaicodempther.genresult`
    ，其下使用官方默认子包（`entity` / `mapper` / `service` / `service.impl` / `controller`）
  - Lombok + JDK 21；`setLogicDeleteColumn("isDelete")` 使 `isDelete` 列生成 `@Column(isLogicDelete = true)`
  - 官方默认 `overwriteEnable=false`，同名文件（含现有 `UserController`）自动跳过
  - 类说明见 [file-class-reference.md](file-class-reference.md) 3.16，逻辑删除列名大小写坑见 [pitfalls.md](pitfalls.md)
    P-19

### 修复（Fixed，2026-11-12）

#### 持久层依赖与启动失败

- `pom.xml`：新增 `org.springframework.boot:spring-boot-starter-jdbc`（Boot 4 的 `DataSourceAutoConfiguration` 在
  `spring-boot-jdbc` 模块，缺该 starter 时没有 `DataSource` / `spring-jdbc` / `spring-tx`）
- `pom.xml`：`mybatis-flex-spring-boot3-starter:1.11.0` 换为 `mybatis-flex-spring-boot4-starter:1.11.8`（Boot 4 专用自动配置，传递
  `mybatis-spring:4.0.0`）；`mybatis-flex-codegen` 同步升到 1.11.8
- 修复启动报错 `NoSuchBeanDefinitionException: No qualifying bean of type ... AppMapper`
  ，根因与验证见 [pitfalls.md](pitfalls.md) P-21

#### 错误码

- `ErrorCode`：新增用户域错误码 `USER_NAME_DUPLICATE(11001)` / `USER_NAME_NOT_FOUND(11002)` / `PASSWORD_ERROR(11003)`
  ；修正其插入位置（原写在常量列表结束符 `;` 之后导致编译失败）及与 10001~10003 重复的问题

#### 接口路径冲突

- `UsersController`：`@RequestMapping("/user")` → `"/users"`，与生成的 CRUD 控制器 `UserController`（`/user`）区分，消除
  `Ambiguous mapping ... /user/page` 启动失败（详见 [file-class-reference.md](file-class-reference.md) 3.10 / 3.10.1）

#### 用户注册查重 SQL 列名错误

- `UserServiceImpl#register`：`queryWrapper.eq("user_account", ...)` → `queryWrapper.eq(User::getUserAccount, ...)`，修复
  `Unknown column 'user_account' in 'where clause'`（表列名为驼峰 `userAccount`，见 [pitfalls.md](pitfalls.md) P-22）

### 验证（Verified，2026-11-12）

| 检查项                                        | 结果                                                                                  |
|--------------------------------------------|-------------------------------------------------------------------------------------|
| `mvnw -DskipTests compile`                 | BUILD SUCCESS（官方模板版重写后再次编译通过）                                                       |
| `Generator#getTables()` 不落盘检查（同参数配置）       | 识别 3 张表；驼峰列名属性名保持；三表 `isDelete` 均标记为逻辑删除                                            |
| `mvnw -B -o clean compile`                 | BUILD SUCCESS（MyBatis-Flex starter 切换后）                                             |
| `mvnw dependency:tree`                     | `mybatis-spring:4.0.0`、`spring-boot-starter-jdbc:4.1.2`、`spring-jdbc:7.0.10`        |
| `mvnw spring-boot:run`                     | 打印 `MyBatisFlex v1.11.8` banner，`appMapper` / `appServiceImpl` 正常创建，无 Bean 缺失、无路径冲突 |
| `GET /api/users/page?pageNum=1&pageSize=5` | 200，`total:30`，返回 id 1~5                                                            |
| `GET /api/user/list`                       | 200，`[]`（Mapper → MySQL 链路打通）                                                       |
| `GET /api/v3/api-docs`                     | 路径中包含 `/users/page`                                                                 |

### 计划中
- 视需要将 `zh.api-doc.*` 改为 `@ConfigurationProperties` 形式并生成配置元数据（获得 IDE 补全）
- 决定错误码与 HTTP 状态的映射方式：当前 5 位业务码使所有错误都返回 HTTP 400，可选方案见 pitfalls P-16

---

## [0.0.1-SNAPSHOT] - 2026-11-08

### 新增（Added）

#### 在线接口文档
- `pom.xml`：引入 `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0`（Spring Boot 4 适配版本）
- `pom.xml`：引入 `com.github.xiaoymin:knife4j-openapi3-ui:4.5.0`（纯静态前端资源）
- `src/main/java/com/zh/zhaicodempther/config/Knife4jConfig.java`：新增 `OpenAPI` 与 `GroupedOpenApi` 两个 Bean
- `application.yml`：新增 `springdoc.*` 配置（`api-docs.path`、`swagger-ui.path`、排序策略、`default-flat-param-object`）
- 文档地址：`http://localhost:8123/api/doc.html`

#### 统一异常处理
- `src/main/java/com/zh/zhaicodempther/exception/ErrorCode.java`：错误码枚举（现为 5 位码：`40000` / `50000` / `10001` 等；`0` 为成功，1xxxx 为业务码）
- `src/main/java/com/zh/zhaicodempther/exception/BizException.java`：业务异常，携带 `ErrorCode`
- `src/main/java/com/zh/zhaicodempther/exception/GlobalExceptionHandler.java`：`@RestControllerAdvice` 全局处理器，覆盖 9 类异常
- `src/main/java/com/zh/zhaicodempther/common/Result.java`：统一响应体（`code` / `message` / `data`）

#### 通用组件
- `src/main/java/com/zh/zhaicodempther/common/PageQuery.java`：新增分页请求封装，含 `pageNum` / `pageSize` / `orderBy`，读取阶段自动纠正越界值（页码默认 1、页大小默认 10、上限 100），提供 `offset()` / `limit()` 便捷方法，字段带 `@Schema` 注解会在 Knife4j 中展示说明
- `src/main/java/com/zh/zhaicodempther/common/PageResult.java`：新增分页结果封装（`total` / `pageNum` / `pageSize` / `list`），提供 `of(...)` / `empty(...)` 工厂方法与 `totalPages()` 总页数计算，集合做了 null 与不可变副本处理
- `src/main/java/com/zh/zhaicodempther/common/DeleteRequest.java`：新增删除请求封装（record），字段 `ids` / `reason`；`ids` 自动去重、不可变，Bean Validation 校验非空 / 非 null / 必须为正整数 / 单次最多 100 条；提供 `of(Long...)`、`isBatch()`、`firstId()`

#### 文档
- `docs/pitfalls.md`：踩坑记录
- `docs/changelog.md`：变更记录（本文件）
- `docs/file-class-reference.md`：文件与类说明

#### 健康检查
- `src/main/java/com/zh/zhaicodempther/controller/HealthController.java`：新增 `GET /api/health/check`，返回纯文本 `ok`
- 说明：该 Controller 由使用者手动添加，文档已同步登记；处理方法当前为 `private`，待改为 `public`（见 pitfalls P-13）

#### 用户分页示例接口
- `src/main/java/com/zh/zhaicodempther/controller/UserController.java`：新增 `GET /api/user/page`，用 30 条内存假数据跑通 `PageQuery` + `PageResult` + `Result` 完整链路，支持 `keyword` 模糊过滤，`@ParameterObject` 使分页字段在 Knife4j 中展开为 `pageNum` / `pageSize` / `orderBy`
- `src/main/java/com/zh/zhaicodempther/model/UserVO.java`：新增用户视图对象（`id` / `name` / `email` / `status` / `createdAt`）

#### 示例接口（Demo）
- `src/main/java/com/zh/zhaicodempther/controller/DemoController.java`：新增三段可复制示例
  - 分页：`GET /api/demo/page`，演示 `PageQuery` + `PageResult` + `Result` 三段式
  - 删除：`DELETE /api/demo/delete`，用 `DeleteRequest` + `@Valid` 演示批量删除与参数校验失败分支
  - 异常：`GET /api/demo/exception/biz|wrap|unexpected`，分别演示«直接抛 BizException»、«catch 后转 BizException 并保留 cause»、«不 catch 交给兜底 handler»

#### 全局跨域
- `src/main/java/com/zh/zhaicodempther/config/CorsConfig.java`：新增全局跨域配置，基于 `CorsFilter` + `FilterRegistrationBean`（`Ordered.HIGHEST_PRECEDENCE`），参数由 `zh.cors.*` 注入，默认任意来源放行
- 选择 `CorsFilter` 而非 `WebMvcConfigurer#addCorsMappings` 的原因：404 / 全局异常响应也能带 CORS 头（见 pitfalls P-18）

### 变更（Changed）

#### 依赖
- `pom.xml`：移除 `com.github.xiaoymin:knife4j-openapi3-jakarta-servlet:4.4.0`（与 Spring Boot 4 不兼容，见 pitfalls P-01）
- `pom.xml`：`cn.hutool:hutool-all:5.8.25` 保持原有版本，仅规范注释与空行格式

#### 配置
- `application.yml`：新增 `zh.api-doc.*`（`title` / `description` / `version` / `contact-name`）
- `application.yml`：新增 `zh.cors.*`（`allowed-origin-patterns` / `allowed-methods` / `allowed-headers` / `exposed-headers` / `allow-credentials` / `max-age`），均带默认值，不配置也可启动
- `Knife4jConfig`：标题、描述、版本、联系人改为通过 `@Value` 从 `zh.api-doc.*` 注入，均带默认值，不配置也能启动

#### 构建环境（本机，非仓库内容）
- `C:\Users\Administrator\.m2\settings.xml`：配置 `aliyun-public` 镜像 + `localRepository=D:\java\Maven\repository`，统一 wrapper / 安装版 Maven / IDEA 三方读取来源（见 pitfalls P-04）
- `.idea/workspace.xml`：`MavenGeneralSettings.userSettingsFile` 由 `D:\java\maven\apache-maven-3.9.12\conf\settings.xml` 改为 `C:\Users\Administrator\.m2\settings.xml`，并显式写入 `localRepository`
- 清理 `~/.m2/wrapper/dists` 下 9 个冗余 Maven 发行版（3.8.1 / 3.9.9 / 3.9.11 / 3.9.12×2 / 3.9.14×2 / 3.9.16×2），回收 146.8 MB
- `C:\Users\Administrator\.m2\repository` 重命名为 `repo.bak` 归档（1.18 GB，保留回滚能力）

### 验证（Verified）
| 检查项 | 结果 |
|---|---|
| `mvnw -DskipTests compile` | BUILD SUCCESS，2.8 s |
| `GET /api/v3/api-docs` | 200，`info.title` 随 `zh.api-doc.title` 变化 |
| `GET /api/doc.html` | 200（Knife4j 界面） |
| `GET /api/webjars/js/*.js` | 200（静态资源在 context-path 下正常） |
| `GET /api/not-exist` | 404 + `{"code":404,"message":"请求的资源不存在","data":null,"success":false}` |
| `GET /api/health/check` | 200，返回 `ok`（`private` 处理方法可用） |
| `GET /api/v3/api-docs` | 已包含 `/health/check`，tag 为 `health-controller` |
| `GET /api/demo/page?pageNum=2&pageSize=5` | 200，`total:12, pageNum:2, pageSize:5`，返回 id 6~10 |
| `DELETE /api/demo/delete {"ids":[1,2,3]}` | 200，`data:3` |
| `DELETE /api/demo/delete {"ids":[1,1,2]}` | 200，`data:2`（自动去重） |
| `DELETE /api/demo/delete {"ids":[]}` | 400 + `ids: 待删除的 ID 列表不能为空` |
| `DELETE /api/demo/delete {"ids":[1,-2]}` | 400 + `ids[1]: ID 必须为正整数` |
| `GET /api/demo/exception/biz?id=999` | 400 + `{"code":10001,"message":"用户不存在，id=999"}` |
| `GET /api/demo/exception/wrap` | 400 + `{"code":50300}`，日志 warn 且带 cause |
| `GET /api/demo/exception/unexpected?index=99` | 400 + `{"code":50000}`，堆栈只进日志 |
| `/api/v3/api-docs` | 含 `/demo/*` 五个路径，`/demo/page` 参数展开为 `pageNum, pageSize, orderBy` |
| `OPTIONS /api/user/page`（带 Origin） | 200，回显 `Allow-Origin`，`Allow-Methods` 为 7 个方法，`Max-Age: 3600` |
| `GET /api/user/page`（带 Origin） | 200，带 `Access-Control-Allow-Origin`，无 `Allow-Credentials`（默认关闭） |
| `GET /api/user/page`（无 Origin） | 不带任何 CORS 头（同源请求不受影响） |
| `GET /api/not-exist`（带 Origin） | 404 + **仍带** `Access-Control-Allow-Origin`（CorsFilter 生效） |
| `DELETE /api/demo/delete`（带 Origin） | 200，`data:2`，跨域 DELETE 正常 |

### 已知限制（Known Limitations）
- 未使用 Knife4j 后端增强：`knife4j.*` 配置不生效（见 pitfalls P-02）
- `zh.api-doc.*` 无 IDE 自动补全（未引入 `spring-boot-configuration-processor`）
