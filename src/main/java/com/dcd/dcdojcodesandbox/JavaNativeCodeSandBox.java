package com.dcd.dcdojcodesandbox;

import com.dcd.dcdojcodesandbox.model.ExecuteCodeRequest;
import com.dcd.dcdojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * 功能
 * 作者：dcd
 * 日期：2024/11/238:17
 */
@Component
public class JavaNativeCodeSandBox extends JavaCodeSandBoxTemplate{
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
