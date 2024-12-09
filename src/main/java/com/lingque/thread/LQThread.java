package com.lingque.thread;

import com.lingque.config.LQProperties;
import lombok.Data;

import java.util.concurrent.*;

/**
 * @author aisen
 * @date 2024/9/24
 * @desc 线程池
 **/
@Data
public class LQThread {

    /**主线程池，负责底层交互专用**/
    private ExecutorService master;
    /**辅助线程池，负责交接主线程的子任务**/
    private ExecutorService slave;

    public static LQThread init(LQProperties p){
        LQThread LQThread = new LQThread();
        LQThread.master = new ThreadPoolExecutor(p.getMasterPool().getCorePoolSize(), p.getMasterPool().getMaximumPoolSize(), p.getMasterPool().getKeepAliveTime(), TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(p.getMasterPool().getMaximumPoolSize() * 10), Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
        LQThread.slave = new ThreadPoolExecutor(p.getSlavePool().getCorePoolSize(), p.getSlavePool().getMaximumPoolSize(), p.getSlavePool().getKeepAliveTime(), TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(p.getSlavePool().getMaximumPoolSize() * 10), Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
        return LQThread;
    }

    /**
     * 主线程池
     * @param runnable
     */
    public void execMaster(Runnable runnable){
        master.execute(runnable);
    }

    /**
     * 辅线程池
     * @param runnable
     */
    public void execSlave(Runnable runnable){
        slave.execute(runnable);
    }





}
