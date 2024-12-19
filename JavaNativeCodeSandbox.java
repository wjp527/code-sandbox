package com.wjp.wojcodesandbox;

import com.wjp.wojcodesandbox.model.ExecuteCodeResponse;
import com.wjp.wojcodesandbox.model.ExecuteCodeRequest;
import org.springframework.stereotype.Component;

/**
 * Java原生实现的Code Sandbox【直接复用模板方法】
 */
@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }



}
