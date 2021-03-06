package thread.chapter08;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BasicThreadPool extends Thread implements ThreadPool{

    // 初始化线程数量，线程池初始化时的初始化数量，后可以动态添加
    private final int initSize;

    // 线程池的最大线程数量
    private final int maxSize;

    // 线程池的核心线程数量， 核心数量， 无任务时遗弃多余线程后的最小维护线程数量
    private final int coreSize;

    // 当前活跃的线程数量
    private int activeCount;

    // 创建线程所需要的工厂
    private final ThreadFactory threadFactory;

    //任务队列
    private final RunnableQueue runnableQueue;

    // 线程池是否已经被关闭
    private volatile boolean isShutdown = false;

    //工作线程队列
    private final Queue<ThreadTask> threadQueue = new ArrayDeque<ThreadTask>();

    private final static DenyPolicy DEFAULT_DENY_POLICY = new DenyPolicy.DiscardDenyPolicy();

    private final static ThreadFactory DEFAULT_THREAD_FACTORY = new DefaultThreadFactory();

    private final long keepAliveTime;

    private final TimeUnit timeUnit;

    public BasicThreadPool(int initSize, int maxSize, int coreSize, int queueSize) {
        this(initSize, maxSize, coreSize, DEFAULT_THREAD_FACTORY, queueSize, DEFAULT_DENY_POLICY, 10, TimeUnit.SECONDS);
    }

    public BasicThreadPool(int initSize, int maxSize, int coreSize, ThreadFactory threadFactory, int queueSize, DenyPolicy denyPolicy,
                           long keepAliveTime, TimeUnit timeUnit) {
        this.initSize = initSize;
        this.maxSize = maxSize;
        this.coreSize = maxSize;
        this.threadFactory = threadFactory;
        this.runnableQueue = new LinkedRunnableQueue(queueSize, denyPolicy, this);
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.init();
    }

    //初始化线程，先创建initSize个线程
    private void init() {
        start();
        for (int i = 0; i < initSize; i++) {
            newThread();
        }
    }

    // 新增一个工作线程
    private void newThread() {
        // 该internalTask接受runnableQueue为参数，任务就是不断地从队列中取出runnable并执行
        InternalTask internalTask = new InternalTask(runnableQueue);
        // 该thread接受一个internalTask为参数并添加到工作线程队列中归ThreadPool管理
        Thread thread = this.threadFactory.createThread(internalTask);
        ThreadTask threadTask = new ThreadTask(thread, internalTask);
        threadQueue.offer(threadTask);
        this.activeCount++ ;
        thread.start();     // 该工作线程直接启动
    }

    // 移除一个工作线程
    private void removeThread() {

        // 工作线程队列中取出一个线程
        ThreadTask threadTask = threadQueue.remove();
        // 将线程停止
        threadTask.internalTask.stop();
        this.activeCount--;
    }

    @Override
    public void execute(Runnable runnable) {
        if (this.isShutdown) {
            throw new IllegalStateException("The thread pool is destory");
        }
        this.runnableQueue.offer(runnable);
    }

    /**
     * 继承自Thread的run方法，主要用户维护吸纳城池数量，比如扩容，回收等工作
     */
    @Override
    public void run() {
        while (!isShutdown && !isInterrupted()) {
            try {
                timeUnit.sleep(keepAliveTime);
            } catch (InterruptedException e) {
                isShutdown = true;
                break;
            }

            synchronized (this) {
                if (isShutdown) {
                    break;
                }

                //当前的队列中有任务尚未处理，并且activeCount<coreSize，则继续扩容
                if (runnableQueue.size() > 0 && activeCount < coreSize) {
                    for (int i = initSize; i < coreSize; i++) {
                        newThread();
                    }
                    continue;
                }
                // 当前的队列中有任务尚未处理，并且activeCount < maxSize，则继续扩容
                if (runnableQueue.size() > 0 && activeCount < maxSize) {
                    for (int i = coreSize; i <maxSize; i++) {
                        newThread();
                    }
                }
                // 没有任务的时候回收线程
                if (runnableQueue.size() == 0 && activeCount < coreSize) {
                    for (int i = coreSize; i < activeCount; i++) {
                        removeThread();
                    }
                }

            }
        }
    }

    @Override
    public void shutdown() {
        synchronized (this) {
            if (isShutdown) {
                return;
            }
            isShutdown = true;
            threadQueue.forEach(threadTask -> {
                threadTask.internalTask.stop();
                threadTask.thread.interrupt();
            });
            this.interrupt();
        }
    }

    @Override
    public int getInitSize() {
        if (isShutdown) {
            throw new IllegalStateException("The thread pool is destory");
        }
        return this.initSize;
    }

    @Override
    public int getMaxSize() {
        if (isShutdown) {
            throw new IllegalStateException("The thread pool is destory");
        }
        return this.maxSize;
    }

    @Override
    public int getCoreSize() {
        if (isShutdown) {
            throw new IllegalStateException("The thread pool is destory");
        }
        return this.coreSize;
    }

    @Override
    public int getQueueSize() {
        if (isShutdown) {
            throw new IllegalStateException("The thread pool is destory");
        }
        return runnableQueue.size();
    }

    @Override
    public int getActiveCount() {
        synchronized (this) {
            return this.activeCount;
        }
    }

    @Override
    public boolean isShutdown() {
        return this.isShutdown;
    }

    private static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger GROUP_COUNTER = new AtomicInteger(1);
        private static final ThreadGroup group = new ThreadGroup("MyTHreadPool-" + GROUP_COUNTER.getAndDecrement());
        private static final AtomicInteger COUNTER = new AtomicInteger(0);

        @Override
        public Thread createThread(Runnable runnable) {
            return new Thread(group, runnable, "thread-pool-" + COUNTER.getAndDecrement());
        }
    }
    //Thread和InternalTask的简单组合
    private static class ThreadTask {
        public ThreadTask(Thread thread, InternalTask internalTask) {
            this.thread = thread;
            this.internalTask = internalTask;
        }
        Thread thread;
        InternalTask internalTask;
    }

}
