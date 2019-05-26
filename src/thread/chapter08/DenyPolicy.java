package thread.chapter08;

/**
 *   任务队列长度达到上限时的任务丢弃策略
 *   有三个内部实现
 */
public interface DenyPolicy {
    void reject(Runnable runnable, ThreadPool threadPool);

    // 直接将任务丢弃，啥也不干
    class DiscardDenyPolicy implements DenyPolicy {
        @Override
        public void reject(Runnable runnable, ThreadPool threadPool) {
            //do nothing
        }
    }

    // 抛出异常
    class AbortDenyPolicy implements DenyPolicy {
        @Override
        public void reject(Runnable runnable, ThreadPool threadPool) {
            throw new RunnableDenyException("The runnable " + runnable + " will be abort.");
        }
    }

    // 在提交者所在的线程执行
    class RunnerDenyPolicy implements DenyPolicy {
        @Override
        public void reject(Runnable runnable, ThreadPool threadPool) {
            if(!threadPool.isShutdown()){
                runnable.run();
            }
        }
    }
}
