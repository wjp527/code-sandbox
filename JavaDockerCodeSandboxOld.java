package com.wjp.wojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.wjp.wojcodesandbox.model.ExecuteCodeResponse;
import com.wjp.wojcodesandbox.model.ExecuteCodeRequest;
import com.wjp.wojcodesandbox.model.ExecuteMessage;
import com.wjp.wojcodesandbox.model.JudgeInfo;
import com.wjp.wojcodesandbox.utils.ProcessUtils;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 模拟执行代码的Java Native实现
 */
public class JavaDockerCodeSandboxOld implements CodeSandbox {

    // 存储用户代码编译后的目录
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    // 执行的java文件类名
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    // 超时时间，单位：毫秒
    private static final Long TIME_OUT = 30000L;

    // 限制拉取镜像【只允许拉取一次】
    private static final Boolean FIRST_INIT = true;


    public static void main(String[] args) throws InterruptedException {
        JavaDockerCodeSandboxOld javaNativeCodeSandbox = new JavaDockerCodeSandboxOld();

        // 读取resource目录下的代码
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        // 无线睡眠【阻塞程序执行】
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

        // 获取输入用例
        List<String> inputList = executeCodeRequest.getInputList();
        // 获取代码
        String code = executeCodeRequest.getCode();
        // 获取语言
        String language = executeCodeRequest.getLanguage();


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
        System.out.println("userCodeParentPath = " + userCodeParentPath);
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

        // 3.创建容器 把文件复制到容器内
        // 获取默认的 Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        // java版本8，并且是轻量级的
        String image = "openjdk:8-alpine";
        // 为true，表示允许进行拉取镜像
        if (FIRST_INIT) {
            // 拉取镜像
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            // 拉取的回调
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                // 异步操作
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像状态: " + item.getStatus());
                    super.onNext(item);
                }
            };

            // awaitCompletion: 阻塞等待 知道镜像下载完毕，才会执行下面的代码。
            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常...");
                throw new RuntimeException(e);
            }

            System.out.println("下载完毕");
        }


        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        // 执行命令
        // withCmd: 允许你在创建容器时指定容器启动后要运行的命令
        // withHostConfig:是把本地的文件同步到容器中，可以让容器访问 【也可以叫做容器挂在目录】
        // withAttachStdin: 指定是否将容器的标准输入（stdin）连接到客户端。
        // withAttachStderr: 指定是否将容器的标准错误（stderr）连接到客户端。
        // withAttachStdout: 指定是否将容器的标准输出（stdout）连接到客户端。
        // withTty: 指定是否为容器分配一个伪终端（TTY）。
        // exec: 执行命令，创建容器。

        HostConfig hostConfig = new HostConfig();
        // 设置容器的内存限制 100MB
        hostConfig.withMemory(100 * 1000 * 1000L);
        // 设置容器的CPU核数限制
        hostConfig.withCpuCount(1L);
//        String profileConfig = ResourceUtil.readUtf8Str("profile.json");
//        hostConfig.withSecurityOpts(Arrays.asList("seccomp=" + profileConfig));

//        hostConfig.withSecurityOpts(Arrays.asList("seccomp=安全管理配置字符串"));
        // 是把本地的文件同步到容器中，可以让容器访问
        // userCodeParentPath: 获取到用户执行文件的父级目录 【tmpCode/6ff09cd8-76be-468f-a679-40cd65af40cc】
        // new Volume("/app"): 容器的路径 不推荐在根目录/
        // 指定文件路径(Volumn)映射
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));


        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                // 禁用网络资源
                .withNetworkDisabled(true)
                // 限制用户不能向 root 根目录 写文件
                .withReadonlyRootfs(true)
                // 设置可交互的容器，能接收多次输入并且输出 start
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                // 设置可交互的容器，能接收多次输入并且输出 end
                .exec();
        // 创建容器钱，增加命令
        // 会返回一个容器id
        System.out.println(createContainerResponse);

        String containerId = createContainerResponse.getId();

        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();

        // 使用交互式容器
        // docker exec recursing_nobel java -cp /app Main 1 3
        // 执行命令获取结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        // 先开启检测内存，在开始运行代码
        final long[] maxMemory = {0L};

        for (String inputArgs : inputList) {
            // 定时器
            StopWatch stopWatch = new StopWatch();
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);

            // 创建并执行一个命令
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    // 要进行执行的命令
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();

            System.out.println("创建执行命令: " + execCreateCmdResponse);

            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null};
            final String[] errorMessage = {null};

            // 获取运行代码所需要的时间
            long time = 0L;

            // 是否超时 默认超时
            final Boolean[] timeout = {true};

            // 执行命令id
            String execId = execCreateCmdResponse.getId();


            if(execId == null) {
                throw new NullPointerException("执行id为空");
            }

            // 处理 execStart 中命令的异步回调
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {

                // 在超时时间内执行完成，就会进入该方法
                @Override
                public void onComplete() {
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();

                    // 如果是error就是输出错误
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果:" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果==:" + message[0]);
                    }
                    super.onNext(frame);
                }
            };


            // 专门获取运行程序中的状态【内存/网络】
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            // 获取占用的内存
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {


                @Override
                public void onNext(Statistics statistics) {
                    // 获取程序运行内存
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                    System.out.println("程序占用内存: " + statistics.getMemoryStats().getUsage());
                }

                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            });

            // 一直都会进行执行，无休止的。因为他要实时获取程序的内容/网络相关的数据
            statsCmd.exec(statisticsResultCallback);

//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//

            try {
                // 开始定时器
                stopWatch.start();
                // awaitCompletion: 阻塞等待 知道镜像下载完毕，才会执行下面的代码。
                // 开始执行
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        // 超过30s，就会往下执行，不管超没超过 30000毫秒
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);

                // 结束定时器
                stopWatch.stop();
                // 获取所消耗的时间
                time = stopWatch.getTotalTimeMillis();

                // 一定要关闭
                statsCmd.close();

            } catch (InterruptedException e) {
                System.out.println("程序执行异常...");
                throw new RuntimeException(e);
            }


            executeMessage.setMessage(message[0]);

            executeMessage.setTime(time);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
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

            System.out.println("executeMessage.getMessage() =- " + executeMessage.getMessage());

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
        judgeInfo.setMemory(maxMemory[0]);

        System.out.println("最大占用内存: " + maxMemory[0]);


        executeCodeReponse.setJudgeInfo(judgeInfo);

        // 5、文件清理
        if (userCodeFile.getParentFile() != null) {
            // 删除用户执行文件目录
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }

        System.out.println("executeCodeReponse = " + executeCodeReponse);
        return executeCodeReponse;
    }

    /**
     * 获取错误响应
     *
     * @param e
     * @return
     */
    public ExecuteCodeResponse getErrorResponse(Throwable e) {
        System.out.println("编译错误: " + e);
        ExecuteCodeResponse executeCodeReponse = new ExecuteCodeResponse();
        executeCodeReponse.setOutputList(new ArrayList<>());
        executeCodeReponse.setMessage(e.getMessage());
        // 表示代码沙箱错误(可能是编译错误)
        executeCodeReponse.setStatus(2);
        executeCodeReponse.setJudgeInfo(new JudgeInfo());
        return executeCodeReponse;
    }


}
