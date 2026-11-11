package com.zh.zhaicodempther.controller;

import com.zh.zhaicodempther.common.DeleteRequest;
import com.zh.zhaicodempther.common.PageQuery;
import com.zh.zhaicodempther.common.PageResult;
import com.zh.zhaicodempther.common.Result;
import com.zh.zhaicodempther.exception.BizException;
import com.zh.zhaicodempther.exception.ErrorCode;
import com.zh.zhaicodempther.model.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 示例控制器：分页请求、删除请求、异常捕获三段参考写法
 * <p>
 * 每一段都可以整体复制到业务模块，只需把 MOCK_DATA 换成 Service 查询结果。
 * 数据为内存假数据，不涉及持久层。
 */
@Slf4j
@Tag(name = "Demo 示例", description = "分页 / 删除 / 异常处理的参考写法，可直接复制改造")
@RestController
@RequestMapping("/demo")
public class DemoController {

    /** 假数据：12 条示例记录 */
    private static final List<UserVO> MOCK_DATA = IntStream.rangeClosed(1, 12)
            .mapToObj(i -> new UserVO(
                    (long) i,
                    "示例" + i,
                    "demo" + i + "@example.com",
                    i % 2 == 0 ? "启用" : "禁用",
                    LocalDateTime.of(2026, 1, 1, 0, 0).plusDays(i)))
            .toList();

    /* ==================== 一、分页请求 demo ==================== */

    /**
     * 分页三步走：
     * <ol>
     *     <li>入参用 {@link PageQuery}，页码/页大小越界已在 getter 里纠正，控制器不写任何判断</li>
     *     <li>取数用 {@code offset()} / {@code limit()}（真实项目换成 service 返回 total + list）</li>
     *     <li>出参用 {@code PageResult.of(list, total, query)} 包装，再套 {@code Result.ok()}</li>
     * </ol>
     * 关键点：{@code @ParameterObject} 让分页字段在 Knife4j 里展开成三个独立输入框。
     */
    @Operation(
            summary = "分页请求示例",
            description = """
                    分页三段式：
                    1. PageQuery 接参（pageNum / pageSize / orderBy 已自动纠正越界）
                    2. offset() + limit() 取当前页数据（真实项目替换为 service 查询）
                    3. PageResult.of(list, total, query) 包装后返回
                    """
    )
    @GetMapping("/page")
    public Result<PageResult<UserVO>> page(@ParameterObject PageQuery query) {
        long total = MOCK_DATA.size();
        int from = (int) Math.min(query.offset(), total);
        int to = (int) Math.min((long) from + query.limit(), total);

        return Result.ok(PageResult.of(MOCK_DATA.subList(from, to), total, query));
    }

    /* ==================== 二、删除请求 demo ==================== */

    /**
     * 删除请求 demo，同时演示「参数校验失败」的异常分支：
     * <ul>
     *     <li>ids 为空 / 缺字段 → 400，提示「待删除的 ID 列表不能为空」</li>
     *     <li>ids 含 0 或负数 → 400，提示「ID 必须为正整数」</li>
     *     <li>ids 超过 100 条 → 400，提示「单次最多删除 100 条」</li>
     * </ul>
     * 以上全部由 {@code @Valid} + GlobalExceptionHandler 完成，方法体内不需要任何判空。
     * 部分网关或客户端不支持带 body 的 DELETE，可把注解换成 {@code @PostMapping("/delete")}。
     */
    @Operation(
            summary = "删除示例（含参数校验失败分支）",
            description = """
                    请求体 DeleteRequest 已封装：
                    - ids 为空 / 缺字段 → 400
                    - ids 含 0 或负数 → 400
                    - ids 超过 100 条 → 400
                    - ids 重复会自动去重，返回的 data 为去重后的条数
                    """
    )
    @DeleteMapping("/delete")
    public Result<Integer> delete(@RequestBody @Valid DeleteRequest request) {
        // 单条与批量共用一个入口，用 isBatch() 区分即可
        if (request.isBatch()) {
            log.info("批量删除 {} 条，原因：{}", request.ids().size(), request.reason());
        } else {
            log.info("删除单条 id={}，原因：{}", request.firstId(), request.reason());
        }
        return Result.ok(request.ids().size());
    }

    /* ==================== 三、异常捕获 demo ==================== */

    /**
     * 场景一：可预期的业务错误，直接抛 {@link BizException}，不要 try-catch
     */
    @Operation(
            summary = "1) 业务异常：直接抛，交给全局处理器",
            description = "找不到数据时 throw new BizException(ErrorCode.DATA_NOT_FOUND, ...)，返回 10001"
    )
    @GetMapping("/exception/biz")
    public Result<UserVO> bizException(@RequestParam long id) {
        return MOCK_DATA.stream()
                .filter(user -> user.id() == id)
                .findFirst()
                .map(Result::ok)
                .orElseThrow(() -> new BizException(ErrorCode.DATA_NOT_FOUND, "用户不存在，id=" + id));
    }

    /**
     * 场景二：底层/第三方异常需要转换，catch 后转成 {@link BizException}
     * <p>
     * 关键：第三个参数传入原始异常，保留 cause，否则日志里只剩下「调用远程服务失败」，
     * 真实的超时 / 序列化异常堆栈丢失，线上无法排查。
     */
    @Operation(
            summary = "2) 第三方异常：catch 后转成业务异常",
            description = "捕获底层异常并转为 BizException，务必把原始异常作为 cause 传入"
    )
    @GetMapping("/exception/wrap")
    public Result<String> wrapException() {
        try {
            return Result.ok(callRemoteService());
        } catch (IllegalStateException e) {
            // 不要把 e.getMessage() 直接透给前端——它可能包含内部主机名、SQL 片段等信息
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "调用远程服务失败，请稍后重试", e);
        }
    }

    /**
     * 场景三：未预期异常不要 catch，交给兜底 handler
     * <p>
     * 兜底 handler 会以 error 级别打印完整堆栈，对外只返回「系统内部异常」，
     * 既不丢排查线索，也不泄露实现细节。
     */
    @Operation(
            summary = "3) 未预期异常：不要 catch，交给兜底 handler",
            description = "index 越界触发 IndexOutOfBoundsException，返回 50000 且不泄露堆栈"
    )
    @GetMapping("/exception/unexpected")
    public Result<UserVO> unexpectedException(@RequestParam(defaultValue = "0") int index) {
        // 故意越界，模拟未预期的运行时异常
        return Result.ok(MOCK_DATA.get(index));
    }

    /**
     * 模拟远程调用失败
     */
    private String callRemoteService() {
        throw new IllegalStateException("remote call timeout after 3000ms");
    }
}
