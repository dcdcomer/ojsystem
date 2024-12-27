package com.dcd.dcdojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.dcd.dcdojcodesandbox.model.ExecuteCodeRequest;
import com.dcd.dcdojcodesandbox.model.ExecuteCodeResponse;
import com.dcd.dcdojcodesandbox.model.ExecuteMessage;
import com.dcd.dcdojcodesandbox.model.JudgeInfo;
import com.dcd.dcdojcodesandbox.utils.ProcessUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 功能 代码沙箱实现类
 * 作者：dcd
 * 日期：2024/8/6 8:43
 */
public class JavaOldNativeCodeSandBox implements CodeSandBox{
    private static final String GLOBAL_CODE_NAME="tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME="Main.java";

    private static final long TIME_OUT=10000L;

    //禁止操作黑名单
    private static final List<String> blackList=Arrays.asList("Files","exec");
    private static final WordTree WORD_TREE=new WordTree();
    static {
        //初始化字典树
        WORD_TREE.addWords(blackList);
    }

    //定义java安全管理器路径
    private static final String SECURITY_MANAGER_CLASS_NAME="MySecurityManager";
    private static final String SECURITY_MANAGER_PATH="G:\\shizhanprojects\\ojsystem\\dcdoj-code-snadbox\\src\\main\\resources\\security";
private static  final  String PATH="G:\\shizhanprojects\\ojsystem\\dcdoj-code-snadbox\\src\\main\\resources\\testCode\\simpleComputeArgs\\Main.java";

    public static void main(String[] args) throws IOException {
        JavaOldNativeCodeSandBox javaNativeCodeSandBox=new JavaOldNativeCodeSandBox();
        ExecuteCodeRequest executeCodeRequest=new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2","3 4"));
        //String code =ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java",StandardCharsets.UTF_8);
        String code =ResourceUtil.readStr(PATH,StandardCharsets.UTF_8);
        //String code= ResourceUtil.readStr("testCode/unsafeCode/ReaderError.java",StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse=javaNativeCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        String code= executeCodeRequest.getCode();
        String language=executeCodeRequest.getLanguage();
        List<String> inputList=executeCodeRequest.getInputList();

        //校验代码中是否包含黑名单的命令
        /*WordTree wordTree=new WordTree();
        wordTree.addWords(blockList);
        FoundWord foundWord=wordTree.matchWord(code);*/
        //FoundWord foundWord=WORD_TREE.matchWord(code);
       // if(foundWord!=null){
         //   System.out.println("输入代码包含禁止词"+foundWord.getFoundWord());
          //  return null;
      //  }

        //2.保存用户代码到文tmpCode文件夹中
        //2.1 获取文件夹的根目录
        String userDir=System.getProperty("user.dir");
        //2.2 定义文件的绝对路径
        String globalCodePathName=userDir+File.separator+GLOBAL_CODE_NAME;
        //2.3 判断文件夹是否存在，没有就新建
        if(!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }
        //2.4 每个用户代码隔离保存，因此每个用户代码单独建立文件
        String userCodeParentPath=globalCodePathName+File.separator+UUID.randomUUID();
        //用户代码的实际路径
        String userCodePath=userCodeParentPath+File.separator+GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile=FileUtil.writeString(code,userCodePath,StandardCharsets.UTF_8);

        //3.编译代码得到.class文件，同时拿到编译结果
        String compileCmd=String.format("javac -encoding utf-8 %s",userCodeFile.getAbsolutePath());
        //使用java提供的进程管理类Process，可以来运行java编译命令和运行命令
        try {
            Process compileProcess=Runtime.getRuntime().exec(compileCmd);
        //4. 调用运行程序工具类
            ExecuteMessage executeMessage=ProcessUtils.runProcessAndGetMessage(compileProcess,"编译代码");
            System.out.println(executeMessage);
        } catch (IOException  e) {
            return getErrorResponse(e);
        }
        //5.执行程序，并得到输出结果
        List<ExecuteMessage> executeMessageList=new ArrayList<>();
        for(String inputArgs:inputList){
            String runCmd=String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s",userCodeParentPath,inputArgs);
            //String runCmd=String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s",userCodeParentPath,inputArgs);
            //  String runCmd=String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s",userCodeParentPath,SECURITY_MANAGER_PATH,SECURITY_MANAGER_CLASS_NAME,inputArgs);
            try {
                Process runProcess=Runtime.getRuntime().exec(runCmd);

                //添加超时控制，防止程序阻塞占用资源不释放
                new Thread(()->{
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了,中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

                ExecuteMessage executeMessage=ProcessUtils.runProcessAndGetMessage(runProcess,"运行程序");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (IOException e) {
                return getErrorResponse(e);
            }
        }
        //6.收集整理输出结果，封装到ExecuteCodeResponse中  需要OutputList message status judgeInfo
        ExecuteCodeResponse executeCodeResponse=new ExecuteCodeResponse();
        //准备第一个参数outputList
        List<String> outputList=new ArrayList<>();
        long maxTime=0;
        //遍历程序输出结果executeMessageList  设置executeCodeResponse的message参数
        for(ExecuteMessage executeMessage:executeMessageList){
            //先检查有没有错误信息 errorMessage不为空，说明用户提交的程序有错,设置状态为3，把错误信息设置为message，并退出程序
            String errorMessage=executeMessage.getErrorMessage();
            if(StrUtil.isNotBlank(errorMessage)){
                executeCodeResponse.setStatus(3);
                executeCodeResponse.setMessage(errorMessage);
                break;
            }
            //每条执行信息都没错，把结果加到outputList中
            outputList.add(executeMessage.getMessage());

            //用最大值来统计时间
            Long time=executeMessage.getTime();
            if(time!=null){
                maxTime=Math.max(maxTime,time);
            }
        }
        //遍历后如果outputList的长度和程序执行输出的信息长度一致，说明程序没有错误
        if(outputList.size()==executeMessageList.size()){
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo=new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(null);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());

        /*7.文件清理
        if(userCodeFile.getParentFile()!=null){
            boolean del=FileUtil.del(userCodeParentPath);
            System.out.println("删除"+(del?"成功":"失败"));
        }*/
        return executeCodeResponse;
}
       //8.错误处理
    private ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse=new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}