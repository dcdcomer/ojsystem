package com.dcd.dcdojcodesandbox.utils;

import cn.hutool.core.date.StopWatch;
import com.dcd.dcdojcodesandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 功能 执行程序并获取输出信息的工具类
 * 作者：dcd
 * 日期：2024/8/21 9:47
 */
public class ProcessUtils {

    public static ExecuteMessage runProcessAndGetMessage(Process compileProcess,String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch=new StopWatch();
            stopWatch.start();
            int exitValue = compileProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            if (exitValue == 0) {
                System.out.println(opName+"成功");
                //编译成功，获取编译后的信息
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
                //StringBuilder compileOutPutStringBuilder = new StringBuilder();
                List<String> outputStrList=new ArrayList<>();
                //逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    //compileOutPutStringBuilder.append(compileOutputLine).append("\n");
                    outputStrList.add(compileOutputLine);
                }
                //executeMessage.setMessage(compileOutPutStringBuilder.toString());
                executeMessage.setMessage(StringUtils.join(outputStrList,"\n"));
                //System.out.println(compileOutPutStringBuilder);
            } else {
                System.out.println(opName+"失败");
                //编译失败，获取编译后的信息
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
                //StringBuilder compileOutPutStringBuilder = new StringBuilder();
                List<String> outputStrList=new ArrayList<>();
                //逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    //compileOutPutStringBuilder.append(compileOutputLine).append("\n");
                    outputStrList.add(compileOutputLine);
                }
                //System.out.println(compileOutPutStringBuilder);
                executeMessage.setMessage(StringUtils.join(outputStrList,"\n"));

                //分批获取异常退出的编译信息
                BufferedReader errorbufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()));
                //StringBuilder errorcompileOutPutStringBuilder = new StringBuilder();
                List<String> errorOutputStrList=new ArrayList<>();
                //逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorbufferedReader.readLine()) != null) {
                    //errorcompileOutPutStringBuilder.append(errorcompileOutputLine).append("\n");
                    errorOutputStrList.add(errorCompileOutputLine);
                }
                //System.out.println(errorcompileOutPutStringBuilder);
                //executeMessage.setErrorMessage(errorcompileOutPutStringBuilder.toString());
                executeMessage.setErrorMessage(StringUtils.join(errorOutputStrList,"\n"));
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (InterruptedException | IOException e) {
           e.printStackTrace();
        }
        return executeMessage;
    }
}
