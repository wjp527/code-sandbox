package com.wjp.wojcodesandbox.unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * 向服务器写文件 (植入危险程序)
 */
public class writeFileError {
    public static void main(String[] args) throws IOException {
        // 获取到项目的根目录
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator + "src/main/resources/木马文件.bat";
        String errorProgram = "java -version 2>&1";
        Files.write(Paths.get(filePath), Arrays.asList(errorProgram));
        System.out.println("写入木马程序成功");
    }
}
