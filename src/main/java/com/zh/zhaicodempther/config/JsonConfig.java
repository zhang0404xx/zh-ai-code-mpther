package com.zh.zhaicodempther.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * 全局 JSON 配置
 * <p>
 * Long 统一序列化为字符串：雪花 ID 超出 JS Number 安全整数范围（2^53-1），
 * 以数字返回会被前端四舍五入，回传后查不到数据。
 * 反序列化无需处理，Jackson 默认可将字符串转回 Long。
 */
@Configuration
public class JsonConfig {

    @Bean
    public JsonMapperBuilderCustomizer longToStringCustomizer() {
        SimpleModule module = new SimpleModule("LongToStringModule");
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        return builder -> builder.addModule(module);
    }
}
