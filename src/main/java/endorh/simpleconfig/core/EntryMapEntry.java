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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		
		public Builder(Map<K, V> value, KB keyEntryBuilder, B entryBuilder) {
			super(new LinkedHashMap<>(value), Map.class);
			this.entryBuilder = entryBuilder.copy();
			this.keyEntryBuilder = keyEntryBuilder.copy();
		}
		
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> expand() {
			return expand(true);
		}
		
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> expand(boolean expand) {
			Builder<K, V, KC, C, KG, G, E, B, KE, KB> copy = copy();
			copy.expand = expand;
			return copy;
		}
		
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> linked() {
			return linked(true);
		}
		
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> linked(boolean linked) {
			Builder<K, V, KC, C, KG, G, E, B, KE, KB> copy = copy();
			copy.linked = linked;
			return copy;
		}
		
		@Override protected EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			final EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> e = new EntryMapEntry<>(
			  parent, name, new LinkedHashMap<>(value), entryBuilder, keyEntryBuilder);
			e.expand = expand;
			e.linked = linked;
			return e;
		}
		
		@Override protected Builder<K, V, KC, C, KG, G, E, B, KE, KB> createCopy() {
			final Builder<K, V, KC, C, KG, G, E, B, KE, KB> copy =
			  new Builder<>(new LinkedHashMap<>(value), keyEntryBuilder, entryBuilder);
			copy.expand = expand;
			copy.linked = linked;
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
					((Map<?, ?>) o).entrySet().stream().findFirst().ifPresent(e -> {
						KC key = keyEntry.fromActualConfig(e.getKey());
						C val = entry.fromActualConfig(e.getValue());
						if (key == null) return;
						if (val == null) val = entry.forConfig(entry.defValue);
						map.put(key, val);
					});
				}
				if (o instanceof Config) {
					Config config = (Config) o;
					config.entrySet().stream().findFirst().ifPresent(e -> {
						KC key = keyEntry.fromActualConfig(e.getKey());
						C val = entry.fromActualConfig(e.getValue());
						if (key == null) return;
						if (val == null) val = entry.forConfig(entry.defValue);
						map.put(key, val);
					});
				}
			}
			return map;
		} else if (value instanceof Config) {
			Map<KC, C> map = new LinkedHashMap<>();
			((CommentedConfig) value).entrySet().forEach(e -> {
				KC key = keyEntry.fromActualConfig(e.getKey());
				C val = entry.fromActualConfig(e.getValue());
				if (key == null) return;
				if (val == null) val = entry.forConfig(entry.defValue);
				map.put(key, val);
			});
			return map;
		} else if (value instanceof Map) {
			Map<KC, C> map = new LinkedHashMap<>();
			((Map<?, ?>) value).forEach((k, v) -> {
				KC key = keyEntry.fromActualConfig(k);
				C val = entry.fromActualConfig(v);
				if (key == null) return;
				if (val == null) val = entry.forConfig(entry.defValue);
				map.put(key, val);
			});
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
		// Invalid keys are ignored
		final Map<K, V> m = new LinkedHashMap<>();
		value.forEach((k, v) -> m.put(keyEntry.fromConfigOrDefault(k), entry.fromConfigOrDefault(v)));
		return m;
	}
	
	@Override
	public List<Pair<KG, G>> forGui(Map<K, V> value) {
		return value.entrySet().stream().map(
		  e -> Pair.of(keyEntry.forGui(e.getKey()), entry.forGui(e.getValue()))
		).collect(Collectors.toList());
	}
	
	@Nullable @Override public Map<K, V> fromGui(@Nullable List<Pair<KG, G>> value) {
		if (value == null)
			return null;
		// For duplicate keys, only the last is kept
		return value.stream().collect(Collectors.toMap(
		  p -> keyEntry.fromGuiOrDefault(p.getKey()), p -> entry.fromGuiOrDefault(p.getValue()),
		  (a, b) -> b, LinkedHashMap::new));
	}
	
	@Override
	protected Predicate<Object> createConfigValidator() {
		return o -> {
			try {
				final Map<KC, C> pre = fromActualConfig(o);
				final Map<K, V> m = fromConfig(pre);
				if (m == null) return false;
				// Tolerate skipping invalid keys, but log a warning
				if (pre.size() != m.size()) LOGGER.warn(
				  "Map config entry " + getGlobalPath() +
				  " has invalid entries, which have been ignored.");
				return isValidValue(m);
			} catch (ClassCastException e) {
				return false;
			}
		};
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
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forActualConfig(forConfig(defValue)), createConfigValidator()));
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
	
	@Override public List<ITextComponent> getErrors(List<Pair<KG, G>> value) {
		return Stream.concat(
		  Stream.of(getError(value)).filter(Optional::isPresent).map(Optional::get),
		  value.stream().flatMap(p -> getElementErrors(p).stream())
		  ).collect(Collectors.toList());
	}
	
	@Override public Optional<ITextComponent> getError(List<Pair<KG, G>> value) {
		Optional<ITextComponent> opt = super.getError(value);
		if (opt.isPresent()) return opt;
		Set<KG> set = new HashSet<>();
		for (Pair<KG, G> pair : value) {
			final KG key = pair.getKey();
			if (!set.add(key))
				return Optional.of(new TranslationTextComponent(
				  "simpleconfig.config.error.duplicate_key", key));
		}
		return Optional.empty();
	}
	
	public Optional<ITextComponent> getCellError(Pair<KG, G> p) {
		// Already handled by the GUI
		// Optional<ITextComponent> e = keyEntry.getError(p.getKey());
		// if (e.isPresent()) return e;
		// e = entry.getError(p.getValue());
		// if (e.isPresent()) return e;
		final K k = keyEntry.fromGui(p.getKey());
		if (hasGUI()
		    && getGUI().stream()
		         .filter(entry -> Objects.equals(keyEntry.fromGui(entry.getKey()), k))
		         .count() > 1
		) return Optional.of(new TranslationTextComponent("simpleconfig.config.error.duplicate_key", k));
		return Optional.empty();
	}
	
	public List<ITextComponent> getElementErrors(Pair<KG, G> p) {
		List<ITextComponent> errors = keyEntry.getErrors(p.getKey());
		errors.addAll(entry.getErrors(p.getValue()));
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
		  .setExpanded(expand);
		return Optional.of(decorate(entryBuilder).build());
	}
}
