package com.zh.zhaicodempther.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.regex.Pattern;

/**
 * 分页请求封装
 * <p>
 * 两种用法：
 * <ol>
 *     <li>直接作为接口入参：{@code GET /api/user/page?pageNum=1&pageSize=20&orderBy=create_time desc}</li>
 *     <li>作为请求体的父类或字段：{@code class UserQuery extends PageQuery} / {@code record UserQuery(PageQuery page, String keyword)}</li>
 * </ol>
 * 页码与页大小越界时读取阶段自动纠正，业务代码无需重复判断。
 */
@Data
public class PageQuery {

    /** 默认页码 */
    public static final int DEFAULT_PAGE_NUM = 1;

    /** 默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 每页最大条数，避免一次查询拖垮数据库 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 排序表达式的字符白名单：只保留字母、数字、下划线、空格、逗号和点 */
    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[^A-Za-z0-9_ ,.]");

    @Schema(description = "页码，从 1 开始，小于 1 时按 1 处理", defaultValue = "1", example = "1")
    private Integer pageNum = DEFAULT_PAGE_NUM;

    @Schema(description = "每页条数，取值 1~100，越界时自动纠正", defaultValue = "10", example = "10")
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    @Schema(description = "排序表达式，如 create_time desc；非法字符会被剔除", example = "create_time desc")
    private String orderBy;

    public Integer getPageNum() {
        return (pageNum == null || pageNum < 1) ? DEFAULT_PAGE_NUM : pageNum;
    }

    public Integer getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 排序表达式，参数为空或清洗后为空时返回 null
     * <p>
     * 注意：这里只做字符级过滤，用于拼接前仍建议按实体字段做白名单校验
     */
    public String getOrderBy() {
        if (orderBy == null || orderBy.isBlank()) {
            return null;
        }
        String sanitized = ILLEGAL_CHARS.matcher(orderBy).replaceAll("").trim();
        return sanitized.isEmpty() ? null : sanitized;
    }

    /**
     * 数据库分页偏移量，即 (pageNum - 1) * pageSize
     */
    public long offset() {
        return (long) (getPageNum() - 1) * getPageSize();
    }

    /**
     * 每页条数，{@link #getPageSize()} 的语义化别名
     */
    public int limit() {
        return getPageSize();
    }
}
