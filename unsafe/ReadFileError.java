package com.wjp.wojcodesandbox.unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 读取项目中的配置文件 application.yml
 */
public class ReadFileError {
    public static void main(String[] args) throws IOException {
        // 获取到项目的根目录
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator + "src/main/resources/application.yml";
        List<String> allLines = Files.readAllLines(Paths.get(filePath));
        System.out.println("userDir = " + userDir);
        System.out.println(String.join("\n", allLines));
    }
}
