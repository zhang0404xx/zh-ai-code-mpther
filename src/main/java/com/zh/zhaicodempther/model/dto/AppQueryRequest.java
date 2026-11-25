package com.zh.zhaicodempther.model.dto;

import com.zh.zhaicodempther.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 应用分页查询请求
 * <p>
 * 用户接口只使用 appName，管理员接口支持除时间外的全部字段；分页参数继承自 {@link PageQuery}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppQueryRequest extends PageQuery implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称（模糊匹配）
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 应用初始化的 prompt（模糊匹配）
     */
    private String initPrompt;

    /**
     * 代码生成类型
     */
    private String codeGenType;

    /**
     * 部署标识
     */
    private String deployKey;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建用户id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
