package thread.chapter08;

import java.util.LinkedList;

public class LinkedRunnableQueue implements RunnableQueue {

    private final int limit;

    private final DenyPolicy denyPolicy;

    private final LinkedList<Runnable> runnableList = new LinkedList<>();

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
