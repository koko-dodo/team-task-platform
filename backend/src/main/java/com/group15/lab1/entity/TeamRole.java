package com.group15.lab1.entity;

/**
 * 团队成员角色枚举。
 * 
 * Owner：团队拥有者，最高权限
 * Admin：团队管理员
 * Member：普通成员
 */
public enum TeamRole {
    Owner,
    Admin,
    Member;

    /**
     * 根据字符串返回对应的 TeamRole。
     * 
     * @param value 角色字符串（大小写不敏感）
     * @return TeamRole 枚举值
     * @throws IllegalArgumentException 如果角色值非法
     */
    public static TeamRole fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("角色不能为空");
        }
        try {
            return TeamRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("非法角色值：" + value);
        }
    }
}
