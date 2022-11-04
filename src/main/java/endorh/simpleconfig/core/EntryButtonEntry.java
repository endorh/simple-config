package endorh.simpleconfig.core;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.EntryButtonEntryBuilder;
import endorh.simpleconfig.core.entry.GUIOnlyEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.impl.builders.EntryButtonFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EntryButtonEntry<V, Gui>
  extends GUIOnlyEntry<V, Gui, EntryButtonEntry<V, Gui>> {
	
	protected AbstractConfigEntry<V, ?, Gui> inner;
	protected BiConsumer<V, ConfigEntryHolder> action;
	protected Supplier<Component> buttonLabelSupplier;
	
	public EntryButtonEntry(
	  ConfigEntryHolder parent, String name,
	  AbstractConfigEntry<V, ?, Gui> inner, V value, BiConsumer<V, ConfigEntryHolder> action,
	  Class<?> typeClass
	) {
		super(parent, name, value, false, typeClass);
		if (!(inner instanceof AtomicEntry)) throw new IllegalArgumentException(
		  "Inner entry must be a key entry");
		this.inner = inner;
		this.action = action;
	}
	
	@SuppressWarnings("unchecked") protected <E extends AbstractConfigEntry<V, ?, Gui> & AtomicEntry<Gui>> E getInner() {
		return (E) inner;
	}
	
	public static class Builder<
	  V, Gui, S extends ConfigEntryBuilder<V, ?, Gui, S> & AtomicEntryBuilder,
	  B extends AbstractConfigEntryBuilder<V, ?, Gui, ?, S, B> & AtomicEntryBuilder
	  > extends GUIOnlyEntry.Builder<
	  V, Gui, EntryButtonEntry<V, Gui>,
	  EntryButtonEntryBuilder<V, Gui, S>, Builder<V, Gui, S, B>
	> implements EntryButtonEntryBuilder<V, Gui, S> {
		
		protected B inner;
		protected BiConsumer<V, ConfigEntryHolder> action;
		protected Supplier<Component> buttonLabelSupplier = () -> new TextComponent("✓");
		
		@SuppressWarnings("unchecked") public Builder(
		  S inner, BiConsumer<V, ConfigEntryHolder> action
		) {
			this((B) inner, action);
		}
		
		public Builder(
		  B inner, BiConsumer<V, ConfigEntryHolder> action
		) {
			super(inner.value, inner.typeClass);
			this.inner = inner;
			this.action = action;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<V, Gui, S, B> withAction(BiConsumer<V, ConfigEntryHolder> action) {
			this.action = action;
			return this;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<V, Gui, S, B> withAction(Consumer<V> action) {
			return withAction((v, h) -> action.accept(v));
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<V, Gui, S, B> label(String translation) {
			Builder<V, Gui, S, B> copy = copy();
			final TranslatableComponent ttc = new TranslatableComponent(translation);
			copy.buttonLabelSupplier = () -> ttc;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<V, Gui, S, B> label(Component label) {
			Builder<V, Gui, S, B> copy = copy();
			copy.buttonLabelSupplier = () -> label;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<V, Gui, S, B> label(
		  Supplier<Component> label
		) {
			Builder<V, Gui, S, B> copy = copy();
			copy.buttonLabelSupplier = label;
			return copy;
		}
		
		@Override protected final EntryButtonEntry<V, Gui> buildEntry(
		  ConfigEntryHolder parent, String name
		) {
			final EntryButtonEntry<V, Gui> entry = new EntryButtonEntry<>(
			  parent, name, DummyEntryHolder.build(parent, inner), value, action, typeClass);
			entry.buttonLabelSupplier = buttonLabelSupplier;
			return entry;
		}
		
		@Override protected Builder<V, Gui, S, B> createCopy(V value) {
			final Builder<V, Gui, S, B> copy = new Builder<>(inner.copy(value), action);
			copy.buttonLabelSupplier = buttonLabelSupplier;
			return copy;
		}
	}
	
	@Override public Gui forGui(V value) {
		return inner.forGui(value);
	}
	
	@Nullable @Override public V fromGui(@Nullable Gui value) {
		return inner.fromGui(value);
	}
	
	@SuppressWarnings("unchecked")
	public <
	  E extends AbstractConfigListEntry<Gui> & IChildListEntry,
	  B extends FieldBuilder<Gui, E, B>
	  > EntryButtonFieldBuilder<Gui, E, B> makeGUIEntry(
	  ConfigFieldBuilder builder, FieldBuilder<Gui, ?, ?> entryBuilder, Consumer<Gui> action
	) {
		return builder.startButton(getDisplayName(), (B) entryBuilder, action);
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<FieldBuilder<Gui, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		EntryButtonFieldBuilder<Gui, ?, ?> entryBuilder = makeGUIEntry(
		  builder, getInner().buildAtomicChildGUIEntry(builder),
		  g -> action.accept(fromGuiOrDefault(g), parent))
		  .withButtonLabel(buttonLabelSupplier)
		  .setIgnoreEdits(true);
		return Optional.of(entryBuilder);
	}
}
