package endorh.simple_config.core;

import endorh.simple_config.core.entry.AbstractListEntry;
import endorh.simple_config.gui.NestedListEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Special config entry containing a list of values managed
 * by another entry<br>
 * @param <V> The type of the elements of the list
 * @param <C> The type of the elements of the list facing the config
 * @param <G> The type of the elements of the list facing the GUI
 * @param <E> The type of the entry nested within the list
 */
public class EntryListEntry
  <V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
    B extends AbstractConfigEntryBuilder<V, C, G, E, B>>
  extends AbstractListEntry<V, C, G, EntryListEntry<V, C, G, E, B>> {
	protected static final String TOOLTIP_KEY_SUFFIX = ".help";
	protected static final String SUB_ELEMENTS_KEY_SUFFIX = ".sub";
	
	protected final AbstractConfigEntry<V, C, G, E> entry;
	protected final B entryBuilder;
	protected ListEntryEntryHolder<V, C, G, E> holder;
	
	@Internal public EntryListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable List<V> value, B entryBuilder) {
		super(parent, name, value);
		holder = new ListEntryEntryHolder<>(parent.getRoot());
		this.entryBuilder = entryBuilder;
		entry = entryBuilder.build(holder, name).withSaver((g, c) -> {});
		if (!entry.canBeNested())
			throw new IllegalArgumentException(
			  "Entry of type " + entry.getClass().getSimpleName() + " can not be " +
			  "nested in a list entry");
		if (translation != null)
			this.translate(translation);
		if (tooltip != null)
			this.tooltip(tooltip);
	}
	
	public static class Builder<V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>>
	  extends AbstractListEntry.Builder<V, C, G, EntryListEntry<V, C, G, E, B>, Builder<V, C, G, E, B>> {
		protected B builder;
		
		public Builder(List<V> value, B builder) {
			super(value);
			this.builder = builder;
		}
		
		@Override
		protected EntryListEntry<V, C, G, E, B> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			return new EntryListEntry<>(parent, name, value, builder);
		}
	}
	
	@Override
	protected EntryListEntry<V, C, G, E, B> translate(String translation) {
		super.translate(translation);
		if (translation != null)
			entry.translate(translation + SUB_ELEMENTS_KEY_SUFFIX);
		return self();
	}
	
	@Override
	protected EntryListEntry<V, C, G, E, B> tooltip(String translation) {
		super.tooltip(translation);
		if (tooltip != null)
			if (tooltip.endsWith(TOOLTIP_KEY_SUFFIX))
				entry.tooltip(tooltip.substring(0, tooltip.length() - TOOLTIP_KEY_SUFFIX.length())
				              + SUB_ELEMENTS_KEY_SUFFIX + TOOLTIP_KEY_SUFFIX);
			else entry.tooltip(tooltip + SUB_ELEMENTS_KEY_SUFFIX);
		return self();
	}
	
	@Override
	protected C elemForConfig(V value) {
		return entry.forConfig(value);
	}
	
	@Override
	protected V elemFromConfig(C value) {
		return entry.fromConfig(value);
	}
	
	@Override
	protected G elemForGui(V value) {
		return entry.forGui(value);
	}
	
	@Override
	protected V elemFromGui(G value) {
		return entry.fromGui(value);
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).defineList(name, forConfig(value), v -> {
			try {
				//noinspection unchecked
				return !entry.supplyError(elemForGui(elemFromConfig((C) v))).isPresent();
			} catch (ClassCastException e) {
				return false;
			}
		}));
	}
	
	protected AbstractConfigListEntry<G> buildCell(
	  ConfigEntryBuilder builder, @Nullable G value
	) {
		final E e = entryBuilder.build(holder, holder.nextName()).withSaver((g, h) -> {})
		  .withDisplayName(new StringTextComponent("•"));
		e.set(entry.fromGuiOrDefault(value));
		final AbstractConfigListEntry<G> g = e.buildGUIEntry(builder)
		  .orElseThrow(() -> new IllegalStateException(
		    "List config entry's sub-entry did not produce a GUI entry"));
		e.guiEntry = g;
		return g;
	}
	
	@Override
	protected Consumer<List<G>> saveConsumer() {
		return super.saveConsumer().andThen(l -> holder.clear());
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<List<G>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		holder.clear();
		final NestedListEntry<G, AbstractConfigListEntry<G>> e =
		  new NestedListEntry<>(
		    getDisplayName(), forGui(get()), expand,
		    () -> this.supplyTooltip(getGUI()),
		    saveConsumer(),
		    () -> forGui(value), builder.getResetButtonKey(),
		    true, insertInTop,
		    (g, en) -> buildCell(builder, g));
		// Worked around with AbstractSimpleConfigEntryHolder#markGUIRestart()
		// e.setRequiresRestart(requireRestart);
		e.setTooltipSupplier(() -> this.supplyTooltip(e.getValue()));
		e.setErrorSupplier(() -> this.supplyError(e.getValue()));
		return Optional.of(e);
	}
}
