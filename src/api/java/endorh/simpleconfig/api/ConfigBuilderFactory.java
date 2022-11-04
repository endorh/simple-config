package endorh.simpleconfig.api;

import endorh.simpleconfig.api.AbstractRange.DoubleRange;
import endorh.simpleconfig.api.AbstractRange.FloatRange;
import endorh.simpleconfig.api.AbstractRange.IntRange;
import endorh.simpleconfig.api.AbstractRange.LongRange;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.entry.*;
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBind;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * See {@link ConfigBuilderFactoryProxy} for documentation.
 */
public interface ConfigBuilderFactory {
	
	// Configs, groups and categories
	
	@NotNull SimpleConfigBuilder builder(String modId, Type type);
	@NotNull SimpleConfigBuilder builder(String modId, Type type, Class<?> configClass);
	
	@NotNull ConfigGroupBuilder group(String name);
	@NotNull ConfigGroupBuilder group(String name, boolean expand);
	
	@NotNull ConfigCategoryBuilder category(String name);
	@NotNull ConfigCategoryBuilder category(String name, Class<?> configClass);
	
	// Boolean entries
	
	@NotNull BooleanEntryBuilder bool(boolean value);
	@NotNull BooleanEntryBuilder yesNo(boolean value);
	@NotNull BooleanEntryBuilder enable(boolean value);
	@NotNull BooleanEntryBuilder onOff(boolean value);
	
	// Strings
	
	@NotNull StringEntryBuilder string(String value);
	
	// Enums
	
	@NotNull @Deprecated <E extends Enum<E>> EnumEntryBuilder<E> enum_(E value);
	@NotNull <E extends Enum<E>> EnumEntryBuilder<E> option(E value);
	
	// Buttons
	
	@NotNull ButtonEntryBuilder button(Runnable action);
	@NotNull ButtonEntryBuilder button(Consumer<ConfigEntryHolder> action);
	
	<V, Gui, B extends ConfigEntryBuilder<V, ?, Gui, B> & AtomicEntryBuilder>
	@NotNull EntryButtonEntryBuilder<V, Gui, B> button(B inner, Consumer<V> action);
	<V, Gui, B extends ConfigEntryBuilder<V, ?, Gui, B> & AtomicEntryBuilder>
	@NotNull EntryButtonEntryBuilder<V, Gui, B> button(
	  B inner, BiConsumer<V, ConfigEntryHolder> action);
	
	// Presets
	
	@NotNull PresetSwitcherEntryBuilder globalPresetSwitcher(
	  Map<String, Map<String, Object>> presets, String path);
	@NotNull PresetSwitcherEntryBuilder localPresetSwitcher(
	  Map<String, Map<String, Object>> presets, String path);
	default @NotNull Map<String, Map<String, Object>> presets(PresetBuilder... presets) {
		final Map<String, Map<String, Object>> map = new LinkedHashMap<>();
		Arrays.stream(presets).forEachOrdered(p -> map.put(p.name, p.build()));
		return Collections.unmodifiableMap(map);
	}
	default @NotNull PresetBuilder preset(String name) {
		return new PresetBuilder(name);
	}
	
	// Numbers
	
	@Deprecated @NotNull ByteEntryBuilder number(byte value);
	@Deprecated @NotNull ByteEntryBuilder number(byte value, byte max);
	@Deprecated @NotNull ByteEntryBuilder number(byte value, byte min, byte max);
	
	@Deprecated @NotNull ShortEntryBuilder number(short value);
	@Deprecated @NotNull ShortEntryBuilder number(short value, short max);
	@Deprecated @NotNull ShortEntryBuilder number(short value, short min, short max);
	
	@NotNull IntegerEntryBuilder number(int value);
	@NotNull IntegerEntryBuilder number(int value, int max);
	@NotNull IntegerEntryBuilder number(int value, int min, int max);
	
	@NotNull IntegerEntryBuilder percent(int value);
	
	@NotNull LongEntryBuilder number(long value);
	@NotNull LongEntryBuilder number(long value, long max);
	@NotNull LongEntryBuilder number(long value, long min, long max);
	
	@NotNull FloatEntryBuilder number(float value);
	@NotNull FloatEntryBuilder number(float value, float max);
	@NotNull FloatEntryBuilder number(float value, float min, float max);
	
	@NotNull FloatEntryBuilder percent(float value);
	
	@NotNull DoubleEntryBuilder number(double value);
	@NotNull DoubleEntryBuilder number(double value, double max);
	@NotNull DoubleEntryBuilder number(double value, double min, double max);
	
	@NotNull DoubleEntryBuilder percent(double value);
	
	@NotNull FloatEntryBuilder fraction(float value);
	@NotNull DoubleEntryBuilder fraction(double value);
	
	@NotNull FloatEntryBuilder volume(float value);
	@NotNull FloatEntryBuilder volume();
	@NotNull DoubleEntryBuilder volume(double value);
	
	// Ranges
	
	@NotNull DoubleRangeEntryBuilder range(DoubleRange range);
	@NotNull DoubleRangeEntryBuilder range(double min, double max);
	
	@NotNull FloatRangeEntryBuilder range(FloatRange range);
	@NotNull FloatRangeEntryBuilder range(float min, float max);
	
	@NotNull LongRangeEntryBuilder range(LongRange range);
	@NotNull LongRangeEntryBuilder range(long min, long max);
	
	@NotNull IntegerRangeEntryBuilder range(IntRange range);
	@NotNull IntegerRangeEntryBuilder range(int min, int max);
	
	// Color
	
	@NotNull ColorEntryBuilder color(Color value);
	
	// Serializable entries
	
	@NotNull PatternEntryBuilder pattern(Pattern pattern);
	@NotNull PatternEntryBuilder pattern(String pattern);
	@NotNull PatternEntryBuilder pattern(String pattern, int flags);
	
	<V> @NotNull SerializableEntryBuilder<V, ?> entry(
	  V value, Function<V, String> serializer, Function<String, Optional<V>> deserializer);
	<V> @NotNull ISerializableEntryBuilder<V> entry(
	  V value, IConfigEntrySerializer<V> serializer);
	<V extends ISerializableConfigEntry<V>> @NotNull ISerializableEntryBuilder<V> entry(V value);
	
	<B> @NotNull BeanEntryBuilder<B> bean(B value);
	
	// Convenience Minecraft entries
	
	@NotNull TagEntryBuilder tag(Tag value);
	@NotNull CompoundTagEntryBuilder compoundTag(CompoundTag value);
	
	@NotNull ResourceLocationEntryBuilder resource(String resourceName);
	@NotNull ResourceLocationEntryBuilder resource(ResourceLocation value);
	
	@OnlyIn(Dist.CLIENT) @NotNull KeyBindEntryBuilder key(ExtendedKeyBind keyBind);
	@OnlyIn(Dist.CLIENT) @NotNull KeyBindEntryBuilder key(KeyBindMapping key);
	@OnlyIn(Dist.CLIENT) @NotNull KeyBindEntryBuilder key(String key);
	@OnlyIn(Dist.CLIENT) @NotNull KeyBindEntryBuilder key();
	
	@NotNull ItemEntryBuilder item(@Nullable Item value);
	@NotNull ItemNameEntryBuilder itemName(@Nullable ResourceLocation value);
	@NotNull ItemNameEntryBuilder itemName(Item value);
	
	@NotNull BlockEntryBuilder block(@Nullable Block value);
	@NotNull BlockNameEntryBuilder blockName(@Nullable ResourceLocation value);
	@NotNull BlockNameEntryBuilder blockName(Block value);
	
	@NotNull FluidEntryBuilder fluid(@Nullable Fluid value);
	@NotNull FluidNameEntryBuilder fluidName(@Nullable ResourceLocation value);
	@NotNull FluidNameEntryBuilder fluidName(Fluid value);
	
	// List entries
	
	@NotNull StringListEntryBuilder stringList(List<String> value);
	@Deprecated @NotNull ByteListEntryBuilder byteList(List<Byte> value);
	@Deprecated @NotNull ShortListEntryBuilder shortList(List<Short> value);
	@NotNull IntegerListEntryBuilder intList(List<Integer> value);
	@NotNull LongListEntryBuilder longList(List<Long> value);
	@NotNull FloatListEntryBuilder floatList(List<Float> value);
	@NotNull DoubleListEntryBuilder doubleList(List<Double> value);
	
	// General lists
	
	<V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntryListEntryBuilder<V, C, G, Builder> list(Builder entry);
	<V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntryListEntryBuilder<V, C, G, Builder> list(Builder entry, List<V> value);
	@SuppressWarnings("unchecked") <V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntryListEntryBuilder<V, C, G, Builder> list(Builder entry, V... values);
	
	// General sets
	
	<V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntrySetEntryBuilder<V, C, G, Builder> set(Builder entry);
	<V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntrySetEntryBuilder<V, C, G, Builder> set(Builder entry, Set<V> value);
	@SuppressWarnings("unchecked") <V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntrySetEntryBuilder<V, C, G, Builder> set(Builder entry, V... values);
	
	// General maps
	
	<K, V, KC, C, KG, G,
	  Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & AtomicEntryBuilder
	  > @NotNull EntryMapEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> map(
	  KeyBuilder keyEntry, Builder entry);
	<K, V, KC, C, KG, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & AtomicEntryBuilder>
	@NotNull EntryMapEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> map(
	  KeyBuilder keyEntry, Builder entry, Map<K, V> value);
	<V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntryMapEntryBuilder<String, V, String, C, String, G, Builder, StringEntryBuilder> map(
	  Builder entry);
	<V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	@NotNull EntryMapEntryBuilder<String, V, String, C, String, G, Builder, StringEntryBuilder> map(
	  Builder entry, Map<String, V> value);
	
	// Pair list
	
	<K, V, KC, C, KG, G,
	  Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & AtomicEntryBuilder
	  > @NotNull EntryPairListEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> pairList(
	  KeyBuilder keyEntry, Builder entry);
	<K, V, KC, C, KG, G,
	  Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & AtomicEntryBuilder>
	@NotNull EntryPairListEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> pairList(
	  KeyBuilder keyEntry, Builder entry, List<Pair<K, V>> value);
	
	// Collection captions
	
	<V, C, G, B extends ListEntryBuilder<V, C, G, B>,
	  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & AtomicEntryBuilder
	  > @NotNull CaptionedCollectionEntryBuilder<List<V>, List<C>, G, B, CV, CC, CG, CB> caption(
	  CB caption, B list
	);
	
	<V, C, G, B extends ConfigEntryBuilder<V, C, G, B>,
	  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & AtomicEntryBuilder
	  > @NotNull CaptionedCollectionEntryBuilder<
	  Set<V>, Set<C>, G, EntrySetEntryBuilder<V, C, G, B>, CV, CC, CG, CB
	> caption(CB caption, EntrySetEntryBuilder<V, C, G, B> set);
	
	<K, V, KC, C, KG, G,
	  KB extends ConfigEntryBuilder<K, KC, KG, KB> & AtomicEntryBuilder,
	  B extends ConfigEntryBuilder<V, C, G, B>,
	  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & AtomicEntryBuilder
	  > @NotNull CaptionedCollectionEntryBuilder<
	  Map<K, V>, Map<KC, C>, Pair<KG, G>, EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB>,
	  CV, CC, CG, CB
	> caption(CB caption, EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB> map);
	
	<K, V, KC, C, KG, G,
	  KB extends ConfigEntryBuilder<K, KC, KG, KB> & AtomicEntryBuilder,
	  B extends ConfigEntryBuilder<V, C, G, B>,
	  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & AtomicEntryBuilder
	  > @NotNull CaptionedCollectionEntryBuilder<
	  List<Pair<K, V>>, List<Pair<KC, C>>, Pair<KG, G>,
	  EntryPairListEntryBuilder<K, V, KC, C, KG, G, B, KB>,
	  CV, CC, CG, CB
	> caption(CB caption, EntryPairListEntryBuilder<K, V, KC, C, KG, G, B, KB> pairList);
	
	// Pairs
	
	<L, R, LC, RC, LG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & AtomicEntryBuilder,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & AtomicEntryBuilder
	  > @NotNull EntryPairEntryBuilder<L, R, LC, RC, LG, RG> pair(LB leftEntry, RB rightEntry);
	<L, R, LC, RC, LG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & AtomicEntryBuilder,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & AtomicEntryBuilder
	  > @NotNull EntryPairEntryBuilder<L, R, LC, RC, LG, RG> pair(LB leftEntry, RB rightEntry, Pair<L, R> value);
	
	// Triples
	
	<L, M, R, LC, MC, RC, LG, MG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & AtomicEntryBuilder,
	  MB extends ConfigEntryBuilder<M, MC, MG, MB> & AtomicEntryBuilder,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & AtomicEntryBuilder
	  > @NotNull EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> triple(
	  LB leftEntry, MB middleEntry, RB rightEntry);
	<L, M, R, LC, MC, RC, LG, MG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & AtomicEntryBuilder,
	  MB extends ConfigEntryBuilder<M, MC, MG, MB> & AtomicEntryBuilder,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & AtomicEntryBuilder
	  > @NotNull EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> triple(
	  LB leftEntry, MB middleEntry, RB rightEntry, Triple<L, M, R> value);
	
	class PresetBuilder {
		protected final Map<String, Object> map = new LinkedHashMap<>();
		protected final String name;
		
		protected PresetBuilder(String presetName) {
			name = presetName;
		}
		
		public <V> @NotNull PresetBuilder add(String path, V value) {
			map.put(path, value);
			return this;
		}
		public @NotNull PresetBuilder n(PresetBuilder nest) {
			for (Map.Entry<String, Object> e: nest.map.entrySet()) {
				final String k = e.getKey();
				map.put(nest.name + "." + k, nest.map.get(k));
			}
			return this;
		}
		
		protected Map<String, Object> build() {
			return Collections.unmodifiableMap(map);
		}
	}
}
