package com.zh.zhaicodempther.aop;

import com.zh.zhaicodempther.annotation.AuthCheck;
import com.zh.zhaicodempther.exception.ErrorCode;
import com.zh.zhaicodempther.exception.ThrowUtils;
import com.zh.zhaicodempther.model.entity.User;
import com.zh.zhaicodempther.model.enums.UserRoleEnum;
import com.zh.zhaicodempther.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限校验切面
 * <p>
 * 拦截标注了 {@link AuthCheck} 的方法或类：未登录抛 {@code UNAUTHORIZED}，角色等级不足抛 {@code FORBIDDEN}，
 * 异常交给 {@code GlobalExceptionHandler} 统一转换为响应。
 * <p>
 * 执行顺序在 {@link ControllerLogAspect} 之内，鉴权失败同样会被日志切面记录。
 *
 * @author zh
 */
@Aspect
@Component
@Order(2)
public class AuthCheckAspect {

    private final UserService userService;

    public AuthCheckAspect(UserService userService) {
        this.userService = userService;
    }

    /**
     * 环绕通知：方法或类上存在 {@link AuthCheck} 时执行权限校验
     */
    @Around("@annotation(com.zh.zhaicodempther.annotation.AuthCheck) "
            + "|| @within(com.zh.zhaicodempther.annotation.AuthCheck)")
    public Object doAuthCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        AuthCheck authCheck = resolveAuthCheck(joinPoint);
        UserRoleEnum mustRole = authCheck.mustRole();
        // 1.获取当前登录用户，未登录时 getLoginUser 抛出 UNAUTHORIZED
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        User loginUser = userService.getLoginUser(request);
        // 2.校验角色等级，未知角色一律拒绝
        UserRoleEnum userRole = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        ThrowUtils.throwIf(userRole == null || !userRole.hasPermission(mustRole), ErrorCode.FORBIDDEN);
        // 3.校验通过，放行
        return joinPoint.proceed();
    }

    /**
     * 解析生效的注解：方法上的优先，其次取类上的
     */
    private AuthCheck resolveAuthCheck(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AuthCheck authCheck = AnnotatedElementUtils.findMergedAnnotation(signature.getMethod(), AuthCheck.class);
        if (authCheck == null) {
            authCheck = AnnotatedElementUtils.findMergedAnnotation(joinPoint.getTarget().getClass(), AuthCheck.class);
        }
        return authCheck;
    }
}
