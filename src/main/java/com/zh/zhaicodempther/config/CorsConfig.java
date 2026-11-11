package com.zh.zhaicodempther.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 全局跨域配置
 * <p>
 * 采用 {@link CorsFilter} 而非 {@code WebMvcConfigurer#addCorsMappings}：
 * 过滤器在 DispatcherServlet 之前执行，预检请求（OPTIONS）直接返回，
 * 且 404、全局异常等没匹配到处理器的响应同样带 CORS 头，
 * 否则浏览器只会报跨域错误，掩盖真实的 404 / 500。
 * <p>
 * 参数由 {@code zh.cors.*} 注入，默认对任意来源放行，适合前后端分离的本地联调；
 * 生产环境务必把 {@code allowed-origin-patterns} 收窄到实际域名。
 */
@Configuration
public class CorsConfig {

    private final String allowedOriginPatterns;
    private final String allowedMethods;
    private final String allowedHeaders;
    private final String exposedHeaders;
    private final boolean allowCredentials;
    private final long maxAge;

    public CorsConfig(
            @Value("${zh.cors.allowed-origin-patterns:*}") String allowedOriginPatterns,
            @Value("${zh.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS,HEAD}") String allowedMethods,
            @Value("${zh.cors.allowed-headers:*}") String allowedHeaders,
            @Value("${zh.cors.exposed-headers:}") String exposedHeaders,
            @Value("${zh.cors.allow-credentials:false}") boolean allowCredentials,
            @Value("${zh.cors.max-age:3600}") long maxAge) {
        this.allowedOriginPatterns = allowedOriginPatterns;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
        this.exposedHeaders = exposedHeaders;
        this.allowCredentials = allowCredentials;
        this.maxAge = maxAge;
    }

    /**
     * 注册跨域过滤器
     * <p>
     * 必须设置最高优先级：否则请求可能先被其他过滤器（如 Spring Security）拦截，
     * 预检请求得不到应答。
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 用 originPatterns 而不是 origins：
        // setAllowedOrigins("*") 与 allowCredentials(true) 同时开启时 Spring 会直接抛异常，
        // originPatterns 支持 * / https://*.example.com 这类通配，且能与凭证共存
        config.setAllowedOriginPatterns(splitToList(allowedOriginPatterns));

        List<String> methods = splitToList(allowedMethods);
        if (!methods.isEmpty()) {
            config.setAllowedMethods(methods);
        }

        // "*" 表示回显浏览器预检请求中声明的方法/请求头
        List<String> headers = splitToList(allowedHeaders);
        if (!headers.isEmpty()) {
            config.setAllowedHeaders(headers);
        }

        // 需要前端读取的自定义响应头（如 token、分页总数）才放这里
        List<String> exposed = splitToList(exposedHeaders);
        if (!exposed.isEmpty()) {
            config.setExposedHeaders(exposed);
        }

        // 开启后浏览器才会携带 Cookie；此时 allowed-origin-patterns 不能是 *
        config.setAllowCredentials(allowCredentials);

        // 预检结果缓存秒数，减少 OPTIONS 请求次数
        config.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 匹配的是 servletPath（不含 context-path=/api），/** 覆盖全部接口
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setName("corsFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * 逗号分隔字符串转列表，自动去空格与空项
     */
    private static List<String> splitToList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
