package endorh.simpleconfig.core;

import endorh.simpleconfig.ui.gui.AbstractConfigScreen;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

@Internal public class FakeEntryHolder implements ISimpleConfigEntryHolder {
	protected final SimpleConfig root;
	protected int id_gen = 0;
	
	@Internal public FakeEntryHolder(
	  SimpleConfig root
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
	public SimpleConfig getRoot() {
		return root;
	}
	
	@Override
	public <T> T get(String path) {
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
}
