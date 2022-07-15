package endorh.simpleconfig.yaml;

import com.google.common.collect.ForwardingMap;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.HashMap;
import java.util.Map;

/**
 * Forwarding map class used for marking purposes.
 */
@Internal public class NonConfigMap<K, V> extends ForwardingMap<K, V> {
	public static <K, V> NonConfigMap<K, V> wrap(Map<K, V> map) {
		return new NonConfigMap<>(map);
	}
	
	public static <K, V> NonConfigMap<K, V> ofHashMap(int initialCapacity) {
		return wrap(new HashMap<>(initialCapacity));
	}
	
	private final Map<K, V> delegate;
	private NonConfigMap(Map<K, V> delegate) {
		this.delegate = delegate;
	}
	
	@Override protected Map<K, V> delegate() {
		return delegate;
	}
}
