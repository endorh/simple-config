package endorh.simpleconfig.core;

import endorh.simpleconfig.api.ISimpleConfig;
import endorh.simpleconfig.api.ISimpleConfig.NoSuchConfigEntryError;
import endorh.simpleconfig.api.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Internal public class CollectionEntryHolder implements ISimpleConfigEntryHolder {
	protected final ISimpleConfig root;
	protected int id_gen = 0;
	
	@Internal public CollectionEntryHolder(
	  ISimpleConfig root
	) {
		this.root = root;
	}
	
	public void clear() {
		id_gen = 0;
	}
	
	public String nextName() {
		return String.valueOf(id_gen++);
	}
	
	@Override
	public ISimpleConfig getRoot() {
		return root;
	}
	
	@Override
	public <T> T get(String path) {
		throw new UnsupportedOperationException();
	}
	
	@Override public <V> void setForGUI(String path, V value) {
		throw new UnsupportedOperationException();
	}
	
	@Override public void setForGUI(String path, Number number) {
		throw new UnsupportedOperationException();
	}
	
	@Override public <T> void doSet(String path, T value) {
		throw new UnsupportedOperationException();
	}
	
	@Override public void markDirty(boolean dirty) {}
	@Override public boolean isDirty() {
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
	@Override public <V> V getFromGUI(String path) {
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