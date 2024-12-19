package com.wjp.wojcodesandbox.security;

import java.security.Permission;

/**
 * 禁用所有权限的安全管理器
 */
public class DenySecurityManager extends SecurityManager{

    // 检查所有权限
    @Override
    public void checkPermission(Permission perm) {
        throw new SecurityException("禁止访问系统资源" + perm.getActions());
    }
}
