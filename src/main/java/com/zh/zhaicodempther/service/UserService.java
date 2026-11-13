package com.zh.zhaicodempther.service;

import com.mybatisflex.core.service.IService;
import com.zh.zhaicodempther.model.entity.User;

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
}
