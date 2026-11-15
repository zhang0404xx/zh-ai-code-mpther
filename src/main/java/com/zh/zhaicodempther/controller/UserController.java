package com.zh.zhaicodempther.controller;

import com.mybatisflex.core.paginate.Page;
import com.zh.zhaicodempther.annotation.AuthCheck;
import com.zh.zhaicodempther.common.PageQuery;
import com.zh.zhaicodempther.common.PageResult;
import com.zh.zhaicodempther.common.Result;
import com.zh.zhaicodempther.exception.ErrorCode;
import com.zh.zhaicodempther.exception.ThrowUtils;
import com.zh.zhaicodempther.model.dto.UserRegisterRequest;
import com.zh.zhaicodempther.model.enums.UserRoleEnum;
import com.zh.zhaicodempther.model.vo.LonginUserVo;
import com.zh.zhaicodempther.model.vo.UserVo;
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
import org.springframework.beans.factory.annotation.Autowired;
import com.zh.zhaicodempther.model.entity.User;
import com.zh.zhaicodempther.service.UserService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 用户 控制层。
 *
 * @author zh
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;


    /**
     * 用户注册
     * /**
     * 用户注册
     * @param userRegisterRequest
     */
    @PostMapping("register")
    public Result<Long> register(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.BAD_REQUEST, "用户信息不能为空");
        long register = userService.register(userRegisterRequest.getUserAccount(), userRegisterRequest.getUserPassword(), userRegisterRequest.getCheckPassword());
        return Result.ok(register);
    }

    /**
     * 用户登录
     *
     * @param userRegisterRequest
     * @return 脱敏后登入
     */
    @PostMapping("login")
    public Result<LonginUserVo> login(@RequestBody UserRegisterRequest userRegisterRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.BAD_REQUEST, "用户信息不能为空");
        LonginUserVo user = userService.userLogin(userRegisterRequest.getUserAccount(), userRegisterRequest.getUserPassword(), request);
        return Result.ok(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request HTTP请求对象
     * @return 脱敏后的当前登录用户信息
     */
    @GetMapping("get/login")
    public Result<LonginUserVo> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return Result.ok(userService.loginUserVo(loginUser));
    }

    /**
     * 用户注销
     *
     * @param request HTTP请求对象
     * @return 注销是否成功
     */
    @PostMapping("logout")
    public Result<Boolean> logout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return Result.ok(result);
    }

    /**
     * 根据 id 查询用户（脱敏），user / admin 均可访问
     *
     * @param id 用户主键
     * @return 脱敏后的用户信息
     */
    @GetMapping("get/vo/{id}")
    @AuthCheck
    public Result<UserVo> getUserVoById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.BAD_REQUEST, "用户 id 不合法");
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.DATA_NOT_FOUND, "用户不存在");
        return Result.ok(userService.getUserVo(user));
    }

    /**
     * 分页查询用户（脱敏），user / admin 均可访问
     *
     * @param query 分页参数（pageNum / pageSize）
     * @return 脱敏后的用户分页结果
     */
    @GetMapping("list/page/vo")
    @AuthCheck
    public Result<PageResult<UserVo>> listUserVoByPage(@ParameterObject PageQuery query) {
        Page<User> userPage = userService.page(Page.of(query.getPageNum(), query.getPageSize()));
        List<UserVo> userVoList = userService.getUserVoList(userPage.getRecords());
        return Result.ok(PageResult.of(userVoList, userPage.getTotalRow(), query));
    }

    /**
     * 保存用户。
     *
     * @param user 用户
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    @AuthCheck(mustRole = UserRoleEnum.ADMIN)
    public boolean save(@RequestBody User user) {
        return userService.save(user);
    }

    /**
     * 根据主键删除用户。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @AuthCheck(mustRole = UserRoleEnum.ADMIN)
    public boolean remove(@PathVariable Long id) {
        return userService.removeById(id);
    }

    /**
     * 根据主键更新用户。
     *
     * @param user 用户
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    @AuthCheck(mustRole = UserRoleEnum.ADMIN)
    public boolean update(@RequestBody User user) {
        return userService.updateById(user);
    }

    /**
     * 查询所有用户。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @AuthCheck(mustRole = UserRoleEnum.ADMIN)
    public List<User> list() {
        return userService.list();
    }

    /**
     * 根据主键获取用户。
     *
     * @param id 用户主键
     * @return 用户详情
     */
    @GetMapping("getInfo/{id}")
    @AuthCheck(mustRole = UserRoleEnum.ADMIN)
    public User getInfo(@PathVariable Long id) {
        return userService.getById(id);
    }

    /**
     * 分页查询用户。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    @AuthCheck(mustRole = UserRoleEnum.ADMIN)
    public Page<User> page(Page<User> page) {
        return userService.page(page);
    }

}
