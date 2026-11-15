package com.zh.zhaicodempther.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {

    USER("用户", "user", 10),
    ADMIN("管理员", "admin", 100);

    private final String text;

    private final String value;

    /**
     * 权限等级，数值越大权限越高；等级间留有间隔，便于后续插入新角色（如 VIP）
     */
    private final int level;

    UserRoleEnum(String text, String value, int level) {
        this.text = text;
        this.value = value;
        this.level = level;
    }

    /**
     * 判断当前角色是否满足所需角色（高等级自动拥有低等级权限）
     *
     * @param required 所需角色，null 表示不限制
     * @return 满足返回 true
     */
    public boolean hasPermission(UserRoleEnum required) {
        return required == null || this.level >= required.level;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}