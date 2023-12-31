package net.vulkanmod.render.chunk.util;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class CircularIntList {
    private int[] list;
    private final int startIndex;
    private int[] previous;
    private int[] next;

    public CircularIntList(int size, int startIndex) {
        this.startIndex = startcki;
        generateList(size);
    }

    private void generateList(int size) {
        int[] list = new int[size];
        this.previous = new int[size];
        this.next = new int[size];

        int k = 0;
        for (int i = startIndex; i < size; ++i) {
            list[k] = i;
            ++k;
        }
        for (int i = 0; i < startIndex; ++i) {
            list[k] = i;
            ++k;
        }

        this.previous[0] = -1;
        System.arraycopy(list, 0, this.previous, 1, size - 1);

        this.next[size - 1] = -1;
        System.arraycopy(list, 1, this.next, 0, size - 1);

        this.list = list;
    }

    public int getNext(int i) {
        return this.next[i];
    }

    public int getPrevious(int i) {
        return this.previous[i];
    }

    public Iterator<Integer> iterator() {
        return new OwnIterator();
    }

    public Iterator<Integer> rangeIterator(int startIndex, int endIndex) {
        return new RangeIterator(startIndex, endIndex);
    }

    public class OwnIterator implements Iterator<Integer> {
        private final AtomicInteger currentIndex = new AtomicInteger(-1);
        private final int maxIndex = list.length - 1;

        @Override
        public boolean hasNext() {
            return currentIndex.get() < maxIndex;
        }

        @Override
        public Integer next() {
            currentIndex.incrementAndGet();
            return list[currentIndex.get()];
        }

        public void restart() {
            currentIndex.set(-1);
        }
    }

    public class RangeIterator implements Iterator<Integer> {
        private final AtomicInteger currentIndex = new AtomicInteger();
        private final int startIndex;
        private final int maxIndex;

        public RangeIterator(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.maxIndex = endIndex;
            Validate.isTrue(this.maxIndex < list.length, "Beyond max size");
            restart();
        }

        @Override
        public boolean hasNext() {
            return currentIndex.get() < maxIndex;
        }

        @Override
        public Integer next() {
            currentIndex.incrementAndGet();
            return list[currentIndex.get()];
        }

        public void restart() {
            currentIndex.set(startIndex - 1);
        }
    }
}
