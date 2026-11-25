package com.zh.zhaicodempther.core.parser;

import com.zh.zhaicodempther.ai.model.HtmlCodeResult;
import com.zh.zhaicodempther.ai.model.MultiFileCodeResult;
import com.zh.zhaicodempther.core.handler.HtmlCodeHandler;
import com.zh.zhaicodempther.core.handler.MultiFileCodeHandler;
import com.zh.zhaicodempther.exception.BizException;
import com.zh.zhaicodempther.exception.ErrorCode;
import com.zh.zhaicodempther.exception.ThrowUtils;
import com.zh.zhaicodempther.model.enums.CodeGenTypeEnum;

/**
 * @Author: zhaohuan
 * @Date: 2023/3/23 16:34
 * @Description: 代码解析执行器，根据生成类型选择对应的解析器
 * @Version: 1.0
 */
public class CodeParserExecutor {

    private static final CodeParser<HtmlCodeResult> HTML_CODE_PARSER = new HtmlCodeHandler();

    private static final CodeParser<MultiFileCodeResult> MULTI_FILE_CODE_PARSER = new MultiFileCodeHandler();

    /**
     * 执行代码解析
     *
     * @param codeContent     模型输出的原始文本
     * @param codeGenTypeEnum 生成类型
     * @return 解析结果（HtmlCodeResult / MultiFileCodeResult）
     */
    public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenTypeEnum) {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.BAD_REQUEST, "生成类型为空");
        return switch (codeGenTypeEnum) {
            case HTML -> HTML_CODE_PARSER.parseCode(codeContent);
            case MULTI_FILE -> MULTI_FILE_CODE_PARSER.parseCode(codeContent);
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "不支持的解析类型：" + codeGenTypeEnum.getValue());
        };
    }
}
