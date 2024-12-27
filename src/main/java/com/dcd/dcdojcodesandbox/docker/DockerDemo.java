package com.dcd.dcdojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.core.DockerClientBuilder;

/**
 * 功能
 * 作者：dcd
 * 日期：2024/10/19:25
 */
public class DockerDemo {
    public static void main(String[] args) throws InterruptedException {
        //获取默认的Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        PingCmd pingCmd = dockerClient.pingCmd();
        pingCmd.exec();

        /*
        //拉取镜像
        String image="nginx:latest";
        PullImageCmd pullImageCmd= dockerClient.pullImageCmd(image);
        //设计成异步，每下载一个镜像都会触发一个onNext，因为下载的时间可能很长，所以不用同步用异步
        //pullImageResultCallback是个回调
        //PullImageResultCallback pullImageResultCallback=new PullImageResultCallback(){
            @Override
            public void onNext(PullResponseItem item) {
                System.out.println("下载镜像:"+item.getStatus());
                super.onNext(item);
            }
        };
        //awaitCompletion()是个阻塞，直到镜像下载完成才会执行下一步
        pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
        System.out.println("下载完成");

        //创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        CreateContainerResponse createContainerResponse = containerCmd.withCmd("echo", "Hello Docker").exec();
        System.out.println(createContainerResponse);
        //获取创建的容器的id
        String containerId=createContainerResponse.getId();

        //查看容器状态
        ListContainersCmd listContainersCmd= dockerClient.listContainersCmd();
        List<Container> containerList=listContainersCmd.withShowAll(true).exec();
        for(Container container:containerList){
            System.out.println(container);
        }

        //启动容器
        dockerClient.startContainerCmd(containerId).exec();

        Thread.sleep(5000L);
        //查看日志
        LogContainerResultCallback logContainerResultCallback=new LogContainerResultCallback(){
            @Override
            public void onNext(Frame item) {
                System.out.println("日志:"+new String(item.getPayload()));
                super.onNext(item);
            }
        };
        dockerClient.logContainerCmd(containerId).withStdErr(true).withStdOut(true).exec(logContainerResultCallback).awaitCompletion();

        //删除容器
        dockerClient.removeContainerCmd(containerId).exec();

        //删除镜像
        dockerClient.removeImageCmd(image).exec();
*/


    }
}