package com.wjp.wojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.rmi.server.LogStream;
import java.util.List;

public class DockerDemo {
    public static void main(String[] args) throws InterruptedException {
        // 获取默认的 Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

//        // 调用 Docker API
//        PingCmd pingCmd = dockerClient.pingCmd();
//        // 执行 Docker API
//        pingCmd.exec();

        // 拉取镜像
        String image = "nginx:latest";
//        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
//        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
//            // 异步操作
//            @Override
//            public void onNext(PullResponseItem item) {
//                System.out.println("下载镜像状态: " + item.getStatus());
//                super.onNext(item);
//            }
//        };
//
//        // awaitCompletion: 阻塞等待 知道镜像下载完毕，才会执行下面的代码。
//        pullImageCmd
//                .exec(pullImageResultCallback)
//                .awaitCompletion();
//
//        System.out.println("下载完毕");

        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        // 执行命令
        // withCmd: 允许你在创建容器时指定容器启动后要运行的命令
        CreateContainerResponse createContainerResponse = containerCmd
                .withCmd("echo", "Hello Docker")
                .exec();
        // 创建容器钱，增加命令
        // 会返回一个容器id
        System.out.println(createContainerResponse);

        String containerId = createContainerResponse.getId();

        // 查看容器状态
        // 获取容器列表
        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
        List<Container> containerList = listContainersCmd.withShowAll(true).exec();

        for(Container container : containerList) {
            System.out.println(container);
        }

        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();

        // 查看日志
        LogContainerResultCallback logContainerResultCallback = dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .exec(new LogContainerResultCallback() {
                    @Override
                    public void onNext(Frame frame) {
                        System.out.println(new String(frame.getPayload()));
                    }
                }).awaitCompletion();


        // 删除容器
        dockerClient.removeContainerCmd(containerId).withForce(true).exec();

        // 删除镜像
        dockerClient.removeImageCmd(image).exec();
    }
}
