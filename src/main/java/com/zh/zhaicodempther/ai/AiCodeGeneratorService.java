package com.zh.zhaicodempther.ai;

import com.zh.zhaicodempther.ai.model.HtmlCodeResult;
import com.zh.zhaicodempther.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {
    /**
     * 生成html代码
     * @param prompt
     * @return
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String prompt);

    /**
     * 生成多文件代码
     * @param prompt
     * @return
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String prompt);

    /**
     * 流式生成html代码（流式输出不支持结构化，返回模型原始文本片段）
     * @param prompt
     * @return
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String prompt);

    /**
     * 流式生成多文件代码（流式输出不支持结构化，返回模型原始文本片段）
     * @param prompt
     * @return
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String prompt);
}
