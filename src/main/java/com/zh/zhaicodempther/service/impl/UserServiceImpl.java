package com.zh.zhaicodempther.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zh.zhaicodempther.constant.UserConstant;
import com.zh.zhaicodempther.exception.BizException;
import com.zh.zhaicodempther.exception.ErrorCode;
import com.zh.zhaicodempther.model.entity.User;
import com.zh.zhaicodempther.mapper.UserMapper;
import com.zh.zhaicodempther.model.enums.UserRoleEnum;
import com.zh.zhaicodempther.model.vo.LonginUserVo;
import com.zh.zhaicodempther.model.vo.UserVo;
import com.zh.zhaicodempther.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用户 服务层实现。
 *
 * @author zh
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{

    /**
     * 用户注册方法
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param checkPassword 确认密码
     * @return 新注册用户的ID
     */
    @Override
    public long register(String userAccount, String userPassword, String checkPassword) {
        //校验参数是否为空
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "参数为空");
        }
    // 校验两次密码是否一致
        if (!userPassword.equals(checkPassword)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "两次密码不一致");
        }
    // 校验密码长度是否在8-16位之间
        if (userPassword.length() < 8 || checkPassword.length() < 8 || userPassword.length() > 16 || checkPassword.length() > 16) {
            throw new BizException(ErrorCode.BAD_REQUEST, "密码长度小于8位或大于16位");
        }
    // 校验用户名长度是否在4-16位之间
        if (userAccount.length() < 4 || userAccount.length() > 16) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户名长度不合法");
        }
        //查询用户是否已存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(User::getUserAccount, userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BizException(ErrorCode.USER_NAME_DUPLICATE, "用户名已存在");
        }

        //加密密码
        String encryptedPassword = encryptPassword(userPassword);

        //创建用户
        User user = User.builder()
                .userAccount(userAccount)
                .userPassword(encryptedPassword)
                .userName(userAccount)
                .userRole(UserRoleEnum.USER.getValue())
                .build();
        boolean save = this.save(user);
        if (!save) {
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "注册失败");
        }
        return user.getId();
    }

    /**
     * 加密密码
     */
    @Override
    public String encryptPassword(String password) {
        // TODO: 实现加密逻辑
        // 盐值加密
        final String salt = "zhang";
        String encryptedPassword = password + salt;
        return  DigestUtils.md5DigestAsHex(encryptedPassword.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将User对象转换为LonginUserVo对象
     * @param user 用户实体对象
     * @return 返回转换后的LonginUserVo对象，如果输入user为null则返回null
     */
    @Override
    public LonginUserVo loginUserVo(User user) {
        // 检查输入参数是否为null
        if (user == null) {
            return null;
        }
        // 创建LonginUserVo对象
        LonginUserVo longinUserVo = new LonginUserVo();
        // 使用BeanUtil工具类将user对象的属性复制到longinUserVo对象中
        BeanUtil.copyProperties(user, longinUserVo);
        // 返回转换后的LonginUserVo对象
        return longinUserVo;
    }

    /**
     * 用户登录方法
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param request HTTP请求对象，用于会话管理
     * @return LoginUserVo 脱敏后的用户信息
     * @throws BizException 当参数不合法或用户不存在时抛出业务异常
     */
    @Override
    public LonginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "参数为空");
        }
        if (userAccount.length() < 4 || userAccount.length() > 16) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户名长度不合法");
        }
        if (userPassword.length() < 8 || userPassword.length() > 16) {
            throw new BizException(ErrorCode.BAD_REQUEST, "密码长度不合法");
        }
        // 2.加密密码，确保密码存储安全
        String encryptedPassword = encryptPassword(userPassword);
        // 3.根据用户名查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(User::getUserAccount, userAccount);
        queryWrapper.eq(User::getUserPassword, encryptedPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NAME_NOT_FOUND, "用户不存在");
        }
        // 4.如果用户存在，则记录用户登入状态，使用会话管理用户状态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 5.返回脱敏后的用户信息，保护用户隐私数据
        return this.loginUserVo(user);
    }

    /**
     * 获取当前登录用户
     * @param request HTTP请求对象，用于读取会话中的登录态
     * @return 当前登录用户实体（以数据库最新数据为准）
     * @throws BizException 当未登录或用户已不存在时抛出业务异常
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1.从会话中获取登录态
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        // 2.根据ID重新查询数据库，避免会话中的用户信息过期
        User user = this.getById(currentUser.getId());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户未登录");
        }
        return user;
    }

    /**
     * 用户注销
     * @param request HTTP请求对象，用于清除会话中的登录态
     * @return 注销成功返回 true
     * @throws BizException 当用户未登录时抛出业务异常
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1.判断是否已登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BizException(ErrorCode.OPERATION_NOT_ALLOWED, "用户未登录");
        }
        // 2.移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 将用户实体转换为脱敏后的用户视图
     * @param user 用户实体
     * @return 用户视图，user 为 null 时返回 null
     */
    @Override
    public UserVo getUserVo(User user) {
        if (user == null) {
            return null;
        }
        // 只复制 UserVo 中声明的字段，密码、逻辑删除等字段不会外泄
        UserVo userVo = new UserVo();
        BeanUtil.copyProperties(user, userVo);
        return userVo;
    }

    /**
     * 批量将用户实体转换为脱敏后的用户视图
     * @param userList 用户实体列表
     * @return 用户视图列表，入参为空时返回空列表
     */
    @Override
    public List<UserVo> getUserVoList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return List.of();
        }
        return userList.stream().map(this::getUserVo).toList();
    }
}
