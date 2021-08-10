package endorh.simple_config.core;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simple_config.core.entry.IAbstractStringKeyEntry;
import endorh.simple_config.gui.StringPairListEntry;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.nbt.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Special config entry containing a map of values of which the values
 * are managed by another entry, as long as its serializable to NBT,
 * and the keys by yet another that also implements
 * {@link IAbstractStringKeyEntry}, that is, it's serializable as a string<br>
 * Currently serializes in the config file as a {@link CompoundNBT}
 */
public class StringToEntryMapEntry<K, V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
  KE extends AbstractConfigEntry<K, ?, ?, KE> & IAbstractStringKeyEntry<K>,
  KB extends AbstractConfigEntryBuilder<K, ?, ?, KE, KB>>
  extends AbstractConfigEntry<Map<K, V>, Map<String, V>, List<Pair<String, G>>,
  StringToEntryMapEntry<K, V, C, G, E, B, KE, KB>> {
	protected final Map<String, E> entries = new HashMap<>();
	protected final B entryBuilder;
	protected final E entry;
	protected final Class<?> entryTypeClass;
	protected final KB keyEntryBuilder;
	protected final KE keyEntry;
	protected final StringMapEntryHolder<V, C, E> holder;
	protected boolean expand;
	
	@Internal public StringToEntryMapEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  Map<K, V> value, @Nonnull B entryBuilder, KB keyEntryBuilder
	) {
		super(parent, name, value);
		this.entryBuilder = entryBuilder;
		entryTypeClass = entryBuilder.typeClass;
		this.keyEntryBuilder = keyEntryBuilder;
		entry = entryBuilder.build(parent, name + "$ v");
		keyEntry = keyEntryBuilder.build(parent, name + "$ k");
		holder = new StringMapEntryHolder<>(parent.getRoot());
		if (!entry.canBeNested())
			throw new IllegalArgumentException(
			  "Entry of type " + entry.getClass().getSimpleName() + " can not be " +
			  "nested in a map entry");
		if (entry.value == null)
			throw new IllegalArgumentException(
			  "Unsupported value type for map config entry. The values " +
			  "cannot be null");
		try {
			toNBT(entry.value);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
			  "Unsupported value type for map config entry: "
			  + entry.typeClass.getName() + "\n  Map config entry values" +
			  "must be serializable as NBT", e);
		}
	}
	
	public static class Builder<K, V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
	  KE extends AbstractConfigEntry<K, ?, ?, KE> & IAbstractStringKeyEntry<K>,
	  KB extends AbstractConfigEntryBuilder<K, ?, ?, KE, KB>>
	  extends AbstractConfigEntryBuilder<Map<K, V>, Map<String, V>,
	  List<Pair<String, G>>, StringToEntryMapEntry<K, V, C, G, E, B, KE, KB>,
	  Builder<K, V, C, G, E, B, KE, KB>> {
		protected final KB keyEntryBuilder;
		protected B entryBuilder;
		protected boolean expand;
		
		public Builder(Map<K, V> value, KB keyEntryBuilder, B entryBuilder) {
			super(value, Map.class);
			this.entryBuilder = entryBuilder;
			this.keyEntryBuilder = keyEntryBuilder;
		}
		
		public Builder<K, V, C, G, E, B, KE, KB> expand() {
			this.expand = true;
			return this;
		}
		
		@Override
		protected StringToEntryMapEntry<K, V, C, G, E, B, KE, KB> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			final StringToEntryMapEntry<K, V, C, G, E, B, KE, KB> e = new StringToEntryMapEntry<>(
			  parent, name, value, entryBuilder, keyEntryBuilder);
			e.expand = expand;
			return e;
		}
	}
	
	@Override
	protected Map<K, V> get(ConfigValue<?> spec) {
		return fromConfigOrDefault(fromActualConfig((String)spec.get()));
	}
	
	@Override
	protected void set(ConfigValue<?> spec, Map<K, V> value) {
		//noinspection unchecked
		((ConfigValue<String>) spec).set(forActualConfig(forConfig(value)));
	}
	
	protected String forActualConfig(Map<String, V> value) {
		final Map<String, C> m = value.entrySet().stream()
		  .collect(Collectors.toMap(Entry::getKey, e -> entry.forConfig(e.getValue())));
		return toNBT(m).toString();
	}
	
	protected @Nullable Map<String, V> fromActualConfig(String value) {
		if (value == null) return null;
		try {
			return mapFromNBT(new JsonToNBT(new StringReader(value)).readStruct());
		} catch (CommandSyntaxException | IllegalArgumentException e) {
			return null;
		}
	}
	
	@Override
	protected Map<String, V> forConfig(Map<K, V> value) {
		final Map<String, V> m = new HashMap<>();
		value.forEach((k, v) -> m.put(keyEntry.serializeStringKey(k), v));
		return m;
	}
	
	@Nullable
	@Override
	@Contract("null->null")
	protected Map<K, V> fromConfig(@Nullable Map<String, V> value) {
		if (value == null) return null;
		// Invalid keys are ignored
		final Map<K, V> m = new HashMap<>();
		value.forEach((s, v) -> keyEntry.deserializeStringKey(s).ifPresent(k -> m.put(k, v)));
		return m;
	}
	
	protected static INBT toNBT(Object o) {
		if (o instanceof INBT) {
			return (INBT) o;
		} else if (o instanceof Map) {
			final CompoundNBT c = new CompoundNBT();
			for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
				if (!(e.getKey() instanceof String))
					throw new IllegalArgumentException(
					  "Unsupported map key type: " + e.getKey().getClass().getName());
				c.put((String) e.getKey(), toNBT(e.getValue()));
			}
			return c;
		} else if (o instanceof String) {
			return StringNBT.valueOf((String) o);
		} else if (o instanceof Byte) {
			return ByteNBT.valueOf((Byte) o);
		} else if (o instanceof Integer) {
			return IntNBT.valueOf((Integer) o);
		} else if (o instanceof Double) {
			return DoubleNBT.valueOf((Double) o);
		} else if (o instanceof Long) {
			return LongNBT.valueOf((Long) o);
		} else if (o instanceof Short) {
			return ShortNBT.valueOf((Short) o);
		} else if (o instanceof Float) {
			return FloatNBT.valueOf((Float) o);
		} else if (o instanceof int[]) {
			return new IntArrayNBT((int[]) o);
		} else if (o instanceof byte[]) {
			return new ByteArrayNBT((byte[]) o);
		} else if (o instanceof long[]) {
			return new LongArrayNBT((long[]) o);
		} else if (o instanceof List) {
			final ListNBT list = new ListNBT();
			for (Object e : (List<?>) o)
				list.add(toNBT(e));
			return list;
		} else throw new IllegalArgumentException(
		  "Unsupported value for NBT serialization: " + o.getClass().getName());
	}
	
	protected Map<String, V> mapFromNBT(CompoundNBT m) {
		final Map<String, V> map = new HashMap<>();
		try {
			for (String k : m.keySet()) {
				//noinspection unchecked
				map.put(k, entry.fromConfig((C) fromNBT(m.get(k))));
			}
			return map;
		} catch (ClassCastException e) {
			return null;
		}
	}
	
	protected static Object fromNBT(INBT o) {
		if (o instanceof CompoundNBT) {
			final Map<String, Object> m = new HashMap<>();
			final CompoundNBT c = (CompoundNBT) o;
			INBTType<?> type = null;
			for (String k : c.keySet()) {
				final INBT e = c.get(k);
				if (e == null) throw new IllegalStateException("Null NBT entry in compound");
				if (type == null)
					type = e.getType();
				else if (type != e.getType())
					throw new IllegalArgumentException("Deserialized NBT compound has values of different types");
				m.put(k, fromNBT(e));
			}
			return m;
		} else if (o instanceof StringNBT) {
			return o.getString();
		} else if (o instanceof ByteNBT) {
			return ((ByteNBT) o).getByte();
		} else if (o instanceof IntNBT) {
			return ((IntNBT) o).getInt();
		} else if (o instanceof DoubleNBT) {
			return ((DoubleNBT) o).getDouble();
		} else if (o instanceof LongNBT) {
			return ((LongNBT) o).getLong();
		} else if (o instanceof ShortNBT) {
			return ((ShortNBT) o).getShort();
		} else if (o instanceof FloatNBT) {
			return ((FloatNBT) o).getFloat();
		} else if (o instanceof ByteArrayNBT) {
			return ((ByteArrayNBT) o).getByteArray();
		} else if (o instanceof IntArrayNBT) {
			return ((IntArrayNBT) o).getIntArray();
		} else if (o instanceof LongArrayNBT) {
			return ((LongArrayNBT) o).getAsLongArray();
		} else if (o instanceof ListNBT) {
			final List<Object> list = new ArrayList<>();
			final ListNBT l = (ListNBT) o;
			INBTType<?> type = null;
			for (INBT e : l) {
				if (type == null)
					type = e.getType();
				else if (type != e.getType())
					throw new IllegalArgumentException("Deserialized NBT list has elements of different types");
				list.add(fromNBT(e));
			}
			return list;
		} else throw new IllegalArgumentException(
		  "Unsupported NBT type for deserialization: " + o.getType().getName());
	}
	
	@Override
	protected List<Pair<String, G>> forGui(Map<K, V> value) {
		return value.entrySet().stream().map(
		  e -> Pair.of(keyEntry.serializeStringKey(e.getKey()), entry.forGui(e.getValue()))
		).collect(Collectors.toList());
	}
	
	@Nullable
	@Override
	protected Map<K, V> fromGui(@Nullable List<Pair<String, G>> value) {
		if (value == null)
			return null;
		try {
			final Map<K, V> m = new HashMap<>();
			value.forEach(
			  p -> keyEntry.deserializeStringKey(p.getKey()).ifPresent(
			    k -> m.put(k, entry.fromGuiOrDefault(p.getValue()))));
			return m;
		} catch (IllegalStateException e) { // Duplicate key
			return null;
		}
	}
	
	@Override
	protected Predicate<Object> configValidator() {
		return o -> {
			if (o instanceof String) {
				final Map<String, V> pre = fromActualConfig((String) o);
				final Map<K, V> m = fromConfig(pre);
				if (m == null)
					return false;
				// skip invalid keys
				// if (pre.size() != m.size())
				// 	return false;
				return !supplyError(forGui(m)).isPresent();
			} else return false;
		};
	}
	
	@Override
	protected Consumer<List<Pair<String, G>>> saveConsumer() {
		return super.saveConsumer().andThen(l -> holder.clear());
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forActualConfig(forConfig(value)), configValidator()));
	}
	
	protected AbstractConfigListEntry<G> buildCell(
	  ConfigEntryBuilder builder, @Nullable G value
	) {
		final E e = entryBuilder.build(holder, holder.nextName()).withSaver((g, h) -> {})
		  .withDisplayName(new StringTextComponent(""));
		e.set(entry.fromGuiOrDefault(value));
		final AbstractConfigListEntry<G> g = e.buildGUIEntry(builder)
		  .orElseThrow(() -> new IllegalStateException(
			 "Map config entry's sub-entry did not produce a GUI entry"));
		e.guiEntry = g;
		return g;
	}
	
	protected Optional<ITextComponent> supplyPairError(Pair<String, G> p) {
		Optional<ITextComponent> e = keyEntry.stringKeyError(p.getKey());
		if (e.isPresent())
			return e;
		final Optional<K> opt = keyEntry.deserializeStringKey(p.getKey());
		if (!opt.isPresent())
			return Optional.of(new TranslationTextComponent(
			  "simple-config.config.error.invalid_key_generic", keyEntry.typeClass.getSimpleName()));
		K k = opt.get();
		e = entry.supplyError(p.getValue());
		if (e.isPresent())
			return e;
		if (getGUI().stream().filter(entry -> keyEntry.deserializeStringKey(entry.getKey()).equals(opt)).count() > 1)
			return Optional.of(new TranslationTextComponent(
			  "simple-config.config.error.duplicate_key", k));
		return Optional.empty();
	}
	
	@Override
	public Optional<AbstractConfigListEntry<List<Pair<String, G>>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final StringPairListEntry<G, AbstractConfigListEntry<G>> e = new StringPairListEntry<>(
		  getDisplayName(), forGui(get()), expand, // () -> this.supplyTooltip(parent.getGUI(name)),
		  () -> this.supplyTooltip(getGUI()),
		  saveConsumer(), () -> forGui(value), () -> Pair.of("", entry.forGui(entry.value)),
		  this::supplyError, this::supplyPairError,
		  builder.getResetButtonKey(), true, false,
		  (p, en) -> buildCell(builder, p != null? p.getValue() : null), true);
		// Worked around with AbstractSimpleConfigEntryHolder#markGUIRestart()
		// e.setRequiresRestart(requireRestart);
		e.setRequiresRestart(false);
		return Optional.of(e);
	}
}
