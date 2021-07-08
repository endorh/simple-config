package endorh.simple_config.core;

import endorh.simple_config.core.entry.AbstractListEntry;
import endorh.simple_config.gui.NestedListEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Special config entry containing a list of values managed
 * by another entry<br>
 * Uses a <i>fake</i> entry holder ({@link ListEntryEntryHolder}) to
 * trick the nested entry into reading its values from a list<br>
 * It sounds hacky because it <b>is</b> hacky
 * @param <V> The type of the elements of the list
 * @param <C> The type of the elements of the list facing the config
 * @param <G> The type of the elements of the list facing the GUI
 * @param <E> The type of the entry nested within the list
 */
public class EntryListEntry
  <V, C, G, E extends AbstractConfigEntry<V, C, G, E>>
  extends AbstractListEntry<V, C, G, EntryListEntry<V, C, G, E>> {
	protected static final String TOOLTIP_KEY_SUFFIX = ".help";
	protected static final String SUB_ELEMENTS_KEY_SUFFIX = ".sub";
	
	protected final AbstractConfigEntry<V, C, G, E> entry;
	protected ListEntryEntryHolder<V, C, G, E> holder;
	
	public static class Builder<V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>>
	  extends AbstractListEntry.Builder<V, C, G, EntryListEntry<V, C, G, E>, Builder<V, C, G, E, B>> {
		protected B builder;
		
		public Builder(List<V> value, B builder) {
			super(value);
			this.builder = builder;
		}
		
		@Override
		protected EntryListEntry<V, C, G, E> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			return new EntryListEntry<>(parent, name, value, builder);
		}
	}
	
	@Internal public EntryListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable List<V> value, AbstractConfigEntryBuilder<V, C, G, E, ?> builder) {
		super(parent, name, value);
		holder = new ListEntryEntryHolder<>();
		entry = builder.build(holder, name).withSaver((g, c) -> {});
		holder.entry = entry;
		if (!entry.canBeNested()) {
			throw new IllegalArgumentException(
			  "Entry of type " + entry.getClass().getSimpleName() + " can not be " +
			  "nested in a list entry");
		}
		if (translation != null)
			this.translate(translation);
		if (tooltip != null)
			this.tooltip(tooltip);
	}
	
	@Override
	protected EntryListEntry<V, C, G, E> translate(String translation) {
		super.translate(translation);
		if (translation != null)
			entry.translate(translation + SUB_ELEMENTS_KEY_SUFFIX);
		return self();
	}
	
	@Override
	protected EntryListEntry<V, C, G, E> tooltip(String translation) {
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
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<List<G>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		holder.setValue(get());
		holder.clear();
		final NestedListEntry<G, AbstractConfigListEntry<G>> e =
		  new NestedListEntry<>(
			 getDisplayName(), forGui(get()), expand,
			 () -> this.supplyTooltip(forGui(get())),
			 saveConsumer().andThen(g -> holder.clear()),
			 () -> forGui(value), builder.getResetButtonKey(),
			 true, false,
			 (g, en) -> {
				 entry.name = holder.nameFor(g);
				 return entry.buildGUIEntry(
					builder
				 ).orElseThrow(() -> new IllegalStateException(
					"Sub entry in list entry did not generate a GUI entry"));
			 }, holder::onDelete);
		e.setRequiresRestart(requireRestart);
		e.setTooltipSupplier(() -> this.supplyTooltip(e.getValue()));
		e.setErrorSupplier(() -> this.supplyError(e.getValue()));
		return Optional.of(e);
	}
}
