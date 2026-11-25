package com.zh.zhaicodempther.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.zh.zhaicodempther.constant.AppConstant;
import com.zh.zhaicodempther.exception.ErrorCode;
import com.zh.zhaicodempther.exception.ThrowUtils;
import com.zh.zhaicodempther.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @Author: zhaohuan
 * @Date: 2023/3/23 16:34
 * @Description: 代码文件保存模板，固定「校验 → 建唯一目录 → 写文件」流程，子类只实现写哪些文件
 * @Version: 1.0
 */
public abstract class CodeFileSaverTemplate<T> {

    /**
     * 文件保存根目录：tmp/code_output
     */
    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 模板方法：保存代码结果
     *
     * @param result 代码结果
     * @return 保存的文件目录
     */
    public final File saveCode(T result) {
        // 1. 校验输入
        validateInput(result);
        // 2. 建唯一目录
        String baseDirPath = buildUniqueDir();
        // 3. 写文件
        saveFiles(result, baseDirPath);
        // 4. 返回目录
        return new File(baseDirPath);
    }

    /**
     * 校验输入，子类可扩展
     */
    protected void validateInput(T result) {
        ThrowUtils.throwIf(result == null, ErrorCode.BAD_REQUEST, "代码结果为空");
    }

    /**
     * 构建文件唯一路径：tmp/code_output/bizType_雪花 ID
     *
     * @return 文件保存路径
     */
    protected final String buildUniqueDir() {
        String bizType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 保存单个文件
     *
     * @param dirPath  文件保存路径
     * @param filename 文件名
     * @param content  文件内容
     */
    protected final void writeToFile(String dirPath, String filename, String content) {
        String filePath = dirPath + File.separator + filename;
        FileUtil.writeString(StrUtil.nullToEmpty(content), filePath, StandardCharsets.UTF_8);
    }

    /**
     * 生成类型，决定目录前缀
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * 把结果写入目录
     *
     * @param result      代码结果
     * @param baseDirPath 保存目录
     */
    protected abstract void saveFiles(T result, String baseDirPath);
}
