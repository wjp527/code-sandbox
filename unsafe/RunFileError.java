package com.wjp.wojcodesandbox.unsafe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 运行其他程序 (比如: 危险程序)
 */
public class RunFileError {
    public static void main(String[] args) throws IOException, InterruptedException {
        // 获取到项目的根目录
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator + "src/main/resources/木马文件.bat";
        Process process = Runtime.getRuntime().exec(filePath);
        process.waitFor();

        // 分片获取进程的正常输出
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        // 逐行读取
        String compileOutputLine;
        while ((compileOutputLine = bufferedReader.readLine()) != null) {
            System.out.println("compileOutputLine = " + compileOutputLine);

        }
        System.out.println("编译完成");

    }
}
