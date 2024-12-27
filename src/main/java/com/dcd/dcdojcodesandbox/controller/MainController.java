package com.dcd.dcdojcodesandbox.controller;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.http.server.HttpServerResponse;
import com.dcd.dcdojcodesandbox.JavaDockerCodeSandBox;
import com.dcd.dcdojcodesandbox.JavaNativeCodeSandBox;
import com.dcd.dcdojcodesandbox.JavaOldDockerCodeSandBox;
import com.dcd.dcdojcodesandbox.model.ExecuteCodeRequest;
import com.dcd.dcdojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 功能
 * 作者：dcd
 * 日期：2024/8/68:23
 */
@RestController("/")
public class MainController {

    //定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER="auth";
    private static final String AUTH_REQUEST_SECRET="secretKey";


    @Resource
    private JavaNativeCodeSandBox javaNativeCodeSandBox;


    @GetMapping("/health")
    public String healthCheck(){
        System.out.println("测试成功");
        return "ok";
    }

    @GetMapping("/docker")
    public String dockerTest(){
        JavaDockerCodeSandBox javaDockerCodeSandBox=new JavaDockerCodeSandBox();
        ExecuteCodeRequest executeCodeRequest=new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2","3 4"));
        //ResourceUtil.readStr() 读取目录下的文件
        String code= ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse=javaDockerCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
        System.out.println("测试成功");
        return "ok";
    }


    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request, HttpServletResponse response){
       //基本认证，判断请求是否有权限调用接口服务
       String authHeader=request.getHeader(AUTH_REQUEST_HEADER);
       if(!AUTH_REQUEST_SECRET.equals(authHeader)){
           response.setStatus(403);
           return null;
       }

        if(executeCodeRequest==null){
            throw new RuntimeException("请求参数为空");
        }
        return javaNativeCodeSandBox.executeCode(executeCodeRequest);
    }



}
