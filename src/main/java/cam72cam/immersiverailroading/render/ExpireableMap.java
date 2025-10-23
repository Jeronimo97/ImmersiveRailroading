package cam72cam.immersiverailroading.render;

import java.util.*;
import java.util.function.BiConsumer;

public class ExpireableMap<K,V> {
	private final Map<K, V> map = new HashMap<>();
	private final Map<K, Long> mapUsage = new HashMap<>();
	private long lastTime = timeS();

	private final int lifeSpan;
	private final boolean refreshAtAccess;
	private final BiConsumer<K, V> removal;

	public ExpireableMap() {
		this(10, true, (k, v) -> {});
	}

	public ExpireableMap(int lifeSpan) {
		this(lifeSpan, true, (k, v) -> {});
	}

	public ExpireableMap(BiConsumer<K, V> removal){
		this(10, true, removal);
	}

	public ExpireableMap(int lifeSpan, boolean refreshAtAccess){
		this(lifeSpan, refreshAtAccess, (k, v) -> {});
	}

	public ExpireableMap(int lifeSpan, boolean refreshAtAccess, BiConsumer<K, V> removal){
		this.lifeSpan = lifeSpan;
		this.refreshAtAccess = refreshAtAccess;
		this.removal = removal;
	}
	
	private static long timeS() {
		return System.currentTimeMillis() / 1000L;
	}
	
	public V get(K key) {
		synchronized(this) {
			if (lastTime + lifeSpan < timeS()) {
				// clear unused
                Set<K> ks = new HashSet<>(map.keySet());
				for (K dk : ks) {
					if (dk != key && mapUsage.get(dk) + lifeSpan < timeS()) {
						removal.accept(dk, map.get(dk));
						map.remove(dk);
						mapUsage.remove(dk);
					}
				}
				lastTime = timeS();
			}
			
			
			if (map.containsKey(key)) {
				if (refreshAtAccess) {
					mapUsage.put(key, timeS());
				}
				return map.get(key);
			}
			return null;
		}
	}

	public void put(K key, V displayList) {
		synchronized(this) {
			if (displayList == null) {
				remove(key);
			} else {
				mapUsage.put(key, timeS());
				map.put(key, displayList);
			}
		}
	}

	public void remove(K key) {
		synchronized(this) {
			if(map.containsKey(key)) {
				removal.accept(key, map.get(key));
				map.remove(key);
				mapUsage.remove(key);
			}
		}
	}

	public Collection<V> values() {
		synchronized(this) {
			if (lastTime + lifeSpan < timeS()) {
				// clear unused
                Set<K> ks = new HashSet<>(map.keySet());
				for (K dk : ks) {
					if (mapUsage.get(dk) + lifeSpan < timeS()) {
						removal.accept(dk, map.get(dk));
						map.remove(dk);
						mapUsage.remove(dk);
					}
				}
				lastTime = timeS();
			}

			return map.values();
		}
	}
}
