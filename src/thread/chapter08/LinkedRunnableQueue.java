package thread.chapter08;

import java.util.LinkedList;

public class LinkedRunnableQueue implements RunnableQueue {

    // 队列的最大容量， final类型，构造时传入，初始化后不允许更改
    private final int limit;

    // 遗弃策略
    private final DenyPolicy denyPolicy;

    // 存放任务的队列 （链表）
    private final LinkedList<Runnable> runnableList = new LinkedList<>();

    // 线程池
    private final ThreadPool threadPool;

    public LinkedRunnableQueue(int limit, DenyPolicy denyPolicy, ThreadPool threadPool) {
        this.limit = limit;
        this.denyPolicy = denyPolicy;
        this.threadPool = threadPool;
    }

    @Override
    public void offer(Runnable runnable) {
        synchronized (runnableList) {
            if(runnableList.size() >= limit){
                denyPolicy.reject(runnable, threadPool);
            } else {
                runnableList.addLast(runnable);
                runnableList.notifyAll();
            }
        }
    }

    @Override
    public Runnable take() throws InterruptedException {
        synchronized (runnableList){
            while (runnableList.isEmpty()) {
                try {
                    // 如果任务队列中没有可执行的任务，则当前线程挂起，进入runnableList 关联的monitor waitset中等待唤醒（新的任务加入）
                    runnableList.wait();
                } catch (InterruptedException e){
                    throw e;
                }
            }
            // 移除队列头部的一个任务
            return runnableList.removeFirst();
        }
    }

    @Override
    public int size() {
        synchronized (runnableList) {
            return runnableList.size();
        }
    }
}
