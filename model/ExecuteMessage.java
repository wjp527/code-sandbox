package com.wjp.wojcodesandbox.model;

import lombok.Data;

/**
 * 进程执行信息
 */
@Data
public class ExecuteMessage {

    private Integer exitValue;

    private String message;

    private String errorMessage;

    /**
     * 执行运行时间
     */

    private Long time;


    /**
     * 执行占用内存
     */
    private Long memory;

}
