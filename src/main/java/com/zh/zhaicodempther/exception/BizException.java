package com.zh.zhaicodempther.exception;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 由业务代码主动抛出，携带 {@link ErrorCode}，由全局异常处理器统一转换为响应体。
 * 例：throw new BizException(ErrorCode.DATA_NOT_FOUND);
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode; // 错误码对象，用于标识具体的错误类型

    /**
     * 构造函数1：使用默认的错误信息
     * @param errorCode 错误码对象，包含错误类型和默认错误信息
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // 调用父类构造函数，设置错误信息为错误码中的默认信息
        this.errorCode = errorCode; // 设置错误码
    }

    /**
     * 使用自定义提示覆盖 ErrorCode 中的默认文案
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 自定义业务异常类的构造方法
     * @param errorCode 错误码对象，包含错误码和错误信息
     * @param message 异常的详细描述信息
     * @param cause 异常的根本原因，通常用于异常链
     */
    public BizException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);  // 调用父类（Exception）的构造方法，初始化异常信息和原因
        this.errorCode = errorCode;  // 初始化错误码属性
    }

    /**
     * 获取错误代码的方法
     * @return 返回错误代码对象，包含错误码相关信息
     */
    public ErrorCode getErrorCode() {
        return errorCode; // 返回错误码对象
    }
}
