package thread.chapter08;


// 内部的任务Runnable（内部使用） 该Runnable的任务就是不断地从queue中获取任务，并且一个一个执行
public class InternalTask implements Runnable {

    private final RunnableQueue runnableQueue;

    private volatile boolean running = true;

    public InternalTask(RunnableQueue runnableQueue) {
        this.runnableQueue = runnableQueue;
    }

    @Override
    public void run() {

        // 如果当前状态是running 并且不是中断状态，将不断的从queue中获取Runnable，然后执行run方法（拿一个执行一个，顺序执行，执行完上一个，再拿一个继续执行）
        while( running && !Thread.currentThread().isInterrupted()){
            try {
                Runnable task = runnableQueue.take();
                task.run();
            } catch (Exception e) {
                running = false;
                break;
            }
        }
    }

    // 停止当前任务，主要会在线程池的shutdown方法使用
    public void stop() {
        this.running = false;
    }
}
