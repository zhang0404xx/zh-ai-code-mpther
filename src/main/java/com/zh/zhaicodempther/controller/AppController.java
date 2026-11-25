package com.zh.zhaicodempther.controller;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zh.zhaicodempther.annotation.AuthCheck;
import com.zh.zhaicodempther.common.PageResult;
import com.zh.zhaicodempther.common.Result;
import com.zh.zhaicodempther.constant.AppConstant;
import com.zh.zhaicodempther.exception.ErrorCode;
import com.zh.zhaicodempther.exception.ThrowUtils;
import com.zh.zhaicodempther.model.dto.AppAddRequest;
import com.zh.zhaicodempther.model.dto.AppAdminUpdateRequest;
import com.zh.zhaicodempther.model.dto.AppQueryRequest;
import com.zh.zhaicodempther.model.dto.AppUpdateRequest;
import com.zh.zhaicodempther.model.entity.User;
import com.zh.zhaicodempther.model.enums.UserRoleEnum;
import com.zh.zhaicodempther.model.vo.AppVo;
import com.zh.zhaicodempther.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.zh.zhaicodempther.model.entity.App;
import com.zh.zhaicodempther.service.AppService;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用 控制层。
 *
 * @author zh
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    // region 用户接口

    /**
     * 创建应用（须填写 initPrompt）
     *
     * @param appAddRequest 创建请求
     * @param request       HTTP请求对象
     * @return 新应用的ID
     */
    @PostMapping("add")
    @AuthCheck
    public Result<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.BAD_REQUEST, "应用信息不能为空");
        User loginUser = userService.getLoginUser(request);
        return Result.ok(appService.createApp(appAddRequest, loginUser));
    }

    /**
     * 根据 id 修改自己的应用（目前只支持修改应用名称）
     *
     * @param appUpdateRequest 更新请求
     * @param request          HTTP请求对象
     * @return 更新是否成功
     */
    @PutMapping("update")
    @AuthCheck
    public Result<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appUpdateRequest == null || appUpdateRequest.getId() == null || appUpdateRequest.getId() <= 0,
                ErrorCode.BAD_REQUEST, "应用 id 不合法");
        ThrowUtils.throwIf(StrUtil.isBlank(appUpdateRequest.getAppName()), ErrorCode.BAD_REQUEST, "应用名称不能为空");
        User loginUser = userService.getLoginUser(request);
        checkOwnApp(appUpdateRequest.getId(), loginUser);
        App app = App.builder()
                .id(appUpdateRequest.getId())
                .appName(appUpdateRequest.getAppName())
                .editTime(LocalDateTime.now())
                .build();
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.INTERNAL_SERVER_ERROR, "更新应用失败");
        return Result.ok(true);
    }

    /**
     * 根据 id 删除自己的应用
     *
     * @param id      应用主键
     * @param request HTTP请求对象
     * @return 删除是否成功
     */
    @DeleteMapping("delete/{id}")
    @AuthCheck
    public Result<Boolean> deleteApp(@PathVariable Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.BAD_REQUEST, "应用 id 不合法");
        User loginUser = userService.getLoginUser(request);
        checkOwnApp(id, loginUser);
        boolean result = appService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.INTERNAL_SERVER_ERROR, "删除应用失败");
        return Result.ok(true);
    }

    /**
     * 根据 id 查看应用详情
     *
     * @param id 应用主键
     * @return 应用视图（附带创建者信息）
     */
    @GetMapping("get/vo/{id}")
    @AuthCheck
    public Result<AppVo> getAppVoById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.BAD_REQUEST, "应用 id 不合法");
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.DATA_NOT_FOUND, "应用不存在");
        return Result.ok(appService.getAppVo(app));
    }

    /**
     * 分页查询自己的应用列表（支持根据名称查询，每页最多 20 个）
     *
     * @param query   分页参数与应用名称
     * @param request HTTP请求对象
     * @return 应用视图分页结果
     */
    @GetMapping("my/list/page/vo")
    @AuthCheck
    public Result<PageResult<AppVo>> listMyAppVoByPage(@ParameterObject AppQueryRequest query, HttpServletRequest request) {
        ThrowUtils.throwIf(query.getPageSize() > AppConstant.USER_APP_MAX_PAGE_SIZE, ErrorCode.BAD_REQUEST,
                "每页最多查询 " + AppConstant.USER_APP_MAX_PAGE_SIZE + " 个应用");
        User loginUser = userService.getLoginUser(request);
        // 只开放名称查询，其余条件由后端限定为当前用户
        AppQueryRequest condition = new AppQueryRequest();
        condition.setAppName(query.getAppName());
        condition.setUserId(loginUser.getId());
        return Result.ok(pageAppVo(appService.getQueryWrapper(condition), query));
    }

    /**
     * 分页查询精选的应用列表（支持根据名称查询，每页最多 20 个）
     *
     * @param query 分页参数与应用名称
     * @return 应用视图分页结果
     */
    @GetMapping("good/list/page/vo")
    @AuthCheck
    public Result<PageResult<AppVo>> listGoodAppVoByPage(@ParameterObject AppQueryRequest query) {
        ThrowUtils.throwIf(query.getPageSize() > AppConstant.USER_APP_MAX_PAGE_SIZE, ErrorCode.BAD_REQUEST,
                "每页最多查询 " + AppConstant.USER_APP_MAX_PAGE_SIZE + " 个应用");
        // 只开放名称查询，优先级固定为精选
        AppQueryRequest condition = new AppQueryRequest();
        condition.setAppName(query.getAppName());
        condition.setPriority(AppConstant.GOOD_APP_PRIORITY);
        return Result.ok(pageAppVo(appService.getQueryWrapper(condition), query));
    }

    // endregion

    // region 管理员接口

    /**
     * 【管理员】根据 id 删除任意应用
     *
     * @param id 应用主键
     * @return 删除是否成功
     */
    @DeleteMapping("admin/delete/{id}")
    @AuthCheck(mustRole = UserRoleEnum.ADMIN)
    public Result<Boolean> deleteAppByAdmin(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.BAD_REQUEST, "应用 id 不合法");
        ThrowUtils.throwIf(appService.getById(id) == null, ErrorCode.DATA_NOT_FOUND, "应用不存在");
        boolean result = appService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.INTERNAL_SERVER_ERROR, "删除应用失败");
        return Result.ok(true);
    }

    /**
     * 【管理员】根据 id 更新任意应用（支持更新应用名称、应用封面、优先级）
     *
     * @param appAdminUpdateRequest 更新请求
     * @return 更新是否成功
     */
    @PutMapping("admin/update")
    @AuthCheck(mustRole = UserRoleEnum.ADMIN)
    public Result<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        ThrowUtils.throwIf(appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null || appAdminUpdateRequest.getId() <= 0,
                ErrorCode.BAD_REQUEST, "应用 id 不合法");
        ThrowUtils.throwIf(appAdminUpdateRequest.getAppName() == null && appAdminUpdateRequest.getCover() == null
                && appAdminUpdateRequest.getPriority() == null, ErrorCode.BAD_REQUEST, "至少需要更新一个字段");
        ThrowUtils.throwIf(appAdminUpdateRequest.getAppName() != null && StrUtil.isBlank(appAdminUpdateRequest.getAppName()),
                ErrorCode.BAD_REQUEST, "应用名称不能为空");
        ThrowUtils.throwIf(appService.getById(appAdminUpdateRequest.getId()) == null, ErrorCode.DATA_NOT_FOUND, "应用不存在");
        // 为 null 的字段不参与更新
        App app = App.builder()
                .id(appAdminUpdateRequest.getId())
                .appName(appAdminUpdateRequest.getAppName())
                .cover(appAdminUpdateRequest.getCover())
                .priority(appAdminUpdateRequest.getPriority())
                .editTime(LocalDateTime.now())
                .build();
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.INTERNAL_SERVER_ERROR, "更新应用失败");
        return Result.ok(true);
    }

    /**
     * 【管理员】分页查询应用列表（支持根据除时间外的任何字段查询，每页数量不受 20 条限制）
     *
     * @param query 分页参数与查询条件
     * @return 应用视图分页结果
     */
    @GetMapping("admin/list/page/vo")
    @AuthCheck(mustRole = UserRoleEnum.ADMIN)
    public Result<PageResult<AppVo>> listAppVoByPageByAdmin(@ParameterObject AppQueryRequest query) {
        return Result.ok(pageAppVo(appService.getQueryWrapper(query), query));
    }

    /**
     * 【管理员】根据 id 查看应用详情
     *
     * @param id 应用主键
     * @return 应用视图（附带创建者信息）
     */
    @GetMapping("admin/get/vo/{id}")
    @AuthCheck(mustRole = UserRoleEnum.ADMIN)
    public Result<AppVo> getAppVoByIdByAdmin(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.BAD_REQUEST, "应用 id 不合法");
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.DATA_NOT_FOUND, "应用不存在");
        return Result.ok(appService.getAppVo(app));
    }

    // endregion

    /**
     * 校验应用存在且属于当前登录用户
     */
    private void checkOwnApp(Long id, User loginUser) {
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.DATA_NOT_FOUND, "应用不存在");
        ThrowUtils.throwIf(!loginUser.getId().equals(oldApp.getUserId()), ErrorCode.FORBIDDEN, "只能操作自己的应用");
    }

    /**
     * 按查询条件分页并转换为应用视图
     */
    private PageResult<AppVo> pageAppVo(QueryWrapper queryWrapper, AppQueryRequest query) {
        Page<App> appPage = appService.page(Page.of(query.getPageNum(), query.getPageSize()), queryWrapper);
        List<AppVo> appVoList = appService.getAppVoList(appPage.getRecords());
        return PageResult.of(appVoList, appPage.getTotalRow(), query);
    }
}
