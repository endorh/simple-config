package endorh.simpleconfig.ui.api;

import net.minecraft.util.text.ITextComponent;

public class EntryError {
	protected ITextComponent error;
	protected INavigableTarget source;
	protected AbstractConfigField<?> entry;
	
	protected EntryError(
	  ITextComponent error, INavigableTarget source
	) {
		this.error = error;
		this.source = source;
		this.entry = source.findParentEntry();
	}
	
	public static EntryError of(ITextComponent error, INavigableTarget source) {
		return new EntryError(error, source);
	}
	
	public static EntryError wrap(ITextComponent error, EntryError cause) {
		return new EntryError(error, cause.source);
	}
	
	public ITextComponent getError() {
		return error;
	}
	
	public INavigableTarget getSource() {
		return source;
	}
	
	public AbstractConfigField<?> getEntry() {
		return entry;
	}
}
