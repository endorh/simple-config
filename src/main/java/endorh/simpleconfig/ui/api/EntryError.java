package endorh.simpleconfig.ui.api;

import net.minecraft.network.chat.Component;

public class EntryError {
	protected Component error;
	protected INavigableTarget source;
	protected AbstractConfigField<?> entry;
	
	protected EntryError(
	  Component error, INavigableTarget source
	) {
		this.error = error;
		this.source = source;
		this.entry = source.findParentEntry();
	}
	
	public static EntryError of(Component error, INavigableTarget source) {
		return new EntryError(error, source);
	}
	
	public static EntryError wrap(Component error, EntryError cause) {
		return new EntryError(error, cause.source);
	}
	
	public Component getError() {
		return error;
	}
	
	public INavigableTarget getSource() {
		return source;
	}
	
	public AbstractConfigField<?> getEntry() {
		return entry;
	}
}
