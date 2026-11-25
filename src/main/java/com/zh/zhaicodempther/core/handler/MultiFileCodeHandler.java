package com.zh.zhaicodempther.core.handler;

import com.zh.zhaicodempther.ai.model.MultiFileCodeResult;
import com.zh.zhaicodempther.core.parser.CodeParser;
import com.zh.zhaicodempther.core.saver.CodeFileSaverTemplate;
import com.zh.zhaicodempther.model.enums.CodeGenTypeEnum;

/**
 * @Author: zhaohuan
 * @Date: 2023/3/23 16:34
 * @Description: 多文件代码处理器，负责解析与保存（写出 index.html / style.css / script.js）
 * @Version: 1.0
 */
public class MultiFileCodeHandler extends CodeFileSaverTemplate<MultiFileCodeResult> implements CodeParser<MultiFileCodeResult> {

    /**
     * 解析多文件代码（html / css / js），缺失的代码块对应字段为 null
     */
    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        result.setHtmlCode(extractCode(codeContent, HTML_CODE_PATTERN));
        result.setCssCode(extractCode(codeContent, CSS_CODE_PATTERN));
        result.setJsCode(extractCode(codeContent, JS_CODE_PATTERN));
        result.setDescription(extractDescription(codeContent));
        return result;
    }

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }
}
