package com.zh.zhaicodempther.core;

import com.zh.zhaicodempther.ai.AiCodeGeneratorService;
import com.zh.zhaicodempther.ai.model.HtmlCodeResult;
import com.zh.zhaicodempther.ai.model.MultiFileCodeResult;
import com.zh.zhaicodempther.exception.BizException;
import com.zh.zhaicodempther.exception.ErrorCode;
import com.zh.zhaicodempther.exception.ThrowUtils;
import com.zh.zhaicodempther.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

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
        File dir = CodeFileSaver.saveHtmlCodeResult(result);
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
        File dir = CodeFileSaver.saveMultiFileCodeResult(result);
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
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCodeStream(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCodeStream(userMessage);
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "不支持的生成类型：" + codeGenTypeEnum.getValue());
        };
    }

    /**
     * 流式生成 HTML 模式的代码，流结束后封装为 HtmlCodeResult 并保存
     *
     * @param userMessage 用户提示词
     * @return 模型原始文本片段
     */
    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
        // defer 保证每次订阅使用独立的拼接缓冲
        return Flux.defer(() -> {
            StringBuilder codeBuilder = new StringBuilder();
            return aiCodeGeneratorService.generateHtmlCodeStream(userMessage)
                    .doOnNext(codeBuilder::append)
                    .doOnComplete(() -> {
                        try {
                        HtmlCodeResult result = CodeParser.parseHtmlCode(codeBuilder.toString());
                        File dir = CodeFileSaver.saveHtmlCodeResult(result);
                        log.info("HTML 代码（流式）已保存：{}", dir.getAbsolutePath());
                        } catch (Exception e) {
                            log.error("HTML 代码（流式）保存失败", e);
                        }
                    });
        });
    }

    /**
     * 流式生成多文件模式的代码，流结束后封装为 MultiFileCodeResult 并保存
     *
     * @param userMessage 用户提示词
     * @return 模型原始文本片段
     */
    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
        return Flux.defer(() -> {
            StringBuilder codeBuilder = new StringBuilder();
            return aiCodeGeneratorService.generateMultiFileCodeStream(userMessage)
                    .doOnNext(codeBuilder::append)
                    .doOnComplete(() -> {
                        try {
                        MultiFileCodeResult result = CodeParser.parseMultiFileCode(codeBuilder.toString());
                        File dir = CodeFileSaver.saveMultiFileCodeResult(result);
                        log.info("多文件代码（流式）已保存：{}", dir.getAbsolutePath());
                        } catch (Exception e) {
                            log.error("多文件代码（流式）保存失败", e);
                        }
                    });
        });
    }
}
