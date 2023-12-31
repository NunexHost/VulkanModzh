package net.vulkanmod.render.chunk.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;

public class StaticQueue<T> implements Iterable<T> {
    final T[] queue;
    int position = 0;
    int limit = 0;
    final int capacity;

    @SuppressWarnings("unchecked")
    public StaticQueue(int initialCapacity) {
        this.capacity = initialCapacity;
        this.queue = (T[])(new Object[initialCapacity]);
    }

    public boolean hasNext() {
        return this.position < this.limit;
    }

    public T poll() {
        T t = this.queue[position];
        this.position++;
        return t;
    }

    public void add(T t) {
        if(t == null)
            return;

        if(limit == capacity) throw new RuntimeException("Exceeded size: "+this.capacity);
        this.queue[limit] = t;
        this.limit++;
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
            final int currentLimit = limit;

            @Override
            public boolean hasNext() {
                return pos < currentLimit;
            }

            @Override
            public T next() {
                return queue[pos++];
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
