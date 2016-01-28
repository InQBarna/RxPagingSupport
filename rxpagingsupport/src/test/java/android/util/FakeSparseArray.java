package android.util;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 28/1/16
 */
public class FakeSparseArray<T> {
    private HashMap<Integer, T> backingMap;

    public FakeSparseArray(int capacity) {
        backingMap = new HashMap<>(capacity);
    }

    public T get(int key) {
        return backingMap.get(key);
    }

    public int size() {
        return backingMap.size();
    }

    public void clear() {
        backingMap.clear();
    }

    public void put(int key, T value) {
        backingMap.put(key, value);
    }

    public int indexOfKey(int key) {
        if (backingMap.containsKey(key)) {
            return 1;
        }
        return -1;
    }

    public void delete(int key) {
        backingMap.remove(key);
    }
}
