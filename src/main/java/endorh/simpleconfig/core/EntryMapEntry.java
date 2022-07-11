package endorh.simpleconfig.core;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.EntryFlag;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.impl.builders.EntryPairListBuilder;
import endorh.simpleconfig.core.NBTUtil.ExpectedType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
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
  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KC, KG>,
  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>>
  extends AbstractConfigEntry<
  Map<K, V>, Map<String, C>, List<Pair<KG, G>>, EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB>> {
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
	  Map<K, V> value, @Nonnull B entryBuilder, KB keyEntryBuilder
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
		if (entry.value == null)
			throw new IllegalArgumentException(
			  "Unsupported value type for map config entry. The values " +
			  "cannot be null");
		try {
			NBTUtil.toNBT(entry.forConfig(entry.value));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
			  "Unsupported value type for map config entry: "
			  + entry.typeClass.getName() + "\n  Map config entry values" +
			  "must be serializable as NBT", e);
		}
	}
	
	public static class Builder<K, V, KC, C, KG, G,
	  E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
	  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KC, KG>,
	  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>>
	  extends AbstractConfigEntryBuilder<
	  Map<K, V>, Map<String, C>, List<Pair<KG, G>>,
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
	
	public String forActualConfig(Map<String, C> value) {
		return NBTUtil.toNBT(value).toString();
	}
	
	protected @Nullable Map<String, C> fromActualConfig(@Nullable Object value) {
		if (!(value instanceof String)) return null;
		final String str = (String) value;
		try {
			CompoundNBT m = new JsonToNBT(new StringReader(str)).readStruct();
			final Map<String, C> map = new LinkedHashMap<>();
			if (linked) {
				m.getAllKeys().stream().map(
				  k -> {
					  final Matcher s = NBTUtil.SPLIT.matcher(k);
					  return s.matches() ? Triple.of(Integer.parseInt(s.group(1)), s.group(2), m.get(k))
					                     : Triple.of(Integer.MAX_VALUE, k, m.get(k));
				  }).sorted(Comparator.comparingInt(Triple::getLeft)).forEachOrdered(t -> {
					  //noinspection unchecked
					  map.put(t.getMiddle(), (C) NBTUtil.fromNBT(t.getRight(), getExpectedType().next.get(0)));
				  });
			} else {
				for (String k : m.getAllKeys())
					//noinspection unchecked
					map.put(k, (C) NBTUtil.fromNBT(m.get(k), getExpectedType().next.get(0)));
			}
			return map;
		} catch (CommandSyntaxException | IllegalArgumentException | ClassCastException e) {
			return null;
		}
	}
	
	@Override public Map<String, C> forConfig(Map<K, V> value) {
		final Map<String, C> m = new LinkedHashMap<>();
		int i = 0;
		for (Entry<K, V> e : value.entrySet()) {
			String k = keyEntry.serializeStringKey(keyEntry.forConfig(e.getKey()));
			if (linked) k = i++ + ":" + k;
			m.put(k, entry.forConfig(e.getValue()));
		}
		return m;
	}
	
	@Nullable @Override @Contract("null->null")
	public Map<K, V> fromConfig(@Nullable Map<String, C> value) {
		if (value == null) return null;
		// Invalid keys are ignored
		final Map<K, V> m = new LinkedHashMap<>();
		value.forEach((s, v) -> keyEntry.deserializeStringKey(s).ifPresent(
		  k -> m.put(keyEntry.fromConfigOrDefault(k), entry.fromConfigOrDefault(v))));
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
			if (o instanceof String) {
				final Map<String, C> pre = fromActualConfig(o);
				final Map<K, V> m = fromConfig(pre);
				if (m == null)
					return false;
				// Tolerate skipping invalid keys, but log a warning
				if (pre.size() != m.size()) LOGGER.warn(
				  "Map config entry " + getGlobalPath() + " has invalid entries, which have been ignored.");
				return isValidValue(m);
			} else return false;
		};
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forActualConfig(forConfig(value)), createConfigValidator()));
	}
	
	@Override protected void buildSpec(ConfigSpec spec, String parentPath) {
		spec.define(parentPath + name, forActualConfig(forConfig(value)), createConfigValidator());
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
		ke.actualValue = ke.value;
		e.actualValue = e.value;
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
	
	@Override public ExpectedType getExpectedType() {
		return new ExpectedType(typeClass, entry.getExpectedType());
	}
}