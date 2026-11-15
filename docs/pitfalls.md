# 踩坑记录

> 记录本项目（`zh-ai-code-mpther`）在搭建与开发过程中遇到的真实问题、根因与解决方式。
> **维护约定**：遇到新坑、或验证发现旧结论不再成立时，在本文件新增/修正章节，并同步更新 [changelog.md](changelog.md)。
>
> 最后更新：2026-11-13（补充 P-09 AOP starter 更名；新增 P-23）

## 索引

| 编号   | 问题                                                   | 影响面   |
|------|------------------------------------------------------|-------|
| P-01 | Knife4j 4.x 与 Spring Boot 4 不兼容                      | 依赖选型  |
| P-02 | `knife4j.*` 配置在纯 UI 模式下不生效                           | 配置    |
| P-03 | `server.servlet.context-path` 使文档地址全部带前缀             | 访问路径  |
| P-04 | `mvnw` 不读安装版 Maven 的 `conf/settings.xml`，镜像失效        | 构建速度  |
| P-05 | 本地仓库来源 id 变更触发一次性重新校验                                | 构建日志  |
| P-06 | `mvnw` 默认用 JDK 11，编译 Java 21 项目失败                    | 构建环境  |
| P-07 | `spring-boot:run` 会先执行 `test-compile`，额外下载测试依赖       | 构建速度  |
| P-08 | IDEA 运行中直接改 `.idea/workspace.xml` 可能被覆盖              | 工具链   |
| P-09 | Spring Boot 4 的 starter 名称与内置 validation 变化          | 依赖/注解 |
| P-10 | `Result` 的 `isSuccess()` 被 Jackson 序列化成 `success` 字段 | 接口契约  |
| P-11 | PowerShell 控制台 GBK 编码导致中文响应"乱码"                      | 排查干扰  |
| P-12 | C 盘 `.m2/repository` 与 D 盘仓库分叉                       | 磁盘/配置 |
| P-13 | 控制器方法写成 `private` 也能跑通，但有隐患                          | 代码规范  |
| P-14 | 分页参数直接用 `pageSize` / `orderBy` 拼 SQL                 | 性能/安全 |
| P-15 | 对象参数加了 `@Parameter` 后不会被展开                           | 接口文档  |
| P-16 | 错误码改成 5 位后，所有错误都返回 HTTP 400                          | 接口契约  |
| P-17 | 带 body 的 DELETE 请求在部分客户端/网关不可用                       | 接口契约  |
| P-18 | 跨域配置的两个坑：`*` 与凭证互斥、404 响应无 CORS 头                    | 跨域    |
| P-19 | 生成器 `setLogicDeleteColumn` 按列名精确匹配（大小写敏感），写错时静默不生效   | 代码生成  |
| P-20 | Hutool 按路径读文件时以 classpath 为基准，传相对文件路径会失败             | 工具链   |
| P-21 | Spring Boot 4 下 Mapper Bean 不存在（starter 版本与 JDBC 依赖） | 依赖/启动 |
| P-22 | `QueryWrapper` 字符串列名写成下划线，报 `Unknown column 'user_account'` | 持久层   |
| P-23 | Hutool `JSONUtil` 序列化 record 得到 `{}`，日志切面返回值为空              | 日志    |

---

## P-01 Knife4j 4.x 与 Spring Boot 4 不兼容

**现象**：按教程引入 `knife4j-openapi3-jakarta-spring-boot-starter`（或 `knife4j-openapi3-jakarta-servlet`）后，应用启动即失败。

**根因**：
- Knife4j 最新版 `4.5.0` 发布于 2024-01，内部锁定 **springdoc-openapi 2.x**，只适配 Spring Boot 3.x（Spring Framework 6）。
- 本项目是 **Spring Boot 4.1.2（Spring Framework 7）**，需要 springdoc **3.x**。
- 两代 springdoc 的自动配置与 API 均有破坏性变更，混用会直接启动报错。

**解决**：拆成"元数据"与"前端"两部分：
```xml
<!-- 负责生成 OpenAPI 元数据，适配 Boot 4 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.2.0</version>
</dependency>
<!-- 只提供 doc.html 静态资源，不含自动配置，不会冲突 -->
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-ui</artifactId>
    <version>4.5.0</version>
</dependency>
```

**验证方式**：启动后 `GET /api/v3/api-docs`、`/api/doc.html`、`/api/webjars/js/*.js` 均返回 200。

**重新评估条件**：Knife4j 发布适配 springdoc 3.x 的 starter 之后，可换回官方 starter 以恢复 `knife4j.*` 后端增强配置。

---

## P-02 `knife4j.*` 配置在纯 UI 模式下不生效

**现象**：`application.yml` 里写了
```yaml
knife4j:
  enable: true
  setting:
    language: zh_cn
```
既不报错也不生效。

**根因**：这组配置由 **starter** 中的 `Knife4jProperties` 绑定。项目只引入了 `knife4j-openapi3-ui`（静态前端 jar，无任何 Java 类），没有 `@ConfigurationProperties` 去消费这些 key，Spring Boot 对未被绑定的属性**静默忽略**。

**解决**：从 yml 中删除该配置块，避免误导。Knife4j 前端侧的分组切换、接口搜索、在线调试等功能不受影响。

**注意**：不要为了"让它生效"去自己写 `@ConfigurationProperties(prefix = "knife4j")`，那是重复造轮子。

---

## P-03 `server.servlet.context-path` 使文档地址全部带前缀

**现象**：访问 `http://localhost:8123/doc.html` 得到 404。

**根因**：`server.servlet.context-path: /api`，所有 Servlet 路径（含 springdoc 端点与 Knife4j 静态资源）都以 `/api` 为前缀。

**解决**：统一使用带前缀的地址：

| 用途 | 地址 |
|---|---|
| Knife4j 界面 | `http://localhost:8123/api/doc.html` |
| 原生 Swagger UI | `http://localhost:8123/api/swagger-ui/index.html` |
| OpenAPI 元数据 | `http://localhost:8123/api/v3/api-docs` |
| 分组配置 | `http://localhost:8123/api/v3/api-docs/swagger-config` |

**附带说明**：`/api/favicon.ico` 返回 404 属正常现象（项目未提供图标），浏览器地址栏会出现一次失败请求，不影响功能。

---

## P-04 `mvnw` 不读安装版 Maven 的 `conf/settings.xml`，镜像失效

**现象**：IDEA 构建很快，命令行 `.\mvnw.cmd` 构建极慢，日志显示 `Downloading from central: https://repo.maven.apache.org/...`（4-18 kB/s），且依赖被下载到 `C:\Users\xxx\.m2\repository`。

**根因**：Maven 配置读取顺序为
`-s 参数` → `${user.home}/.m2/settings.xml`（用户级） → **当前 Maven 安装目录的 `conf/settings.xml`**（全局级）。

- 阿里云镜像与 `<localRepository>D:\java\Maven\repository</localRepository>` 只写在 `D:\java\maven\apache-maven-3.9.12\conf\settings.xml`（安装版全局配置）。
- Maven Wrapper 会**自己下载一份 Maven 3.10.0** 到 `~/.m2/wrapper/dists`，读取的是那份发行版自己的 `conf/settings.xml`（无镜像）。
- 用户级 `~/.m2/settings.xml` 当时是空模板，于是回落到官方 central + 默认 C 盘仓库。

**解决**：把镜像与仓库路径写入**用户级** `C:\Users\Administrator\.m2\settings.xml`，使 wrapper、安装版 Maven、IDEA 三方共用：
```xml
<localRepository>D:\java\Maven\repository</localRepository>
<mirrors>
  <mirror>
    <id>aliyun-public</id>
    <mirrorOf>central</mirrorOf>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

**验证方式**：
```powershell
.\mvnw.cmd help:evaluate -Dexpression=settings.localRepository -q -DforceStdout
# 期望输出 D:\java\Maven\repository

.\mvnw.cmd dependency:get -Dartifact=org.apache.commons:commons-lang3:3.18.0
# 日志应出现 Downloading from aliyun-public
```
修复后同一项目全量编译：`BUILD SUCCESS, Total time: 2.8 s`（修复前数分钟）。

---

## P-05 本地仓库来源 id 变更触发一次性重新校验

**现象**：更换 settings.xml 后，构建日志大量出现
`Artifact ... is present in the local repository, but cached from a remote repository ID that is unavailable in current build context, verifying that is downloadable from [aliyun-public ...]`。

**根因**：每个 artifact 目录下的 `_remote.repositories` 文件记录了它当初的来源仓库 id（旧配置下是 `central`）。当前生效的仓库 id 变成 `aliyun-public`，Maven 认为来源不匹配，于是重新校验可下载性。

**解决**：无需处理，**一次性现象**，跑一遍完整构建后即消失。若想彻底跳过可用 `-o`（离线），但离线模式下同样会遇到校验失败，不建议长期使用。

---

## P-06 `mvnw` 默认用 JDK 11，编译 Java 21 项目失败

**现象**：命令行执行 `.\mvnw.cmd -v` 显示 `Java version: 11.0.27, runtime: D:\java\jdk11`，而项目 `java.version=21`。

**根因**：系统环境变量 `JAVA_HOME` 指向 JDK 11；IDEA 内部用自己的 JDK 21（`D:/Java/jdk21`），两者互不影响。

**解决**：命令行执行 Maven 前显式覆盖：
```powershell
$env:JAVA_HOME='D:\Java\jdk21'
.\mvnw.cmd -B -DskipTests compile
```

---

## P-07 `spring-boot:run` 会先执行 `test-compile`

**现象**：仅想启动应用，却看到 `spring-boot-starter-test`、`spring-test`、`byte-buddy` 等测试依赖被下载。

**根因**：`spring-boot:run` 默认的前置生命周期是 `test-compile`。

**解决**：首次构建属正常成本；若确实不需要测试编译，可用 `-Dspring-boot.run.useTestClasspath=false` 或先 `mvn compile` 再直接 `java -cp` 运行 `target/classes`。

---

## P-08 IDEA 运行中直接改 `.idea/workspace.xml` 可能被覆盖

**现象**：手动修改 `workspace.xml` 中的 `MavenGeneralSettings` 后，重启 IDEA 发现被还原。

**根因**：IDEA 运行时在内存中持有 workspace 状态，退出或触发保存时会把内存状态写回文件。

**解决**：修改前建议关闭 IDEA；或在 `File → Settings → Build, Execution, Deployment → Build Tools → Maven` 中直接改 UI（勾选 `User settings file` 的 Override），改完 Apply 即可固化。

---

## P-09 Spring Boot 4 的 starter 名称与内置 validation 变化

**要点**：
1. **starter 名称变化**：Boot 4 使用 `spring-boot-starter-webmvc`，不再是 `spring-boot-starter-web`；AOP 使用
   `spring-boot-starter-aspectj`，`spring-boot-starter-aop` 在 4.1.2 的 BOM 中已不存在（照搬 Boot 3 教程会报缺少版本号）。
2. **内置 validation**：`spring-boot-starter-webmvc` 已传递引入 `spring-boot-validation`（含 `hibernate-validator 9.1.5` + `jakarta.validation-api 3.1.1`），因此 `@Valid` / `@NotBlank` 开箱可用，**不需要**再单独引 `spring-boot-starter-validation`。
3. **Jackson 版本**：Boot 4 主用 Jackson 3（`tools.jackson.core:jackson-databind:3.1.7`），同时传递 Jackson 2；Java `record` 序列化正常。

**验证方式**：
```powershell
.\mvnw.cmd -B dependency:tree | Select-String "validation|jackson-databind"
```

---

## P-10 `Result` 的 `isSuccess()` 被 Jackson 序列化成 `success` 字段

**现象**：接口返回 `{"code":404,"message":"...","data":null,"success":false}`，比预期多一个 `success`。

**根因**：Jackson 会把形如 `isXxx()` 的无参方法识别为属性 `xxx`。`Result` 是 record，`isSuccess()` 被视为业务读取方法并参与序列化。

**处理**：保留该字段（前端可直接判断成功与否）或加 `@JsonIgnore` / 改名为 `checkSuccess()` 取消输出。

---

## P-11 PowerShell 控制台 GBK 编码导致中文响应"乱码"

**现象**：`Invoke-WebRequest` 打印出的 JSON 中文变成 `æ¥å£ææ` 之类。

**根因**：接口返回 UTF-8，但 PowerShell 控制台默认 GBK 显示，属**展示层**问题。

**解决**：浏览器或 `curl.exe` 查看即可；PowerShell 内可设置 `[Console]::OutputEncoding=[Text.Encoding]::UTF8`。

---

## P-12 C 盘 `.m2/repository` 与 D 盘仓库分叉

**现象**：机器上同时存在 `C:\Users\Administrator\.m2\repository`（1181 MB / 18698 文件）与 `D:\java\Maven\repository`（847 MB）。

**根因**：见 P-04。C 盘那份是早期使用默认配置时累积的仓库，其中 **737 个 jar 在 D 盘不存在**（涉及 springfox、mybatis-plus、TDengine、rabbitmq、jjwt、undertow 等旧技术栈）。

**处理**：统一到 D 盘后，将 C 盘仓库重命名为 `repo.bak` 归档（不直接删除），观察 1-2 周无异常再清理，避免历史项目的依赖需要重新下载、个别已下架版本无法拉回。

**回滚方式**：将 `repo.bak` 改回 `repository` 即可。

---

## P-13 控制器方法写成 `private` 也能跑通，但有隐患

**现象**：`HealthController` 的处理方法声明为 `private String check()`，访问 `GET /api/health/check` 仍返回 200 `ok`，且 `/v3/api-docs` 中也能看到该接口。

**根因**：Spring MVC 通过反射扫描带映射注解的方法，不受 Java 访问修饰符限制（内部 `setAccessible(true)`）。因此 `private`/`protected` 方法同样会被注册为处理器。

**隐患**：
1. 依赖反射绕过访问控制，在启用 AOP 代理（CGLIB 子类代理无法覆写 private 方法）、AOT 编译或 GraalVM 原生镜像等场景下可能失效。
2. 与 IDE、静态分析、团队规范冲突，且无法被单元测试直接调用。
3. 异常堆栈与日志可读性变差，排查成本上升。

**解决**：控制器处理方法统一声明为 `public`。

**验证方式**：修改后重跑应用，`GET /api/health/check` 仍为 200，`/api/v3/api-docs` 中接口不变。

---

## P-14 分页参数直接用 `pageSize` / `orderBy` 拼 SQL

**现象**：分页接口上线后，偶发慢查询（单次扫描全表），或无排序功能的接口也能被传入 `orderBy` 任意表达式。

**根因**：
1. `pageSize` 未设上限，前端传 `100000` 直接变成大 `LIMIT`。
2. `orderBy` 是 SQL 子句片段，**不能用 `#{}` 预编译占位符**，一旦直接 `${}` 拼接就存在 SQL 注入可能。

**解决**：用 `common.PageQuery` 统一兜底：
- `getPageSize()` 夹到 `1~100`，`getPageNum()` 小于 1 时归 1；
- `getOrderBy()` 剔除白名单以外的字符（仅保留字母、数字、下划线、空格、逗号、点）。

**仍有风险的部分**：字符过滤挡不住 `case when ... then` 这类无特殊字符的表达式，因此真正拼接到 SQL 前，建议再按**实体字段白名单**校验（如 `Set<String> SORTABLE_FIELDS`）。

**适用边界**：使用 MyBatis-Plus 的 `Page` / `QueryWrapper.orderBy` 时同样需要该白名单，框架不会帮你挡注入。

---

## P-15 对象参数加了 `@Parameter` 后不会被展开

**现象**：`GET /api/users/page` 已经开启 `springdoc.default-flat-param-object: true`，但在 `/v3/api-docs` 里参数只有单个
`query`，看不到 `pageNum` / `pageSize` / `orderBy`，Knife4j 界面上也无法逐个填写。

**根因**：给 Bean 参数加了 `@Parameter` 注解后，springdoc 会把它当成**单个参数**描述，不再执行对象展平逻辑，`default-flat-param-object` 因此失效。

**解决**：改用 springdoc 的 `@ParameterObject` 注解：
```java
@GetMapping("/page")
public Result<PageResult<UserVO>> page(@ParameterObject PageQuery query) { ... }
```
修复后参数列表变为：`pageNum, pageSize, orderBy, keyword`。

**验证方式**：
```powershell
(Invoke-WebRequest 'http://localhost:8123/api/v3/api-docs' -UseBasicParsing).Content | ConvertFrom-Json |
  Select-Object -ExpandProperty paths | Select-Object -ExpandProperty '/users/page'
# 期望 parameters 中包含 pageNum / pageSize / orderBy
```

**适用范围**：任何要作为 query 参数接收的 Bean（分页、搜索条件、筛选条件）都适用；请求体（`@RequestBody`）不受影响。

---

## P-16 错误码改成 5 位后，所有错误都返回 HTTP 400

**现象**：`ErrorCode` 现为 `40000` / `50000` / `50300` 等 5 位码，实测 `GET /api/demo/exception/unexpected` 返回 `HTTP 400 + {"code":50000}`，`/demo/exception/wrap` 返回 `HTTP 400 + {"code":50300}`——系统内部异常和服务不可用都没有返回 5xx。

**根因**：`GlobalExceptionHandler.build(int code, String message)` 用 `HttpStatus.resolve(code)` 把业务码反查成 HTTP 状态。该方法只认标准的三位状态码（100~599），传入 `50000` 返回 `null`，于是走 `status != null ? status : HttpStatus.BAD_REQUEST` 的兜底分支，**所有错误统一变成 400**。

**方案 A（推荐，语义最正）**：业务码与 HTTP 状态解耦，状态由 `ErrorCode` 单独携带：
```java
public enum ErrorCode {
    BAD_REQUEST(40000, 400, "请求参数错误"),
    INTERNAL_SERVER_ERROR(50000, 500, "系统内部异常"),
    SERVICE_UNAVAILABLE(50300, 503, "服务暂不可用"),
    DATA_NOT_FOUND(10001, 400, "数据不存在");   // 业务码默认 400
    private final int code;
    private final int httpStatus;
}
```
处理器改成 `HttpStatus.resolve(errorCode.getHttpStatus())`。

**方案 B（最小改动）**：保留现状，把 `HttpStatus.resolve` 换成 `code / 100` 取整数，即 `50000 / 100 = 500`、`40000 / 100 = 400`——前提是所有错误码都按「HTTP 状态 × 100 + 序号」编码。

**方案 C（前端全按 code 判断）**：HTTP 状态统一 200，错误信息只放在 `code` 里。改动最小，但会破坏 HTTP 语义，网关/监控无法按 5xx 告警。

**当前状态**：代码未改，仍为「business code 5 位 + HTTP 全 400」。已确认这是设计取舍而非 bug，但如果前端或网关依赖 HTTP 状态做重试/告警，需要先落地方案 A 或 B。

**验证方式**：
```powershell
Invoke-WebRequest 'http://localhost:8123/api/demo/exception/unexpected?index=99' -UseBasicParsing
# 期望：方案 A/B 下为 500；当前为 400
```

---

## P-17 带 body 的 DELETE 请求在部分客户端/网关不可用

**现象**：`DELETE /api/demo/delete` 用 `@RequestBody` 接收 `DeleteRequest`，在 Knife4j、axios、OkHttp 中正常，但 `HttpURLConnection`（Java 原生）与部分 Nginx / API 网关配置会丢弃或拒绝 DELETE 的 body。

**根因**：HTTP 规范不禁止 DELETE 带 body，但 `HttpURLConnection` 不支持、部分代理会剥离请求体，属于**实现层差异**。

**解决**（三选一）：
1. 改成 `@PostMapping("/delete")`，路径带动词，最省心；
2. 保留 DELETE，把 ID 放 query：`DELETE /api/demo/delete?ids=1,2,3`；
3. 维持现状，但在接口文档与前端约定中明确要求客户端支持 DELETE body。

**本项目选择**：DemoController 用 `@DeleteMapping + @RequestBody`（配合 DeleteRequest 演示封装），注释中已标注可改为 POST。业务模块若走网关，建议优先方案 1 或 2。

**验证方式**：
```powershell
Invoke-WebRequest -Uri 'http://localhost:8123/api/demo/delete' -Method Delete `
  -Body '{"ids":[1,2,3]}' -ContentType 'application/json' -UseBasicParsing
# 期望 {"code":0,...,"data":3}
```

---

## P-18 跨域配置的两个坑：`*` 与凭证互斥、404 响应无 CORS 头

### 坑 1：`setAllowedOrigins("*")` + `setAllowCredentials(true)` 直接抛异常

**现象**：按旧教程写 `allowedOrigins("*")` 并开启 `allowCredentials(true)`，启动或首次请求时报错：
`When allowCredentials is true, allowedOrigins cannot contain the special value "*"`。

**根因**：浏览器规范禁止「任意来源 + 携带凭证」的组合（等于把 Cookie 交给任意站点）。Spring 在 `CorsConfiguration.validateAllowCredentials()` 中主动拦截。

**解决**：改用 `setAllowedOriginPatterns(...)`（Spring 5.3+），它支持 `*` 与 `https://*.example.com` 通配，且能与 `allowCredentials(true)` 共存。本项目 `CorsConfig` 已统一用 `originPatterns`。

### 坑 2：`WebMvcConfigurer#addCorsMappings` 下 404 / 异常响应没有 CORS 头

**现象**：接口正常时跨域没问题，但请求一个不存在路径，控制台只报 CORS 错误，看不到真正的 404。

**根因**：`addCorsMappings` 的 CORS 处理发生在 `DispatcherServlet` 内部（找到处理器之后），没匹配到 handler 的请求走不到该逻辑，响应自然不带 `Access-Control-Allow-Origin`，于是被浏览器当成「跨域失败」。

**解决**：改用 `CorsFilter` + `FilterRegistrationBean`，并设 `Ordered.HIGHEST_PRECEDENCE`——过滤器在 Servlet 之前执行，预检直接返回，404 / 异常响应同样带 CORS 头。若将来引入 Spring Security，CorsFilter 必须在 Security 过滤器链之前，或者在 Security 中开 `http.cors()` 并复用同一个 `CorsConfigurationSource`。

**验证方式**（本项目实测）：
```powershell
# 预检：200 + Allow-Origin / Allow-Methods / Max-Age
Invoke-WebRequest 'http://localhost:8123/api/users/page' -Method Options `
  -Headers @{'Origin'='http://localhost:5173';'Access-Control-Request-Method'='GET'} -UseBasicParsing
# 404 也带 CORS 头
Invoke-WebRequest 'http://localhost:8123/api/not-exist' -Headers @{'Origin'='http://localhost:5173'} -UseBasicParsing
```

**扩展坑**：`UrlBasedCorsConfigurationSource` 匹配的是 **servletPath**（不含 `context-path=/api`），所以注册 `/**` 即可覆盖全部接口，写成 `/api/**` 反而全部不生效。

---

## P-19 生成器 `setLogicDeleteColumn` 按列名精确匹配，写错时静默不生效

**现象**：`StrategyConfig.setLogicDeleteColumn("is_delete")` 或写成 `"ISDELETE"` 后，生成的实体没有任何逻辑删除标记，也没有任何报错或警告。

**根因**：MyBatis-Flex 1.11.0 的 `StrategyConfig#getColumnConfig(table, column)` 内部用
`columnName.equals(logicDeleteColumn)` 做**区分大小写的精确比较**，既不忽略大小写，也不做驼峰/下划线转换；不相等时只是不设置
`logicDelete`，属于静默失败。

**解决**：传入与数据库完全一致的列名（本项目 `sql/create_table.sql` 写作 `isDelete`，生成器里也直接写 `"isDelete"`
）；若担心库间大小写差异，改用显式列配置：

```java
strategyConfig.setColumnConfig("user",
                               ColumnConfig.create().

setColumnName("isDelete").

setLogicDelete(true));
```

**验证方式**：不落盘，直接看生成器解析出的列元数据：

```java
new Generator(dataSource, globalConfig).getTables()
    .forEach(t -> t.getColumns().forEach(c ->
        System.out.println(t.getName() + "." + c.getName() + " logicDelete="
            + c.getColumnConfig().getLogicDelete())));
// 期望三张表的 isDelete 均输出 logicDelete=true
```

**附带注意**：生成器用 Hutool `YamlUtil.loadByPath("application.yml")` 读取 classpath 资源，实际读的是 `target/classes`
里的副本；改完 `src/main/resources/application.yml` 未重新构建时会连旧库（路径解析规则见 P-20）。

---

## P-20 Hutool 按路径读文件时以 classpath 为基准，传相对文件路径会失败

**现象**：

- `YamlUtil.loadByPath("src/main/resources/application.yml")` 抛 `NoResourceException`；
- `FileUtil.getReader("src/main/resources/application.yml", UTF_8)` 抛 `FileNotFoundException`，报错路径变成
  `target/classes/src/main/resources/application.yml`。

**根因**：Hutool 的 `ResourceUtil.getResourceObj` / `FileUtil` 的 String 重载把入参当 **classpath 资源名**
解析，不会按进程工作目录（项目根）找文件；只有 classpath 中不存在时才回落到文件路径，而回落时仍以 classpath 根为基准拼接。

**解决**：

- 读 classpath 资源：`YamlUtil.loadByPath("application.yml")`（项目生成器采用此写法，要求 `target/classes` 已同步）；
- 必须读源文件时：用 JDK API 明确以工作目录为基准，如
  `Files.newBufferedReader(Path.of("src/main/resources/application.yml"))`。

**验证方式**：同一目录下分别运行上述两种写法，前者能打印出 `url/username/password`，后者抛异常；换成 `application.yml` 作为
`loadByPath` 入参后返回正常。

---

## P-21 Spring Boot 4 下 Mapper Bean 不存在（starter 版本与 JDBC 依赖）

**现象**：启动报
`NoSuchBeanDefinitionException: No qualifying bean of type 'com.zh.zhaicodempther.mapper.AppMapper' available`，
而 `AppMapper` 只是个继承 `BaseMapper<App>` 的空接口，堆栈底层落在 `AppServiceImpl` 继承的 `ServiceImpl` 字段 `mapper`
的注入上。

**根因（两个叠在一起）**：

1. `pom.xml` 只声明了 `mysql-connector-j` + `HikariCP`，没有 `spring-boot-starter-jdbc`。Boot 4 把 JDBC 自动配置拆到
   `spring-boot-jdbc` 模块（`org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`），缺该 starter 就没有
   `DataSource`、没有 `spring-jdbc` / `spring-tx`，MyBatis-Flex 自动配置的前置条件不满足，Mapper 扫描根本不启动。
2. `mybatis-flex-spring-boot3-starter:1.11.0` 的 `MybatisFlexAutoConfiguration` 用
   `@AutoConfigureAfter(org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class)` 引用 Boot 3
   的旧包名，Boot 4 中该类已迁移，排序失效。补上 JDBC starter 后 Mapper Bean 虽被扫描注册，但依赖 `DataSource` 的
   `sqlSessionFactory` Bean 未创建，报错变为 `Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required`。它传递的
   `mybatis-spring:3.0.4` 也是 Spring 6 世代。

**解决**：

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
<groupId>com.mybatis-flex</groupId>
<artifactId>mybatis-flex-spring-boot4-starter</artifactId>
<version>1.11.8</version>
</dependency>
```

- MyBatis-Flex 自 `1.11.5` 起提供 Boot 4 专用 starter，自动配置在 `com.mybatisflex.spring.boot.v4` 包，
  `@AutoConfigureAfter` 指向 `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`，传递
  `mybatis-spring:4.0.0`。
- `mybatis-flex-codegen` 同步升到同一版本，避免与 `mybatis-flex-core` 跨版本混用。

**验证方式**：

```powershell
$env:JAVA_HOME='D:\Java\jdk21'
.\mvnw.cmd -B -o clean compile          # BUILD SUCCESS
.\mvnw.cmd -B -o dependency:tree | Select-String 'mybatis|starter-jdbc|spring-jdbc'
# 期望：mybatis-flex-spring-boot4-starter:1.11.8、mybatis-spring:4.0.0、spring-boot-starter-jdbc:4.1.2
.\mvnw.cmd -B spring-boot:run
# 期望：打印 MyBatisFlex v1.11.8 banner，appMapper / appServiceImpl 正常创建
```

**附带注意**：依赖修好后暴露出另一个阻塞启动的问题——生成的 `UserController` 与示例 `UsersController` 都映射
`GET /user/page`，报 `Ambiguous mapping`；示例接口已改到 `/users/page`
（见 [file-class-reference.md](file-class-reference.md) 3.10.1）。

**重新评估条件**：回到 Spring Boot 3.x 应改回 `mybatis-flex-spring-boot3-starter`；若暂时无法升级 starter，可自建
`@MapperScan` + `FlexSqlSessionFactoryBean` 手动配置兜底（官方 starter 的自动配置思路同样可照搬）。

---

## P-22 `QueryWrapper` 字符串列名写成下划线，报 `Unknown column 'user_account'`

**现象**：调用注册接口时抛
`java.sql.SQLSyntaxErrorException: Unknown column 'user_account' in 'where clause'`。

**根因**：`UserServiceImpl#register` 中写了 `queryWrapper.eq("user_account", userAccount)`。字符串形式的列名会**原样**
拼进 SQL，不经过实体映射；而 `sql/create_table.sql` 的列名是驼峰 `userAccount`（实体也是 `@Column("userAccount")`），库中不存在
`user_account` 列。

**解决**：改用方法引用，由 MyBatis-Flex 按实体 `@Column` 解析真实列名：

```java
queryWrapper.eq(User::getUserAccount, userAccount);
```

**适用边界**：本项目所有表均为驼峰列名（`userId`、`isDelete`、`createTime` 等），凡是 `QueryWrapper` / `orderBy` 中手写字符串列名的地方都有同样风险，
优先使用 `User::getXxx` 方法引用或 APT 生成的 `USER.USER_ACCOUNT` 表定义。

---

## P-23 Hutool `JSONUtil` 序列化 record 得到 `{}`，日志切面返回值为空

**现象**：`ControllerLogAspect` 初版用 `JSONUtil.toJsonStr` 序列化返回值，`GET /api/demo/page` 的日志为 `返回: {}`，而接口
实际响应正常。

**根因**：`Result` / `PageResult` 是 Java `record`，访问器是 `code()` 而非 `getCode()`；Hutool 5.8.25 按 JavaBean getter
反射取值，识别不到 record 组件，静默输出空对象。

**解决**：切面构造器注入 Spring 容器中的 Jackson 3 `tools.jackson.databind.ObjectMapper`，用 `writeValueAsString`
序列化——与接口响应规则一致（含 `success` 字段、日期格式）。

**验证方式**：启动后调用 `GET /api/demo/page?pageNum=1&pageSize=2`，日志返回值包含 `"code":0` 与 `"total":12`。

**适用边界**：凡涉及 record（`Result`、`PageResult`、`DeleteRequest`、`UserVO`）的 JSON 序列化，都不要用 Hutool
`JSONUtil` / `BeanUtil.beanToMap`。切面只作用于 `public` 方法，`HealthController#check`（`private`）不会记日志，见 P-13。

**重新评估条件**：Hutool 升级到支持 record 的版本（如 6.x）后可重新评估，但仍建议与接口共用 Jackson。
