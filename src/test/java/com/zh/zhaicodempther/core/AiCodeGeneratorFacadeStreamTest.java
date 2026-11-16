package com.zh.zhaicodempther.core;

import cn.hutool.core.io.FileUtil;
import com.zh.zhaicodempther.ai.AiCodeGeneratorService;
import com.zh.zhaicodempther.constant.AppConstant;
import com.zh.zhaicodempther.exception.BizException;
import com.zh.zhaicodempther.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 门面流式生成单元测试：模拟 AI 服务返回的流式片段，不调用真实模型
 */
@ExtendWith(MockitoExtension.class)
class AiCodeGeneratorFacadeStreamTest {

    private static final String PROMPT = "做个zh的登录页面";

    @Mock
    private AiCodeGeneratorService aiCodeGeneratorService;

    @InjectMocks
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    /**
     * 测试前已存在的输出目录，用于识别本次测试新建的目录
     */
    private Set<String> dirsBefore;

    /**
     * 本次测试新建的目录，测试结束后清理
     */
    private final List<File> createdDirs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        dirsBefore = listOutputDirs();
    }

    @AfterEach
    void tearDown() {
        createdDirs.forEach(FileUtil::del);
    }

    @Test
    void htmlStream_passThroughChunksAndSaveParsedHtml() {
        // 代码块标记被拆在两个片段之间，模拟真实流式输出
        List<String> chunks = List.of(
                "这是一个登录页。\n```ht",
                "ml\n<!DOCTYPE html>\n<html><body>登录</body></html>\n",
                "```");
        Mockito.when(aiCodeGeneratorService.generateHtmlCodeStream(PROMPT)).thenReturn(Flux.fromIterable(chunks));

        List<String> received = aiCodeGeneratorFacade.generateAndSaveCodeStream(PROMPT, CodeGenTypeEnum.HTML)
                .collectList()
                .block();

        // 片段原样透传
        Assertions.assertEquals(chunks, received);
        // 流结束后解析并落盘，index.html 只含代码块内容
        File dir = findSingleNewDir(CodeGenTypeEnum.HTML);
        Assertions.assertEquals("<!DOCTYPE html>\n<html><body>登录</body></html>", readFile(dir, "index.html"));
        Mockito.verify(aiCodeGeneratorService, Mockito.never()).generateHtmlCode(Mockito.anyString());
    }

    @Test
    void multiFileStream_passThroughChunksAndSaveThreeFiles() {
        List<String> chunks = List.of(
                "```html\n<link rel=\"stylesheet\" href=\"style.css\">\n```\n",
                "```css\nbody{margin:0}\n```\n```javascript\nconsole.log('hi');\n",
                "```\n以上是登录页代码");
        Mockito.when(aiCodeGeneratorService.generateMultiFileCodeStream(PROMPT)).thenReturn(Flux.fromIterable(chunks));

        List<String> received = aiCodeGeneratorFacade.generateAndSaveCodeStream(PROMPT, CodeGenTypeEnum.MULTI_FILE)
                .collectList()
                .block();

        Assertions.assertEquals(chunks, received);
        File dir = findSingleNewDir(CodeGenTypeEnum.MULTI_FILE);
        Assertions.assertEquals("<link rel=\"stylesheet\" href=\"style.css\">", readFile(dir, "index.html"));
        Assertions.assertEquals("body{margin:0}", readFile(dir, "style.css"));
        Assertions.assertEquals("console.log('hi');", readFile(dir, "script.js"));
    }

    @Test
    void htmlStream_errorPropagatedAndNothingSaved() {
        Mockito.when(aiCodeGeneratorService.generateHtmlCodeStream(PROMPT))
                .thenReturn(Flux.concat(Flux.just("```html\n<html>"), Flux.error(new IllegalStateException("模型中断"))));

        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
                () -> aiCodeGeneratorFacade.generateAndSaveCodeStream(PROMPT, CodeGenTypeEnum.HTML).blockLast());

        Assertions.assertEquals("模型中断", e.getMessage());
        Assertions.assertTrue(newDirs(CodeGenTypeEnum.HTML).isEmpty(), "流异常结束时不应落盘");
    }

    @Test
    void stream_notSubscribed_modelNotCalled() {
        // 只构建不订阅：Flux.defer 延迟到订阅时才调用 AI 服务
        aiCodeGeneratorFacade.generateAndSaveCodeStream(PROMPT, CodeGenTypeEnum.MULTI_FILE);

        Mockito.verifyNoInteractions(aiCodeGeneratorService);
    }

    @Test
    void stream_nullType_throwsBizException() {
        Assertions.assertThrows(BizException.class,
                () -> aiCodeGeneratorFacade.generateAndSaveCodeStream(PROMPT, null));
        Mockito.verifyNoInteractions(aiCodeGeneratorService);
    }

    private Set<String> listOutputDirs() {
        File[] dirs = new File(AppConstant.CODE_OUTPUT_ROOT_DIR).listFiles(File::isDirectory);
        return dirs == null ? new HashSet<>() : Arrays.stream(dirs).map(File::getName).collect(Collectors.toSet());
    }

    private List<File> newDirs(CodeGenTypeEnum type) {
        String prefix = type.getValue() + "_";
        return listOutputDirs().stream()
                .filter(name -> !dirsBefore.contains(name) && name.startsWith(prefix))
                .map(name -> new File(AppConstant.CODE_OUTPUT_ROOT_DIR, name))
                .toList();
    }

    private File findSingleNewDir(CodeGenTypeEnum type) {
        List<File> dirs = newDirs(type);
        createdDirs.addAll(dirs);
        Assertions.assertEquals(1, dirs.size(), "应新建且仅新建一个输出目录");
        return dirs.get(0);
    }

    private String readFile(File dir, String filename) {
        File file = new File(dir, filename);
        Assertions.assertTrue(file.isFile(), filename + " 未生成");
        return FileUtil.readString(file, StandardCharsets.UTF_8);
    }
}
