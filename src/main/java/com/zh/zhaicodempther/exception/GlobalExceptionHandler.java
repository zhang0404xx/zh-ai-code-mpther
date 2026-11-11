package com.zh.zhaicodempther.exception;

import com.zh.zhaicodempther.common.Result;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一把异常转换为 {@link Result} 响应体，HTTP 状态码与业务码保持一致：
 * 4xx/5xx 使用标准状态码，业务码（1xxx）统一返回 400。
 */
@Hidden
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：由业务代码主动抛出，属于预期内异常，用 warn 级别记录
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return build(e.getErrorCode().getCode(), e.getMessage());
    }

    /**
     * 请求体参数校验失败（@RequestBody + @Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return buildBadRequest(message);
    }

    /**
     * 表单/Query 对象绑定校验失败
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return buildBadRequest(message);
    }

    /**
     * 方法参数校验失败（@RequestParam / @PathVariable + 校验注解）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return buildBadRequest(message);
    }

    /**
     * 缺少必填请求参数、参数类型不匹配、请求体不可读
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<Result<Void>> handleBadRequest(Exception e) {
        log.warn("请求参数异常: {}", e.getMessage());
        return buildBadRequest(e.getMessage());
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return build(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 请求媒体类型不支持
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("媒体类型不支持: {}", e.getMessage());
        return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * 静态资源/接口路径不存在
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getMessage());
        return build(ErrorCode.NOT_FOUND);
    }

    /**
     * 兜底处理：未预期的异常，记录完整堆栈
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return build(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 格式化字段错误信息
     * 将字段名和对应的错误消息组合成一个格式化的字符串
     *
     * @param fieldError 包含字段错误信息的FieldError对象
     * @return 返回格式化后的错误信息字符串，格式为"字段名: 错误消息"
     */
    private String formatFieldError(FieldError fieldError) {
    // 获取字段名并拼接默认错误消息
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    /**
     * 构建一个表示"错误请求"(Bad Request)的响应实体
     * @param message 自定义的错误信息，如果为null或空字符串，则使用默认的错误信息
     * @return 返回一个包含错误代码和错误信息的ResponseEntity对象
     */
    private ResponseEntity<Result<Void>> buildBadRequest(String message) {
    // 检查输入的消息是否为null或空字符串
        String finalMessage = (message == null || message.isBlank())
            // 如果为null或空，则使用错误代码中定义的默认消息
                ? ErrorCode.BAD_REQUEST.getMessage()
            // 否则使用传入的自定义消息
                : message;
    // 构建并返回包含错误代码和最终消息的响应实体
        return build(ErrorCode.BAD_REQUEST.getCode(), finalMessage);
    }

    /**
     * 构建响应实体方法
     * 根据错误代码和错误信息构建一个响应实体
     * @param errorCode 错误代码枚举，包含错误代码和错误信息
     * @return 返回一个包含错误代码和错误信息的响应实体
     */
    private ResponseEntity<Result<Void>> build(ErrorCode errorCode) {
    // 调用另一个重载的build方法，传入错误代码和错误信息
        return build(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 构建一个带有状态码和消息的ResponseEntity
     * @param code HTTP状态码
     * @param message 返回的消息内容
     * @return 包含状态码和消息的ResponseEntity对象
     */
    private ResponseEntity<Result<Void>> build(int code, String message) {
    // 根据状态码解析对应的HttpStatus
        HttpStatus status = HttpStatus.resolve(code);
    // 返回ResponseEntity，如果解析的status为null则使用BAD_REQUEST作为默认状态
        return ResponseEntity
                .status(status != null ? status : HttpStatus.BAD_REQUEST)
                .body(Result.fail(code, message));  // 设置响应体为Result.fail对象
    }
}
