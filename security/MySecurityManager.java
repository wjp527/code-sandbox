package com.wjp.wojcodesandbox.security;

import java.security.Permission;

/**
 * 自定义权限安全管理器
 */
public class MySecurityManager extends SecurityManager{

    // 检查所有权限
    @Override
    public void checkPermission(Permission perm) {
        // 取消注释后，默认就是开启所有权限
        // super.checkPermission(perm);

    }

    // 检测程序是否允许执行
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("checkExec 权限异常" + cmd);
    }

    // 检查程序是否允许读操作
    @Override
    public void checkRead(String file) {
        if(file.contains("hutool")) {
            return ;
        }
        throw new SecurityException("checkRead 权限异常" + file);
    }

    // 检查程序是否允许写操作
    @Override
    public void checkWrite(String file) {
        throw new SecurityException("checkWrite 权限异常" + file);
    }

    // 检查程序是否允许删除操作
    @Override
    public void checkDelete(String file) {
        throw new SecurityException("checkDelete 权限异常" + file);
    }

    // 检查程序是否允许网络操作
    @Override
    public void checkConnect(String host, int port) {
        throw new SecurityException("checkConnect 权限异常" + host + ":" + port);
    }
}
