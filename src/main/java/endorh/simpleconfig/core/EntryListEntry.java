package endorh.simpleconfig.core;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.entry.EntryListEntryBuilder;
import endorh.simpleconfig.core.entry.AbstractListEntry;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.EntryListFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Special config entry containing a list of values managed
 * by another entry<br>
 * @param <V> The type of the elements of the list
 * @param <C> The type of the elements of the list facing the config
 * @param <G> The type of the elements of the list facing the GUI
 */
public class EntryListEntry
  <V, C, G, B extends AbstractConfigEntryBuilder<V, C, G, ?, ?, B>>
  extends AbstractListEntry<V, C, G, EntryListEntry<V, C, G, B>> {
	protected static final String TOOLTIP_KEY_SUFFIX = ":help";
	protected static final String SUB_ELEMENTS_KEY_SUFFIX = ":sub";
	
	protected final AbstractConfigEntry<V, C, G> entry;
	protected final B entryBuilder;
	protected CollectionEntryHolder holder;
	
	@Internal public EntryListEntry(
	  ConfigEntryHolder parent, String name,
	  @Nullable List<V> value, B entryBuilder) {
		super(parent, name, value);
		holder = new CollectionEntryHolder(parent.getRoot());
		this.entryBuilder = entryBuilder;
		entry = entryBuilder.build(holder, name);
		if (!entry.canBeNested())
			throw new IllegalArgumentException(
			  "Entry of type " + entry.getClass().getSimpleName() + " can not be " +
			  "nested in a list entry");
		if (translation != null)
			setTranslation(translation);
		if (tooltip != null)
			setTooltipKey(tooltip);
	}
	
	public static class Builder<
	  V, C, G,
	  S extends ConfigEntryBuilder<V, C, G, S>,
	  B extends AbstractConfigEntryBuilder<V, C, G, ?, S, B>
	> extends AbstractListEntry.Builder<
	  V, C, G, EntryListEntry<V, C, G, B>,
	  EntryListEntryBuilder<V, C, G, S>,
	  Builder<V, C, G, S, B>
	> implements EntryListEntryBuilder<V, C, G, S> {
		protected B builder;
		
		@SuppressWarnings("unchecked") public Builder(List<V> value, ConfigEntryBuilder<V, C, G, ?> builder) {
			this(value, (B) builder);
		}
		
		public Builder(List<V> value, B builder) {
			super(new ArrayList<>(value), builder.typeClass);
			this.builder = builder.copy();
		}
		
		@Override protected EntryListEntry<V, C, G, B> buildEntry(
		  ConfigEntryHolder parent, String name
		) {
			return new EntryListEntry<>(parent, name, value, builder);
		}
		
		@Override protected Builder<V, C, G, S, B> createCopy(List<V> value) {
			return new Builder<>(new ArrayList<>(value), builder);
		}
	}
	
	@Override public void setTranslation(String translation) {
		super.setTranslation(translation);
		if (translation != null)
			entry.setTranslation(translation + SUB_ELEMENTS_KEY_SUFFIX);
	}
	
	@Override public void setTooltipKey(String translation) {
		super.setTooltipKey(translation);
		if (tooltip != null)
			if (tooltip.endsWith(TOOLTIP_KEY_SUFFIX)) {
				entry.setTooltipKey(tooltip.substring(0, tooltip.length() - TOOLTIP_KEY_SUFFIX.length())
				                    + SUB_ELEMENTS_KEY_SUFFIX + TOOLTIP_KEY_SUFFIX);
			} else entry.setTooltipKey(tooltip + SUB_ELEMENTS_KEY_SUFFIX);
	}
	
	@Override public Object forActualConfig(@Nullable List<C> value) {
		if (value == null) return null;
		return value.stream().map(entry::forActualConfig).collect(Collectors.toList());
	}
	
	@Override @Nullable public List<C> fromActualConfig(@Nullable Object value) {
		if (!(value instanceof List<?>)) return null;
		List<C> list = new ArrayList<>();
		for (Object elem : (List<?>) value) {
			C c = entry.fromActualConfig(elem);
			if (c == null) return null;
			list.add(c);
		}
		return list;
	}
	
	@Override protected C elemForConfig(V value) {
		return entry.forConfig(value);
	}
	@Override protected @Nullable V elemFromConfig(C value) {
		return entry.fromConfig(value);
	}
	@Override protected G elemForGui(V value) {
		return entry.forGui(value);
	}
	@Override protected @Nullable V elemFromGui(G value) {
		return entry.fromGui(value);
	}
	
	@Override public List<Component> getElementErrors(int index, G value) {
		List<Component> errors = super.getElementErrors(index, value);
		entry.getErrorsFromGUI(value).stream()
		  .map(e -> addIndex(e, index))
		  .forEach(errors::add);
		return errors;
	}
	
	@Override protected @Nullable String getListTypeComment() {
		return entry.getConfigCommentTooltip();
	}
	
	@OnlyIn(Dist.CLIENT) protected AbstractConfigListEntry<G> buildCell(
	  ConfigFieldBuilder builder
	) {
		final AbstractConfigEntry<V, C, G> e = entryBuilder.build(holder, holder.nextName());
		e.setSaver((g, h) -> {});
		e.setDisplayName(new TextComponent("•"));
		e.nonPersistent = true;
		e.actualValue = e.defValue;
		final AbstractConfigListEntry<G> g = e.buildGUIEntry(builder).map(FieldBuilder::build)
		  .orElseThrow(() -> new IllegalStateException(
		    "List config entry's sub-entry did not produce a GUI entry"));
		g.removeTag(EntryTag.NON_PERSISTENT);
		e.setGuiEntry(g);
		return g;
	}
	
	@Override protected Consumer<List<G>> createSaveConsumer() {
		return super.createSaveConsumer().andThen(l -> holder.clear());
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<FieldBuilder<List<G>, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		holder.clear();
		final EntryListFieldBuilder<G, AbstractConfigListEntry<G>> valBuilder =
		  builder.startEntryList(getDisplayName(), forGui(get()), en -> buildCell(builder));
		return Optional.of(decorate(valBuilder));
	}
}
