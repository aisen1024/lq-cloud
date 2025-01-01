package cn.lingque.mq.exten.itf;

import java.util.List;

/**
 * @author aisen
 * @date 2024/11/14
 * @desc 简单说一下
 **/
public interface IMQConsumer<H,T> {
    void consumer(List<H> handle);

}
