package com.wjp.wojcodesandbox.security;

import cn.hutool.core.io.FileUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class testSecurityManager {
    public static void main(String[] args) {
        System.setSecurityManager(new MySecurityManager());

        List<String> strings = FileUtil.readLines("D:\\fullStack\\wOj\\wOj-code-sandbox\\src\\main\\resources\\application.yml", StandardCharsets.UTF_8);

        System.out.println(strings);

    }
}
