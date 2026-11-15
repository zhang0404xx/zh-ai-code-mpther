package com.zh.zhaicodempther.service;

import com.mybatisflex.core.service.IService;
import com.zh.zhaicodempther.model.entity.User;
import com.zh.zhaicodempther.model.vo.LonginUserVo;
import com.zh.zhaicodempther.model.vo.UserVo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author zh
 */
public interface UserService extends IService<User> {


    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     */
    long register(String userAccount,String userPassword ,String checkPassword);

    /**
     * 加密用户密码的方法
     * @param password 原始密码字符串
     * @return 加密后的密码字符串
     */
    String encryptPassword(String password);

    /**
     * 根据用户对象获取登录用户视图对象
     * @param user 用户实体对象
     * @return 返回登录用户视图对象(LonginUserVo)
     */
    LonginUserVo loginUserVo(User user);

    /**
     * 用户登入
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     */
    LonginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     * @param request HTTP请求对象，用于读取会话中的登录态
     * @return 当前登录用户实体（以数据库最新数据为准）
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     * @param request HTTP请求对象，用于清除会话中的登录态
     * @return 注销是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 将用户实体转换为脱敏后的用户视图
     * @param user 用户实体
     * @return 用户视图，user 为 null 时返回 null
     */
    UserVo getUserVo(User user);

    /**
     * 批量将用户实体转换为脱敏后的用户视图
     * @param userList 用户实体列表
     * @return 用户视图列表，入参为空时返回空列表
     */
    List<UserVo> getUserVoList(List<User> userList);
}
