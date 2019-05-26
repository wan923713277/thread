package thread.chapter08;

//任务队列
public interface RunnableQueue {
    void offer(Runnable runnable);

    Runnable take() throws InterruptedException;

    int size();
}
