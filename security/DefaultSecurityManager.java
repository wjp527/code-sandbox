package com.wjp.wojcodesandbox.security;

import java.security.Permission;

/**
 * 默认安全管理器
 */
public class DefaultSecurityManager extends SecurityManager{

    // 检查所有权限
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不做任何限制");
        System.out.println("权限：" + perm);
//        super.checkPermission(perm);
    }
}
