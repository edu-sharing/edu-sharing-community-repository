package org.edu_sharing.repository.server.tools.cache;


import java.util.concurrent.ConcurrentLinkedQueue;
import org.jetbrains.annotations.NotNull;

public class MemoryQueue<T> implements Queue<T>{

    java.util.Queue<T> queue = new ConcurrentLinkedQueue<>();

    @Override
    public T poll() {
        return queue.poll();
    }

    @Override
    public boolean offer(@NotNull T e) {
        return queue.offer( e);
    }

    @Override
    public int size() {
        return queue.size();
    }
}
