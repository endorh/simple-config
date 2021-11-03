package endorh.simple_config.core;

/**
 * Dummy entry holder containing a single entry
 */
public class DummyEntryHolder<V> implements ISimpleConfigEntryHolder {
	
	protected SimpleConfig root;
	
	public DummyEntryHolder(SimpleConfig root) {
		this.root = root;
	}
	
	public static <V, E extends AbstractConfigEntry<V, ?, ?, E>> E build(
	  ISimpleConfigEntryHolder parent, AbstractConfigEntryBuilder<V, ?, ?, E, ?> builder
	) {
		final E e = builder.build(new DummyEntryHolder<>(parent.getRoot()), "");
		e.nonPersistent = true;
		e.set(builder.value);
		return e;
	}
	
	@Override public SimpleConfig getRoot() {
		return root;
	}
	
	@Override public <T> T get(String path) {
		throw new UnsupportedOperationException();
	}
	
	@Override public <G> G getGUI(String path) {
		throw new UnsupportedOperationException();
	}
	
	@Override public <T> void doSet(String path, T value) {
		throw new UnsupportedOperationException();
	}
	
	@Override public void markDirty(boolean dirty) {}
	
	@Override public <G> void doSetGUI(String path, G value) {
		throw new UnsupportedOperationException();
	}
}
