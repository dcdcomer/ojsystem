package com.dcd.dcdojcodesandbox.model;

import lombok.Data;

/**
 * 功能 表示程序进程信息的类
 * 作者：dcd
 * 日期：2024/8/610:22
 */
@Data
public class ExecuteMessage {

    private Integer exitValue;

    private String message;

    private String errorMessage;

    private Long time;

    private Long memory;
}
