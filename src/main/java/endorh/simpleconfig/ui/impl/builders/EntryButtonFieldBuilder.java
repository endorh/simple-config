package endorh.simpleconfig.ui.impl.builders;

import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.entries.EntryButtonListEntry;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EntryButtonFieldBuilder<
  V, E extends AbstractConfigListEntry<V> & IChildListEntry,
  B extends FieldBuilder<V, E, B>
> extends FieldBuilder<V, EntryButtonListEntry<V, E>, EntryButtonFieldBuilder<V, E, B>> {
	private final B entryBuilder;
	private Consumer<V> action;
	private Supplier<Component> buttonLabelSupplier = () -> Component.literal("");
	
	public EntryButtonFieldBuilder(
	  ConfigFieldBuilder builder, Component name, B entryBuilder, Consumer<V> action
	) {
		super(EntryButtonListEntry.class, builder, name, entryBuilder.value);
		this.entryBuilder = entryBuilder;
		this.action = action;
	}
	
	public EntryButtonFieldBuilder<V, E, B> withAction(Consumer<V> action) {
		this.action = action;
		return this;
	}
	
	public EntryButtonFieldBuilder<V, E, B> withButtonLabel(Component label) {
		return withButtonLabel(() -> label);
	}
	
	public EntryButtonFieldBuilder<V, E, B> withButtonLabel(Supplier<Component> supplier) {
		buttonLabelSupplier = supplier;
		return this;
	}
	
	@Override protected EntryButtonListEntry<V, E> buildEntry() {
		return new EntryButtonListEntry<>(
		  fieldNameKey, entryBuilder.build(), action, buttonLabelSupplier);
	}
}
