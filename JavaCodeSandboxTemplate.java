package com.wjp.wojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.wjp.wojcodesandbox.model.ExecuteCodeResponse;
import com.wjp.wojcodesandbox.model.ExecuteCodeRequest;
import com.wjp.wojcodesandbox.model.ExecuteMessage;
import com.wjp.wojcodesandbox.model.JudgeInfo;
import com.wjp.wojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;

import java.util.List;
import java.util.UUID;

/**
 * Java代码沙箱模版方法的实现
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {

    // 存储用户代码编译后的目录
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    // 执行的java文件类名
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    // 超时时间，单位：毫秒
    private static final Long TIME_OUT = 50000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        // 开启安全管理器，禁止执行一些危险操作
//        System.setSecurityManager(new DenySecurityManager());

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 获取到用户的工作目录
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 对每一个文件创建一个单独的目录
        UUID uuid = UUID.randomUUID();
        // 获取到用户执行文件的父级目录 【tmpCode/6ff09cd8-76be-468f-a679-40cd65af40cc】
        String userCodeParentPath = globalCodePathName + File.separator + uuid;


        // 1、将用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);


        // 2. 编译代码，得到class文件
        // javac -encoding utf-8 Main: 解决中文乱码问题
        ExecuteMessage compileFileMessage = compileFile(userCodeFile);
        System.out.println("编译信息: " + compileFileMessage);

        // 3.执行代码，得到结果
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);

        // 4.收集整理输出结果
        ExecuteCodeResponse executeCodeReponse = getOutputResponse(executeMessageList);
        System.out.println("输出信息: " + executeCodeReponse);

        // 5、文件清理
        boolean result = deleteFile(userCodeFile);
        if (!result) {
            log.error("文件清理失败: " + userCodeFile.getAbsolutePath());

        }

        return executeCodeReponse;
    }


    /**
     * 1、将用户的代码保存为文件
     *
     * @param code
     * @return
     */
    public File saveCodeToFile(String code) {
        // 获取到用户的工作目录
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在，不存在则创建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        // 1、将用户的代码保存为文件

        // 1-1. 把用户的代码隔离分层
        // 对每一个文件创建一个单独的目录
        UUID uuid = UUID.randomUUID();
        // 获取到用户执行文件的父级目录 【tmpCode/6ff09cd8-76be-468f-a679-40cd65af40cc】
        String userCodeParentPath = globalCodePathName + File.separator + uuid;
        // 创建用户执行文件的路径 【tmpCode/6ff09cd8-76be-468f-a679-40cd65af40cc/Main.java】
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;


        // 1-2. 保存用户的代码到文件
        File userCodeFile = FileUtil.writeString(code, userCodePath, "UTF-8");

        return userCodeFile;
    }

    /**
     * 2. 编译代码，得到class文件
     *
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        // javac -encoding utf-8 Main: 解决中文乱码问题
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);

            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");

            if (executeMessage.getExitValue() != 0) {
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 3.执行代码，得到结果
     *
     * @param compiledFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File compiledFile, List<String> inputList) {
        String userCodeParentPath = compiledFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        System.out.println("userCodeParentPath = " + userCodeParentPath);
        for (String inputArgs : inputList) {
            // 解决第二个问题： java -Xmx4096m 限制内存
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);

            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);

                // 解决第一个问题: 超时控制
                new Thread(() -> {
                    try {
                        // 先睡5秒
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时...");
                        // 醒来后，程序还没执行完毕，直接杀死线程
                        runProcess.destroy();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).start();

                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
//                ExecuteMessage executeMessage = ProcessUtils.runInteractProcessAndGetMessage(runProcess, inputArgs);
                // 获取输出信息
                executeMessageList.add(executeMessage);
                System.out.println("运行信息: " + executeMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4.收集整理输出结果
     *
     * @param executeMessageList
     * @return
     */

    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeReponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用 最大值，便于判断是否超时
        long maxTime = 0;
        // 获取最大内存值
        long maxMemoryNew = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            // 错误消息不为空，则直接返回错误信息
            if (StrUtil.isAllNotBlank(errorMessage)) {
                executeCodeReponse.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeReponse.setStatus(3);
                break;
            }
            System.out.println("executeMessage = " + executeMessage);
            outputList.add(executeMessage.getMessage());

            // 设置最大的运行时间
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }

            Long mege = executeMessage.getMemory();
            System.out.println("executeMessage = " + executeMessage);
            if(mege!=null) {
                maxMemoryNew = Math.max(maxMemoryNew, mege);
            }

        }

        // 正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeReponse.setStatus(2);
        }

        executeCodeReponse.setOutputList(outputList);

        JudgeInfo judgeInfo = new JudgeInfo();

        // 设置最大的运行时间
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemoryNew);


        executeCodeReponse.setJudgeInfo(judgeInfo);

        return executeCodeReponse;
    }


    /**
     * 5.文件清理
     *
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            // 获取到用户执行文件的父级目录
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            // 删除用户执行文件目录
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }



}
