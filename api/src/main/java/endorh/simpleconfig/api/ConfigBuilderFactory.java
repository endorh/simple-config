package endorh.simpleconfig.api;

import endorh.simpleconfig.api.AbstractRange.DoubleRange;
import endorh.simpleconfig.api.AbstractRange.FloatRange;
import endorh.simpleconfig.api.AbstractRange.IntRange;
import endorh.simpleconfig.api.AbstractRange.LongRange;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.entry.*;
import endorh.simpleconfig.ui.hotkey.ExtendedKeyBind;
import endorh.simpleconfig.ui.hotkey.KeyBindMapping;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

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
	
	SimpleConfigBuilder builder(String modId, Type type);
	SimpleConfigBuilder builder(String modId, Type type, Class<?> configClass);
	
	ConfigGroupBuilder group(String name);
	ConfigGroupBuilder group(String name, boolean expand);
	
	ConfigCategoryBuilder category(String name);
	ConfigCategoryBuilder category(String name, Class<?> configClass);
	
	// Boolean entries
	
	BooleanEntryBuilder bool(boolean value);
	BooleanEntryBuilder yesNo(boolean value);
	BooleanEntryBuilder enable(boolean value);
	BooleanEntryBuilder onOff(boolean value);
	
	// Strings
	
	StringEntryBuilder string(String value);
	
	// Enums
	
	@Deprecated <E extends Enum<E>> EnumEntryBuilder<E> enum_(E value);
	<E extends Enum<E>> EnumEntryBuilder<E> option(E value);
	
	// Buttons
	
	ButtonEntryBuilder button(Runnable action);
	ButtonEntryBuilder button(Consumer<ConfigEntryHolder> action);
	
	<V, Gui, B extends ConfigEntryBuilder<V, ?, Gui, B> & KeyEntryBuilder<Gui>>
	EntryButtonEntryBuilder<V, Gui, B> button(B inner, Consumer<V> action);
	<V, Gui, B extends ConfigEntryBuilder<V, ?, Gui, B> & KeyEntryBuilder<Gui>>
	EntryButtonEntryBuilder<V, Gui, B> button(
	  B inner, BiConsumer<V, ConfigEntryHolder> action);
	
	// Presets
	
	PresetSwitcherEntryBuilder globalPresetSwitcher(
	  Map<String, Map<String, Object>> presets, String path);
	PresetSwitcherEntryBuilder localPresetSwitcher(
	  Map<String, Map<String, Object>> presets, String path);
	default Map<String, Map<String, Object>> presets(PresetBuilder... presets) {
		final Map<String, Map<String, Object>> map = new LinkedHashMap<>();
		Arrays.stream(presets).forEachOrdered(p -> map.put(p.name, p.build()));
		return Collections.unmodifiableMap(map);
	}
	default PresetBuilder preset(String name) {
		return new PresetBuilder(name);
	}
	
	// Numbers
	
	@Deprecated ByteEntryBuilder number(byte value);
	@Deprecated ByteEntryBuilder number(byte value, byte max);
	@Deprecated ByteEntryBuilder number(byte value, byte min, byte max);
	
	@Deprecated ShortEntryBuilder number(short value);
	@Deprecated ShortEntryBuilder number(short value, short max);
	@Deprecated ShortEntryBuilder number(short value, short min, short max);
	
	IntegerEntryBuilder number(int value);
	IntegerEntryBuilder number(int value, int max);
	IntegerEntryBuilder number(int value, int min, int max);
	
	IntegerEntryBuilder percent(int value);
	
	LongEntryBuilder number(long value);
	LongEntryBuilder number(long value, long max);
	LongEntryBuilder number(long value, long min, long max);
	
	FloatEntryBuilder number(float value);
	FloatEntryBuilder number(float value, float max);
	FloatEntryBuilder number(float value, float min, float max);
	
	FloatEntryBuilder percent(float value);
	
	DoubleEntryBuilder number(double value);
	DoubleEntryBuilder number(double value, double max);
	DoubleEntryBuilder number(double value, double min, double max);
	
	DoubleEntryBuilder percent(double value);
	
	FloatEntryBuilder fraction(float value);
	DoubleEntryBuilder fraction(double value);
	
	FloatEntryBuilder volume(float value);
	FloatEntryBuilder volume();
	DoubleEntryBuilder volume(double value);
	
	// Ranges
	
	DoubleRangeEntryBuilder range(DoubleRange range);
	DoubleRangeEntryBuilder range(double min, double max);
	
	FloatRangeEntryBuilder range(FloatRange range);
	FloatRangeEntryBuilder range(float min, float max);
	
	LongRangeEntryBuilder range(LongRange range);
	LongRangeEntryBuilder range(long min, long max);
	
	IntegerRangeEntryBuilder range(IntRange range);
	IntegerRangeEntryBuilder range(int min, int max);
	
	// Color
	
	ColorEntryBuilder color(Color value);
	
	// Serializable entries
	
	PatternEntryBuilder pattern(Pattern pattern);
	PatternEntryBuilder pattern(String pattern);
	PatternEntryBuilder pattern(String pattern, int flags);
	
	<V> SerializableEntryBuilder<V, ?> entry(
	  V value, Function<V, String> serializer, Function<String, Optional<V>> deserializer);
	<V> ISerializableEntryBuilder<V> entry(
	  V value, IConfigEntrySerializer<V> serializer);
	<V extends ISerializableConfigEntry<V>> ISerializableEntryBuilder<V> entry(V value);
	
	<B> BeanEntryBuilder<B> bean(B value);
	
	// Convenience Minecraft entries
	
	INBTEntryBuilder nbtValue(INBT value);
	CompoundNBTEntryBuilder nbtTag(CompoundNBT value);
	
	ResourceLocationEntryBuilder resource(String resourceName);
	ResourceLocationEntryBuilder resource(ResourceLocation value);
	
	@OnlyIn(Dist.CLIENT) KeyBindEntryBuilder key(ExtendedKeyBind keyBind);
	@OnlyIn(Dist.CLIENT) KeyBindEntryBuilder key(KeyBindMapping key);
	@OnlyIn(Dist.CLIENT) KeyBindEntryBuilder key(String key);
	@OnlyIn(Dist.CLIENT) KeyBindEntryBuilder key();
	
	ItemEntryBuilder item(@Nullable Item value);
	ItemNameEntryBuilder itemName(@Nullable ResourceLocation value);
	ItemNameEntryBuilder itemName(Item value);
	
	BlockEntryBuilder block(@Nullable Block value);
	BlockNameEntryBuilder blockName(@Nullable ResourceLocation value);
	BlockNameEntryBuilder blockName(Block value);
	
	FluidEntryBuilder fluid(@Nullable Fluid value);
	FluidNameEntryBuilder fluidName(@Nullable ResourceLocation value);
	FluidNameEntryBuilder fluidName(Fluid value);
	
	// List entries
	
	StringListEntryBuilder stringList(List<String> value);
	@Deprecated ByteListEntryBuilder byteList(List<Byte> value);
	@Deprecated ShortListEntryBuilder shortList(List<Short> value);
	IntegerListEntryBuilder intList(List<Integer> value);
	LongListEntryBuilder longList(List<Long> value);
	FloatListEntryBuilder floatList(List<Float> value);
	DoubleListEntryBuilder doubleList(List<Double> value);
	
	// Captioned collections
	
	<V, C, G, B extends ListEntryBuilder<V, C, G, B>,
	  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & KeyEntryBuilder<CG>>
	CaptionedListEntryBuilder<V, C, G, B, CV, CC, CG, CB> caption(CB caption, B list);
	
	<K, V, KC, C, KG, G,
	  MB extends EntryMapEntryBuilder<K, V, KC, C, KG, G, ?, ?>,
	  CV, CC, CG,
	  CB extends ConfigEntryBuilder<CV, CC, CG, CB> & KeyEntryBuilder<CG>
	> CaptionedMapEntryBuilder<K, V, KC, C, KG, G, MB, CV, CC, CG, CB> caption(CB caption, MB map);
	
	// General lists
	
	<V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	EntryListEntryBuilder<V, C, G, Builder> list(Builder entry);
	<V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	EntryListEntryBuilder<V, C, G, Builder> list(Builder entry, List<V> value);
	@SuppressWarnings("unchecked") <V, C, G,
	  Builder extends ConfigEntryBuilder<V, C, G, Builder>
	> EntryListEntryBuilder<V, C, G, Builder> list(Builder entry, V... values);
	
	// General maps
	
	<K, V, KC, C, KG, G,
	  Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & KeyEntryBuilder<KG>
	> EntryMapEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> map(
	  KeyBuilder keyEntry, Builder entry);
	<K, V, KC, C, KG, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & KeyEntryBuilder<KG>>
	EntryMapEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> map(
	  KeyBuilder keyEntry, Builder entry, Map<K, V> value);
	<V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	EntryMapEntryBuilder<String, V, String, C, String, G, Builder, StringEntryBuilder> map(
	  Builder entry);
	<V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>>
	EntryMapEntryBuilder<String, V, String, C, String, G, Builder, StringEntryBuilder> map(
	  Builder entry, Map<String, V> value);
	
	// Pair list
	
	<K, V, KC, C, KG, G,
	  Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & KeyEntryBuilder<KG>
	> EntryPairListEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> pairList(
	  KeyBuilder keyEntry, Builder entry);
	<K, V, KC, C, KG, G,
	  Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & KeyEntryBuilder<KG>>
	EntryPairListEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> pairList(
	  KeyBuilder keyEntry, Builder entry, List<Pair<K, V>> value);
	
	// Pairs
	
	<L, R, LC, RC, LG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & KeyEntryBuilder<LG>,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & KeyEntryBuilder<RG>
	> EntryPairEntryBuilder<L, R, LC, RC, LG, RG> pair(LB leftEntry, RB rightEntry);
	<L, R, LC, RC, LG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & KeyEntryBuilder<LG>,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & KeyEntryBuilder<RG>
	> EntryPairEntryBuilder<L, R, LC, RC, LG, RG> pair(LB leftEntry, RB rightEntry, Pair<L, R> value);
	
	// Triples
	
	<L, M, R, LC, MC, RC, LG, MG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & KeyEntryBuilder<LG>,
	  MB extends ConfigEntryBuilder<M, MC, MG, MB> & KeyEntryBuilder<MG>,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & KeyEntryBuilder<RG>
	> EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> triple(
	  LB leftEntry, MB middleEntry, RB rightEntry);
	<L, M, R, LC, MC, RC, LG, MG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & KeyEntryBuilder<LG>,
	  MB extends ConfigEntryBuilder<M, MC, MG, MB> & KeyEntryBuilder<MG>,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & KeyEntryBuilder<RG>
	> EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> triple(
	  LB leftEntry, MB middleEntry, RB rightEntry, Triple<L, M, R> value);
	
	class PresetBuilder {
		protected final Map<String, Object> map = new LinkedHashMap<>();
		protected final String name;
		
		protected PresetBuilder(String presetName) {
			name = presetName;
		}
		
		public <V> PresetBuilder add(String path, V value) {
			map.put(path, value);
			return this;
		}
		public PresetBuilder n(PresetBuilder nest) {
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
