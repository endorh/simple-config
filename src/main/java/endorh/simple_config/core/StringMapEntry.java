package endorh.simple_config.core;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simple_config.gui.StringPairListEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.nbt.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StringMapEntry<V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
  B extends AbstractConfigEntryBuilder<V, C, G, E, B>>
  extends AbstractConfigEntry<Map<String, V>, CompoundNBT, List<Pair<String, G>>,
  StringMapEntry<V, C, G, E, B>> {
	protected final Map<String, E> entries = new HashMap<>();
	protected final B entryBuilder;
	protected final E entry;
	protected final Class<?> entryTypeClass;
	protected final StringMapEntryHolder<V, C, E, B> holder;
	protected Function<String, Optional<ITextComponent>> keyErrorSupplier;
	protected boolean expand;
	
	@Internal public StringMapEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  Map<String, V> value, @Nonnull B entryBuilder
	) {
		super(parent, name, value);
		this.entryBuilder = entryBuilder;
		entryTypeClass = entryBuilder.typeClass;
		entry = entryBuilder.build(parent, name + "$ ");
		holder = new StringMapEntryHolder<>(parent.getRoot(), entryBuilder);
	}
	
	public static class Builder<V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>>
	  extends AbstractConfigEntryBuilder<Map<String, V>, CompoundNBT,
	  List<Pair<String, G>>, StringMapEntry<V, C, G, E, B>, Builder<V, C, G, E, B>> {
		protected B entryBuilder;
		protected Function<String, Optional<ITextComponent>> keyErrorSupplier;
		protected boolean expand;
		
		public Builder(Map<String, V> value, B entryBuilder) {
			super(value, Map.class);
			this.entryBuilder = entryBuilder;
		}
		
		public Builder<V, C, G, E, B> keyError(Function<String, Optional<ITextComponent>> errorSupplier) {
			this.keyErrorSupplier = errorSupplier;
			return this;
		}
		
		public Builder<V, C, G, E, B> expand() {
			this.expand = true;
			return this;
		}
		
		@Override
		protected StringMapEntry<V, C, G, E, B> buildEntry(ISimpleConfigEntryHolder parent, String name) {
			final StringMapEntry<V, C, G, E, B> e = new StringMapEntry<>(parent, name, value, entryBuilder);
			e.keyErrorSupplier = keyErrorSupplier != null? keyErrorSupplier : k -> Optional.empty();
			e.expand = expand;
			return e;
		}
	}
	
	@Override
	protected Map<String, V> get(ConfigValue<?> spec) {
		return fromConfigOrDefault(fromActualConfig((String)spec.get()));
	}
	
	@Override
	protected void set(ConfigValue<?> spec, Map<String, V> value) {
		//noinspection unchecked
		((ConfigValue<String>) spec).set(forActualConfig(forConfig(value)));
	}
	
	protected String forActualConfig(CompoundNBT value) {
		return value.toString();
	}
	
	protected @Nullable CompoundNBT fromActualConfig(String value) {
		try {
			return new JsonToNBT(new StringReader(value)).readStruct();
		} catch (CommandSyntaxException e) {
			return null;
		}
	}
	
	@Override
	protected CompoundNBT forConfig(Map<String, V> value) {
		final Map<String, C> m = value.entrySet().stream()
		  .collect(Collectors.toMap(Entry::getKey, e -> entry.forConfig(e.getValue())));
		return (CompoundNBT) toNBT(m);
	}
	
	@Nullable
	@Override
	protected Map<String, V> fromConfig(@Nullable CompoundNBT value) {
		if (value == null) return null;
		try {
			return mapFromNBT(value);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
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
	protected List<Pair<String, G>> forGui(Map<String, V> value) {
		return value.entrySet().stream().map(e -> Pair.of(e.getKey(), entry.forGui(e.getValue())))
		  .collect(Collectors.toList());
	}
	
	@Nullable
	@Override
	protected Map<String, V> fromGui(@Nullable List<Pair<String, G>> value) {
		try {
			return value != null ? value.stream().collect(Collectors.toMap(
			  Pair::getKey, p -> entry.fromGuiOrDefault(p.getValue()))) : null;
		} catch (IllegalStateException e) { // Duplicate key
			return null;
		}
	}
	
	@Override
	protected Predicate<Object> configValidator() {
		return o -> {
			if (o instanceof String) {
				final Map<String, V> m = fromConfig(fromActualConfig((String) o));
				return m != null && !supplyError(forGui(m)).isPresent();
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
	
	protected AbstractConfigListEntry<G> buildCell(ConfigEntryBuilder builder, @Nullable G value) {
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
		Optional<ITextComponent> e = keyErrorSupplier.apply(p.getKey());
		if (!e.isPresent())
			e = entry.supplyError(p.getValue());
		if (e.isPresent())
			return e;
		if (getGUI().stream().filter(entry -> entry.getKey().equals(p.getKey())).count() > 1)
			return Optional.of(new TranslationTextComponent(
			  "simple-config.config.error.duplicate_key", p.getKey()));
		return Optional.empty();
	}
	
	@Override
	protected Optional<AbstractConfigListEntry<List<Pair<String, G>>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final StringPairListEntry<G, AbstractConfigListEntry<G>> e = new StringPairListEntry<>(
		  getDisplayName(), forGui(get()), expand, () -> this.supplyTooltip(parent.getGUI(name)),
		  saveConsumer(), () -> forGui(value), () -> Pair.of("", entry.forGui(entry.value)),
		  this::supplyError, this::supplyPairError,
		  builder.getResetButtonKey(), true, false,
		  (p, en) -> buildCell(builder, p != null? p.getValue() : null), true);
		e.setRequiresRestart(requireRestart);
		return Optional.of(e);
	}
}
