package endorh.simple_config.core;

import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.HashMap;
import java.util.Map;

public class StringMapEntryHolder<V, C, E extends AbstractConfigEntry<V, C, ?, E>,
  B extends AbstractConfigEntryBuilder<V, C, ?, E, B>>
  implements ISimpleConfigEntryHolder {
	protected final SimpleConfig root;
	protected final B builder;
	protected final Map<String, V> values = new HashMap<>();
	protected final E entry;
	protected int id_gen = 0;
	
	@Internal public StringMapEntryHolder(
	  SimpleConfig root, B builder
	) {
		this.root = root;
		this.builder = builder;
		this.entry = builder.build(this, " ");
	}
	
	public void clear() {
		values.clear();
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
	
	@Override
	public <G> G getGUI(String path) {
		//noinspection unchecked
		return (G) entry.forGui(get(path));
	}
	
	@Override
	public <T> void doSet(String path, T value) {
		//noinspection unchecked
		values.put(path, (V) value);
	}
	
	@Override
	public void markDirty(boolean dirty) {}
}
