package com.wjp.wojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.wjp.wojcodesandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程工具类
 */
public class ProcessUtils {
    /**
     * 执行进程，并获取信息
     *
     * @param runProcess
     * @param opName
     * @return
     */

    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        // 设置返回的信息
        ExecuteMessage executeMessage = new ExecuteMessage();


        try {
            // 启动计时器
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            // 等待程序执行完毕，获取状态码
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            if (exitValue == 0) {
                System.out.println("编译成功");
                // 分片获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();
                List<String> outputString = new ArrayList<>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputString.add(compileOutputLine);
                }
                executeMessage.setMessage(StringUtils.join(outputString, "\n"));
                System.out.println(opName + "输出: " + compileOutputStringBuilder);
            } else {
                System.out.println(opName + "失败，错误码: " + exitValue);
                // 分片获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();

                List<String> outputString = new ArrayList<>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputString.add(compileOutputLine);
                }
                executeMessage.setMessage(StringUtils.join(outputString, "\n"));

                executeMessage.setMessage(compileOutputStringBuilder.toString());
                System.out.println(opName + "输出: " + compileOutputStringBuilder);

                // 获取错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                StringBuilder errorOutputStringBuilder = new StringBuilder();


                // 逐行读取
                List<String> errorOutputStrList = new ArrayList<>();
                String errorCompilOutputLine;
                while ((errorCompilOutputLine = errorBufferedReader.readLine()) != null) {
                    errorOutputStrList.add(errorCompilOutputLine);
                }
                executeMessage.setErrorMessage(StringUtils.join(outputString, "\n"));

                executeMessage.setErrorMessage(StringUtils.join(errorOutputStrList, "\n"));
                System.out.println(opName + "错误: " + errorOutputStringBuilder);
            }
            // 停止计时器
            stopWatch.stop();
            long totalTimeMillis = stopWatch.getTotalTimeMillis();
            executeMessage.setTime(totalTimeMillis);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return executeMessage;
    }

    /**
     * 执行交互式进程，并获取信息
     *
     * @param runProcess
     * @param args
     * @return
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();


        try {
            //向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(join);
            //相当于按下回车，执行发送
            outputStreamWriter.flush();
            //分批获取进程的正常输出
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            //逐行读取
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            //记得资源释放，否则会卡死
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

}
