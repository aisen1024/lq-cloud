package cn.lingque.thread;

import cn.lingque.config.LQProperties;
import lombok.Data;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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

    public static class NamedThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        @SuppressWarnings("removal")
        public NamedThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "LQ-THREAD-" + name + "-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }


    public static LQThread init(LQProperties p){
        LQThread LQThread = new LQThread();
        LQThread.master = new ThreadPoolExecutor(p.getMasterPool().getCorePoolSize(), p.getMasterPool().getMaximumPoolSize(), p.getMasterPool().getKeepAliveTime(), TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(p.getMasterPool().getMaximumPoolSize() * 10),new NamedThreadFactory("master"), new ThreadPoolExecutor.CallerRunsPolicy());
        LQThread.slave = new ThreadPoolExecutor(p.getSlavePool().getCorePoolSize(), p.getSlavePool().getMaximumPoolSize(), p.getSlavePool().getKeepAliveTime(), TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(p.getSlavePool().getMaximumPoolSize() * 10),new NamedThreadFactory("slave"), new ThreadPoolExecutor.CallerRunsPolicy());
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
