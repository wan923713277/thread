package thread.chapter08;

public interface ThreadFactory {
    Thread createThread(Runnable runnable);

}
