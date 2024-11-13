package com.ishland.c2me.base.common.threadstate;

import java.io.Closeable;
import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadState {

    private final ArrayDeque<RunningWork> runningWorks = new ArrayDeque<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void push(RunningWork runningWork) {
        lock.writeLock().lock();
        try {
            runningWorks.push(runningWork);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void pop(RunningWork runningWork) {
        lock.writeLock().lock();
        try {
            RunningWork popped = runningWorks.peek();
            if (popped != runningWork) {
                IllegalArgumentException exception = new IllegalArgumentException("Corrupt ThreadState");
                exception.printStackTrace();
                throw exception;
            }
            runningWorks.pop();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public WorkClosable begin(RunningWork runningWork) {
        lock.writeLock().lock();
        try {
            runningWorks.push(runningWork);
            return new WorkClosable(this, runningWork);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public RunningWork[] toArray() {
        lock.readLock().lock();
        try {
            return runningWorks.toArray(RunningWork[]::new);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static record WorkClosable(ThreadState state, RunningWork work) implements Closeable {
        @Override
        public void close() {
            state.pop(work);
        }
    }

}
