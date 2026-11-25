package com.zh.zhaicodempther.core.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: zhaohuan
 * @Date: 2023/3/23 16:34
 * @Description: 代码解析器策略接口，把流式输出拼接后的 Markdown 文本解析为结构化结果
 * @Version: 1.0
 */
public interface CodeParser<T> {

    /**
     * html 代码块
     */
    Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * css 代码块
     */
    Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * js 代码块（兼容 js / javascript）
     */
    Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * 任意代码块，用于剥离代码后提取描述
     */
    Pattern ANY_CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");

    /**
     * 解析代码
     *
     * @param codeContent 模型输出的原始文本
     * @return 结构化结果
     */
    T parseCode(String codeContent);

    /**
     * 提取第一个匹配的代码块内容
     */
    default String extractCode(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * 剥离所有代码块后剩余的说明文字
     */
    default String extractDescription(String content) {
        return ANY_CODE_BLOCK_PATTERN.matcher(content).replaceAll("").trim();
    }
}
