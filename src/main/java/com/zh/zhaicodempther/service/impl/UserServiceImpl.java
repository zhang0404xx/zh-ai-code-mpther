package com.zh.zhaicodempther.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zh.zhaicodempther.exception.BizException;
import com.zh.zhaicodempther.exception.ErrorCode;
import com.zh.zhaicodempther.model.entity.User;
import com.zh.zhaicodempther.mapper.UserMapper;
import com.zh.zhaicodempther.model.enums.UserRoleEnum;
import com.zh.zhaicodempther.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 用户 服务层实现。
 *
 * @author zh
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{

    @Override
    public long register(String userAccount, String userPassword, String checkPassword) {
        //校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "参数为空");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "两次密码不一致");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8 || userPassword.length() > 16 || checkPassword.length() > 16) {
            throw new BizException(ErrorCode.BAD_REQUEST, "密码长度小于8位或大于16位");
        }
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
}
