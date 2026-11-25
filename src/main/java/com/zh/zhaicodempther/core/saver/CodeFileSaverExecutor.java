package com.zh.zhaicodempther.core.saver;

import com.zh.zhaicodempther.ai.model.HtmlCodeResult;
import com.zh.zhaicodempther.ai.model.MultiFileCodeResult;
import com.zh.zhaicodempther.core.handler.HtmlCodeHandler;
import com.zh.zhaicodempther.core.handler.MultiFileCodeHandler;
import com.zh.zhaicodempther.exception.BizException;
import com.zh.zhaicodempther.exception.ErrorCode;
import com.zh.zhaicodempther.exception.ThrowUtils;
import com.zh.zhaicodempther.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * @Author: zhaohuan
 * @Date: 2023/3/23 16:34
 * @Description: 代码保存执行器，根据生成类型选择对应的保存器
 * @Version: 1.0
 */
public class CodeFileSaverExecutor {

    private static final CodeFileSaverTemplate<HtmlCodeResult> HTML_CODE_FILE_SAVER = new HtmlCodeHandler();

    private static final CodeFileSaverTemplate<MultiFileCodeResult> MULTI_FILE_CODE_FILE_SAVER = new MultiFileCodeHandler();

    /**
     * 执行代码保存
     *
     * @param codeResult      代码结果（HtmlCodeResult / MultiFileCodeResult）
     * @param codeGenTypeEnum 生成类型
     * @return 保存的文件目录
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenTypeEnum) {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.BAD_REQUEST, "生成类型为空");
        return switch (codeGenTypeEnum) {
            case HTML -> HTML_CODE_FILE_SAVER.saveCode((HtmlCodeResult) codeResult);
            case MULTI_FILE -> MULTI_FILE_CODE_FILE_SAVER.saveCode((MultiFileCodeResult) codeResult);
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "不支持的保存类型：" + codeGenTypeEnum.getValue());
        };
    }
}
