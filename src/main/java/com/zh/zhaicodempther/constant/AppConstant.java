package com.zh.zhaicodempther.constant;

/**
 * 应用常量
 */
public interface AppConstant {

    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 用户分页查询应用时每页最大条数
     */
    int USER_APP_MAX_PAGE_SIZE = 20;

    /**
     * 创建应用时由 initPrompt 截取默认应用名称的最大长度
     */
    int DEFAULT_APP_NAME_LENGTH = 12;

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 应用部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost";
}