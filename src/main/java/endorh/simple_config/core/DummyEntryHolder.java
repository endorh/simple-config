package endorh.simple_config.core;

/**
 * Dummy entry holder containing a single entry
 */
public class DummyEntryHolder<V> implements ISimpleConfigEntryHolder {
	
	protected SimpleConfig root;
	protected V value;
	
	public DummyEntryHolder(SimpleConfig root, AbstractConfigEntryBuilder<V, ?, ?, ?, ?> builder) {
		this.root = root;
		this.value = builder.value;
	}
	
	public static <E extends AbstractConfigEntry<?, ?, ?, E>> E build(
	  ISimpleConfigEntryHolder parent, AbstractConfigEntryBuilder<?, ?, ?, E, ?> builder
	) {
		return builder.build(new DummyEntryHolder<>(parent.getRoot(), builder), "");
	}
	
	@Override public SimpleConfig getRoot() {
		return root;
	}
	
	@Override public <T> T get(String path) {
		//noinspection unchecked
		return (T) value;
	}
	
	@Override public <G> G getGUI(String path) {
		throw new UnsupportedOperationException();
	}
	
	@Override public <T> void doSet(String path, T value) {
		//noinspection unchecked
		this.value = (V) value;
	}
	
	@Override public void markDirty(boolean dirty) {}
	
	@Override public <G> void doSetGUI(String path, G value) {
		throw new UnsupportedOperationException();
	}
}
