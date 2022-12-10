package endorh.simpleconfig.core;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.entry.EntrySetEntryBuilder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.EntryListFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class EntrySetEntry<
  V, C, G, B extends AbstractConfigEntryBuilder<V, C, G, ?, ?, B>
> extends AbstractConfigEntry<Set<V>, Set<C>, List<G>> {
	protected static final String TOOLTIP_KEY_SUFFIX = ":help";
	protected static final String SUB_ELEMENTS_KEY_SUFFIX = ":sub";
	
	protected Class<?> innerType;
	protected Function<V, Optional<ITextComponent>> elemErrorSupplier;
	protected boolean expand;
	protected int minSize = 0;
	protected int maxSize = Integer.MAX_VALUE;
	protected final AbstractConfigEntry<V, C, G> entry;
	protected final B entryBuilder;
	protected CollectionEntryHolder holder;
	
	@Internal public EntrySetEntry(
	  ConfigEntryHolder parent, String name,
	  @Nullable Set<V> value, B entryBuilder
	) {
		super(parent, name, value);
		holder = new CollectionEntryHolder(parent.getRoot());
		this.entryBuilder = entryBuilder;
		entry = entryBuilder.build(holder, name);
		if (!entry.canBeNested())
			throw new IllegalArgumentException(
			  "Entry of type " + entry.getClass().getSimpleName() + " can not be " +
			  "nested in a set entry");
		if (translation != null)
			setTranslation(translation);
		if (tooltip != null)
			setTooltipKey(tooltip);
	}
	
	public static class Builder<
	  V, C, G,
	  S extends ConfigEntryBuilder<V, C, G, S>,
	  B extends AbstractConfigEntryBuilder<V, C, G, ?, S, B>
	> extends AbstractConfigEntryBuilder<
	  Set<V>, Set<C>, List<G>, EntrySetEntry<V, C, G, B>,
	  EntrySetEntryBuilder<V, C, G, S>,
	  Builder<V, C, G, S, B>
	> implements EntrySetEntryBuilder<V, C, G, S> {
		protected B builder;
		protected Class<?> innerType;
		protected Function<V, Optional<ITextComponent>> elemErrorSupplier = v -> Optional.empty();
		protected boolean expand;
		protected int minSize = 0;
		protected int maxSize = Integer.MAX_VALUE;
		
		@SuppressWarnings("unchecked")
		public Builder(Set<V> value, ConfigEntryBuilder<V, C, G, ?> builder) {
			this(value, (B) builder);
		}
		
		public Builder(Set<V> value, B builder) {
			super(new HashSet<>(value), EntryType.of(Set.class, builder.type));
			innerType = builder.typeClass;
			this.builder = builder.copy();
		}
		
		@Override public @NotNull EntrySetEntryBuilder<V, C, G, S> expand() {
			return expand(true);
		}
		
		@Override public @NotNull EntrySetEntryBuilder<V, C, G, S> expand(boolean expand) {
			Builder<V, C, G, S, B> copy = copy();
			copy.expand = true;
			return copy;
		}
		
		@Override public @NotNull EntrySetEntryBuilder<V, C, G, S> minSize(int minSize) {
			Builder<V, C, G, S, B> copy = copy();
			copy.minSize = minSize;
			return copy;
		}
		
		@Override public @NotNull EntrySetEntryBuilder<V, C, G, S> maxSize(int maxSize) {
			Builder<V, C, G, S, B> copy = copy();
			copy.maxSize = maxSize;
			return copy;
		}
		
		@Override public @NotNull EntrySetEntryBuilder<V, C, G, S> elemError(
		  Function<V, Optional<ITextComponent>> errorSupplier
		) {
			Builder<V, C, G, S, B> copy = copy();
			copy.elemErrorSupplier = errorSupplier;
			return copy;
		}
		
		@Override protected EntrySetEntry<V, C, G, B> buildEntry(
		  ConfigEntryHolder parent, String name
		) {
			EntrySetEntry<V, C, G, B> entry = new EntrySetEntry<>(parent, name, value, builder);
			entry.innerType = innerType;
			entry.elemErrorSupplier = elemErrorSupplier;
			entry.expand = expand;
			entry.minSize = minSize;
			entry.maxSize = maxSize;
			return entry;
		}
		
		@Override protected Builder<V, C, G, S, B> createCopy(Set<V> value) {
			Builder<V, C, G, S, B> copy = new Builder<>(new HashSet<>(value), builder);
			copy.elemErrorSupplier = elemErrorSupplier;
			copy.expand = expand;
			copy.minSize = minSize;
			copy.maxSize = maxSize;
			return copy;
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
	
	@Override public Object forActualConfig(@Nullable Set<C> value) {
		if (value == null) return null;
		return value.stream().map(entry::forActualConfig).collect(Collectors.toSet());
	}
	
	@Override @Nullable public Set<C> fromActualConfig(@Nullable Object value) {
		if (!(value instanceof Collection<?>)) return null;
		Set<C> set = new HashSet<>();
		for (Object elem: (Collection<?>) value) {
			C c = entry.fromActualConfig(elem);
			if (c == null) return null;
			set.add(c);
		}
		return set;
	}
	
	@Override public List<G> forGui(Set<V> set) {
		return set.stream().map(this::elemForGui).collect(Collectors.toList());
	}
	
	@Override public @Nullable Set<V> fromGui(@Nullable List<G> list) {
		if (list == null) return null;
		Set<V> res = new HashSet<>();
		for (G g: list) {
			V v = elemFromGui(g);
			if (v == null) return null;
			res.add(v);
		}
		return res;
	}
	
	@Override public Set<C> forConfig(Set<V> set) {
		return set.stream().map(this::elemForConfig).collect(Collectors.toSet());
	}
	
	@Override public @Nullable Set<V> fromConfig(@Nullable Set<C> set) {
		if (set == null) return null;
		Set<V> res = new HashSet<>();
		for (C c: set) {
			V v = elemFromConfig(c);
			if (v == null) return null;
			res.add(v);
		}
		return res;
	}
	
	protected C elemForConfig(V value) {
		return entry.forConfig(value);
	}
	protected @Nullable V elemFromConfig(C value) {
		return entry.fromConfig(value);
	}
	protected G elemForGui(V value) {
		return entry.forGui(value);
	}
	protected @Nullable V elemFromGui(G value) {
		return entry.fromGui(value);
	}
	
	@Override public boolean hasPresentation() {
		return super.hasPresentation() || entry.hasPresentation();
	}
	
	@Override protected Set<V> doForPresentation(Set<V> value) {
		return super.doForPresentation(value.stream().map(entry::forPresentation).collect(Collectors.toSet()));
	}
	@Override protected Set<V> doFromPresentation(Set<V> value) {
		return super.doFromPresentation(value).stream().map(entry::fromPresentation).collect(Collectors.toSet());
	}
	
	protected static ITextComponent addIndex(ITextComponent message, int index) {
		if (index < 0) return message;
		return message.deepCopy().appendString(", ").append(new TranslationTextComponent(
		  "simpleconfig.config.error.at_index",
		  new StringTextComponent(String.format("%d", index + 1)).mergeStyle(TextFormatting.DARK_AQUA)));
	}
	
	@Override public List<ITextComponent> getErrorsFromGUI(List<G> value) {
		return Stream.concat(
		  Stream.of(getErrorFromGUI(value)).filter(Optional::isPresent).map(Optional::get),
		  IntStream.range(0, value.size()).boxed()
			 .flatMap(i -> getElementErrors(i, value.get(i)).stream())
		).collect(Collectors.toList());
	}
	
	@Override public Optional<ITextComponent> getErrorFromGUI(List<G> value) {
		int size = value.size();
		if (size < minSize) {
			return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.set." + (minSize == 1? "empty" : "min_size"),
			  new StringTextComponent(String.valueOf(minSize)).mergeStyle(TextFormatting.DARK_AQUA)));
		} else if (size > maxSize) {
			return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.set.max_size",
			  new StringTextComponent(String.valueOf(maxSize)).mergeStyle(TextFormatting.DARK_AQUA)));
		}
		return super.getErrorFromGUI(value);
	}
	
	public Optional<ITextComponent> getElementError(int index, G value) {
		V elem = elemFromGui(value);
		if (elem == null) return Optional.of(
		  addIndex(new TranslationTextComponent("simpleconfig.config.error.missing_value"), index));
		return elemErrorSupplier.apply(elem).map(e -> addIndex(e, index));
	}
	
	public List<ITextComponent> getElementErrors(int index, G value) {
		return Stream.concat(
		  Stream.of(getElementError(index, value)).filter(Optional::isPresent).map(Optional::get),
		  entry.getErrorsFromGUI(value).stream()
		).collect(Collectors.toList());
	}
	
	@OnlyIn(Dist.CLIENT) protected AbstractConfigListEntry<G> buildCell(
	  ConfigFieldBuilder builder
	) {
		final AbstractConfigEntry<V, C, G> e = entryBuilder.build(holder, holder.nextName());
		e.setSaver((g, h) -> {});
		e.setDisplayName(new StringTextComponent("•"));
		e.nonPersistent = true;
		e.actualValue = e.defValue;
		final AbstractConfigListEntry<G> g = e.buildGUIEntry(builder).map(FieldBuilder::build)
		  .orElseThrow(() -> new IllegalStateException(
			 "Set config entry's sub-entry did not produce a GUI entry"));
		g.removeTag(EntryTag.NON_PERSISTENT);
		e.setGuiEntry(g);
		return g;
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		String typeComment = entry.getConfigCommentTooltip();
		if (typeComment != null) tooltips.add("Set: " + typeComment);
		return tooltips;
	}
	
	@Override protected Consumer<List<G>> createSaveConsumer() {
		return super.createSaveConsumer().andThen(l -> holder.clear());
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<FieldBuilder<List<G>, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		holder.clear();
		final EntryListFieldBuilder<G, AbstractConfigListEntry<G>>
		  entryBuilder = builder
		  .startEntryList(getDisplayName(), forGui(get()), en -> buildCell(builder))
		  .setIgnoreOrder(true)
		  .setCellErrorSupplier(this::getElementError)
		  .setExpanded(expand)
		  .setCaptionControlsEnabled(false)
		  .setInsertInFront(false);
		return Optional.of(decorate(entryBuilder));
	}
}
