package com.zh.zhaicodempther.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zh.zhaicodempther.ai.model.HtmlCodeResult;
import com.zh.zhaicodempther.ai.model.MultiFileCodeResult;
import com.zh.zhaicodempther.constant.AppConstant;
import com.zh.zhaicodempther.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @Author: zhaohuan
 * @Date: 2023/3/23 16:34
 * @Description: 文件保存器，用于保存生成的代码文件
 * @Version: 1.0
 */
public class CodeFileSaver {

    /**
     * 指定文件保存路径 绝对路径
     * 默认值：tmp/code_output
     * 可配置项
     * code.output.root.dir
     * code.output.root.dir.default: tmp/code_output
     */
    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 保存 html 网页代码文件
     * @param result 代码结果
     * @return 保存的文件目录
     */
    public static File saveHtmlCodeResult(HtmlCodeResult result) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        return new File(baseDirPath);
    }

    /**
     * 保存多文件代码文件
     * @param result 代码结果
     * @return 保存的文件目录
     * 包含 html、css、js 代码
     */
    public static File saveMultiFileCodeResult(MultiFileCodeResult result) {
        String baseDirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
        return new File(baseDirPath);
    }

    /**
     * 构建文件唯一路径：指定文件名 方式重复文件名 路径：tmp/code_output/bizType_雪花 ID
     * @param bizType 业务类型
     * @return 文件保存路径
     */
    private static String buildUniqueDir(String bizType) {
        String uniqueDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 保存单个文件
     * @param dirPath 文件保存路径
     * @param filename 文件名
     * @param content 文件内容
     */
    private static void writeToFile(String dirPath, String filename, String content) {
        String filePath = dirPath + File.separator + filename;
        FileUtil.writeString(StrUtil.nullToEmpty(content), filePath, StandardCharsets.UTF_8);
    }
}
