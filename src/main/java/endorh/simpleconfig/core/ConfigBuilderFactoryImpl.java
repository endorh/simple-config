package endorh.simpleconfig.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import endorh.simpleconfig.api.*;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.entry.*;
import endorh.simpleconfig.api.range.DoubleRange;
import endorh.simpleconfig.api.range.FloatRange;
import endorh.simpleconfig.api.range.IntRange;
import endorh.simpleconfig.api.range.LongRange;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBind;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl.CategoryBuilder;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl.GroupBuilder;
import endorh.simpleconfig.core.entry.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.lang.Math.round;

@Internal public class ConfigBuilderFactoryImpl implements ConfigBuilderFactory {
	@Internal public ConfigBuilderFactoryImpl() {}
	
	private static String getThreadModID() {
		String modId = ModLoadingContext.get().getActiveContainer().getModId();
		if ("minecraft".equals(modId)) throw new IllegalStateException(
		  "Cannot get mod ID from thread at this point. " +
		  "Register your config earlier, or specify your mod ID explicitly");
		return modId;
	}
	
	@Override public @NotNull SimpleConfigBuilder config(Type type) {
		return config(getThreadModID(), type);
	}
	@Override public @NotNull SimpleConfigBuilder config(Type type, Class<?> configClass) {
		return config(getThreadModID(), type, configClass);
	}
	@Override public @NotNull SimpleConfigBuilder config(String modId, Type type) {
		if (modId == null) modId = getThreadModID();
		return new SimpleConfigBuilderImpl(modId, type);
	}
	@Override public @NotNull SimpleConfigBuilder config(String modId, Type type, Class<?> configClass) {
		if (modId == null) modId = getThreadModID();
		return new SimpleConfigBuilderImpl(modId, type, configClass);
	}
	
	@Override public @NotNull ConfigGroupBuilder group(String name) {
		return group(name, false);
	}
	@Override public @NotNull ConfigGroupBuilder group(String name, boolean expand) {
		return new GroupBuilder(name, expand);
	}
	
	@Override public @NotNull ConfigCategoryBuilder category(String name) {
		return new CategoryBuilder(name);
	}
	@Override public @NotNull ConfigCategoryBuilder category(String name, Class<?> configClass) {
		return new CategoryBuilder(name, configClass);
	}
	
	// Entries
	
	// Basic types
	@Override public @NotNull BooleanEntry.Builder bool(boolean value) {
		return new BooleanEntry.Builder(value);
	}
	
	@Override public @NotNull BooleanEntry.Builder yesNo(boolean value) {
		return new BooleanEntry.Builder(value).text(BooleanEntryBuilder.BooleanDisplayer.YES_NO);
	}
	
	@Override public @NotNull BooleanEntry.Builder enable(boolean value) {
		return bool(value).text(BooleanEntryBuilder.BooleanDisplayer.ENABLED_DISABLED);
	}
	
	@Override public @NotNull BooleanEntry.Builder onOff(boolean value) {
		return bool(value).text(BooleanEntryBuilder.BooleanDisplayer.ON_OFF);
	}
	
	@Override public @NotNull StringEntry.Builder string(String value) {
		return new StringEntry.Builder(value);
	}
	
	@Override @Deprecated public <E extends Enum<E>> EnumEntry.@NotNull Builder<E> enum_(E value) {
		return new EnumEntry.Builder<>(value);
	}
	
	@Override public <E extends Enum<E>> EnumEntry.@NotNull Builder<E> option(E value) {
		return new EnumEntry.Builder<>(value);
	}
	
	@Override public @NotNull <T> OptionEntryBuilder<@NotNull T> option(T value, List<T> options) {
		return new OptionEntry.Builder<>(value).withOptions(options);
	}
	
	@Override
	public @NotNull <T> OptionEntryBuilder<@NotNull T> option(T value, Supplier<List<T>> options) {
		return new OptionEntry.Builder<>(value).withOptions(options);
	}
	
	@SafeVarargs @Override public final @NotNull <T> OptionEntryBuilder<@NotNull T> option(
	  T value, T... options
	) {
		return new OptionEntry.Builder<>(value).withOptions(options);
	}
	
	
	@Override public @NotNull ButtonEntry.Builder button(Runnable action) {
		return new ButtonEntry.Builder(h -> action.run());
	}
	
	@Override public @NotNull ButtonEntry.Builder button(Consumer<ConfigEntryHolder> action) {
		return new ButtonEntry.Builder(action);
	}
	
	@Override
	public <V, Gui, B extends ConfigEntryBuilder<V, ?, Gui, B> & AtomicEntryBuilder> @NotNull EntryButtonEntryBuilder<V, Gui, B> button(
	  B inner, Consumer<V> action
	) {
		return button(inner, (v, h) -> action.accept(v));
	}
	
	@Override
	public <V, Gui, B extends ConfigEntryBuilder<V, ?, Gui, B> & AtomicEntryBuilder> @NotNull EntryButtonEntryBuilder<V, Gui, B> button(
	  B inner, BiConsumer<V, ConfigEntryHolder> action
	) {
		return new EntryButtonEntry.Builder<>(inner, action);
	}
	
	@Override public @NotNull PresetSwitcherEntry.Builder globalPresetSwitcher(
	  Map<String, Map<String, Object>> presets, String path
	) {
		return new PresetSwitcherEntry.Builder(presets, path, true);
	}
	
	
	@Override public @NotNull PresetSwitcherEntry.Builder localPresetSwitcher(
	  Map<String, Map<String, Object>> presets, String path
	) {
		return new PresetSwitcherEntry.Builder(presets, path, false);
	}
	
	// Byte
	
	@Override public @Deprecated @NotNull ByteEntry.Builder number(byte value) {
		return new ByteEntry.Builder(value);
	}
	
	@Override public @Deprecated @NotNull ByteEntryBuilder number(byte value, byte max) {
		return number(value, (byte) 0, max);
	}
	
	@Override public @Deprecated @NotNull ByteEntryBuilder number(byte value, byte min, byte max) {
		return new ByteEntry.Builder(value).range(min, max);
	}
	
	// Short
	
	@Override public @Deprecated @NotNull ShortEntry.Builder number(short value) {
		return new ShortEntry.Builder(value);
	}
	
	@Override public @Deprecated @NotNull ShortEntryBuilder number(short value, short max) {
		return number(value, (short) 0, max);
	}
	
	@Override public @Deprecated @NotNull ShortEntryBuilder number(short value, short min, short max) {
		return new ShortEntry.Builder(value).range(min, max);
	}
	
	// Int
	
	@Override public @NotNull IntegerEntry.Builder number(int value) {
		return new IntegerEntry.Builder(value);
	}
	
	@Override public @NotNull IntegerEntryBuilder number(int value, int max) {
		return number(value, 0, max);
	}
	
	@Override public @NotNull IntegerEntryBuilder number(int value, int min, int max) {
		return new IntegerEntry.Builder(value).range(min, max);
	}
	
	@Override public @NotNull IntegerEntryBuilder percent(int value) {
		return number(value, 0, 100)
		  .slider("simpleconfig.format.slider.percentage");
	}
	
	// Long
	
	@Override public @NotNull LongEntry.Builder number(long value) {
		return new LongEntry.Builder(value);
	}
	
	@Override public @NotNull LongEntryBuilder number(long value, long max) {
		return number(value, 0L, max);
	}
	
	@Override public @NotNull LongEntryBuilder number(long value, long min, long max) {
		return new LongEntry.Builder(value).range(min, max);
	}
	
	// Float
	
	@Override public @NotNull FloatEntry.Builder number(float value) {
		return new FloatEntry.Builder(value);
	}
	
	@Override public @NotNull FloatEntryBuilder number(float value, float max) {
		return number(value, 0F, max);
	}
	
	@Override public @NotNull FloatEntryBuilder number(float value, float min, float max) {
		return new FloatEntry.Builder(value).range(min, max);
	}
	
	@Override public @NotNull FloatEntryBuilder percent(float value) {
		return number(value, 0F, 100F)
		  .slider("simpleconfig.format.slider.percentage.float")
		  .fieldScale(0.01F);
	}
	
	// Double
	
	@Override public @NotNull DoubleEntry.Builder number(double value) {
		return new DoubleEntry.Builder(value);
	}
	
	@Override public @NotNull DoubleEntryBuilder number(double value, double max) {
		return number(value, 0D, max);
	}
	
	@Override public @NotNull DoubleEntryBuilder number(double value, double min, double max) {
		return new DoubleEntry.Builder(value).range(min, max);
	}
	
	@Override public @NotNull DoubleEntryBuilder percent(double value) {
		return number(value, 0D, 100D)
		  .slider("simpleconfig.format.slider.percentage.float")
		  .fieldScale(0.01);
	}
	
	@Override public @NotNull FloatEntryBuilder fraction(float value) {
		if (0F > value || value > 1F)
			throw new IllegalArgumentException(
			  "Fraction values must be within [0, 1], passed " + value);
		return number(value).range(0, 1).slider();
	}
	
	@Override public @NotNull DoubleEntryBuilder fraction(double value) {
		if (0D > value || value > 1D)
			throw new IllegalArgumentException(
			  "Fraction values must be within [0, 1], passed " + value);
		return number(value).range(0, 1).slider();
	}
	
	@Override public @NotNull FloatEntryBuilder volume(float value) {
		return fraction(value).slider(v -> new TranslatableComponent(
		  "simpleconfig.format.slider.volume", round(v * 100F)));
	}
	
	@Override public @NotNull FloatEntryBuilder volume() {
		return volume(1F);
	}
	
	@Override public @NotNull DoubleEntryBuilder volume(double value) {
		return fraction(value).slider(v -> new TranslatableComponent(
		  "simpleconfig.format.slider.volume", round(v * 100D)));
	}
	
	@Override public @NotNull DoubleRangeEntry.Builder range(DoubleRange range) {
		return new DoubleRangeEntry.Builder(range);
	}
	
	@Override public @NotNull DoubleRangeEntry.Builder range(double min, double max) {
		return range(DoubleRange.inclusive(min, max));
	}
	
	@Override public @NotNull FloatRangeEntry.Builder range(FloatRange range) {
		return new FloatRangeEntry.Builder(range);
	}
	
	@Override public @NotNull FloatRangeEntry.Builder range(float min, float max) {
		return range(FloatRange.inclusive(min, max));
	}
	
	@Override public @NotNull LongRangeEntry.Builder range(LongRange range) {
		return new LongRangeEntry.Builder(range);
	}
	
	@Override public @NotNull LongRangeEntry.Builder range(long min, long max) {
		return range(LongRange.inclusive(min, max));
	}
	
	@Override public @NotNull IntegerRangeEntry.Builder range(IntRange range) {
		return new IntegerRangeEntry.Builder(range);
	}
	
	@Override public @NotNull IntegerRangeEntry.Builder range(int min, int max) {
		return range(IntRange.inclusive(min, max));
	}
	
	@Override public @NotNull ColorEntry.Builder color(Color value) {
		return new ColorEntry.Builder(value);
	}
	
	@Override public @NotNull PatternEntry.Builder pattern(Pattern pattern) {
		return new PatternEntry.Builder(pattern);
	}
	
	@Override public @NotNull PatternEntry.Builder pattern(String pattern) {
		return new PatternEntry.Builder(pattern);
	}
	
	@Override public @NotNull PatternEntry.Builder pattern(String pattern, int flags) {
		return new PatternEntry.Builder(pattern, flags);
	}
	
	// String serializable entries
	
	@Override public <V> SerializableEntry.@NotNull Builder<V> entry(
	  V value, Function<V, String> serializer, Function<String, Optional<V>> deserializer
	) {
		return new SerializableEntry.Builder<>(value, serializer, deserializer);
	}
	
	@Override public <V> SerializableEntry.@NotNull Builder<V> entry(
	  V value, ConfigEntrySerializer<V> serializer
	) {
		return new SerializableEntry.Builder<>(value, serializer);
	}
	
	@Override public <V extends ISerializableConfigEntry<V>> SerializableEntry.@NotNull Builder<V> entry(
	  V value
	) {
		return new SerializableEntry.Builder<>(value, value.getConfigSerializer());
	}
	
	// Java Beans
	
	@Override public <B> @NotNull BeanEntryBuilder<B> bean(B value) {
		return BeanEntry.Builder.create(value);
	}
	
	// Convenience Minecraft entries
	
	@Override public @NotNull TagEntry.Builder tag(Tag value) {
		return new TagEntry.Builder(value);
	}
	
	@Override public @NotNull CompoundTagEntry.Builder compoundTag(CompoundTag value) {
		return new CompoundTagEntry.Builder(value);
	}
	
	@Override public @NotNull ResourceLocationEntry.Builder resource(String resourceName) {
		return new ResourceLocationEntry.Builder(new ResourceLocation(resourceName));
	}
	
	@Override public @NotNull ResourceLocationEntry.Builder resource(ResourceLocation value) {
		return new ResourceLocationEntry.Builder(value);
	}
	
	@Override @OnlyIn(Dist.CLIENT) public @NotNull KeyBindEntry.Builder key(ExtendedKeyBind keyBind) {
		return key(keyBind.getDefinition())
		  .bakeTo(keyBind)
		  .withDefaultSettings(keyBind.getDefinition().getSettings());
	}
	
	@Override @OnlyIn(Dist.CLIENT) public @NotNull KeyBindEntry.Builder key(KeyBindMapping key) {
		return new KeyBindEntry.Builder(key);
	}
	
	@Override @OnlyIn(Dist.CLIENT) public @NotNull KeyBindEntry.Builder key(String key) {
		return new KeyBindEntry.Builder(key);
	}
	
	@Override @OnlyIn(Dist.CLIENT) public @NotNull KeyBindEntry.Builder key() {
		return new KeyBindEntry.Builder();
	}
	
	@Override public @NotNull ItemEntry.Builder item(@Nullable Item value) {
		return new ItemEntry.Builder(value);
	}
	
	@Override public @NotNull ItemNameEntry.Builder itemName(@Nullable ResourceLocation value) {
		return new ItemNameEntry.Builder(value);
	}
	
	@Override public @NotNull ItemNameEntry.Builder itemName(Item value) {
		return itemName(value.getRegistryName());
	}
	
	@Override public @NotNull BlockEntry.Builder block(@Nullable Block value) {
		return new BlockEntry.Builder(value);
	}
	
	@Override public @NotNull BlockNameEntry.Builder blockName(@Nullable ResourceLocation value) {
		return new BlockNameEntry.Builder(value);
	}
	
	@Override public @NotNull BlockNameEntry.Builder blockName(Block value) {
		return blockName(value.getRegistryName());
	}
	
	@Override public @NotNull FluidEntry.Builder fluid(@Nullable Fluid value) {
		return new FluidEntry.Builder(value);
	}
	
	@Override public @NotNull FluidNameEntry.Builder fluidName(@Nullable ResourceLocation value) {
		return new FluidNameEntry.Builder(value);
	}
	
	@Override public @NotNull FluidNameEntry.Builder fluidName(Fluid value) {
		return fluidName(value.getRegistryName());
	}
	
	// List entries
	
	@Override public @NotNull StringListEntry.Builder stringList(List<String> value) {
		return new StringListEntry.Builder(value);
	}
	
	@Override @Deprecated
	public @NotNull ByteListEntry.Builder byteList(List<Byte> value) {
		return new ByteListEntry.Builder(value);
	}
	
	@Override @Deprecated
	public @NotNull ShortListEntry.Builder shortList(List<Short> value) {
		return new ShortListEntry.Builder(value);
	}
	
	@Override public @NotNull IntegerListEntry.Builder intList(List<Integer> value) {
		return new IntegerListEntry.Builder(value);
	}
	
	@Override public @NotNull LongListEntry.Builder longList(List<Long> value) {
		return new LongListEntry.Builder(value);
	}
	
	@Override public @NotNull FloatListEntry.Builder floatList(List<Float> value) {
		return new FloatListEntry.Builder(value);
	}
	
	@Override public @NotNull DoubleListEntry.Builder doubleList(List<Double> value) {
		return new DoubleListEntry.Builder(value);
	}
	
	// General lists
	
	@Override public <V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntryListEntryBuilder<V, C, G, Builder> list(Builder entry) {
		return list(entry, Collections.emptyList());
	}
	
	@Override public <V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntryListEntryBuilder<V, C, G, Builder> list(Builder entry, List<V> value) {
		return new EntryListEntry.Builder<>(value, entry);
	}
	
	@SafeVarargs
	@Override public final <V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntryListEntryBuilder<V, C, G, Builder> list(Builder entry, V... values) {
		return list(entry, Lists.newArrayList(values));
	}
	
	// General sets
	
	@Override public <V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntrySetEntryBuilder<V, C, G, Builder> set(Builder entry) {
		return set(entry, Collections.emptySet());
	}
	
	@Override public <V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntrySetEntryBuilder<V, C, G, Builder> set(Builder entry, Set<V> value) {
		return new EntrySetEntry.Builder<>(value, entry);
	}
	
	@SafeVarargs
	@Override public final <V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntrySetEntryBuilder<V, C, G, Builder> set(Builder entry, V... values) {
		return set(entry, Sets.newHashSet(values));
	}
	
	// General maps
	
	@Override public <
	  K, V, KC, C, KG, G,
	  Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & AtomicEntryBuilder
	> @NotNull EntryMapEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> map(
	  KeyBuilder keyEntry, Builder entry
	) {
		return map(keyEntry, entry, new LinkedHashMap<>());
	}
	
	@Override public <
	  K, V, KC, C, KG, G,
	  Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & AtomicEntryBuilder
	> @NotNull EntryMapEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> map(
	  KeyBuilder keyEntry, Builder entry, Map<K, V> value
	) {
		return new EntryMapEntry.Builder<>(value, keyEntry, entry);
	}
	
	@Override public <
	  V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>
	> @NotNull EntryMapEntryBuilder<String, V, String, C, String, G, Builder, StringEntryBuilder> map(
	  Builder entry
	) {
		return map(entry, new LinkedHashMap<>());
	}
	
	@Override
	public <V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>> @NotNull EntryMapEntryBuilder<String, V, String, C, String, G, Builder, StringEntryBuilder> map(
	  Builder entry, Map<String, V> value
	) {
		return map(string(""), entry, value);
	}
	
	// Pair list
	
	@Override public <
	  K, V, KC, C, KG, G,
	  Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & AtomicEntryBuilder
	> @NotNull EntryPairListEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> pairList(
	  KeyBuilder keyEntry, Builder entry
	) {
		return pairList(keyEntry, entry, new ArrayList<>());
	}
	
	@Override
	public <K, V, KC, C, KG, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>, KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & AtomicEntryBuilder> @NotNull EntryPairListEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> pairList(
	  KeyBuilder keyEntry, Builder entry, List<Pair<K, V>> value
	) {
		return new EntryPairListEntry.Builder<>(value, keyEntry, entry);
	}
	
	// Collection captions
	
	@Override public <
	  V, C, G, B extends ListEntryBuilder<V, C, G, B>,
	  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & AtomicEntryBuilder
	> @NotNull CaptionedCollectionEntryBuilder<List<V>, List<C>, G, B, CV, CC, CG, CB> caption(
	  CB caption, B list
	) {
		return new CaptionedCollectionEntry.Builder<>(
		  Pair.of(caption.getValue(), list.getValue()), list, caption);
	}
	
	@Override
	public @NotNull <V, C, G, B extends ConfigEntryBuilder<V, C, G, B>, CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & AtomicEntryBuilder> CaptionedCollectionEntryBuilder<Set<V>, Set<C>, G, EntrySetEntryBuilder<V, C, G, B>, CV, CC, CG, CB> caption(
	  CB caption, EntrySetEntryBuilder<V, C, G, B> set
	) {
		return new CaptionedCollectionEntry.Builder<>(
		  Pair.of(caption.getValue(), set.getValue()), set, caption);
	}
	
	@Override
	public @NotNull <K, V, KC, C, KG, G, KB extends ConfigEntryBuilder<K, KC, KG, KB> & AtomicEntryBuilder, B extends ConfigEntryBuilder<V, C, G, B>, CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & AtomicEntryBuilder> CaptionedCollectionEntryBuilder<Map<K, V>, Map<KC, C>, Pair<KG, G>, EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB>, CV, CC, CG, CB> caption(
	  CB caption, EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB> map
	) {
		return new CaptionedCollectionEntry.Builder<>(
		  Pair.of(caption.getValue(), map.getValue()), map, caption);
	}
	
	@Override
	public @NotNull <K, V, KC, C, KG, G, KB extends ConfigEntryBuilder<K, KC, KG, KB> & AtomicEntryBuilder, B extends ConfigEntryBuilder<V, C, G, B>, CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & AtomicEntryBuilder> CaptionedCollectionEntryBuilder<List<Pair<K, V>>, List<Pair<KC, C>>, Pair<KG, G>, EntryPairListEntryBuilder<K, V, KC, C, KG, G, B, KB>, CV, CC, CG, CB> caption(
	  CB caption, EntryPairListEntryBuilder<K, V, KC, C, KG, G, B, KB> pairList
	) {
		return new CaptionedCollectionEntry.Builder<>(
		  Pair.of(caption.getValue(), pairList.getValue()), pairList, caption);
	}
	
	// Pairs
	
	@Override public <
	  L, R, LC, RC, LG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & AtomicEntryBuilder,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & AtomicEntryBuilder
	> @NotNull EntryPairEntryBuilder<L, R, LC, RC, LG, RG> pair(LB leftEntry, RB rightEntry) {
		return pair(leftEntry, rightEntry, Pair.of(leftEntry.getValue(), rightEntry.getValue()));
	}
	
	@Override public <
	  L, R, LC, RC, LG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & AtomicEntryBuilder,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & AtomicEntryBuilder
	> @NotNull EntryPairEntryBuilder<L, R, LC, RC, LG, RG> pair(
	  LB leftEntry, RB rightEntry, Pair<L, R> value
	) {
		return new EntryPairEntry.Builder<>(value, leftEntry, rightEntry);
	}
	
	// Triple
	
	
	@Override public <
	  L, M, R, LC, MC, RC, LG, MG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & AtomicEntryBuilder,
	  MB extends ConfigEntryBuilder<M, MC, MG, MB> & AtomicEntryBuilder,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & AtomicEntryBuilder
	> @NotNull EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> triple(
	  LB leftEntry, MB middleEntry, RB rightEntry
	) {
		return triple(leftEntry, middleEntry, rightEntry, Triple.of(
		  leftEntry.getValue(), middleEntry.getValue(), rightEntry.getValue()));
	}
	
	@Override public <
	  L, M, R, LC, MC, RC, LG, MG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & AtomicEntryBuilder,
	  MB extends ConfigEntryBuilder<M, MC, MG, MB> & AtomicEntryBuilder,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & AtomicEntryBuilder
	> @NotNull EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> triple(
	  LB leftEntry, MB middleEntry, RB rightEntry, Triple<L, M, R> value
	) {
		return new EntryTripleEntry.Builder<>(
		  value, leftEntry, middleEntry, rightEntry);
	}
}
