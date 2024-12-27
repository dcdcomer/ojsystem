package com.dcd.dcdojcodesandbox.security;

import java.security.Permission;

/**
 * 功能 自己的安全管理器
 * 作者：dcd
 * 日期：2024/8/2116:06
 */
public class MySecurityManager extends SecurityManager{
    //放开所有权限
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不做任何限制");
        System.out.println(perm);
       // super.checkPermission(perm);
    }

    //检测程序是否可执行文件
    @Override
    public void checkExec(String cmd) {
        //super.checkExec(cmd);
        throw new SecurityException("权限异常"+cmd);
    }

    //检测程序是否可读文件
    @Override
    public void checkRead(String file) {
        //super.checkRead(file, context);
        throw new SecurityException("权限异常"+file);
    }

    //检测程序是否可写文件
    @Override
    public void checkWrite(String file) {
        //super.checkWrite(file);
        throw new SecurityException("权限异常"+file);
    }

    //检测程序是否可删除文件
    @Override
    public void checkDelete(String file) {
        //super.checkDelete(file);
        throw new SecurityException("权限异常"+file);
    }

    //检测程序是否可连接网络
    @Override
    public void checkConnect(String host, int port) {
        //super.checkConnect(host, port);
        throw new SecurityException("权限异常"+host+":"+port);
    }
}
