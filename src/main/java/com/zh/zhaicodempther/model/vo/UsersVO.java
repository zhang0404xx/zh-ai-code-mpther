package com.zh.zhaicodempther.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 用户视图对象（示例用）
 *
 * @param id        用户 ID
 * @param name      用户名
 * @param email     邮箱
 * @param status    状态：启用 / 禁用
 * @param createdAt 创建时间
 */
@Schema(description = "用户信息")
public record UsersVO(
        @Schema(description = "用户 ID", example = "1") Long id,
        @Schema(description = "用户名", example = "用户1") String name,
        @Schema(description = "邮箱", example = "user1@example.com") String email,
        @Schema(description = "状态", example = "启用") String status,
        @Schema(description = "创建时间", example = "2026-01-02 00:00:00") LocalDateTime createdAt) {
}
