package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.EntryFlag;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.impl.builders.EntryPairListBuilder;
import endorh.simpleconfig.yaml.NonConfigMap;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;

/**
 * Special config entry containing a map of values of which the values
 * are managed by another entry, as long as it's serializable to NBT,
 * and the keys by yet another that also implements
 * {@link IKeyEntry}. This excludes using as key entries other lists,
 * maps and GUI only entries, since their rendering or serialization
 * is not supported.<br>
 * Currently, serializes in the config file as a {@link CompoundNBT}
 */
public class EntryMapEntry<K, V, KC, C, KG, G,
  E extends AbstractConfigEntry<V, C, G, E>,
  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KG>,
  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>>
  extends AbstractConfigEntry<
  Map<K, V>, Map<KC, C>, List<Pair<KG, G>>, EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB>> {
	private static final Logger LOGGER = LogManager.getLogger();
	protected final KB keyEntryBuilder;
	protected final KE keyEntry;
	protected final B entryBuilder;
	protected final E entry;
	protected @Nullable List<Pair<KG, G>> guiCache;
	protected final Class<?> keyEntryTypeClass;
	protected final Class<?> entryTypeClass;
	protected final FakeEntryHolder holder;
	protected BiFunction<K, V, Optional<ITextComponent>> elemErrorSupplier = (k, v) -> Optional.empty();
	protected int minSize = 0;
	protected int maxSize = Integer.MAX_VALUE;
	protected boolean expand;
	protected boolean linked;
	
	@Internal public EntryMapEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  Map<K, V> value, @NotNull B entryBuilder, KB keyEntryBuilder
	) {
		super(parent, name, value);
		holder = new FakeEntryHolder(parent.getRoot());
		this.entryBuilder = entryBuilder;
		entryTypeClass = entryBuilder.typeClass;
		this.keyEntryBuilder = keyEntryBuilder;
		keyEntryTypeClass = keyEntryBuilder.typeClass;
		entry = entryBuilder.build(holder, name + "$ v");
		keyEntry = keyEntryBuilder.build(holder, name + "$ k");
		if (!entry.canBeNested())
			throw new IllegalArgumentException(
			  "Entry of type " + entry.getClass().getSimpleName() + " can not be " +
			  "nested in a map entry");
		if (entry.defValue == null)
			throw new IllegalArgumentException(
			  "Unsupported value type for map config entry. The values " +
			  "cannot be null");
	}
	
	public static class Builder<K, V, KC, C, KG, G,
	  E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
	  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KG>,
	  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>>
	  extends AbstractConfigEntryBuilder<
	  Map<K, V>, Map<KC, C>, List<Pair<KG, G>>,
	  EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB>,
	  Builder<K, V, KC, C, KG, G, E, B, KE, KB>> {
		protected final KB keyEntryBuilder;
		protected B entryBuilder;
		protected boolean expand;
		protected boolean linked;
		protected BiFunction<K, V, Optional<ITextComponent>> elemErrorSupplier = (k, v) -> Optional.empty();
		protected int minSize = 0;
		protected int maxSize = Integer.MAX_VALUE;
		
		public Builder(Map<K, V> value, KB keyEntryBuilder, B entryBuilder) {
			super(new LinkedHashMap<>(value), Map.class);
			this.entryBuilder = entryBuilder.copy();
			this.keyEntryBuilder = keyEntryBuilder.copy();
		}
		
		/**
		 * Display this entry expanded in the GUI by default.
		 * @see #expand(boolean)
		 */
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> expand() {
			return expand(true);
		}
		
		/**
		 * Display this entry expanded in the GUI by default.
		 * @param expand Whether to expand this entry by default.
		 * @see #expand()
		 */
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> expand(boolean expand) {
			Builder<K, V, KC, C, KG, G, E, B, KE, KB> copy = copy();
			copy.expand = expand;
			return copy;
		}
		
		/**
		 * Use a linked map to preserve the order of the entries.<br>
		 * In the config file it will be represented as a YAML ordered map.<br>
		 * <i>Note that if you manually set the value of this entry to a non-linked map
		 * the order will be lost.</i>
		 * @see #linked(boolean)
		 */
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> linked() {
			return linked(true);
		}
		
		/**
		 * Use a linked map to preserve the order of the entries.<br>
		 * In the config file it will be represented as a YAML ordered map.<br>
		 * <i>Note that if you manually set the value of this entry to a non-linked map
		 * the order will be lost.</i>
		 * @param linked Whether to use a linked map.
		 * @see #linked()
		 */
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> linked(boolean linked) {
			Builder<K, V, KC, C, KG, G, E, B, KE, KB> copy = copy();
			copy.linked = linked;
			return copy;
		}
		
		/**
		 * Set an error supplier for each entry instead of the whole map.<br>
		 * The map will be deemed invalid if a single entry is invalid.
		 * @param supplier The supplier for the error.
		 */
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> entryError(
		  BiFunction<K, V, Optional<ITextComponent>> supplier
		) {
			Builder<K, V, KC, C, KG, G, E, B, KE, KB> copy = copy();
			copy.elemErrorSupplier = supplier;
			return copy;
		}
		
		/**
		 * Set the minimum (inclusive) size of the map.
		 * @param minSize The inclusive minimum size of the map.
		 */
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> minSize(int minSize) {
			Builder<K, V, KC, C, KG, G, E, B, KE, KB> copy = copy();
			copy.minSize = minSize;
			return copy;
		}
		
		/**
		 * Set the maximum (inclusive) size of the map.
		 * @param maxSize The inclusive maximum size of the map.
		 */
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> maxSize(int maxSize) {
			Builder<K, V, KC, C, KG, G, E, B, KE, KB> copy = copy();
			copy.maxSize = maxSize;
			return copy;
		}
		
		@Override protected EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			final EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> e = new EntryMapEntry<>(
			  parent, name, new LinkedHashMap<>(value), entryBuilder, keyEntryBuilder);
			e.expand = expand;
			e.linked = linked;
			e.elemErrorSupplier = elemErrorSupplier;
			e.minSize = minSize;
			e.maxSize = maxSize;
			return e;
		}
		
		@Override protected Builder<K, V, KC, C, KG, G, E, B, KE, KB> createCopy() {
			final Builder<K, V, KC, C, KG, G, E, B, KE, KB> copy =
			  new Builder<>(new LinkedHashMap<>(value), keyEntryBuilder, entryBuilder);
			copy.expand = expand;
			copy.linked = linked;
			copy.elemErrorSupplier = elemErrorSupplier;
			copy.minSize = minSize;
			copy.maxSize = maxSize;
			return copy;
		}
	}
	
	@Override public Object forActualConfig(Map<KC, C> value) {
		if (linked) {
			return value.entrySet().stream()
			  .map(e -> Util.make(NonConfigMap.ofHashMap(1), m -> m.put(
				 keyEntry.forActualConfig(e.getKey()), entry.forActualConfig(e.getValue())))
			  ).collect(Collectors.toList());
		} else return NonConfigMap.wrap(value);
	}
	
	@Override public @Nullable Map<KC, C> fromActualConfig(@Nullable Object value) {
		if (value instanceof List) {
			//noinspection unchecked
			List<Object> seq = (List<Object>) value;
			Map<KC, C> map = new LinkedHashMap<>();
			for (Object o : seq) {
				if (o instanceof Map) {
					Map<?, ?> mm = (Map<?, ?>) o;
					if (mm.entrySet().size() != 1) return null;
					Map.Entry<?, ?> e = mm.entrySet().stream().findFirst()
					  .orElseThrow(IllegalStateException::new);
					KC key = keyEntry.fromActualConfig(e.getKey());
					C val = entry.fromActualConfig(e.getValue());
					if (key == null || val == null) return null;
					map.put(key, val);
				} else if (o instanceof Config) {
					Config config = (Config) o;
					if (config.entrySet().size() != 1) return null;
					Config.Entry e = config.entrySet().stream().findFirst()
					  .orElseThrow(IllegalStateException::new);
					KC key = keyEntry.fromActualConfig(e.getKey());
					C val = entry.fromActualConfig(e.getValue());
					if (key == null || val == null) return null;
					map.put(key, val);
				}
			}
			return map;
		} else if (value instanceof Config) {
			Map<KC, C> map = new LinkedHashMap<>();
			for (CommentedConfig.Entry e: ((CommentedConfig) value).entrySet()) {
				KC key = keyEntry.fromActualConfig(e.getKey());
				C val = entry.fromActualConfig(e.getValue());
				if (key == null || val == null) return null;
				map.put(key, val);
			}
			return map;
		} else if (value instanceof Map) {
			Map<KC, C> map = new LinkedHashMap<>();
			for (Entry<?, ?> e: ((Map<?, ?>) value).entrySet()) {
				KC key = keyEntry.fromActualConfig(e.getKey());
				C val = entry.fromActualConfig(e.getValue());
				if (key == null || val == null) return null;
				map.put(key, val);
			}
			return map;
		}
		return null;
	}
	
	@Override public Map<KC, C> forConfig(Map<K, V> value) {
		final Map<KC, C> m = new LinkedHashMap<>();
		for (Entry<K, V> e : value.entrySet())
			m.put(keyEntry.forConfig(e.getKey()), entry.forConfig(e.getValue()));
		return m;
	}
	
	@Nullable @Override @Contract("null->null")
	public Map<K, V> fromConfig(@Nullable Map<KC, C> value) {
		if (value == null) return null;
		List<Pair<K, V>> pairs = value.entrySet().stream().map(e -> Pair.of(
		  keyEntry.fromConfig(e.getKey()), entry.fromConfig(e.getValue()))
		).collect(Collectors.toList());
		if (pairs.stream().anyMatch(p -> p.getLeft() == null || p.getRight() == null)) return null;
		final Map<K, V> m = new LinkedHashMap<>();
		pairs.forEach(p -> m.put(p.getKey(), p.getValue()));
		return m;
	}
	
	@Override
	public List<Pair<KG, G>> forGui(Map<K, V> value) {
		return value.entrySet().stream().map(
		  e -> Pair.of(keyEntry.forGui(e.getKey()), entry.forGui(e.getValue()))
		).collect(Collectors.toList());
	}
	
	@Nullable @Override public Map<K, V> fromGui(@Nullable List<Pair<KG, G>> value) {
		if (value == null) return null;
		List<Pair<K, V>> pairs = value.stream()
		  .map(e -> Pair.of(keyEntry.fromGui(e.getKey()), entry.fromGui(e.getValue())))
		  .collect(Collectors.toList());
		if (pairs.stream().anyMatch(e -> e.getKey() == null || e.getValue() == null)) return null;
		// For duplicate keys, only the last is kept
		return pairs.stream().collect(
		  Collectors.toMap(Pair::getKey, Pair::getValue, (a, b) -> b, LinkedHashMap::new));
	}
	
	protected @Nullable String getMapTypeComment() {
		String keyComment = keyEntry.getConfigCommentTooltip();
		String valueComment = entry.getConfigCommentTooltip();
		return (keyComment.isEmpty()? "?" : keyComment) + " >> " +
		       (valueComment.isEmpty()? "?" : valueComment);
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		String typeComment = getMapTypeComment();
		if (typeComment != null) tooltips.add((linked? "Sorted Map: " : "Map: ") + typeComment);
		return tooltips;
	}
	
	@Override protected void buildSpec(ConfigSpec spec, String parentPath) {
		spec.define(parentPath + name, forActualConfig(forConfig(defValue)), createConfigValidator());
	}
	
	@OnlyIn(Dist.CLIENT) protected <KGE extends AbstractConfigListEntry<KG> & IChildListEntry>
	Pair<KGE, AbstractConfigListEntry<G>> buildCell(
	  ConfigEntryBuilder builder
	) {
		final KE ke = keyEntryBuilder.build(holder, holder.nextName())
		  .withSaver((g, h) -> {})
		  .withDisplayName(new StringTextComponent(""));
		ke.nonPersistent = true;
		final E e = entryBuilder.build(holder, holder.nextName())
		  .withSaver((g, h) -> {})
		  .withDisplayName(new StringTextComponent(""));
		e.nonPersistent = true;
		ke.actualValue = ke.defValue;
		e.actualValue = e.defValue;
		KGE kg = ke.buildChildGUIEntry(builder);
		final AbstractConfigListEntry<G> g = e.buildGUIEntry(builder)
		  .orElseThrow(() -> new IllegalStateException(
			 "Map config entry's sub-entry did not produce a GUI entry"));
		g.removeEntryFlag(EntryFlag.NON_PERSISTENT);
		ke.guiEntry = kg;
		e.guiEntry = g;
		return Pair.of(kg, g);
	}
	
	@Override public Optional<ITextComponent> getErrorFromGUI(List<Pair<KG, G>> value) {
		if (value.size() < minSize) {
			return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.list." + (minSize == 1? "empty" : "too_small"),
			  coloredNumber(minSize)));
		} else if (value.size() > maxSize) return Optional.of(new TranslationTextComponent(
		  "simpleconfig.config.error.list.too_large",
		  coloredNumber(maxSize)));
		return super.getErrorFromGUI(value);
	}
	
	protected static IFormattableTextComponent coloredNumber(int minSize) {
		return new StringTextComponent(String.valueOf(minSize))
		  .mergeStyle(TextFormatting.DARK_AQUA);
	}
	
	@Override public List<ITextComponent> getErrorsFromGUI(List<Pair<KG, G>> value) {
		return Stream.concat(
		  Stream.of(getErrorFromGUI(value), getDuplicateError(value))
		    .filter(Optional::isPresent).map(Optional::get),
		  value.stream().flatMap(p -> getElementErrors(p).stream())
		  ).collect(Collectors.toList());
	}
	
	protected Optional<ITextComponent> getDuplicateError(List<Pair<KG, G>> value) {
		Set<KG> set = new HashSet<>();
		for (Pair<KG, G> pair : value) {
			final KG key = pair.getKey();
			if (!set.add(key))
				return Optional.of(new TranslationTextComponent(
				  "simpleconfig.config.error.duplicate_key", key));
		}
		return Optional.empty();
	}
	
	public Optional<ITextComponent> getCellError(int index, Pair<KG, G> p) {
		// Already handled by the GUI
		// Optional<ITextComponent> e = keyEntry.getError(p.getKey());
		// if (e.isPresent()) return e;
		// e = entry.getError(p.getValue());
		// if (e.isPresent()) return e;
		K key = keyEntry.fromGui(p.getKey());
		V value = entry.fromGui(p.getValue());
		if (key == null || value == null) return Optional.of(new TranslationTextComponent(
		  "simpleconfig.config.error.missing_value"));
		return elemErrorSupplier.apply(key, value);
	}
	
	public List<Optional<ITextComponent>> getMultiCellError(List<Pair<KG, G>> gui) {
		Map<K, List<Integer>> groups = IntStream.range(0, gui.size())
		  .mapToObj(i -> Pair.of(keyEntry.fromGui(gui.get(i).getKey()), i))
		  .collect(Collectors.<Pair<K, Integer>, K, List<Integer>>toMap(
		    Pair::getKey, p -> singletonList(p.getValue()), (a, b) -> {
				 ArrayList<Integer> indices = new ArrayList<>(a);
				 indices.addAll(b);
				 return indices;
			 }));
		List<Optional<ITextComponent>> errors = gui.stream()
		  .map(p -> Optional.<ITextComponent>empty()).collect(Collectors.toList());
		groups.values().stream().filter(l -> l.size() > 1).forEach(
		  g -> g.forEach(i -> errors.set(i, Optional.of(new TranslationTextComponent(
			 "simpleconfig.config.error.duplicate_key", gui.get(i).getKey())
		  ))));
		return errors;
	}
	
	public List<ITextComponent> getElementErrors(Pair<KG, G> p) {
		List<ITextComponent> errors = keyEntry.getErrorsFromGUI(p.getKey());
		errors.addAll(entry.getErrorsFromGUI(p.getValue()));
		return errors;
	}
	
	@Override protected Consumer<List<Pair<KG, G>>> createSaveConsumer() {
		return super.createSaveConsumer().andThen(g -> guiCache = g);
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<AbstractConfigListEntry<List<Pair<KG, G>>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		List<Pair<KG, G>> guiValue = forGui(get());
		if (guiCache != null && Objects.equals(fromGui(guiValue), fromGui(guiCache)))
			guiValue = guiCache;
		final EntryPairListBuilder<KG, G, ? extends AbstractConfigListEntry<KG>, AbstractConfigListEntry<G>>
		  entryBuilder = builder
		  .startEntryPairList(getDisplayName(), guiValue, en -> buildCell(builder))
		  .setIgnoreOrder(!linked)
		  .setCellErrorSupplier(this::getCellError)
		  .setMultiCellErrorSupplier(this::getMultiCellError)
		  .setExpanded(expand);
		return Optional.of(decorate(entryBuilder).build());
	}
}
