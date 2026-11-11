package com.zh.zhaicodempther.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 分页结果封装，与 {@link PageQuery} 配套
 * <p>
 * 响应示例：
 * <pre>
 * {
 *   "code": 0,
 *   "message": "成功",
 *   "data": { "total": 128, "pageNum": 1, "pageSize": 10, "list": [ ... ] }
 * }
 * </pre>
 *
 * @param total    总记录数
 * @param pageNum  当前页码
 * @param pageSize 每页条数
 * @param list     当前页数据，无数据时为空集合而非 null
 */
@Schema(description = "分页结果")
public record PageResult<T>(
        @Schema(description = "总记录数", example = "128") long total,
        @Schema(description = "当前页码", example = "1") int pageNum,
        @Schema(description = "每页条数", example = "10") int pageSize,
        @Schema(description = "当前页数据") List<T> list) {

    public PageResult {
        list = list == null ? List.of() : List.copyOf(list);
    }

    /**
     * 由查询条件与结果集构建，页码与页大小取自入参，保证与请求一致
     */
    public static <T> PageResult<T> of(List<T> list, long total, PageQuery query) {
        return new PageResult<>(total, query.getPageNum(), query.getPageSize(), list);
    }

    /**
     * 手动指定分页参数时使用（例如后端强制改写页大小）
     */
    public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
        return new PageResult<>(total, pageNum, pageSize, list);
    }

    /**
     * 空结果，保留请求的分页参数，便于前端统一渲染分页条
     */
    public static <T> PageResult<T> empty(PageQuery query) {
        return new PageResult<>(0L, query.getPageNum(), query.getPageSize(), List.of());
    }

    /**
     * 总页数
     */
    public long totalPages() {
        return pageSize <= 0 ? 0L : (total + pageSize - 1) / pageSize;
    }
}
