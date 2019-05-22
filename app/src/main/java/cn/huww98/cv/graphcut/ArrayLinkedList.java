package cn.huww98.cv.graphcut;

import java.util.*;

public class ArrayLinkedList extends AbstractSequentialList<Integer> implements Queue<Integer> {
    private int[] next;
    private int tailIndex;
    private int size = 0;

    private static int TAIL = -2, NOT_IN_LIST = -1;

    public ArrayLinkedList(int size) {
        this.next = new int[size + 1]; // Last element for head
        tailIndex = size;
        Arrays.fill(next, NOT_IN_LIST);
        next[headIndex()] = TAIL;
    }

    @Override
    public boolean add(Integer integer) {
        if (contains(integer)) {
            return false;
        }

        assert next[tailIndex] == TAIL;
        next[tailIndex] = integer;
        tailIndex = integer;
        next[integer] = TAIL;
        size++;
        return true;
    }

    @Override
    public ListIterator<Integer> listIterator(int index) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean contains(Object o) {
        int element = (int)o;
        return next[element] != NOT_IN_LIST;
    }

    @Override
    public boolean offer(Integer integer) {
        return add(integer);
    }

    private int headIndex() {
        return next.length - 1;
    }

    @Override
    public Integer remove() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        int element = next[headIndex()];
        next[headIndex()] = next[element];
        next[element] = NOT_IN_LIST;
        if (size() == 1) {
            tailIndex = headIndex();
        }
        size--;
        return element;
    }

    @Override
    public Integer poll() {
        if(isEmpty()) {
            return null;
        }
        return remove();
    }

    @Override
    public Integer element() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return next[headIndex()];
    }

    @Override
    public Integer peek() {
        if(isEmpty()) {
            return null;
        }
        return element();
    }
}
