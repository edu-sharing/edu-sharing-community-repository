package org.edu_sharing.repository.server.tools.cache;

public class QueueFactoryLocal <T> implements QueueFactory{
    @Override
    public Queue<T> createQueue(String name) {
        return new MemoryQueue<>();
    }
}
