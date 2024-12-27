package com.dcd.dcdojcodesandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.dcd.dcdojcodesandbox.model.ExecuteCodeRequest;
import com.dcd.dcdojcodesandbox.model.ExecuteCodeResponse;
import com.dcd.dcdojcodesandbox.model.ExecuteMessage;
import com.dcd.dcdojcodesandbox.model.JudgeInfo;
import com.dcd.dcdojcodesandbox.utils.ProcessUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;

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
 * 功能 代码沙箱实现类
 * 作者：dcd
 * 日期：2024/8/6 8:43
 */
public class JavaOldDockerCodeSandBox implements CodeSandBox {
    private static final String GLOBAL_CODE_NAME="tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME="Main.java";

    private static final long TIME_OUT=10000L;
    /*
      //定义java安全管理器路径
      private static final String SECURITY_MANAGER_CLASS_NAME="MySecurityManager";
      private static final String SECURITY_MANAGER_PATH="G:\\shizhanprojects\\ojsystem\\dcdoj-code-snadbox\\src\\main\\resources\\security";
  */
    //定义拉取镜像的关凯
    private static final Boolean FIRST_INIT=true;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList=executeCodeRequest.getInputList();
        String code= executeCodeRequest.getCode();
        String language=executeCodeRequest.getLanguage();

        //1.保存用户的代码
        String userDir=System.getProperty("user.dir");
        String globalCodePathName=userDir+File.separator+GLOBAL_CODE_NAME;
        if(!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }
        String userCodeParentPath=globalCodePathName+File.separator+UUID.randomUUID();
        String userCodePath=userCodeParentPath+File.separator+GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile=FileUtil.writeString(code,userCodePath,StandardCharsets.UTF_8);

        //2.编译代码，得到.class文件，并获取运行结果
        String compileCmd=String.format("javac -encoding utf-8 %s",userCodeFile.getAbsolutePath());
        try {
            //使用Prcress进程类执行java运行命令
            Process compileProcess=Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage= ProcessUtils.runProcessAndGetMessage(compileProcess,"编译代码");
            System.out.println(executeMessage);
        }catch (IOException e){
            //throw new RuntimeException(e);
            return getErrorResponse(e);
        }

        //3.将class文件上传到容器
        //获取默认的Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        //拉取镜像
        String image="openjdk:8-alpine";
        //如果是第一次拉取镜像
        if(FIRST_INIT){
            PullImageCmd pullImageCmd= dockerClient.pullImageCmd(image);
            //设计成异步，每下载一个镜像都会触发一个onNext，因为下载的时间可能很长，所以不用同步用异步
            //pullImageResultCallback是个回调
            PullImageResultCallback pullImageResultCallback=new PullImageResultCallback(){
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像:"+item.getStatus());
                    super.onNext(item);
                }
            };
            //awaitCompletion()是个阻塞，直到镜像下载完成才会执行下一步
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
        }
        System.out.println("下载完成");

        //创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        //创建容器时可以指定文件路径映射，作用是把本地的文件同步到容器中，可以让容器访问，也叫容器挂载目录
        HostConfig hostConfig=new HostConfig();
        hostConfig.withMemory(100*1000*1000L);//限制内存
        hostConfig.withCpuCount(1L);
        hostConfig.setBinds(new Bind(userCodeParentPath,new Volume("/app")));
        //.withAttachStdin(true).withAttachStderr(true).withAttachStdout(true)开启这三个命令的作用是把docker容器和本地终端进行连接，能获取你的输入并能够在终端获取输出
        //.withTty(true)开启交互式终端
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)//禁止联网，限制网络资源
                .withReadonlyRootfs(true)//限制往根目录写文件
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        System.out.println(createContainerResponse);
        //获取创建的容器的id
        String containerId=createContainerResponse.getId();

        //4.启动容器,执行代码,并获取输出结果
        dockerClient.startContainerCmd(containerId).exec();
        List<ExecuteMessage> executeMessageList=new ArrayList<>();
        //注意，要把命令按照空格拆分，作为一个数组传递，否则会被识别为一个字符串，而不是多个参数
        //docker exec keen_blackwell java -cp /app Main 1 3
        //执行命令并获取结果
        for (String inputArgs : inputList){
            StopWatch stopWatch=new StopWatch();

            String[] inputArgsArray=inputArgs.split(" ");
            String[] cmdArray= ArrayUtil.append(new String[]{"java","-cp","/app","Main"},inputArgsArray);
            ExecCreateCmdResponse execCreateCmdResponse= dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true).exec();
            System.out.println("创建执行命令:"+execCreateCmdResponse);

            ExecuteMessage executeMessage=new ExecuteMessage();
            final String[] message = {null};
            final String[] errormessage = {null};
            long time=0L;
            //定义是否超时标记
            final boolean[] timeout = {true};

            String execId=execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback=new ExecStartResultCallback(){
                //onComplete是如果程序执行结束就会执行的方法，也就是说如果程序结束了就执行了这方法，timeout就会被设置为false
                //这代表没有超时,我们可以根据timeout的值是false还是true来看程序是否超时
                @Override
                public void onComplete() {
                    timeout[0] =false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType=frame.getStreamType();
                    if(StreamType.STDERR.equals(streamType)){
                        errormessage[0] =new String(frame.getPayload());
                        System.out.println("输出结果错误:"+errormessage[0]);
                    }else {
                        message[0] =new String(frame.getPayload());
                        System.out.println("输出结果:"+message[0]);
                    }
                    super.onNext(frame);
                }
            };

            //获取程序占用内存
            final long[] maxMemory = {0L};
            StatsCmd statsCmd= dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback= statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用:"+statistics.getMemoryStats().getUsage());
                    maxMemory[0] =Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void onStart(Closeable closeable) {}

                @Override
                public void onError(Throwable throwable) {}

                @Override
                public void onComplete() {}

                @Override
                public void close() throws IOException {}
            });
            statsCmd.exec(statisticsResultCallback);

            try {
                stopWatch.start();
                dockerClient.execStartCmd(execId).exec(execStartResultCallback).awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);
                stopWatch.stop();
                time=stopWatch.getLastTaskTimeMillis();
                statsCmd.close();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errormessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }

        //5.收集整理输出结果
        //即将代码运行后获取的输出结果提取出来把=值赋给executeCodeResponse的各个参数
        ExecuteCodeResponse executeCodeResponse=new ExecuteCodeResponse();
        List<String> outPutList=new ArrayList<>();
        long maxTime=0;
        //遍历第4步获取的代码运行后的结果executeMessageList,从里面提取出错误信息和正确信息
        for (ExecuteMessage executeMessage:executeMessageList) {
            String errorMessage=executeMessage.getErrorMessage();
            if(StrUtil.isNotBlank(errorMessage)){
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);
                break;
            }
            outPutList.add(executeMessage.getMessage());
            Long time=executeMessage.getTime();
            if(time!=null){
                maxTime=Math.max(maxTime,time);
            }
        }
        //正常运行
        if(outPutList.size()==executeMessageList.size()){
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outPutList);
        JudgeInfo judgeInfo=new JudgeInfo();
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        //6.文件清理
        if(userCodeFile.getParentFile()!=null){
            boolean del=FileUtil.del(userCodeParentPath);
            System.out.println("删除"+(del?"成功": "失败"));
        }

        return  executeCodeResponse;
    }

    /**
     * 错误处理类
     * @param e
     * @return
     */
    //7.错误处理
    private ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse=new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        //2表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }


    public static void main(String[] args) throws IOException {
        JavaOldDockerCodeSandBox javaNativeCodeSandBox=new JavaOldDockerCodeSandBox();
        ExecuteCodeRequest executeCodeRequest=new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2","3 4"));
        //ResourceUtil.readStr() 读取目录下的文件
        String code= ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java",StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse=javaNativeCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
}
