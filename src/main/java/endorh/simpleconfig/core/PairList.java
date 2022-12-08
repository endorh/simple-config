package endorh.simpleconfig.core;

import com.google.common.collect.ForwardingList;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public class PairList<K, V> extends ForwardingList<Pair<K, V>> {
	private final List<Pair<K, V>> delegate;
	
	public PairList() {
		delegate = new ArrayList<>();
	}
	
	public PairList(int capacity) {
		delegate = new ArrayList<>(capacity);
	}
	
	public PairList(List<Pair<K, V>> delegate) {
		this.delegate = new ArrayList<>(delegate);
	}
	
	@Override protected @NotNull List<Pair<K, V>> delegate() {
		return delegate;
	}
	
	public K getKey(int pos) {
		return delegate.get(pos).getLeft();
	}
	
	public V getValue(int pos) {
		return delegate.get(pos).getRight();
	}
	
	public List<V> get(K key) {
		return delegate.stream()
		  .filter(p -> Objects.equals(key, p.getKey()))
		  .map(Pair::getValue)
		  .collect(Collectors.toList());
	}
	
	public @Nullable V getFirst(K key) {
		return getFirstOrDefault(key, null);
	}
	
	public V getFirstOrDefault(K key, V def) {
		return delegate.stream()
		  .filter(p -> Objects.equals(key, p.getKey()))
		  .map(Pair::getValue)
		  .findFirst().orElse(def);
	}
	
	public List<K> getKeys(V value) {
		return delegate.stream()
		  .filter(p -> Objects.equals(value, p.getValue()))
		  .map(Pair::getKey)
		  .collect(Collectors.toList());
	}
	
	public @Nullable K getFirstKey(V value) {
		return getFirstKeyOrDefault(value, null);
	}
	
	public K getFirstKeyOrDefault(V value, K def) {
		return delegate.stream()
		  .filter(p -> Objects.equals(value, p.getValue()))
		  .map(Pair::getKey)
		  .findFirst().orElse(def);
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
	
	public List<K> keyList() {
		return delegate.stream().map(Pair::getKey).collect(Collectors.toList());
	}
	
	public List<V> valueList() {
		return delegate.stream().map(Pair::getValue).collect(Collectors.toList());
	}
	
	public Map<K, V> toMap() {
		return delegate.stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}
	
	public Map<K, V> toMap(BinaryOperator<V> mergeFunction) {
		return delegate.stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue, mergeFunction));
	}
	
	public void forEach(BiConsumer<K, V> a) {
		for (Pair<K, V> pair: delegate) a.accept(pair.getKey(), pair.getValue());
	}
}
