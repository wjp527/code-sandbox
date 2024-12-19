package com.wjp.wojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.wjp.wojcodesandbox.model.ExecuteCodeResponse;
import com.wjp.wojcodesandbox.model.ExecuteCodeRequest;
import com.wjp.wojcodesandbox.model.ExecuteMessage;
import com.wjp.wojcodesandbox.model.JudgeInfo;
import com.wjp.wojcodesandbox.utils.ProcessUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 模拟执行代码的Java Native实现
 */
@Component
public class JavaNativeCodeSandboxOld implements CodeSandbox {

    // 存储用户代码编译后的目录
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    // 执行的java文件类名
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    // 超时时间，单位：毫秒
    private static final Long TIME_OUT = 50000L;

//    // 设置黑名单
//    private static final List<String> BLACKLIST = Arrays.asList("Files", "exec");
//
//    private static final WordTree WORDTREE = new WordTree();
//
//    // 设置静态代码块
//    static {
//        // 初始化黑名单词树 只需要初始化一次，不用每次启动程序都执行
//        WORDTREE.addWords(BLACKLIST);
//    }

    public static void main(String[] args) {
        JavaNativeCodeSandboxOld javaNativeCodeSandbox = new JavaNativeCodeSandboxOld();

        // 读取resource目录下的代码
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
//         无线睡眠【阻塞程序执行】
//        String code = ResourceUtil.readStr("testCode/unsafeCode/SleepError.java", StandardCharsets.UTF_8);
        // 无限占用空间【浪费系统内存】
//        String code = ResourceUtil.readStr("testCode/unsafeCode/MemoryError.java", StandardCharsets.UTF_8);
        // 向服务器写文件 (植入危险程序)
//        String code = ResourceUtil.readStr("testCode/unsafeCode/writeFileError.java", StandardCharsets.UTF_8);
        // 读取项目中的配置文件 application.yml
//        String code = ResourceUtil.readStr("testCode/unsafeCode/ReadFileError.java", StandardCharsets.UTF_8);
        // 运行其他程序 (比如: 危险程序)
//        String code = ResourceUtil.readStr("testCode/unsafeCode/RunFileError.java", StandardCharsets.UTF_8);

        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4"));
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");

        ExecuteCodeResponse executeCodeReponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println("输出结果: " + executeCodeReponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        // 开启安全管理器，禁止执行一些危险操作
//        System.setSecurityManager(new DenySecurityManager());

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 解决第三个问题: 通过代码来执行一些危险操作
        // 检验黑名单代码
        // 如果找到了匹配的单词
//        FoundWord foundWord = WORDTREE.matchWord(code);
//        if (foundWord != null) {
//            // 打印找到的单词
//            System.out.println("包含禁止词: " + foundWord.getWord());
//            return null;
//        }

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


        // 2. 编译代码，得到class文件
        // javac -encoding utf-8 Main: 解决中文乱码问题
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);

            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println("编译信息: " + executeMessage);
        } catch (Exception e) {
            return getErrorResponse(e);
        }


        // 3.执行代码，得到结果
        // 解决运行时中文乱码: java -Dfile.encoding=UTF-8 -cp %s Main %s
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
                return getErrorResponse(e);
            }
        }

        // 4.收集整理输出结果
        ExecuteCodeResponse executeCodeReponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用 最大值，便于判断是否超时
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            // 错误消息不为空，则直接返回错误信息
            if (StrUtil.isAllNotBlank(errorMessage)) {
                executeCodeReponse.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeReponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());

            // 设置最大的运行时间
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }

        // 正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeReponse.setStatus(1);
        }

        executeCodeReponse.setOutputList(outputList);

        JudgeInfo judgeInfo = new JudgeInfo();

        // 设置最大的运行时间
        judgeInfo.setTime(maxTime);

//        judgeInfo.setMemory();

        executeCodeReponse.setJudgeInfo(judgeInfo);

        // 5、文件清理
//        if (userCodeFile.getParentFile() != null) {
//            // 删除用户执行文件目录
//            boolean del = FileUtil.del(userCodeParentPath);
//            System.out.println("删除" + (del ? "成功" : "失败"));
//        }

        return executeCodeReponse;
    }

    /**
     * 获取错误响应
     *
     * @param e
     * @return
     */
    public ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeReponse = new ExecuteCodeResponse();
        executeCodeReponse.setOutputList(new ArrayList<>());
        executeCodeReponse.setMessage(e.getMessage());
        // 表示代码沙箱错误(可能是编译错误)
        executeCodeReponse.setStatus(2);
        executeCodeReponse.setJudgeInfo(new JudgeInfo());
        return executeCodeReponse;
    }


}
