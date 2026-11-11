package com.zh.zhaicodempther.exception;

/**
 * 统一错误码
 * <p>
 * 4xx/5xx 段与 HTTP 状态码保持一致，便于全局异常处理器直接映射响应状态；
 * 1xxx 段为业务错误码，返回时统一映射为 HTTP 400。
 */
public enum ErrorCode {

    /** 成功 */
    SUCCESS(0, "成功"),

    /** 请求参数错误 */
    BAD_REQUEST(40000, "请求参数错误"),
    /** 未认证或登录已过期 */
    UNAUTHORIZED(40100, "未认证或登录已过期"),
    /** 没有操作权限 */
    FORBIDDEN(40300, "没有操作权限"),
    /** 请求的资源不存在 */
    NOT_FOUND(40400, "请求的资源不存在"),
    /** 请求方法不支持 */
    METHOD_NOT_ALLOWED(40500, "请求方法不支持"),
    /** 不支持的媒体类型 */
    UNSUPPORTED_MEDIA_TYPE(41500, "不支持的媒体类型"),
    /** 系统内部异常 */
    INTERNAL_SERVER_ERROR(50000, "系统内部异常"),
    /** 服务暂不可用 */
    SERVICE_UNAVAILABLE(50300, "服务暂不可用"),

    /** 业务处理失败 */
    BUSINESS_ERROR(10000, "业务处理失败"),
    /** 数据不存在 */
    DATA_NOT_FOUND(10001, "数据不存在"),
    /** 数据已存在 */
    DATA_DUPLICATE(10002, "数据已存在"),
    /** 当前状态不允许该操作 */
    OPERATION_NOT_ALLOWED(10003, "当前状态不允许该操作");

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
