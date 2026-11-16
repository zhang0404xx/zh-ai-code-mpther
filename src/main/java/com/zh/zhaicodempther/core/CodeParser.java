package com.zh.zhaicodempther.core;

import com.zh.zhaicodempther.ai.model.HtmlCodeResult;
import com.zh.zhaicodempther.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: zhaohuan
 * @Date: 2023/3/23 16:34
 * @Description: 代码解析器，把流式输出拼接后的 Markdown 文本解析为结构化结果
 * @Version: 1.0
 */
public class CodeParser {

    /**
     * html 代码块
     */
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * css 代码块
     */
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * js 代码块（兼容 js / javascript）
     */
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * 任意代码块，用于剥离代码后提取描述
     */
    private static final Pattern ANY_CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");

    /**
     * 解析单文件 html 代码，没有 html 代码块时把整段文本当作 html
     */
    public static HtmlCodeResult parseHtmlCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();
        String htmlCode = extractCode(codeContent, HTML_CODE_PATTERN);
        result.setHtmlCode(htmlCode != null ? htmlCode : codeContent.trim());
        result.setDescription(extractDescription(codeContent));
        return result;
    }

    /**
     * 解析多文件代码（html / css / js），缺失的代码块对应字段为 null
     */
    public static MultiFileCodeResult parseMultiFileCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        result.setHtmlCode(extractCode(codeContent, HTML_CODE_PATTERN));
        result.setCssCode(extractCode(codeContent, CSS_CODE_PATTERN));
        result.setJsCode(extractCode(codeContent, JS_CODE_PATTERN));
        result.setDescription(extractDescription(codeContent));
        return result;
    }

    /**
     * 提取第一个匹配的代码块内容
     */
    private static String extractCode(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * 剥离所有代码块后剩余的说明文字
     */
    private static String extractDescription(String content) {
        return ANY_CODE_BLOCK_PATTERN.matcher(content).replaceAll("").trim();
    }
}
