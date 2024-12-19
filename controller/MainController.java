package com.wjp.wojcodesandbox.controller;

import cn.hutool.http.server.HttpServerRequest;
import com.wjp.wojcodesandbox.JavaDockerCodeSandbox;
import com.wjp.wojcodesandbox.JavaNativeCodeSandbox;
import com.wjp.wojcodesandbox.model.ExecuteCodeResponse;
import com.wjp.wojcodesandbox.model.ExecuteCodeRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController("/")
public class MainController {

    // java 原生实现代码沙箱
    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    // Java docker实现代码沙箱
    @Resource
    private JavaDockerCodeSandbox javaDockerCodeSandbox;

    // 定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "secretKey";



    @GetMapping("/health")
    public String healthCheck(){
        return "ok";
    }


    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                    HttpServletResponse response) {
        // 基本的认证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            response.setStatus(403);
            return null;
        }
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
        return javaDockerCodeSandbox.executeCode(executeCodeRequest);
    }


}
