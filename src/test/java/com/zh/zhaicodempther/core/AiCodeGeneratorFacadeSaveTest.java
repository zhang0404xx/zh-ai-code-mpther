package com.zh.zhaicodempther.core;

import cn.hutool.core.io.FileUtil;
import com.zh.zhaicodempther.ai.AiCodeGeneratorService;
import com.zh.zhaicodempther.ai.model.HtmlCodeResult;
import com.zh.zhaicodempther.ai.model.MultiFileCodeResult;
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 门面同步生成单元测试：模拟 AI 服务返回结构化结果，验证经保存执行器落盘，不调用真实模型
 */
@ExtendWith(MockitoExtension.class)
class AiCodeGeneratorFacadeSaveTest {

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
    void html_saveIndexHtml() {
        HtmlCodeResult result = new HtmlCodeResult();
        result.setHtmlCode("<!DOCTYPE html>\n<html><body>登录</body></html>");
        result.setDescription("登录页");
        Mockito.when(aiCodeGeneratorService.generateHtmlCode(PROMPT)).thenReturn(result);

        File dir = track(aiCodeGeneratorFacade.generateAndSaveCode(PROMPT, CodeGenTypeEnum.HTML));

        Assertions.assertTrue(dir.isDirectory(), "输出目录未生成");
        Assertions.assertTrue(dir.getName().startsWith(CodeGenTypeEnum.HTML.getValue() + "_"), "目录前缀应为 html_");
        Assertions.assertEquals(result.getHtmlCode(), readFile(dir, "index.html"));
        Mockito.verify(aiCodeGeneratorService, Mockito.never()).generateMultiFileCode(Mockito.anyString());
    }

    @Test
    void multiFile_saveThreeFiles() {
        MultiFileCodeResult result = new MultiFileCodeResult();
        result.setHtmlCode("<link rel=\"stylesheet\" href=\"style.css\">");
        result.setCssCode("body{margin:0}");
        result.setJsCode("console.log('hi');");
        Mockito.when(aiCodeGeneratorService.generateMultiFileCode(PROMPT)).thenReturn(result);

        File dir = track(aiCodeGeneratorFacade.generateAndSaveCode(PROMPT, CodeGenTypeEnum.MULTI_FILE));

        Assertions.assertTrue(dir.getName().startsWith(CodeGenTypeEnum.MULTI_FILE.getValue() + "_"), "目录前缀应为 multi_file_");
        Assertions.assertEquals(result.getHtmlCode(), readFile(dir, "index.html"));
        Assertions.assertEquals(result.getCssCode(), readFile(dir, "style.css"));
        Assertions.assertEquals(result.getJsCode(), readFile(dir, "script.js"));
    }

    @Test
    void multiFile_missingCode_writeEmptyFile() {
        MultiFileCodeResult result = new MultiFileCodeResult();
        result.setHtmlCode("<html></html>");
        Mockito.when(aiCodeGeneratorService.generateMultiFileCode(PROMPT)).thenReturn(result);

        File dir = track(aiCodeGeneratorFacade.generateAndSaveCode(PROMPT, CodeGenTypeEnum.MULTI_FILE));

        // 缺失的代码块写为空文件
        Assertions.assertEquals("", readFile(dir, "style.css"));
        Assertions.assertEquals("", readFile(dir, "script.js"));
    }

    @Test
    void html_nullResult_throwsAndNothingSaved() {
        Mockito.when(aiCodeGeneratorService.generateHtmlCode(PROMPT)).thenReturn(null);

        Assertions.assertThrows(BizException.class,
                () -> aiCodeGeneratorFacade.generateAndSaveCode(PROMPT, CodeGenTypeEnum.HTML));

        // 校验在建目录之前，结果为空时不应建目录
        Assertions.assertTrue(newDirs().isEmpty(), "结果为空时不应建目录");
    }

    @Test
    void nullType_throwsBizException() {
        Assertions.assertThrows(BizException.class,
                () -> aiCodeGeneratorFacade.generateAndSaveCode(PROMPT, null));
        Mockito.verifyNoInteractions(aiCodeGeneratorService);
    }

    private File track(File dir) {
        Assertions.assertNotNull(dir);
        createdDirs.add(dir);
        return dir;
    }

    private Set<String> listOutputDirs() {
        File[] dirs = new File(AppConstant.CODE_OUTPUT_ROOT_DIR).listFiles(File::isDirectory);
        return dirs == null ? new HashSet<>() : Arrays.stream(dirs).map(File::getName).collect(Collectors.toSet());
    }

    private List<String> newDirs() {
        return listOutputDirs().stream().filter(name -> !dirsBefore.contains(name)).toList();
    }

    private String readFile(File dir, String filename) {
        File file = new File(dir, filename);
        Assertions.assertTrue(file.isFile(), filename + " 未生成");
        return FileUtil.readString(file, StandardCharsets.UTF_8);
    }
}
