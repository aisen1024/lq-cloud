package com.lingque.mq.exten.itf;

public interface ITimingWheelHandle<T> extends ILQMessage<T> {

    //是否停止
    boolean isStopWithNoMsg();

    /**
     * 时间key
     * @return
     */
    String timeKey();

}