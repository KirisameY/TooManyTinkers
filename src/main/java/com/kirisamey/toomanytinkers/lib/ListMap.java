package com.kirisamey.toomanytinkers.lib;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ListMap<T> {
    private final List<T> LIST = new ArrayList<>();
    private final Map<T, Integer> MAP = new HashMap<>();


    public int tryAdd(T element) {
        if (MAP.putIfAbsent(element, LIST.size()) != null) {
            return -1;
        }
        LIST.add(element);
        return LIST.size() - 1;
    }

    public T getValue(int index) {
        return LIST.get(index);
    }

    public int getIndex(T value) {
        return MAP.getOrDefault(value, -1);
    }

    public int size() {
        return LIST.size();
    }

    public void clear() {
        LIST.clear();
        MAP.clear();
    }
}
