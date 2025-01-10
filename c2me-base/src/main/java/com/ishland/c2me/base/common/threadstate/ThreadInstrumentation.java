package com.ishland.c2me.base.common.threadstate;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.lang.management.ThreadInfo;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadInstrumentation {

    private static final ScheduledExecutorService CLEANER = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat("ThreadStateHolderCleaner")
                    .setDaemon(true)
                    .build()
    );

    private static final ConcurrentHashMap<Thread, ThreadState> threadStateMap = new ConcurrentHashMap<>();

    private static final ThreadLocal<ThreadState> threadStateThreadLocal = ThreadLocal.withInitial(() -> getOrCreate(Thread.currentThread()));

    static {
        CLEANER.scheduleAtFixedRate(
                () -> threadStateMap.entrySet().removeIf(entry -> !entry.getKey().isAlive()),
                30,
                30,
                TimeUnit.SECONDS
        );
    }

    public static ThreadState getOrCreate(Thread thread) {
        return threadStateMap.computeIfAbsent(thread, unused -> new ThreadState());
    }

    public static ThreadState get(Thread thread) {
        return threadStateMap.get(thread);
    }

    public static ThreadState getCurrent() {
        return threadStateThreadLocal.get();
    }

    public static String printState(ThreadInfo threadInfo) {
        return printState(threadInfo.getThreadName(), threadInfo.getThreadId(), findFromTid(threadInfo.getThreadId()));
    }

    public static Set<Map.Entry<Thread, ThreadState>> entrySet() {
        return Collections.unmodifiableSet(threadStateMap.entrySet());
    }

    public static String printState(String name, long tid, ThreadState state) {
        if (state != null) {
            RunningWork[] runningWorks = state.toArray();
            if (runningWorks.length != 0) {
                StringBuilder builder = new StringBuilder();
                builder.append("Task trace for thread \"").append(name).append("\" Id=").append(tid).append(" (obtained on a best-effort basis)\n");
                for (RunningWork runningWork : runningWorks) {
                    builder.append(runningWork.toString().indent(4)); // newline included in indent()
                }
                return builder.toString();
            }
        }
        return null;
    }

    private static ThreadState findFromTid(long tid) {
        for (Map.Entry<Thread, ThreadState> entry : threadStateMap.entrySet()) {
            if (entry.getKey().threadId() == tid) {
                return entry.getValue();
            }
        }
        return null;
    }

}
