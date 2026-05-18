package deque;

import java.util.Iterator;

public class ArrayDeque<T> implements Deque<T>, Iterable<T> {

    private T[] items;
    private int size;
    private int nextFirst;
    private int nextLast;
    private static final int INITIAL_CAPACITY = 8;
    private static final double MIN_USAGE_RATIO = 0.25;

    /** Creates an empty array deque. */
    public ArrayDeque() {
        items = (T[]) new Object[INITIAL_CAPACITY];
        size = 0;
        nextFirst = 4;
        nextLast = 5;
    }

    /** Returns the index immediately before the given index in the circular array. */
    private int minusOne(int index) {
        return (index - 1 + items.length) % items.length;
    }

    /** Returns the index immediately after the given index in the circular array. */
    private int plusOne(int index) {
        return (index + 1) % items.length;
    }

    /** Resizes the underlying array to the target capacity. */
    private void resize(int capacity) {
        T[] newItems = (T[]) new Object[capacity];
        int current = plusOne(nextFirst);
        // items[nextFirst + 1, nextLast - 1] to newItems[0, size - 1]
        for (int i = 0; i < size; i++) {
            newItems[i] = items[current];
            current = plusOne(current);
        }
        items = newItems;
        nextFirst = capacity - 1;
        nextLast = size;
    }

    @Override
    public void addFirst(T item) {
        if (size == items.length) {
            resize(items.length * 2);
        }
        items[nextFirst] = item;
        nextFirst = minusOne(nextFirst);
        size += 1;
    }

    @Override
    public void addLast(T item) {
        if (size == items.length) {
            resize(items.length * 2);
        }
        items[nextLast] = item;
        nextLast = plusOne(nextLast);
        size += 1;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void printDeque() {
        int current = plusOne(nextFirst);
        for (int i = 0; i < size; i++) {
            System.out.print(items[current]);
            if (i < size - 1) {
                System.out.print(" ");
            }
            current = plusOne(current);
        }
        System.out.println();
    }

    /** Downsizes the array if usage is below 25% for arrays of length >= 16. */
    private void checkDownsize() {
        if (items.length >= 16 && size < items.length * MIN_USAGE_RATIO) {
            resize(items.length / 2);
        }
    }

    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        nextFirst = plusOne(nextFirst);
        T removed = items[nextFirst];
        items[nextFirst] = null;
        size -= 1;
        checkDownsize();
        return removed;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        nextLast = minusOne(nextLast);
        T removed = items[nextLast];
        items[nextLast] = null;
        size -= 1;
        checkDownsize();
        return removed;
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        int actualIndex = (plusOne(nextFirst) + index) % items.length;
        return items[actualIndex];
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    private class ArrayDequeIterator implements Iterator<T> {
        private int pos;

        ArrayDequeIterator() {
            pos = 0;
        }

        @Override
        public boolean hasNext() {
            return pos < size;
        }

        @Override
        public T next() {
            T item = get(pos);
            pos += 1;
            return item;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Deque)) {
            return false;
        }
        Deque<T> other = (Deque<T>) o;
        if (size() != other.size()) {
            return false;
        }
        for (int i = 0; i < size(); i++) {
            T myItem = get(i);
            T otherItem = other.get(i);
            if (!myItem.equals(otherItem)) {
                return false;
            }
        }
        return true;
    }
}
