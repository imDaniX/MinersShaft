package me.imdanix.mine.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Weighted collection class based off DarkSeraphim's answer
 * https://www.spigotmc.org/threads/probability.449617/#post-3868549
 */
public class WeightedCollection<T> {
    private final List<T> elements;

    public WeightedCollection() {
        elements = new ArrayList<>();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public void add(T element, int weight) {
        for(int i = 0; i < weight; i++)
            this.elements.add(element);
    }

    public T next() {
        return this.elements.get(ThreadLocalRandom.current().nextInt(this.elements.size()));
    }
}
