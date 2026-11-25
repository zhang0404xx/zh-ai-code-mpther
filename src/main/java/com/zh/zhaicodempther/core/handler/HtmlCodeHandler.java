package com.zh.zhaicodempther.core.handler;

import com.zh.zhaicodempther.ai.model.HtmlCodeResult;
import com.zh.zhaicodempther.core.parser.CodeParser;
import com.zh.zhaicodempther.core.saver.CodeFileSaverTemplate;
import com.zh.zhaicodempther.model.enums.CodeGenTypeEnum;

/**
 * @Author: zhaohuan
 * @Date: 2023/3/23 16:34
 * @Description: 单文件 html 代码处理器，负责解析与保存（写出 index.html）
 * @Version: 1.0
 */
public class HtmlCodeHandler extends CodeFileSaverTemplate<HtmlCodeResult> implements CodeParser<HtmlCodeResult> {

    /**
     * 解析单文件 html 代码，没有 html 代码块时把整段文本当作 html
     */
    @Override
    public HtmlCodeResult parseCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();
        String htmlCode = extractCode(codeContent, HTML_CODE_PATTERN);
        result.setHtmlCode(htmlCode != null ? htmlCode : codeContent.trim());
        result.setDescription(extractDescription(codeContent));
        return result;
    }

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }
}
