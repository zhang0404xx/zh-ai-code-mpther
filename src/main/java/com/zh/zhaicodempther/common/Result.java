package com.zh.zhaicodempther.common;

import com.zh.zhaicodempther.exception.ErrorCode;

/**
 * 统一响应体
 * 这是一个泛型记录类，用于封装API接口的统一响应格式
 *
 * @param code    状态码，取值见 {@link ErrorCode}
 * @param message 提示信息
 * @param data    业务数据，失败时为 null
 */
public record Result<T>(int code, String message, T data) {

    /**
     * 返回成功的响应，不带数据
     *
     * @param <T> 数据类型
     * @return 成功的响应对象
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 返回成功的响应，带数据
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功的响应对象
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 返回失败的响应，使用错误码和默认消息
     *
     * @param errorCode 错误码
     * @param <T>       数据类型
     * @return 失败的响应对象
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 返回失败的响应，使用错误码和自定义消息
     *
     * @param errorCode 错误码
     * @param message   自定义错误消息
     * @param <T>       数据类型
     * @return 失败的响应对象
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return fail(errorCode.getCode(), message);
    }

    /**
     * 返回失败的响应，使用自定义状态码和消息
     *
     * @param code    状态码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 失败的响应对象
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 判断响应是否成功
     *
     * @return 如果状态码为成功则返回true，否则返回false
     */
    public boolean isSuccess() {
        return code == ErrorCode.SUCCESS.getCode();
    }
}
