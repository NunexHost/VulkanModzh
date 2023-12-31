package net.vulkanmod.render.chunk.util;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class CircularIntList {
    private int[] list;
    private final int startIndex;

    private int[] previous;
    private int[] next;

    private int currentIndex;

    public CircularIntList(int size, int startIndex) {
        this.startIndex = startIndex;

        this.generateList(size);
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

        this.list = list;
    }

    public int getNext(int i) {
        return this.next[i];
    }

    public int getPrevious(int i) {
        return this.previous[i];
    }

    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private int currentIndex = -1;

            @Override
            public boolean hasNext() {
                return currentIndex < list.length - 1;
            }

            @Override
            public Integer next() {
                currentIndex++;
                return list[currentIndex];
            }

            public int getCurrentIndex() {
                return currentIndex;
            }

            public void restart() {
                currentIndex = -1;
            }
        };
    }

    public Iterator<Integer> rangeIterator(int startIndex, int endIndex) {
        return new Iterator<Integer>() {
            private int currentIndex = startIndex - 1;

            @Override
            public boolean hasNext() {
                return currentIndex < endIndex - 1;
            }

            @Override
            public Integer next() {
                currentIndex++;
                try {
                    return list[currentIndex];
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }

            public int getCurrentIndex() {
                return currentIndex;
            }

            public void restart() {
                currentIndex = startIndex - 1;
            }
        };
    }
}
