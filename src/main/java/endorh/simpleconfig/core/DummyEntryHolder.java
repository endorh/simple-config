package endorh.simpleconfig.core;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.NoSuchConfigEntryError;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dummy entry holder containing a single entry
 */
public class DummyEntryHolder<V> implements ConfigEntryHolder {
	protected SimpleConfig root;
	
	public DummyEntryHolder(SimpleConfig root) {
		this.root = root;
	}
	
	public static <V, E extends AbstractConfigEntry<V, ?, ?>> E build(
	  ConfigEntryBuilder<V, ?, ?, ?> builder
	) {
		return build(SimpleConfigImpl.DUMMY, builder);
	}
	
	public static <V, E extends AbstractConfigEntry<V, ?, ?>> E build(
	  ConfigEntryHolder parent, ConfigEntryBuilder<V, ?, ?, ?> builder
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
	
	@Override public @NotNull SimpleConfig getRoot() {
		return root;
	}
	
	@Override public <T> T get(String path) {
		throw new UnsupportedOperationException();
	}
	
	@Override public <V> void set(String path, V value) {
		throw new UnsupportedOperationException();
	}
	
	@Override public <T> T getBaked(String path) {
		throw new UnsupportedOperationException();
	}
	
	@Override public <T> void setBaked(String path, T value) {
		throw new UnsupportedOperationException();
	}
	
	@Override public <T> void setForGUI(String path, T value) {
		throw new UnsupportedOperationException();
	}
	
	@Override public void setForGUI(String path, Number number) {
		throw new UnsupportedOperationException();
	}
	
	@Override public @Nullable AbstractConfigScreen getGUI() {
		throw new UnsupportedOperationException();
	}
	
	@Override public <G> void setGUI(String path, G value) {
		throw new UnsupportedOperationException();
	}
	
	@Override public @NotNull ConfigEntryHolder getChild(String path) {
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
	
	@Override public void markDirty(boolean dirty) {}
	@Override public boolean isDirty() {
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
