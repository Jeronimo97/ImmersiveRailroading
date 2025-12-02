package cam72cam.immersiverailroading.util;

import java.util.*;

/**
 * An HashMap that triggers an event if a value is changed
 * @author poizzy
 */
public abstract class ObservableMap<K, V> extends HashMap<K, V> {
    public ObservableMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ObservableMap(int initialCapacity) {
        super(initialCapacity);
    }

    public ObservableMap() {
    }

    public ObservableMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    public abstract void onChange(K key, V oldValue, V newValue);

    @Override
    public V put(K key, V value) {
        V oldVal = super.put(key, value);
        onChange(key, oldVal, value);
        return oldVal;
    }
}
