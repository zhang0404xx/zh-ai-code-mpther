package com.zh.zhaicodempther.core;

import com.zh.zhaicodempther.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.List;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File dir = aiCodeGeneratorFacade.generateAndSaveCode("做个zh的登录页面，尽量不超过20行", CodeGenTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(dir);
        Assertions.assertTrue(new File(dir, "index.html").isFile());
    }

    @Test
    void testGenerateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("做个zh的登录页面，尽量不超过20行", CodeGenTypeEnum.HTML);
        Assertions.assertNotNull(file);
    }

    @Test
    void generateAndSaveHtmlCodeStream() {
        // 阻塞等待流结束（结束时触发解析与保存），仅用于测试
        List<String> chunks = aiCodeGeneratorFacade.generateAndSaveCodeStream("做个zh的登录页面，尽量不超过20行", CodeGenTypeEnum.HTML)
                .collectList()
                .block();
        Assertions.assertNotNull(chunks);
        Assertions.assertFalse(String.join("", chunks).isBlank());
    }

    @Test
    void generateAndSaveMultiFileCodeStream() {
        List<String> chunks = aiCodeGeneratorFacade.generateAndSaveCodeStream("做个zh的登录页面，尽量不超过20行", CodeGenTypeEnum.MULTI_FILE)
                .collectList()
                .block();
        Assertions.assertNotNull(chunks);
        Assertions.assertFalse(String.join("", chunks).isBlank());
    }
}
