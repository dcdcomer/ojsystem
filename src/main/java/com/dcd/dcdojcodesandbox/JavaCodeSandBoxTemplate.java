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
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 功能
 * 作者：dcd
 * 日期：2024/11/236:54
 */
@Slf4j
public abstract class JavaCodeSandBoxTemplate implements CodeSandBox{

    private static final String GLOBAL_CODE_NAME="tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME="Main.java";

    private static final long TIME_OUT=10000L;

    /*定义java安全管理器路径
    private static final String SECURITY_MANAGER_CLASS_NAME="MySecurityManager";
    private static final String SECURITY_MANAGER_PATH="G:\\shizhanprojects\\ojsystem\\dcdoj-code-snadbox\\src\\main\\resources\\security";
    private static  final  String PATH="G:\\shizhanprojects\\ojsystem\\dcdoj-code-snadbox\\src\\main\\resources\\testCode\\simpleComputeArgs\\Main.java";
    */

    /**
     * 第2步 保存用户代码为文件
     * 接受参数: 用户的代码
     * 返回结果: 保存代码的文件
     */
    public File saveCodeToFile(String code){
        //2.保存用户代码到文tmpCode文件夹中
        //2.1 获取文件夹的根目录
        String userDir=System.getProperty("user.dir");
        //2.2 定义文件的绝对路径
        String globalCodePathName=userDir+ File.separator+GLOBAL_CODE_NAME;
        //2.3 判断文件夹是否存在，没有就新建
        if(!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }
        //2.4 每个用户代码隔离保存，因此每个用户代码单独建立文件
        String userCodeParentPath=globalCodePathName+File.separator+ UUID.randomUUID();
        //用户代码的实际路径
        String userCodePath=userCodeParentPath+File.separator+GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile=FileUtil.writeString(code,userCodePath,StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 第3步编译代码得到.class文件，同时拿到编译结果
     * 接受参数：第2步得到的代码文件
     * @return 编译结果
     */
    public ExecuteMessage compileFile(File userCodeFile){
        String compileCmd=String.format("javac -encoding utf-8 %s",userCodeFile.getAbsolutePath());
        //使用java提供的进程管理类Process，可以来运行java编译命令和运行命令
        try {
            Process compileProcess=Runtime.getRuntime().exec(compileCmd);
            //4. 调用运行程序工具类
            ExecuteMessage executeMessage= ProcessUtils.runProcessAndGetMessage(compileProcess,"编译代码");
            if(executeMessage.getExitValue()!=0){
                //return getErrorResponse();
                throw new RuntimeException("编译错误");
            }
            //System.out.println(executeMessage);
            return executeMessage;
        } catch (IOException  e) {
            //return getErrorResponse(e);
            throw new RuntimeException(e);
        }
    }

    /**
     *  5.执行代码，并得到输出结果
     * 接受的参数： 编译后的代码文件  输入示例参数列表 全局代码路径名称globalPathName
     * @return  代码执行完毕的结果输出列表
     */
    public List<ExecuteMessage> runFile(File userCodeFile,List<String> inputList){

        /*
        String userDir=System.getProperty("user.dir");
        String globalCodePathName=userDir+ File.separator+GLOBAL_CODE_NAME;
        //每个用户代码隔离保存，因此每个用户代码单独建立文件
        String userCodeParentPath=globalCodePathName+File.separator+ UUID.randomUUID();
         */
        String userCodeParentPath=userCodeFile.getParentFile().getAbsolutePath();

        List<ExecuteMessage> executeMessageList=new ArrayList<>();
        for(String inputArgs:inputList){
            //加了安全管理器的命令 因为我的java版本可能已经不太适合安全管理器了，所以这里就不加安全管理器
            //String runCmd=String.format("java -Xmx4096m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s",userCodeParentPath,SECURITY_MANAGER_PATH,SECURITY_MANAGER_CLASS_NAME,inputArgs);
            String runCmd=String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s",userCodeParentPath,inputArgs);
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
                //return getErrorResponse(e);
                throw new RuntimeException("程序执行错误",e);
            }
        }
        return executeMessageList;
    }


    /**
     * //6.收集整理输出结果，封装到ExecuteCodeResponse中  需要OutputList message status judgeInfo
     * 需要的参数 第5步的executeMessageList
     * @return  ExecuteCodeResponse
     */
    public ExecuteCodeResponse getOutPutResponse(List<ExecuteMessage> executeMessageList){
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
        return executeCodeResponse;
    }

    /**
     *  //7.文件清理
     * 接受参数
     * @return
     */
    public boolean deleteFile(File userCodeFile){
        String userCodeParentPath=userCodeFile.getParentFile().getAbsolutePath();
        if(userCodeFile.getParentFile()!=null){
            boolean del=FileUtil.del(userCodeParentPath);
            System.out.println("删除"+(del?"成功":"失败"));
            return del;
        }
        return true;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        String code= executeCodeRequest.getCode();
        String language=executeCodeRequest.getLanguage();
        List<String> inputList=executeCodeRequest.getInputList();

        //2.保存用户代码为文件
        File userCodeFile=saveCodeToFile(code);

        //3.编译代码得到.class文件，同时拿到编译结果
        ExecuteMessage compileFileExecuteMessage=compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);

        //5.执行代码，并得到输出结果
       List<ExecuteMessage> executeMessageList = runFile(userCodeFile,inputList);

        //6.收集整理输出结果，封装到ExecuteCodeResponse中  需要OutputList message status judgeInfo
       ExecuteCodeResponse outputResponse=getOutPutResponse(executeMessageList);

        //7.文件清理
        boolean b=deleteFile(userCodeFile);
        if(!b){
            log.error("清理文件错误,userCodeFilePath={}",userCodeFile.getAbsolutePath());
        }

        return outputResponse;
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
