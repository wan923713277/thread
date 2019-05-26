package thread.chapter08;

/**
 * 创建线程的工厂
 */

@FunctionalInterface        //java8注解，函数式接口
public interface ThreadFactory {
    Thread createThread(Runnable runnable);

}
