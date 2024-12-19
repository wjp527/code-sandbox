package com.wjp.wojcodesandbox.unsafe;

/**
 * 无线睡眠【阻塞程序执行】
 */
public class SleepError {
    public static void main(String[] args) {
        long ONE_HOUR = 60 * 60 * 1000L;
        try {
            Thread.sleep(ONE_HOUR);
            System.out.println("睡完了");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
