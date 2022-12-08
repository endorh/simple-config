package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;
import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.EntryTag;
import endorh.simpleconfig.api.entry.EntryMapEntryBuilder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.impl.builders.EntryPairListBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.yaml.NonConfigMap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
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
 * {@link AtomicEntry}. This excludes using as key entries other lists,
 * maps and GUI only entries, since their rendering or serialization
 * is not supported.<br>
 * Currently, serializes in the config file as a {@link CompoundTag}
 */
public class EntryMapEntry<K, V, KC, C, KG, G,
  B extends AbstractConfigEntryBuilder<V, C, G, ?, ?, B>,
  KB extends AbstractConfigEntryBuilder<K, KC, KG, ?, ?, KB>>
  extends AbstractConfigEntry<
  Map<K, V>, Map<KC, C>, List<Pair<KG, G>>> {
	protected final KB keyEntryBuilder;
	protected final AbstractConfigEntry<K, KC, KG> keyEntry;
	protected final B entryBuilder;
	protected final AbstractConfigEntry<V, C, G> entry;
	protected @Nullable List<Pair<KG, G>> guiCache;
	protected final Class<?> keyEntryTypeClass;
	protected final Class<?> entryTypeClass;
	protected final CollectionEntryHolder holder;
	protected BiFunction<K, V, Optional<Component>> elemErrorSupplier = (k, v) -> Optional.empty();
	protected int minSize = 0;
	protected int maxSize = Integer.MAX_VALUE;
	protected boolean expand;
	protected boolean linked;
	
	@Internal public EntryMapEntry(
	  ConfigEntryHolder parent, String name,
	  Map<K, V> value, @NotNull B entryBuilder, KB keyEntryBuilder
	) {
		super(parent, name, value);
		holder = new CollectionEntryHolder(parent.getRoot());
		this.entryBuilder = entryBuilder;
		entryTypeClass = entryBuilder.typeClass;
		this.keyEntryBuilder = keyEntryBuilder;
		keyEntryTypeClass = keyEntryBuilder.typeClass;
		entry = entryBuilder.build(holder, name + "$ v");
		keyEntry = keyEntryBuilder.build(holder, name + "$ k");
		if (!(keyEntry instanceof AtomicEntry)) throw new IllegalStateException(
		  "KeyEntryBuilder created a non-key entry: " + keyEntryBuilder.getClass().getCanonicalName());
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
	  S extends ConfigEntryBuilder<V, C, G, S>,
	  B extends AbstractConfigEntryBuilder<V, C, G, ?, S, B>,
	  KS extends ConfigEntryBuilder<K, KC, KG, KS> & AtomicEntryBuilder,
	  KB extends AbstractConfigEntryBuilder<K, KC, KG, ?, KS, KB> & AtomicEntryBuilder
	> extends AbstractConfigEntryBuilder<
	  Map<K, V>, Map<KC, C>, List<Pair<KG, G>>,
	  EntryMapEntry<K, V, KC, C, KG, G, B, KB>,
	  EntryMapEntryBuilder<K, V, KC, C, KG, G, S, KS>,
	  Builder<K, V, KC, C, KG, G, S, B, KS, KB>
	> implements EntryMapEntryBuilder<
	  K, V, KC, C, KG, G, S, KS
	> {
		protected final KB keyEntryBuilder;
		protected B entryBuilder;
		protected boolean expand;
		protected boolean linked;
		protected BiFunction<K, V, Optional<Component>> elemErrorSupplier = (k, v) -> Optional.empty();
		protected int minSize = 0;
		protected int maxSize = Integer.MAX_VALUE;
		
		@SuppressWarnings("unchecked") public <KBB extends ConfigEntryBuilder<K, KC, KG, KBB> & AtomicEntryBuilder> Builder(
		  Map<K, V> value, KBB keyEntryBuilder,
		  ConfigEntryBuilder<V, C, G, ?> entryBuilder
		) {
			this(value, (KB) keyEntryBuilder, (B) entryBuilder);
		}
		
		public Builder(Map<K, V> value, KB keyEntryBuilder, B entryBuilder) {
			super(new LinkedHashMap<>(value), EntryType.of(Map.class, keyEntryBuilder.type, entryBuilder.type));
			this.entryBuilder = entryBuilder.copy();
			this.keyEntryBuilder = keyEntryBuilder.copy();
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<K, V, KC, C, KG, G, S, B, KS, KB> expand() {
			return expand(true);
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<K, V, KC, C, KG, G, S, B, KS, KB> expand(
		  boolean expand
		) {
			Builder<K, V, KC, C, KG, G, S, B, KS, KB> copy = copy();
			copy.expand = expand;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<K, V, KC, C, KG, G, S, B, KS, KB> linked() {
			return linked(true);
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<K, V, KC, C, KG, G, S, B, KS, KB> linked(
		  boolean linked
		) {
			Builder<K, V, KC, C, KG, G, S, B, KS, KB> copy = copy();
			copy.linked = linked;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<K, V, KC, C, KG, G, S, B, KS, KB> entryError(
		  BiFunction<K, V, Optional<Component>> supplier
		) {
			Builder<K, V, KC, C, KG, G, S, B, KS, KB> copy = copy();
			copy.elemErrorSupplier = supplier;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<K, V, KC, C, KG, G, S, B, KS, KB> minSize(
		  int minSize
		) {
			Builder<K, V, KC, C, KG, G, S, B, KS, KB> copy = copy();
			copy.minSize = minSize;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<K, V, KC, C, KG, G, S, B, KS, KB> maxSize(
		  int maxSize
		) {
			Builder<K, V, KC, C, KG, G, S, B, KS, KB> copy = copy();
			copy.maxSize = maxSize;
			return copy;
		}
		
		@Override protected EntryMapEntry<K, V, KC, C, KG, G, B, KB> buildEntry(
		  ConfigEntryHolder parent, String name
		) {
			final EntryMapEntry<K, V, KC, C, KG, G, B, KB> e = new EntryMapEntry<>(
			  parent, name, new LinkedHashMap<>(value), entryBuilder, keyEntryBuilder);
			e.expand = expand;
			e.linked = linked;
			e.elemErrorSupplier = elemErrorSupplier;
			e.minSize = minSize;
			e.maxSize = maxSize;
			return e;
		}
		
		@Contract(value="_ -> new", pure=true)
		@Override protected Builder<K, V, KC, C, KG, G, S, B, KS, KB> createCopy(Map<K, V> value) {
			final Builder<K, V, KC, C, KG, G, S, B, KS, KB> copy =
			  new Builder<>(new LinkedHashMap<>(value), keyEntryBuilder, entryBuilder);
			copy.expand = expand;
			copy.linked = linked;
			copy.elemErrorSupplier = elemErrorSupplier;
			copy.minSize = minSize;
			copy.maxSize = maxSize;
			return copy;
		}
	}
	
	@Override public Object forActualConfig(@Nullable Map<KC, C> value) {
		if (value == null) return null;
		if (linked) {
			List<Map<Object, Object>> list = new ArrayList<>();
			value.forEach((k, v) -> list.add(NonConfigMap.singleton(
			  keyEntry.forActualConfig(k), entry.forActualConfig(v))));
			return list;
		} else {
			NonConfigMap<Object, Object> map = NonConfigMap.ofHashMap(value.size());
			value.forEach((k, v) -> map.put(keyEntry.forActualConfig(k), entry.forActualConfig(v)));
			return map;
		}
	}
	
	@Override public @Nullable Map<KC, C> fromActualConfig(@Nullable Object value) {
		if (value instanceof List) {
			//noinspection unchecked
			List<Object> seq = (List<Object>) value;
			Map<KC, C> map = new LinkedHashMap<>();
			for (Object o : seq) {
				if (o instanceof Map<?, ?> mm) {
					if (mm.entrySet().size() != 1) return null;
					Map.Entry<?, ?> e = mm.entrySet().stream().findFirst()
					  .orElseThrow(IllegalStateException::new);
					KC key = keyEntry.fromActualConfig(e.getKey());
					C val = entry.fromActualConfig(e.getValue());
					if (key == null || val == null) return null;
					map.put(key, val);
				} else if (o instanceof Config config) {
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
		).toList();
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
		  .map(e -> Pair.of(keyEntry.fromGui(e.getKey()), entry.fromGui(e.getValue()))).toList();
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
	  ConfigFieldBuilder builder
	) {
		final AbstractConfigEntry<K, KC, KG> ke = keyEntryBuilder.build(holder, holder.nextName());
		ke.setSaver((g, h) -> {});
		ke.setDisplayName(new TextComponent(""));
		ke.nonPersistent = true;
		final AbstractConfigEntry<V, C, G> e = entryBuilder.build(holder, holder.nextName());
		e.setSaver((g, h) -> {});
		e.setDisplayName(new TextComponent(""));
		e.nonPersistent = true;
		ke.actualValue = ke.defValue;
		e.actualValue = e.defValue;
		//noinspection unchecked
		KGE kg = (KGE) ((AtomicEntry<KG>) ke).buildAtomicChildGUIEntry(builder).build();
		final AbstractConfigListEntry<G> g = e.buildGUIEntry(builder).map(FieldBuilder::build)
		  .orElseThrow(() -> new IllegalStateException(
			 "Map config entry's sub-entry did not produce a GUI entry"));
		g.removeTag(EntryTag.NON_PERSISTENT);
		ke.setGuiEntry(kg);
		e.setGuiEntry(g);
		return Pair.of(kg, g);
	}
	
	@Override public Optional<Component> getErrorFromGUI(List<Pair<KG, G>> value) {
		if (value.size() < minSize) {
			return Optional.of(new TranslatableComponent(
			  "simpleconfig.config.error.list." + (minSize == 1? "empty" : "min_size"),
			  coloredNumber(minSize)));
		} else if (value.size() > maxSize) return Optional.of(new TranslatableComponent(
		  "simpleconfig.config.error.list.max_size",
		  coloredNumber(maxSize)));
		return super.getErrorFromGUI(value);
	}
	
	protected static MutableComponent coloredNumber(int minSize) {
		return new TextComponent(String.valueOf(minSize))
		  .withStyle(ChatFormatting.DARK_AQUA);
	}
	
	@Override public List<Component> getErrorsFromGUI(List<Pair<KG, G>> value) {
		return Stream.concat(
		  Stream.of(getErrorFromGUI(value), getDuplicateError(value))
		    .filter(Optional::isPresent).map(Optional::get),
		  value.stream().flatMap(p -> getElementErrors(p).stream())
		  ).collect(Collectors.toList());
	}
	
	protected Optional<Component> getDuplicateError(List<Pair<KG, G>> value) {
		Set<KG> set = new HashSet<>();
		for (Pair<KG, G> pair : value) {
			final KG key = pair.getKey();
			if (!set.add(key))
				return Optional.of(new TranslatableComponent(
				  "simpleconfig.config.error.duplicate_key", key));
		}
		return Optional.empty();
	}
	
	public Optional<Component> getCellError(int index, Pair<KG, G> p) {
		K key = keyEntry.fromGui(p.getKey());
		V value = entry.fromGui(p.getValue());
		if (key == null || value == null) return Optional.of(new TranslatableComponent(
		  "simpleconfig.config.error.missing_value"));
		return elemErrorSupplier.apply(key, value);
	}
	
	public List<Optional<Component>> getMultiCellError(List<Pair<KG, G>> gui) {
		Map<K, List<Integer>> groups = IntStream.range(0, gui.size())
		  .mapToObj(i -> Pair.of(keyEntry.fromGui(gui.get(i).getKey()), i))
		  .collect(Collectors.<Pair<K, Integer>, K, List<Integer>>toMap(
		    Pair::getKey, p -> singletonList(p.getValue()), (a, b) -> {
				 ArrayList<Integer> indices = new ArrayList<>(a);
				 indices.addAll(b);
				 return indices;
			 }));
		List<Optional<Component>> errors = gui.stream()
		  .map(p -> Optional.<Component>empty()).collect(Collectors.toList());
		groups.values().stream().filter(l -> l.size() > 1).forEach(
		  g -> g.forEach(i -> errors.set(i, Optional.of(new TranslatableComponent(
			 "simpleconfig.config.error.duplicate_key", gui.get(i).getKey())
		  ))));
		return errors;
	}
	
	public List<Component> getElementErrors(Pair<KG, G> p) {
		List<Component> errors = keyEntry.getErrorsFromGUI(p.getKey());
		errors.addAll(entry.getErrorsFromGUI(p.getValue()));
		return errors;
	}
	
	@Override protected Consumer<List<Pair<KG, G>>> createSaveConsumer() {
		return super.createSaveConsumer().andThen(g -> guiCache = g);
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<FieldBuilder<List<Pair<KG, G>>, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
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
		return Optional.of(decorate(entryBuilder));
	}
}
