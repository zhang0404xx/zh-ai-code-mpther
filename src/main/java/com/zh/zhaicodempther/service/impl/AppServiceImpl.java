package com.zh.zhaicodempther.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zh.zhaicodempther.constant.AppConstant;
import com.zh.zhaicodempther.exception.BizException;
import com.zh.zhaicodempther.exception.ErrorCode;
import com.zh.zhaicodempther.exception.ThrowUtils;
import com.zh.zhaicodempther.model.dto.AppAddRequest;
import com.zh.zhaicodempther.model.dto.AppQueryRequest;
import com.zh.zhaicodempther.model.entity.App;
import com.zh.zhaicodempther.mapper.AppMapper;
import com.zh.zhaicodempther.model.entity.User;
import com.zh.zhaicodempther.model.enums.CodeGenTypeEnum;
import com.zh.zhaicodempther.model.vo.AppVo;
import com.zh.zhaicodempther.model.vo.UserVo;
import com.zh.zhaicodempther.service.AppService;
import com.zh.zhaicodempther.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author zh
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

    @Resource
    private UserService userService;

    /**
     * 创建应用
     * @param appAddRequest 创建请求，initPrompt 必填
     * @param loginUser 当前登录用户，作为应用创建者
     * @return 新应用的ID
     */
    @Override
    public long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 1.校验参数
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.BAD_REQUEST, "初始化 prompt 不能为空");
        // 2.构造应用：名称暂取 prompt 前若干字，代码生成类型暂固定为多文件模式
        App app = App.builder()
                .appName(StrUtil.sub(initPrompt.trim(), 0, AppConstant.DEFAULT_APP_NAME_LENGTH))
                .initPrompt(initPrompt)
                .codeGenType(CodeGenTypeEnum.MULTI_FILE.getValue())
                .priority(AppConstant.DEFAULT_APP_PRIORITY)
                .userId(loginUser.getId())
                .build();
        // 3.保存
        boolean save = this.save(app);
        if (!save) {
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "创建应用失败");
        }
        return app.getId();
    }

    /**
     * 将应用实体转换为应用视图（附带脱敏后的创建者信息）
     * @param app 应用实体
     * @return 应用视图，app 为 null 时返回 null
     */
    @Override
    public AppVo getAppVo(App app) {
        if (app == null) {
            return null;
        }
        AppVo appVo = new AppVo();
        BeanUtil.copyProperties(app, appVo);
        if (app.getUserId() != null) {
            appVo.setUser(userService.getUserVo(userService.getById(app.getUserId())));
        }
        return appVo;
    }

    /**
     * 批量将应用实体转换为应用视图，创建者信息批量查询避免 N+1
     * @param appList 应用实体列表
     * @return 应用视图列表，入参为空时返回空列表
     */
    @Override
    public List<AppVo> getAppVoList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return List.of();
        }
        // 1.一次查出所有创建者
        Set<Long> userIds = appList.stream().map(App::getUserId).collect(Collectors.toSet());
        Map<Long, UserVo> userVoMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVo, (a, b) -> a));
        // 2.组装视图
        return appList.stream().map(app -> {
            AppVo appVo = new AppVo();
            BeanUtil.copyProperties(app, appVo);
            appVo.setUser(userVoMap.get(app.getUserId()));
            return appVo;
        }).toList();
    }

    /**
     * 根据查询请求构造查询条件（不含时间字段），默认按创建时间倒序
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.BAD_REQUEST, "查询参数不能为空");
        // 列名一律使用方法引用，走实体 @Column 映射（表列名为驼峰）
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(App::getId, appQueryRequest.getId(), appQueryRequest.getId() != null)
                .like(App::getAppName, appQueryRequest.getAppName(), StrUtil.isNotBlank(appQueryRequest.getAppName()))
                .like(App::getCover, appQueryRequest.getCover(), StrUtil.isNotBlank(appQueryRequest.getCover()))
                .like(App::getInitPrompt, appQueryRequest.getInitPrompt(), StrUtil.isNotBlank(appQueryRequest.getInitPrompt()))
                .eq(App::getCodeGenType, appQueryRequest.getCodeGenType(), StrUtil.isNotBlank(appQueryRequest.getCodeGenType()))
                .eq(App::getDeployKey, appQueryRequest.getDeployKey(), StrUtil.isNotBlank(appQueryRequest.getDeployKey()))
                .eq(App::getPriority, appQueryRequest.getPriority(), appQueryRequest.getPriority() != null)
                .eq(App::getUserId, appQueryRequest.getUserId(), appQueryRequest.getUserId() != null)
                .orderBy(App::getCreateTime, false);
        return queryWrapper;
    }
}
