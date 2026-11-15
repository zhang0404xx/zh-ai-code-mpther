package com.zh.zhaicodempther.aop;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Controller 层日志切面
 * <p>
 * 记录所有 Controller 方法的请求地址、请求参数、返回结果与执行耗时；
 * 异常只记录耗时与异常信息后原样抛出，具体响应交给 {@code GlobalExceptionHandler} 处理。
 * <p>
 * 序列化使用 Spring 容器中的 Jackson {@link ObjectMapper}，与接口实际响应规则一致（Hutool JSONUtil 无法序列化 record）。
 * 注意：{@code private} 方法无法被代理拦截，不会记录日志。
 * <p>
 * 执行顺序在权限切面之外（{@code @Order(1)}），鉴权失败也会记录。
 *
 * @author zh
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class ControllerLogAspect {

    /**
     * 日志中单段内容的最大长度，超出部分截断
     */
    private static final int MAX_LOG_LENGTH = 2000;

    /**
     * 密码类字段脱敏：匹配 JSON 中 key 含 password 的字符串值
     */
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("(\"[^\"]*(?i:password)[^\"]*\"\\s*:\\s*)\"[^\"]*\"");

    private final ObjectMapper objectMapper;

    public ControllerLogAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 切入点：controller 包及子包下所有方法
     */
    @Pointcut("execution(* com.zh.zhaicodempther.controller..*(..))")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：记录调用信息与执行耗时
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
        String requestInfo = getRequestInfo();
        String params = buildParams(signature.getParameterNames(), joinPoint.getArgs());

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("[{}] {} | 参数: {} | 返回: {} | 耗时: {} ms",
                    requestInfo, methodName, params, toLogString(result), cost);
            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("[{}] {} | 参数: {} | 异常: {}: {} | 耗时: {} ms",
                    requestInfo, methodName, params, e.getClass().getSimpleName(), e.getMessage(), cost);
            throw e;
        }
    }

    /**
     * 获取当前 HTTP 请求的方法与 URI
     */
    private String getRequestInfo() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getMethod() + " " + request.getRequestURI();
        }
        return "N/A";
    }

    /**
     * 组装参数名与参数值，跳过不可序列化的 Servlet / 文件 / 流等参数
     */
    private String buildParams(String[] names, Object[] args) {
        if (args == null || args.length == 0) {
            return "{}";
        }
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String name = names != null && i < names.length ? names[i] : "arg" + i;
            Object arg = args[i];
            if (arg instanceof MultipartFile file) {
                params.put(name, "MultipartFile(" + file.getOriginalFilename() + ", " + file.getSize() + " bytes)");
            } else if (isIgnoredArg(arg)) {
                continue;
            } else {
                params.put(name, arg);
            }
        }
        return toLogString(params);
    }

    /**
     * 判断是否为无需记录的参数
     */
    private boolean isIgnoredArg(Object arg) {
        return arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof HttpSession
                || arg instanceof BindingResult
                || arg instanceof InputStream
                || arg instanceof OutputStream;
    }

    /**
     * 对象转 JSON 字符串，并做密码脱敏与长度截断；序列化失败时退化为 toString
     */
    private String toLogString(Object obj) {
        if (obj == null) {
            return "null";
        }
        String str;
        try {
            str = obj instanceof CharSequence ? obj.toString() : objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            str = String.valueOf(obj);
        }
        str = PASSWORD_PATTERN.matcher(str).replaceAll("$1\"******\"");
        if (str.length() > MAX_LOG_LENGTH) {
            str = str.substring(0, MAX_LOG_LENGTH) + "...(共 " + str.length() + " 字符)";
        }
        return str;
    }
}
