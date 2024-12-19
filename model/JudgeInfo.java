package com.wjp.wojcodesandbox.model;

import lombok.Data;


/**
 * 判题信息(json 数组)
 * @author wjp
 */
@Data
public class JudgeInfo {


    /**
     * 程序执行信息
     */
    private String message;

    /**
     * 消耗内容
     */
    private Long memory;


    /**
     * 时间消耗【ms】
     */
    private Long time;


}
