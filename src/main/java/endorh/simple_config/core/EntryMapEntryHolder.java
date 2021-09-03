package endorh.simple_config.core;

import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class EntryMapEntryHolder
  implements ISimpleConfigEntryHolder {
	protected final SimpleConfig root;
	protected final Map<String, Object> values = new HashMap<>();
	protected int id_gen = 0;
	
	@Internal public EntryMapEntryHolder(
	  SimpleConfig root
	) {
		this.root = root;
	}
	
	public void clear() {
		values.keySet().retainAll(
		  values.keySet().stream().filter(p -> p.contains("$"))
		    .collect(Collectors.toSet()));
		id_gen = 0;
	}
	
	public String nextName() {
		return String.valueOf(id_gen++);
	}
	
	@Override
	public SimpleConfig getRoot() {
		return root;
	}
	
	@Override
	public <T> T get(String path) {
		//noinspection unchecked
		return (T) values.get(path);
	}
	
	@Override public <T> void doSet(String path, T value) {
		values.put(path, value);
	}
	
	@Override public void markDirty(boolean dirty) {}
	
	// Should never be called
	@Override public <G> G getGUI(String path) {
		throw new UnsupportedOperationException();
	}
	@Override public <G> void doSetGUI(String path, G value) {
		throw new UnsupportedOperationException();
	}
}
