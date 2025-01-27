package com.ishland.c2me.base.common;

import com.ishland.c2me.base.ModuleEntryPoint;
import com.ishland.c2me.base.common.scheduler.SingleThreadExecutor;
import com.ishland.flowsched.executor.ExecutorManager;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class GlobalExecutors {

//    private static final C2MEForkJoinWorkerThreadFactory factory = new C2MEForkJoinWorkerThreadFactory("c2me", "C2ME worker #%d", Thread.NORM_PRIORITY - 1);
    public static final int GLOBAL_EXECUTOR_PARALLELISM = (int) ModuleEntryPoint.globalExecutorParallelism;
    private static final AtomicInteger prioritizedSchedulerCounter = new AtomicInteger(0);
    public static final ExecutorManager prioritizedScheduler = new ExecutorManager(GlobalExecutors.GLOBAL_EXECUTOR_PARALLELISM, thread -> {
        thread.setDaemon(true);
        thread.setPriority(Math.max(Thread.MIN_PRIORITY, Math.min(Thread.MAX_PRIORITY, (int) ModuleEntryPoint.threadPoolPriority)));
        thread.setName("c2me-worker-%d".formatted(prioritizedSchedulerCounter.getAndIncrement()));
    });

    public static final Executor asyncScheduler;

    static {
        final SingleThreadExecutor thread = new SingleThreadExecutor();
        thread.setDaemon(true);
        thread.setName("c2me-sched");
        thread.start();
        asyncScheduler = thread;
    }

//    public static final TaskExecutor<Runnable> asyncSchedulerTaskExecutor = TaskExecutor.create(asyncScheduler, "c2me-sched");

}
