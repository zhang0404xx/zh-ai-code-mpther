package com.zh.zhaicodempther.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.zh.zhaicodempther.model.dto.AppAddRequest;
import com.zh.zhaicodempther.model.dto.AppQueryRequest;
import com.zh.zhaicodempther.model.entity.App;
import com.zh.zhaicodempther.model.entity.User;
import com.zh.zhaicodempther.model.vo.AppVo;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author zh
 */
public interface AppService extends IService<App> {

    /**
     * 创建应用
     * @param appAddRequest 创建请求，initPrompt 必填
     * @param loginUser 当前登录用户，作为应用创建者
     * @return 新应用的ID
     */
    long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 将应用实体转换为应用视图（附带脱敏后的创建者信息）
     * @param app 应用实体
     * @return 应用视图，app 为 null 时返回 null
     */
    AppVo getAppVo(App app);

    /**
     * 批量将应用实体转换为应用视图，创建者信息批量查询避免 N+1
     * @param appList 应用实体列表
     * @return 应用视图列表，入参为空时返回空列表
     */
    List<AppVo> getAppVoList(List<App> appList);

    /**
     * 根据查询请求构造查询条件（不含时间字段），默认按创建时间倒序
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);
}
