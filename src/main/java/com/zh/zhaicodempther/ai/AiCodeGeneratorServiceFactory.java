package com.zh.zhaicodempther.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 代码生成服务工厂
 * ChatModel 由 langchain4j-open-ai-spring-boot4-starter 按 langchain4j.open-ai.chat-model.* 自动装配
 * StreamingChatModel 按 langchain4j.open-ai.streaming-chat-model.* 自动装配，供返回 Flux 的流式方法使用
 */
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    /**
     * 创建 AI 代码生成服务（LangChain4j 为接口生成动态代理）
     * @return AiCodeGeneratorService
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}
