package net.vulkanmod.render.chunk.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;

public static class StaticQueue<T> implements Iterable<T> {
    final int[] queue;
    int position = 0;
    int limit = 0;
    final int capacity;

    public StaticQueue() {
        this(1024);
    }

    @SuppressWarnings("unchecked")
    public StaticQueue(int initialCapacity) {
        this.capacity = initialCapacity;

        this.queue = new int[initialCapacity];
    }

    public boolean hasNext() {
        return this.position < this.limit;
    }

    public T poll() {
        return (T) this.queue[this.position++];
    }

    public void add(T t) {
        if(t == null)
            return;

        if(limit == capacity) throw new RuntimeException("Exceeded size: "+this.capacity);
        this.queue[this.limit++] = (t == null ? 0 : 1); // Mantém um valor não nulo para indicar que não está vazio
    }

    public int size() {
        return limit;
    }

    public void clear() {
        this.position = 0;
        this.limit = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int pos = 0;
            final int limit = StaticQueue.this.limit;

            @Override
            public boolean hasNext() {
                return pos < limit;
            }

            @Override
            public T next() {
                return (T) StaticQueue.this.queue[pos++];
            }
        };
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        for(int i = 0; i < this.limit; ++i) {
            action.accept(this.queue[i]);
        }
    }
}
