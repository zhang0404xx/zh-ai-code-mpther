package com.zh.zhaicodempther.core;

import com.zh.zhaicodempther.ai.AiCodeGeneratorService;
import com.zh.zhaicodempther.ai.model.HtmlCodeResult;
import com.zh.zhaicodempther.ai.model.MultiFileCodeResult;
import com.zh.zhaicodempther.core.parser.CodeParserExecutor;
import com.zh.zhaicodempther.core.saver.CodeFileSaverExecutor;
import com.zh.zhaicodempther.exception.BizException;
import com.zh.zhaicodempther.exception.ErrorCode;
import com.zh.zhaicodempther.exception.ThrowUtils;
import com.zh.zhaicodempther.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.function.Supplier;

/**
 * @Author: zhaohuan
 * @Date: 2023/3/23 16:34
 * @Description: 代码生成器 门面类 只用作用组合代码生成和保存功能
 * @Version: 1.0
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.BAD_REQUEST, "生成类型为空");
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCode(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCode(userMessage);
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "不支持的生成类型：" + codeGenTypeEnum.getValue());
        };
    }

    /**
     * 生成 HTML 模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveHtmlCode(String userMessage) {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
        File dir = CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML);
        log.info("HTML 代码已保存：{}", dir.getAbsolutePath());
        return dir;
    }

    /**
     * 生成多文件模式的代码并保存
     *
     * @param userMessage 用户提示词
     * @return 保存的目录
     */
    private File generateAndSaveMultiFileCode(String userMessage) {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
        File dir = CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE);
        log.info("多文件代码已保存：{}", dir.getAbsolutePath());
        return dir;
    }

    /**
     * 统一入口：根据类型流式生成代码，流结束后解析并保存
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 模型原始文本片段
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.BAD_REQUEST, "生成类型为空");
        // 类型校验同步抛出；模型调用延迟到订阅时
        Supplier<Flux<String>> codeStreamSupplier = switch (codeGenTypeEnum) {
            case HTML -> () -> aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
            case MULTI_FILE -> () -> aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "不支持的生成类型：" + codeGenTypeEnum.getValue());
        };
        // defer 保证每次订阅使用独立的拼接缓冲
        return Flux.defer(() -> processCodeStream(codeStreamSupplier.get(), codeGenTypeEnum));
    }

    /**
     * 通用流式处理：透传片段，流正常结束后经解析执行器与保存执行器落盘
     *
     * @param codeStream      模型原始文本流
     * @param codeGenTypeEnum 生成类型
     * @return 模型原始文本片段
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenTypeEnum) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream
                .doOnNext(codeBuilder::append)
                .doOnComplete(() -> {
                    try {
                        Object result = CodeParserExecutor.executeParser(codeBuilder.toString(), codeGenTypeEnum);
                        File dir = CodeFileSaverExecutor.executeSaver(result, codeGenTypeEnum);
                        log.info("{} 代码（流式）已保存：{}", codeGenTypeEnum.getValue(), dir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("{} 代码（流式）保存失败", codeGenTypeEnum.getValue(), e);
                    }
                });
    }
}
