package endorh.simple_config.core;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.gui.entries.EntryPairListListEntry;
import endorh.simple_config.clothconfig2.impl.builders.DecoratedListEntryBuilder;
import endorh.simple_config.core.NBTUtil.ExpectedType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static endorh.simple_config.core.NBTUtil.fromNBT;
import static endorh.simple_config.core.NBTUtil.toNBT;

public class DecoratedMapEntry<K, V, KC, C, KG, G,
  E extends AbstractConfigEntry<V, C, G, E>,
  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KC, KG>,
  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>,
  MB extends EntryMapEntry.Builder<K, V, KC, C, KG, G, E, B, KE, KB>,
  CV, CC, CG, CE extends AbstractConfigEntry<CV, CC, CG, CE> & IKeyEntry<CC, CG>>
  extends AbstractConfigEntry<Pair<CV, Map<K, V>>, Pair<CC, Map<String, C>>, Pair<CG, List<Pair<KG, G>>>,
  DecoratedMapEntry<K, V, KC, C, KG, G, E, B, KE, KB, MB, CV, CC, CG, CE>> {
	
	protected final EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> mapEntry;
	protected final CE captionEntry;
	
	protected DecoratedMapEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  Pair<CV, Map<K, V>> value, EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> mapEntry, CE captionEntry
	) {
		super(parent, name, value);
		this.mapEntry = mapEntry.withSaver((v, h) -> {});
		this.captionEntry = captionEntry.withSaver((v, h) -> {});
	}
	
	public static class Builder<K, V, KC, C, KG, G,
	  E extends AbstractConfigEntry<V, C, G, E>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
	  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KC, KG>,
	  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>,
	  MB extends EntryMapEntry.Builder<K, V, KC, C, KG, G, E, B, KE, KB>,
	  CV, CC, CG, CE extends AbstractConfigEntry<CV, CC, CG, CE> & IKeyEntry<CC, CG>,
	  CB extends AbstractConfigEntryBuilder<CV, CC, CG, CE, CB>>
	  extends AbstractConfigEntryBuilder<Pair<CV, Map<K, V>>, Pair<CC, Map<String, C>>, Pair<CG, List<Pair<KG, G>>>,
	  DecoratedMapEntry<K, V, KC, C, KG, G, E, B, KE, KB, MB, CV, CC, CG, CE>,
	  DecoratedMapEntry.Builder<K, V, KC, C, KG, G, E, B, KE, KB, MB, CV, CC, CG, CE, CB>> {
		protected MB mapEntryBuilder;
		protected CB captionEntryBuilder;
		
		public Builder(Pair<CV, Map<K, V>> value, MB mapEntryBuilder, CB captionEntryBuilder) {
			super(value, Pair.class);
			this.mapEntryBuilder = mapEntryBuilder;
			this.captionEntryBuilder = captionEntryBuilder;
		}
		
		@Override protected DecoratedMapEntry<K, V, KC, C, KG, G, E, B, KE, KB, MB, CV, CC, CG, CE> buildEntry(
		  ISimpleConfigEntryHolder parent, String name
		) {
			final EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> me =
			  DummyEntryHolder.build(parent, mapEntryBuilder).withSaver((v, h) -> {});
			// final EntryMapEntry<K, V, KC, C, KG, G, E, B, KE, KB> me =
			//   mapEntryBuilder.buildEntry(parent, name + "$val");
			final CE ce = DummyEntryHolder.build(parent, captionEntryBuilder).withSaver((v, h) -> {});
			// final CE ce = captionEntryBuilder.buildEntry(parent, name + "$key");
			return new DecoratedMapEntry<>(parent, name, value, me, ce);
		}
		
		@Override protected Builder<K, V, KC, C, KG, G, E, B, KE, KB, MB, CV, CC, CG, CE, CB> createCopy() {
			//noinspection unchecked
			return new DecoratedMapEntry.Builder<>(
			  value, (MB) mapEntryBuilder.copy(), captionEntryBuilder.copy());
		}
	}
	
	public String forActualConfig(@Nullable Pair<CC, Map<String, C>> value) {
		if (value == null) return "";
		final CompoundNBT nbt = new CompoundNBT();
		nbt.put("k", toNBT(value.getKey()));
		nbt.put("v", toNBT(value.getValue()));
		return nbt.toString();
	}
	
	public @Nullable Pair<CC, Map<String, C>> fromActualConfig(@Nullable Object value) {
		if (!(value instanceof String)) return null;
		final String str = (String) value;
		try {
			final CompoundNBT nbt = new JsonToNBT(new StringReader(str)).readStruct();
			//noinspection unchecked
			final CC key = (CC) fromNBT(nbt.get("k"), getExpectedType().next.get(0));
			//noinspection unchecked
			final Map<String, C> val = (Map<String, C>) fromNBT(nbt.get("v"), getExpectedType().next.get(1));
			return Pair.of(key, val);
		} catch (CommandSyntaxException | IllegalArgumentException | ClassCastException e) {
			return null;
		}
	}
	
	@Override public Pair<CC, Map<String, C>> forConfig(Pair<CV, Map<K, V>> value) {
		return Pair.of(captionEntry.forConfig(value.getKey()),
		               mapEntry.forConfig(value.getValue()));
	}
	
	@Nullable @Override public Pair<CV, Map<K, V>> fromConfig(
	  @Nullable Pair<CC, Map<String, C>> value
	) {
		if (value == null) return null;
		return Pair.of(captionEntry.fromConfigOrDefault(value.getKey()),
		               mapEntry.fromConfigOrDefault(value.getValue()));
	}
	
	@Override public Pair<CG, List<Pair<KG, G>>> forGui(Pair<CV, Map<K, V>> value) {
		return Pair.of(captionEntry.forGui(value.getKey()), mapEntry.forGui(value.getValue()));
	}
	
	@Nullable @Override public Pair<CV, Map<K, V>> fromGui(@Nullable Pair<CG, List<Pair<KG, G>>> value) {
		if (value == null) return null;
		return Pair.of(captionEntry.fromGuiOrDefault(value.getKey()),
		               mapEntry.fromGuiOrDefault(value.getValue()));
	}
	
	@Override protected Predicate<Object> configValidator() {
		return o -> {
			if (o instanceof String) {
				final Pair<CC, Map<String, C>> pre = fromActualConfig(o);
				final Pair<CV, Map<K, V>> p = fromConfig(pre);
				if (p == null) return false;
				return !supplyError(forGui(p)).isPresent()
				       && !captionEntry.supplyError(captionEntry.forGui(p.getKey())).isPresent();
			} else return false;
		};
	}
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forActualConfig(forConfig(value)), configValidator()));
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<AbstractConfigListEntry<Pair<CG, List<Pair<KG, G>>>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		mapEntry.withDisplayName(getDisplayName());
		final Optional<AbstractConfigListEntry<List<Pair<KG, G>>>> opt =
		  mapEntry.buildGUIEntry(builder);
		if (!opt.isPresent())
			throw new IllegalStateException("List entry has no GUI entry");
		final DecoratedListEntryBuilder<Pair<KG, G>, ?, CG, ?> entryBuilder = builder.makeDecoratedList(
		  getDisplayName(), (EntryPairListListEntry<KG, G, ?, ?>) opt.get(), captionEntry.buildChildGUIEntry(builder), forGui(get()));
		return Optional.of(decorate(entryBuilder).build());
	}
	
	@Override public ExpectedType getExpectedType() {
		return new ExpectedType(typeClass, captionEntry.getExpectedType(), mapEntry.getExpectedType());
	}
}
