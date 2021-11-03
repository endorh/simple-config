package endorh.simple_config.core;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.api.IChildListEntry;
import endorh.simple_config.clothconfig2.impl.builders.EntryPairListBuilder;
import endorh.simple_config.core.NBTUtil.ExpectedType;
import endorh.simple_config.core.entry.AbstractListEntry;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static endorh.simple_config.core.NBTUtil.fromNBT;
import static endorh.simple_config.core.NBTUtil.toNBT;

public class EntryPairListEntry<K, V, KC, C, KG, G,
  E extends AbstractConfigEntry<V, C, G, E>,
  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KC, KG>,
  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>>
  extends AbstractListEntry<
    Pair<K, V>, Pair<String, C>, Pair<KG, G>, EntryPairListEntry<K, V, KC, C, KG, G, E, B, KE, KB>> {
	protected final KB keyEntryBuilder;
	protected final KE keyEntry;
	protected final B entryBuilder;
	protected final E entry;
	protected final Class<?> keyEntryTypeClass;
	protected final Class<?> entryTypeClass;
	protected final FakeEntryHolder holder;
	protected boolean expand;
	
	@Internal public EntryPairListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  List<Pair<K, V>> value, @Nonnull B entryBuilder, KB keyEntryBuilder
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
			toNBT(entry.forConfig(entry.value));
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
	  extends AbstractListEntry.Builder<
	  Pair<K, V>, Pair<String, C>, Pair<KG, G>,
	  EntryPairListEntry<K, V, KC, C, KG, G, E, B, KE, KB>,
	  Builder<K, V, KC, C, KG, G, E, B, KE, KB>> {
		protected final KB keyEntryBuilder;
		protected B entryBuilder;
		protected boolean expand;
		
		public Builder(List<Pair<K, V>> value, KB keyEntryBuilder, B entryBuilder) {
			super(value, Map.class);
			this.entryBuilder = entryBuilder;
			this.keyEntryBuilder = keyEntryBuilder;
		}
		
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> expand() {
			return expand(true);
		}
		
		public Builder<K, V, KC, C, KG, G, E, B, KE, KB> expand(boolean expand) {
			Builder<K, V, KC, C, KG, G, E, B, KE, KB> copy = copy();
			copy.expand = expand;
			return copy;
		}
		
		@Override protected EntryPairListEntry<K, V, KC, C, KG, G, E, B, KE, KB> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			final EntryPairListEntry<K, V, KC, C, KG, G, E, B, KE, KB> e = new EntryPairListEntry<>(
			  parent, name, value, entryBuilder, keyEntryBuilder);
			e.expand = expand;
			return e;
		}
		
		@Override protected Builder<K, V, KC, C, KG, G, E, B, KE, KB> createCopy() {
			final Builder<K, V, KC, C, KG, G, E, B, KE, KB> copy =
			  new Builder<>(value, keyEntryBuilder.copy(), entryBuilder.copy());
			copy.expand = expand;
			return copy;
		}
	}
	
	public String forActualConfig(@Nullable List<Pair<String, C>> value) {
		if (value == null) return null;
		final Map<String, C> m = new HashMap<>();
		int i = 0;
		for (Pair<String, C> p : value) m.put(i++ + ":" + p.getKey(), p.getValue());
		return toNBT(m).toString();
	}
	
	protected @Nullable List<Pair<String, C>> fromActualConfig(@Nullable Object value) {
		if (!(value instanceof String)) return null;
		final String str = (String) value;
		try {
			CompoundNBT m = new JsonToNBT(new StringReader(str)).readStruct();
			final List<Pair<String, C>> list = new ArrayList<>();
			m.keySet().stream().map(k -> {
				final Matcher s = NBTUtil.SPLIT.matcher(k);
				return s.matches() ? Triple.of(Integer.parseInt(s.group(1)), s.group(2), m.get(k))
				                   : Triple.of(Integer.MAX_VALUE, k, m.get(k));
			}).sorted(Comparator.comparingInt(Triple::getLeft)).forEachOrdered(t -> {
				//noinspection unchecked
				list.add(Pair.of(t.getMiddle(), (C) fromNBT(t.getRight(), getExpectedType().next.get(0))));
			});
			return list;
		} catch (CommandSyntaxException | IllegalArgumentException | ClassCastException e) {
			return null;
		}
	}
	
	@Override public List<Pair<String, C>> forConfig(List<Pair<K, V>> value) {
		return value.stream().map(p -> Pair.of(
		  keyEntry.serializeStringKey(keyEntry.forConfig(p.getKey())), entry.forConfig(p.getValue())
		)).collect(Collectors.toList());
	}
	
	@Nullable @Override @Contract("null->null")
	public List<Pair<K, V>> fromConfig(@Nullable List<Pair<String, C>> value) {
		if (value == null) return null;
		final List<Pair<K, V>> l = new ArrayList<>();
		for (Pair<String, C> p : value)
			keyEntry.deserializeStringKey(p.getKey()).ifPresent(
			  k -> l.add(Pair.of(keyEntry.fromConfigOrDefault(k), entry.fromConfigOrDefault(p.getValue()))));
		return l;
	}
	
	@Override
	public List<Pair<KG, G>> forGui(List<Pair<K, V>> value) {
		return value.stream().map(
		  p -> Pair.of(keyEntry.forGui(p.getKey()), entry.forGui(p.getValue()))
		).collect(Collectors.toList());
	}
	
	@Nullable @Override public List<Pair<K, V>> fromGui(@Nullable List<Pair<KG, G>> value) {
		if (value == null) return null;
		return value.stream().map(
		  p -> Pair.of(keyEntry.fromGuiOrDefault(p.getKey()), entry.fromGuiOrDefault(p.getValue()))
		).collect(Collectors.toList());
	}
	
	@Override
	protected Predicate<Object> configValidator() {
		return o -> {
			if (o instanceof String) {
				final List<Pair<String, C>> pre = fromActualConfig(o);
				final List<Pair<K, V>> l = fromConfig(pre);
				if (l == null) return false;
				return !supplyError(forGui(l)).isPresent();
			} else return false;
		};
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forActualConfig(forConfig(value)), configValidator()));
	}
	
	@Override protected void buildSpec(ConfigSpec spec, String parentPath) {
		spec.define(parentPath + name, forActualConfig(forConfig(value)), configValidator());
	}
	
	@OnlyIn(Dist.CLIENT) protected <KGE extends AbstractConfigListEntry<KG> & IChildListEntry>
	Pair<KGE, AbstractConfigListEntry<G>> buildCell(ConfigEntryBuilder builder) {
		final KE ke = keyEntryBuilder.build(holder, holder.nextName())
		  .withSaver((g, h) -> {})
		  .withDisplayName(new StringTextComponent(""));
		ke.nonPersistent = true;
		final E e = entryBuilder.build(holder, holder.nextName()).withSaver((g, h) -> {})
		  .withDisplayName(new StringTextComponent(""));
		e.nonPersistent = true;
		ke.set(ke.value);
		e.set(e.value);
		KGE kg = ke.buildChildGUIEntry(builder);
		final AbstractConfigListEntry<G> g = e.buildGUIEntry(builder)
		  .orElseThrow(() -> new IllegalStateException(
			 "Map config entry's sub-entry did not produce a GUI entry"));
		ke.guiEntry = kg;
		e.guiEntry = g;
		return Pair.of(kg, g);
	}
	
	public Optional<ITextComponent> supplyElementError(Pair<KG, G> p) {
		Optional<ITextComponent> e = keyEntry.supplyError(p.getKey());
		if (e.isPresent()) return e;
		return entry.supplyError(p.getValue());
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<AbstractConfigListEntry<List<Pair<KG, G>>>> buildGUIEntry(ConfigEntryBuilder builder) {
		final EntryPairListBuilder<KG, G, ? extends AbstractConfigListEntry<KG>, AbstractConfigListEntry<G>>
		  entryBuilder = builder
		  .startEntryPairList(getDisplayName(), forGui(get()), en -> buildCell(builder))
		  .setIgnoreOrder(true)
		  .setCellErrorSupplier(this::supplyElementError)
		  .setExpanded(expand);
		return Optional.of(decorate(entryBuilder).build());
	}
	
	@Override public ExpectedType getExpectedType() {
		return new ExpectedType(typeClass, entry.getExpectedType());
	}
}