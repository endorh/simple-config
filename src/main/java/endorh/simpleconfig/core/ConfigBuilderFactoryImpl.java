package endorh.simpleconfig.core;

import com.google.common.collect.Lists;
import endorh.simpleconfig.api.AbstractRange.DoubleRange;
import endorh.simpleconfig.api.AbstractRange.FloatRange;
import endorh.simpleconfig.api.AbstractRange.IntRange;
import endorh.simpleconfig.api.AbstractRange.LongRange;
import endorh.simpleconfig.api.*;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.entry.*;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl.CategoryBuilder;
import endorh.simpleconfig.core.SimpleConfigBuilderImpl.GroupBuilder;
import endorh.simpleconfig.core.entry.*;
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
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

@Internal public class ConfigBuilderFactoryImpl implements ConfigBuilderFactory {
	@Internal public ConfigBuilderFactoryImpl() {}
	
	@Override public SimpleConfigBuilder builder(String modId, Type type) {
		return new SimpleConfigBuilderImpl(modId, type);
	}
	@Override public SimpleConfigBuilder builder(String modId, Type type, Class<?> configClass) {
		return new SimpleConfigBuilderImpl(modId, type, configClass);
	}
	
	@Override public ConfigGroupBuilder group(String name) {
		return group(name, false);
	}
	@Override public ConfigGroupBuilder group(String name, boolean expand) {
		return new GroupBuilder(name, expand);
	}
	
	@Override public ConfigCategoryBuilder category(String name) {
		return new CategoryBuilder(name);
	}
	@Override public ConfigCategoryBuilder category(String name, Class<?> configClass) {
		return new CategoryBuilder(name, configClass);
	}
	
	// Entries
	
	// Basic types
	@Override public BooleanEntry.Builder bool(boolean value) {
		return new BooleanEntry.Builder(value);
	}
	
	@Override public BooleanEntry.Builder yesNo(boolean value) {
		return new BooleanEntry.Builder(value).text(BooleanEntryBuilder.BooleanDisplayer.YES_NO);
	}
	
	@Override public BooleanEntry.Builder enable(boolean value) {
		return bool(value).text(BooleanEntryBuilder.BooleanDisplayer.ENABLED_DISABLED);
	}
	
	@Override public BooleanEntry.Builder onOff(boolean value) {
		return bool(value).text(BooleanEntryBuilder.BooleanDisplayer.ON_OFF);
	}
	
	@Override public StringEntry.Builder string(String value) {
		return new StringEntry.Builder(value);
	}
	
	@Override @Deprecated public <E extends Enum<E>> EnumEntry.Builder<E> enum_(E value) {
		return new EnumEntry.Builder<>(value);
	}
	
	@Override public <E extends Enum<E>> EnumEntry.Builder<E> option(E value) {
		return new EnumEntry.Builder<>(value);
	}
	
	
	@Override public ButtonEntry.Builder button(Runnable action) {
		return new ButtonEntry.Builder(h -> action.run());
	}
	
	@Override public ButtonEntry.Builder button(Consumer<ConfigEntryHolder> action) {
		return new ButtonEntry.Builder(action);
	}
	
	@Override
	public <V, Gui, B extends ConfigEntryBuilder<V, ?, Gui, B> & KeyEntryBuilder<Gui>> EntryButtonEntryBuilder<V, Gui, B> button(
	  B inner, Consumer<V> action
	) {
		return button(inner, (v, h) -> action.accept(v));
	}
	
	@Override
	public <V, Gui, B extends ConfigEntryBuilder<V, ?, Gui, B> & KeyEntryBuilder<Gui>> EntryButtonEntryBuilder<V, Gui, B> button(
	  B inner, BiConsumer<V, ConfigEntryHolder> action
	) {
		return new EntryButtonEntry.Builder<>(inner, action);
	}
	
	@SuppressWarnings("unchecked") private static <
	  V, G, E extends AbstractConfigEntry<V, ?, G> & IKeyEntry<G>,
	  BB extends ConfigEntryBuilder<V, ?, G, BB> & KeyEntryBuilder<G>,
	  B extends AbstractConfigEntryBuilder<V, ?, G, E, BB, B> & KeyEntryBuilder<G>
	  > B cast3(BB builder) {
		checkBuilder(builder);
		return (B) builder;
	}
	
	private static void checkBuilder(ConfigEntryBuilder<?, ?, ?, ?> builder) {
		if (!(builder instanceof AbstractConfigEntryBuilder)) throw new IllegalArgumentException(
		  "Mixed API use: Builder is not subclass of AbstractConfigEntryBuilder");
	}
	
	@Override public PresetSwitcherEntry.Builder globalPresetSwitcher(
	  Map<String, Map<String, Object>> presets, String path
	) {
		return new PresetSwitcherEntry.Builder(presets, path, true);
	}
	
	
	@Override public PresetSwitcherEntry.Builder localPresetSwitcher(
	  Map<String, Map<String, Object>> presets, String path
	) {
		return new PresetSwitcherEntry.Builder(presets, path, false);
	}
	
	// Byte
	
	@Override public @Deprecated ByteEntry.Builder number(byte value) {
		return new ByteEntry.Builder(value);
	}
	
	@Override public @Deprecated ByteEntryBuilder number(byte value, byte max) {
		return number(value, (byte) 0, max);
	}
	
	@Override public @Deprecated ByteEntryBuilder number(byte value, byte min, byte max) {
		return new ByteEntry.Builder(value).range(min, max);
	}
	
	// Short
	
	@Override public @Deprecated ShortEntry.Builder number(short value) {
		return new ShortEntry.Builder(value);
	}
	
	@Override public @Deprecated ShortEntryBuilder number(short value, short max) {
		return number(value, (short) 0, max);
	}
	
	@Override public @Deprecated ShortEntryBuilder number(short value, short min, short max) {
		return new ShortEntry.Builder(value).range(min, max);
	}
	
	// Int
	
	@Override public IntegerEntry.Builder number(int value) {
		return new IntegerEntry.Builder(value);
	}
	
	@Override public IntegerEntryBuilder number(int value, int max) {
		return number(value, 0, max);
	}
	
	@Override public IntegerEntryBuilder number(int value, int min, int max) {
		return new IntegerEntry.Builder(value).range(min, max);
	}
	
	@Override public IntegerEntryBuilder percent(int value) {
		return number(value, 0, 100)
		  .slider("simpleconfig.format.slider.percentage");
	}
	
	// Long
	
	@Override public LongEntry.Builder number(long value) {
		return new LongEntry.Builder(value);
	}
	
	@Override public LongEntryBuilder number(long value, long max) {
		return number(value, 0L, max);
	}
	
	@Override public LongEntryBuilder number(long value, long min, long max) {
		return new LongEntry.Builder(value).range(min, max);
	}
	
	// Float
	
	@Override public FloatEntry.Builder number(float value) {
		return new FloatEntry.Builder(value);
	}
	
	@Override public FloatEntryBuilder number(float value, float max) {
		return number(value, 0F, max);
	}
	
	@Override public FloatEntryBuilder number(float value, float min, float max) {
		return new FloatEntry.Builder(value).range(min, max);
	}
	
	@Override public FloatEntryBuilder percent(float value) {
		return number(value, 0F, 100F)
		  .slider("simpleconfig.format.slider.percentage.float")
		  .fieldScale(0.01F);
	}
	
	// Double
	
	@Override public DoubleEntry.Builder number(double value) {
		return new DoubleEntry.Builder(value);
	}
	
	@Override public DoubleEntryBuilder number(double value, double max) {
		return number(value, 0D, max);
	}
	
	@Override public DoubleEntryBuilder number(double value, double min, double max) {
		return new DoubleEntry.Builder(value).range(min, max);
	}
	
	@Override public DoubleEntryBuilder percent(double value) {
		return number(value, 0D, 100D)
		  .slider("simpleconfig.format.slider.percentage.float")
		  .fieldScale(0.01);
	}
	
	@Override public FloatEntryBuilder fraction(float value) {
		if (0F > value || value > 1F)
			throw new IllegalArgumentException(
			  "Fraction values must be within [0, 1], passed " + value);
		return number(value).range(0, 1).slider();
	}
	
	@Override public DoubleEntryBuilder fraction(double value) {
		if (0D > value || value > 1D)
			throw new IllegalArgumentException(
			  "Fraction values must be within [0, 1], passed " + value);
		return number(value).range(0, 1).slider();
	}
	
	@Override public FloatEntryBuilder volume(float value) {
		return fraction(value).slider("simpleconfig.format.slider.volume");
	}
	
	@Override public FloatEntryBuilder volume() {
		return volume(1F);
	}
	
	@Override public DoubleEntryBuilder volume(double value) {
		return fraction(value).slider("simpleconfig.format.slider.volume");
	}
	
	@Override public DoubleRangeEntry.Builder range(DoubleRange range) {
		return new DoubleRangeEntry.Builder(range);
	}
	
	@Override public DoubleRangeEntry.Builder range(double min, double max) {
		return range(DoubleRange.inclusive(min, max));
	}
	
	@Override public FloatRangeEntry.Builder range(FloatRange range) {
		return new FloatRangeEntry.Builder(range);
	}
	
	@Override public FloatRangeEntry.Builder range(float min, float max) {
		return range(FloatRange.inclusive(min, max));
	}
	
	@Override public LongRangeEntry.Builder range(LongRange range) {
		return new LongRangeEntry.Builder(range);
	}
	
	@Override public LongRangeEntry.Builder range(long min, long max) {
		return range(LongRange.inclusive(min, max));
	}
	
	@Override public IntegerRangeEntry.Builder range(IntRange range) {
		return new IntegerRangeEntry.Builder(range);
	}
	
	@Override public IntegerRangeEntry.Builder range(int min, int max) {
		return range(IntRange.inclusive(min, max));
	}
	
	@Override public ColorEntry.Builder color(Color value) {
		return new ColorEntry.Builder(value);
	}
	
	@Override public PatternEntry.Builder pattern(Pattern pattern) {
		return new PatternEntry.Builder(pattern);
	}
	
	@Override public PatternEntry.Builder pattern(String pattern) {
		return new PatternEntry.Builder(pattern);
	}
	
	@Override public PatternEntry.Builder pattern(String pattern, int flags) {
		return new PatternEntry.Builder(pattern, flags);
	}
	
	// String serializable entries
	
	@Override public <V> SerializableEntry.Builder<V> entry(
	  V value, Function<V, String> serializer, Function<String, Optional<V>> deserializer
	) {
		return new SerializableEntry.Builder<>(value, serializer, deserializer);
	}
	
	@Override public <V> SerializableEntry.Builder<V> entry(
	  V value, IConfigEntrySerializer<V> serializer
	) {
		return new SerializableEntry.Builder<>(value, serializer);
	}
	
	@Override public <V extends ISerializableConfigEntry<V>> SerializableEntry.Builder<V> entry(
	  V value
	) {
		return new SerializableEntry.Builder<>(value, value.getConfigSerializer());
	}
	
	// Convenience Minecraft entries
	
	
	@Override public INBTEntry.Builder nbtValue(INBT value) {
		return new INBTEntry.Builder(value);
	}
	
	@Override public CompoundNBTEntry.Builder nbtTag(CompoundNBT value) {
		return new CompoundNBTEntry.Builder(value);
	}
	
	@Override public ResourceLocationEntry.Builder resource(String resourceName) {
		return new ResourceLocationEntry.Builder(new ResourceLocation(resourceName));
	}
	
	@Override public ResourceLocationEntry.Builder resource(ResourceLocation value) {
		return new ResourceLocationEntry.Builder(value);
	}
	
	@Override @OnlyIn(Dist.CLIENT) public KeyBindEntry.Builder key(ExtendedKeyBind keyBind) {
		return key(keyBind.getDefinition())
		  .bakeTo(keyBind)
		  .withDefaultSettings(keyBind.getDefinition().getSettings());
	}
	
	@Override @OnlyIn(Dist.CLIENT) public KeyBindEntry.Builder key(KeyBindMapping key) {
		return new KeyBindEntry.Builder(key);
	}
	
	@Override @OnlyIn(Dist.CLIENT) public KeyBindEntry.Builder key(String key) {
		return new KeyBindEntry.Builder(key);
	}
	
	@Override @OnlyIn(Dist.CLIENT) public KeyBindEntry.Builder key() {
		return new KeyBindEntry.Builder();
	}
	
	@Override public ItemEntry.Builder item(@Nullable Item value) {
		return new ItemEntry.Builder(value);
	}
	
	@Override public ItemNameEntry.Builder itemName(@Nullable ResourceLocation value) {
		return new ItemNameEntry.Builder(value);
	}
	
	@Override public ItemNameEntry.Builder itemName(Item value) {
		return itemName(value.getRegistryName());
	}
	
	@Override public BlockEntry.Builder block(@Nullable Block value) {
		return new BlockEntry.Builder(value);
	}
	
	@Override public BlockNameEntry.Builder blockName(@Nullable ResourceLocation value) {
		return new BlockNameEntry.Builder(value);
	}
	
	@Override public BlockNameEntry.Builder blockName(Block value) {
		return blockName(value.getRegistryName());
	}
	
	@Override public FluidEntry.Builder fluid(@Nullable Fluid value) {
		return new FluidEntry.Builder(value);
	}
	
	@Override public FluidNameEntry.Builder fluidName(@Nullable ResourceLocation value) {
		return new FluidNameEntry.Builder(value);
	}
	
	@Override public FluidNameEntry.Builder fluidName(Fluid value) {
		return fluidName(value.getRegistryName());
	}
	
	// List entries
	
	@Override public StringListEntry.Builder stringList(List<String> value) {
		return new StringListEntry.Builder(value);
	}
	
	@Override @Deprecated
	public ByteListEntry.Builder byteList(List<Byte> value) {
		return new ByteListEntry.Builder(value);
	}
	
	@Override @Deprecated
	public ShortListEntry.Builder shortList(List<Short> value) {
		return new ShortListEntry.Builder(value);
	}
	
	@Override public IntegerListEntry.Builder intList(List<Integer> value) {
		return new IntegerListEntry.Builder(value);
	}
	
	@Override public LongListEntry.Builder longList(List<Long> value) {
		return new LongListEntry.Builder(value);
	}
	
	@Override public FloatListEntry.Builder floatList(List<Float> value) {
		return new FloatListEntry.Builder(value);
	}
	
	@Override public DoubleListEntry.Builder doubleList(List<Double> value) {
		return new DoubleListEntry.Builder(value);
	}
	
	// Caption
	
	@Override public <
	  V, C, G, B extends ListEntryBuilder<V, C, G, B>,
	  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & KeyEntryBuilder<CG>
	  > CaptionedListEntryBuilder<V, C, G, B, CV, CC, CG, CB> caption(CB caption, B list) {
		return new CaptionedListEntry.Builder<>(
		  Pair.of(caption.getValue(), list.getValue()), list, caption);
	}
	
	@Override
	public <
	  K, V, KC, C, KG, G,
	  MB extends EntryMapEntryBuilder<K, V, KC, C, KG, G, ?, ?>,
	  CV, CC, CG, CB extends ConfigEntryBuilder<CV, CC, CG, CB> & KeyEntryBuilder<CG>
	  > CaptionedMapEntryBuilder<K, V, KC, C, KG, G, MB, CV, CC, CG, CB> caption(
	  CB caption, MB map
	) {
		// noinspection unchecked
		return (CaptionedMapEntryBuilder<K, V, KC, C, KG, G, MB, CV, CC, CG, CB>)
		  new CaptionedMapEntry.Builder<>(
			 Pair.of(caption.getValue(), map.getValue()), map, caption);
	}
	
	// List entry
	
	
	@Override public <
	  V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>
	  > EntryListEntryBuilder<V, C, G, Builder> list(Builder entry) {
		return list(entry, Collections.emptyList());
	}
	
	@Override
	public <V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>> EntryListEntryBuilder<V, C, G, Builder> list(
	  Builder entry, List<V> value
	) {
		return new EntryListEntry.Builder<>(value, entry);
	}
	
	@SafeVarargs
	@Override public final <
	  V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>
	  > EntryListEntryBuilder<V, C, G, Builder> list(Builder entry, V... values) {
		return list(entry, Lists.newArrayList(values));
	}
	
	// Map entry
	
	
	@Override public <
	  K, V, KC, C, KG, G,
	  Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & KeyEntryBuilder<KG>
	  > EntryMapEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> map(
	  KeyBuilder keyEntry, Builder entry
	) {
		return map(keyEntry, entry, new LinkedHashMap<>());
	}
	
	@Override public <
	  K, V, KC, C, KG, G,
	  Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & KeyEntryBuilder<KG>
	  > EntryMapEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> map(
	  KeyBuilder keyEntry, Builder entry, Map<K, V> value
	) {
		return new EntryMapEntry.Builder<>(value, keyEntry, entry);
	}
	
	@Override public <
	  V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>
	  > EntryMapEntryBuilder<String, V, String, C, String, G, Builder, StringEntryBuilder> map(
	  Builder entry
	) {
		return map(entry, new LinkedHashMap<>());
	}
	
	@Override
	public <V, C, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>> EntryMapEntryBuilder<String, V, String, C, String, G, Builder, StringEntryBuilder> map(
	  Builder entry, Map<String, V> value
	) {
		return map(string(""), entry, value);
	}
	
	// Pair list
	
	
	@Override public <
	  K, V, KC, C, KG, G,
	  Builder extends ConfigEntryBuilder<V, C, G, Builder>,
	  KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & KeyEntryBuilder<KG>
	  > EntryPairListEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> pairList(
	  KeyBuilder keyEntry, Builder entry
	) {
		return pairList(keyEntry, entry, new ArrayList<>());
	}
	
	@Override
	public <K, V, KC, C, KG, G, Builder extends ConfigEntryBuilder<V, C, G, Builder>, KeyBuilder extends ConfigEntryBuilder<K, KC, KG, KeyBuilder> & KeyEntryBuilder<KG>> EntryPairListEntryBuilder<K, V, KC, C, KG, G, Builder, KeyBuilder> pairList(
	  KeyBuilder keyEntry, Builder entry, List<Pair<K, V>> value
	) {
		return new EntryPairListEntry.Builder<>(value, keyEntry, entry);
	}
	
	// Pairs
	
	
	@Override public <
	  L, R, LC, RC, LG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & KeyEntryBuilder<LG>,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & KeyEntryBuilder<RG>
	  > EntryPairEntryBuilder<L, R, LC, RC, LG, RG> pair(LB leftEntry, RB rightEntry) {
		return pair(leftEntry, rightEntry, Pair.of(leftEntry.getValue(), rightEntry.getValue()));
	}
	
	@Override public <
	  L, R, LC, RC, LG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & KeyEntryBuilder<LG>,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & KeyEntryBuilder<RG>
	  > EntryPairEntryBuilder<L, R, LC, RC, LG, RG> pair(
	  LB leftEntry, RB rightEntry, Pair<L, R> value
	) {
		return new EntryPairEntry.Builder<>(value, leftEntry, rightEntry);
	}
	
	// Triple
	
	
	@Override public <
	  L, M, R, LC, MC, RC, LG, MG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & KeyEntryBuilder<LG>,
	  MB extends ConfigEntryBuilder<M, MC, MG, MB> & KeyEntryBuilder<MG>,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & KeyEntryBuilder<RG>
	  > EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> triple(
	  LB leftEntry, MB middleEntry, RB rightEntry
	) {
		return triple(leftEntry, middleEntry, rightEntry, Triple.of(
		  leftEntry.getValue(), middleEntry.getValue(), rightEntry.getValue()));
	}
	
	@Override public <
	  L, M, R, LC, MC, RC, LG, MG, RG,
	  LB extends ConfigEntryBuilder<L, LC, LG, LB> & KeyEntryBuilder<LG>,
	  MB extends ConfigEntryBuilder<M, MC, MG, MB> & KeyEntryBuilder<MG>,
	  RB extends ConfigEntryBuilder<R, RC, RG, RB> & KeyEntryBuilder<RG>
	  > EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> triple(
	  LB leftEntry, MB middleEntry, RB rightEntry, Triple<L, M, R> value
	) {
		return new EntryTripleEntry.Builder<>(
		  value, leftEntry, middleEntry, rightEntry);
	}
}
