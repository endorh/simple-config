package endorh.simpleconfig.core;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ISimpleConfig;
import endorh.simpleconfig.api.ISimpleConfig.NoSuchConfigEntryError;
import endorh.simpleconfig.api.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dummy entry holder containing a single entry
 */
public class DummyEntryHolder<V> implements ISimpleConfigEntryHolder {
	protected ISimpleConfig root;
	
	public DummyEntryHolder(ISimpleConfig root) {
		this.root = root;
	}
	
	public static <V, E extends AbstractConfigEntry<V, ?, ?>> E build(
	  ISimpleConfigEntryHolder parent, ConfigEntryBuilder<V, ?, ?, ?> builder
	) {
		if (!(builder instanceof AbstractConfigEntryBuilder)) throw new IllegalArgumentException(
		  "ConfigEntryBuilder is not instance of AbstractConfigEntryBuilder");
		//noinspection unchecked
		AbstractConfigEntryBuilder<V, ?, ?, E, ?, ?> b = (AbstractConfigEntryBuilder<V, ?, ?, E, ?, ?>) builder;
		final E e = b.build(new DummyEntryHolder<>(parent.getRoot()), "");
		e.nonPersistent = true;
		e.actualValue = b.getValue();
		e.setSaver((v, h) -> {});
		return e;
	}
	
	@Override public ISimpleConfig getRoot() {
		return root;
	}
	
	@Override public <T> T get(String path) {
		throw new UnsupportedOperationException();
	}
	
	@Override public <V> void setForGUI(String path, V value) {
		throw new UnsupportedOperationException();
	}
	
	@Override public void setForGUI(String path, Number number) {
		throw new UnsupportedOperationException();
	}
	
	@Override public @Nullable AbstractConfigScreen getGUI() {
		throw new UnsupportedOperationException();
	}
	
	@Override public @NotNull ISimpleConfigEntryHolder getChild(String path) {
		throw new NoSuchConfigEntryError(path);
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
	
	@Override public void reset() {
		throw new UnsupportedOperationException();
	}
	
	@Override public void reset(String path) {
		throw new UnsupportedOperationException();
	}
	
	@Override public boolean resetInGUI(String path) {
		throw new UnsupportedOperationException();
	}
	
	@Override public boolean restoreInGUI(String path) {
		throw new UnsupportedOperationException();
	}
}
