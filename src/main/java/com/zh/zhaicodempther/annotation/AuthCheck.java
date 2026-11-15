package com.zh.zhaicodempther.annotation;

import com.zh.zhaicodempther.model.enums.UserRoleEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 * <p>
 * 标注在 Controller 方法或类上，由 {@code AuthCheckAspect} 校验当前登录用户角色；
 * 方法上的注解优先于类上的注解。角色按 {@link UserRoleEnum#getLevel()} 分级，高等级角色自动拥有低等级权限。
 *
 * <pre>
 * &#64;AuthCheck                                  // 登录即可访问
 * &#64;AuthCheck(mustRole = UserRoleEnum.ADMIN)   // 仅管理员可访问
 * </pre>
 *
 * @author zh
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthCheck {

    /**
     * 访问所需的最低角色，默认普通用户（即登录即可）
     */
    UserRoleEnum mustRole() default UserRoleEnum.USER;
}
