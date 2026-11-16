package com.zh.zhaicodempther.ai;

import com.zh.zhaicodempther.ai.model.HtmlCodeResult;
import com.zh.zhaicodempther.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做个zh的博客，尽量不超过20行");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("做个zh的留言板，尽量不超过50行");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateHtmlCodeStream() {
        // 阻塞收集全部片段，仅用于测试
        List<String> chunks = aiCodeGeneratorService.generateHtmlCodeStream("做个zh的博客，尽量不超过20行")
                .collectList()
                .block();
        Assertions.assertNotNull(chunks);
        Assertions.assertFalse(String.join("", chunks).isBlank());
    }

    @Test
    void generateMultiFileCodeStream() {
        List<String> chunks = aiCodeGeneratorService.generateMultiFileCodeStream("做个zh的留言板，尽量不超过50行")
                .collectList()
                .block();
        Assertions.assertNotNull(chunks);
        Assertions.assertFalse(String.join("", chunks).isBlank());
    }
}