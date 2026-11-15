package com.zh.zhaicodempther.controller;

import com.zh.zhaicodempther.common.PageQuery;
import com.zh.zhaicodempther.common.PageResult;
import com.zh.zhaicodempther.common.Result;
import com.zh.zhaicodempther.model.vo.UsersVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 用户示例接口
 * <p>
 * 演示 {@link PageQuery} + {@link PageResult} + {@link Result} 的完整链路，
 * 数据为内存中的假数据，不涉及持久层。真实实现请替换为 Service 查询。
 * <p>
 * 路径使用复数 {@code /users}，避免与生成的 CRUD 控制器 {@link UserController}（{@code /user}）冲突。
 */
@Tag(name = "用户管理", description = "示例接口，数据为内存假数据")
@RestController
@RequestMapping("/users")
public class UsersController {

    /** 假数据：30 条用户 */
    private static final List<UsersVO> MOCK_USERS = IntStream.rangeClosed(1, 30)
            .mapToObj(i -> new UsersVO(
                    (long) i,
                    "用户" + i,
                    "user" + i + "@example.com",
                    i % 2 == 0 ? "启用" : "禁用",
                    LocalDateTime.of(2026, 1, 1, 0, 0).plusDays(i)))
            .toList();

    @Operation(
            summary = "分页查询用户",
            description = """
                    演示分页链路：
                    1. pageNum / pageSize 越界会被 PageQuery 自动纠正（页大小上限 100）
                    2. 响应体为 Result<PageResult<UserVO>>
                    3. keyword 为空时返回全部数据
                    """
    )
    @GetMapping("/page")
    public Result<PageResult<UsersVO>> page(
            @ParameterObject PageQuery query,
            @Parameter(description = "用户名关键字，模糊匹配", example = "用户1")
            @RequestParam(required = false) String keyword) {

        List<UsersVO> filtered = (keyword == null || keyword.isBlank())
                ? MOCK_USERS
                : MOCK_USERS.stream()
                        .filter(user -> user.name().contains(keyword.trim()))
                        .toList();

        long total = filtered.size();
        int from = (int) Math.min(query.offset(), total);
        int to = (int) Math.min((long) from + query.limit(), total);

        return Result.ok(PageResult.of(filtered.subList(from, to), total, query));
    }
}
