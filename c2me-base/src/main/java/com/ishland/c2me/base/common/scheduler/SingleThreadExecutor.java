package com.ishland.c2me.base.common.scheduler;

import com.google.common.base.Preconditions;
import io.netty.util.internal.PlatformDependent;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class SingleThreadExecutor extends Thread implements Executor {

    private final AtomicInteger size = new AtomicInteger();
    public final Queue<Runnable> queue = PlatformDependent.newMpscQueue();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Object sync = new Object();

    @Override
    public void run() {
        main_loop:
        while (true) {
            if (pollTasks()) {
                continue;
            }
            if (this.shutdown.get()) {
                return;
            }

//            // attempt to spin-wait before sleeping
//            if (!pollTasks()) {
//                Thread.interrupted(); // clear interrupt flag
//                for (int i = 0; i < 5000; i ++) {
//                    if (pollTasks()) continue main_loop;
//                    LockSupport.parkNanos("Spin-waiting for tasks", 10_000); // 100us
//                }
//            }

            synchronized (sync) {
                if (this.size.get() != 0 || this.shutdown.get()) continue main_loop;
                try {
                    sync.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private boolean pollTasks() {
        boolean hasWork = false;
        Runnable task;
        while ((task = queue.poll()) != null) {
            this.size.decrementAndGet();
            try {
                task.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            hasWork = true;
        }
        return hasWork;
    }

    @Override
    public void execute(@NotNull Runnable command) {
        Preconditions.checkNotNull(command, "command");
        final boolean wasEmpty = this.size.getAndIncrement() == 0;
        this.queue.add(command);
        if (wasEmpty) {
            synchronized (sync) {
                sync.notify();
            }
        }
    }
}
