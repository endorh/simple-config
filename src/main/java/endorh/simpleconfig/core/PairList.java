package endorh.simpleconfig.core;

import com.google.common.collect.ForwardingList;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class PairList<K, V> extends ForwardingList<Pair<K, V>> {
	private final List<Pair<K, V>> delegate;
	
	public PairList() {
		this(new ArrayList<>());
	}
	
	public PairList(int capacity) {
		this(new ArrayList<>(capacity));
	}
	
	public PairList(List<Pair<K, V>> delegate) {
		this.delegate = delegate;
	}
	
	@Override protected List<Pair<K, V>> delegate() {
		return delegate;
	}
	
	public K getKey(int pos) {
		return delegate.get(pos).getLeft();
	}
	
	public V getValue(int pos) {
		return delegate.get(pos).getRight();
	}
	
	public int indexOfKey(K key) {
		int i = 0;
		for (Pair<K, V> pair: delegate) {
			if (Objects.equals(pair.getKey(), key)) return i;
			i++;
		}
		return -1;
	}
	
	public int indexOfValue(V value) {
		int i = 0;
		for (Pair<K, V> pair: delegate) {
			if (Objects.equals(pair.getValue(), value)) return i;
			i++;
		}
		return -1;
	}
	
	public void forEach(BiConsumer<K, V> a) {
		for (Pair<K, V> pair: delegate) a.accept(pair.getKey(), pair.getValue());
	}
}
