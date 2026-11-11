package com.zh.zhaicodempther.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 删除请求封装
 * <p>
 * 统一单条与批量删除的入参形态，避免每个模块各写一套 id / ids 字段：
 * <pre>
 * DELETE /api/demo/delete
 * { "ids": [1, 2, 3], "reason": "用户主动注销" }
 * </pre>
 * 校验交给 Bean Validation：ids 为空、元素为 null 或非正数、条数超过 {@link #MAX_BATCH_SIZE}，
 * 都会由全局异常处理器转成 400 + 统一响应体，业务代码不需要判空。
 * 后端内部调用可用 {@link #of(Long...)} 快速构造。
 *
 * @param ids    待删除的 ID 列表，不能为空
 * @param reason 删除原因，可选，建议写入审计日志
 */
@Schema(description = "删除请求，支持单条与批量")
public record DeleteRequest(

        @Schema(description = "待删除的 ID 列表，不能为空，单次最多 100 条", example = "[1, 2, 3]")
        @NotEmpty(message = "待删除的 ID 列表不能为空")
        @Size(max = MAX_BATCH_SIZE, message = "单次最多删除 {max} 条")
        List<@NotNull(message = "ID 不能为 null") @Positive(message = "ID 必须为正整数") Long> ids,

        @Schema(description = "删除原因，可选，最长 200 字", example = "用户主动注销")
        @Size(max = 200, message = "删除原因最长 {max} 个字符")
        String reason) {

    /** 单次批量删除上限，防止一个请求打垮数据库 */
    public static final int MAX_BATCH_SIZE = 100;

    /**
     * 去重并转为不可变列表
     * <p>
     * ids 为 null 时保持 null，交给 {@code @NotEmpty} 判定；
     * 这里不能用 {@code List.copyOf}——它遇到 null 元素会直接抛 NPE，
     * 异常发生在校验之前，会绕过 400 变成 500。
     */
    public DeleteRequest {
        ids = ids == null ? null : ids.stream().distinct().toList();
    }

    /**
     * 后端内部构造：{@code DeleteRequest.of(1L, 2L)}
     */
    public static DeleteRequest of(Long... ids) {
        return new DeleteRequest(ids == null ? null : List.of(ids), null);
    }

    /**
     * 是否为批量删除（去重后超过 1 条），用于单条走主键删除、批量走 IN 删除
     */
    public boolean isBatch() {
        return ids != null && ids.size() > 1;
    }

    /**
     * 单条删除时的唯一 ID；批量时返回第一条，列表为空返回 null
     */
    public Long firstId() {
        return (ids == null || ids.isEmpty()) ? null : ids.getFirst();
    }
}
