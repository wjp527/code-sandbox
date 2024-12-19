package com.wjp.wojcodesandbox;


import com.wjp.wojcodesandbox.model.ExecuteCodeResponse;
import com.wjp.wojcodesandbox.model.ExecuteCodeRequest;

/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {
    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */

    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) throws InterruptedException;
}
