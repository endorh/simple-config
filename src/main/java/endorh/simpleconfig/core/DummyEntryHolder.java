package endorh.simpleconfig.core;

import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import org.jetbrains.annotations.Nullable;

/**
 * Dummy entry holder containing a single entry
 */
public class DummyEntryHolder<V> implements ISimpleConfigEntryHolder {
	protected SimpleConfig root;
	
	public DummyEntryHolder(SimpleConfig root) {
		this.root = root;
	}
	
	public static <V, E extends AbstractConfigEntry<V, ?, ?>> E build(
	  ISimpleConfigEntryHolder parent, AbstractConfigEntryBuilder<V, ?, ?, E, ?> builder
	) {
		final E e = builder.build(new DummyEntryHolder<>(parent.getRoot()), "");
		e.nonPersistent = true;
		e.actualValue = builder.value;
		e.setSaver((v, h) -> {});
		return e;
	}
	
	@Override public SimpleConfig getRoot() {
		return root;
	}
	
	@Override public <T> T get(String path) {
		throw new UnsupportedOperationException();
	}
	
	@Override public @Nullable AbstractConfigScreen getGUI() {
		throw new UnsupportedOperationException();
	}
	@Override public boolean hasGUI(String path) {
		throw new UnsupportedOperationException();
	}
	@Override public <G> G getGUI(String path) {
		throw new UnsupportedOperationException();
	}
	@Override public <T> T getFromGUI(String path) {
		throw new UnsupportedOperationException();
	}
	@Override public <T> void doSet(String path, T value) {
		throw new UnsupportedOperationException();
	}
	
	@Override public void markDirty(boolean dirty) {}
	@Override public boolean isDirty() {
		throw new UnsupportedOperationException();
	}
	
	@Override public <G> void doSetGUI(String path, G value) {
		throw new UnsupportedOperationException();
	}
}
