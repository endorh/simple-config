package endorh.simple_config.core.entry;

import endorh.simple_config.clothconfig2.api.Modifier;
import endorh.simple_config.clothconfig2.api.ModifierKeyCode;
import endorh.simple_config.core.*;
import endorh.simple_config.core.annotation.Entry;
import endorh.simple_config.core.annotation.HasAlpha;
import endorh.simple_config.core.annotation.Slider;
import endorh.simple_config.core.entry.KeyBindEntry.Builder;
import net.minecraft.block.Block;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.simple_config.core.AbstractConfigEntryBuilder.getValue;
import static endorh.simple_config.core.ReflectionUtil.checkType;
import static endorh.simple_config.core.SimpleConfigClassParser.*;

/**
 * Contains builder methods for all the built-in config entry types
 */
@SuppressWarnings("unused")
public class Builders {
	// Basic types
	
	/**
	 * Boolean entry<br>
	 * You may change the text that appears in the button using
	 * {@link BooleanEntry.Builder#text}
	 */
	public static BooleanEntry.Builder bool(boolean value) {
		return new BooleanEntry.Builder(value);
	}
	
	/**
	 * Boolean entry<br>
	 * Uses the labels "Enabled" and "Disabled" instead of the usual "Yes" and "No"<br>
	 * You may also provide your own labels using {@link BooleanEntry.Builder#text}
	 */
	public static BooleanEntry.Builder enable(boolean value) {
		return bool(value).text("simple-config.format.bool.enable");
	}
	
	/**
	 * String entry
	 */
	public static StringEntry.Builder string(String value) {
		return new StringEntry.Builder(value);
	}
	
	/**
	 * Enum entry
	 */
	public static <E extends Enum<E>> EnumEntry.Builder<E> enum_(E value) {
		return new EnumEntry.Builder<>(value);
	}
	
	/**
	 * Button entry<br>
	 * Displays a button in the GUI that can trigger an arbitrary action.<br>
	 * The action may receive the immediate parent of the entry as parameter.
	 */
	public static ButtonEntry.Builder button(Runnable action) {
		return new ButtonEntry.Builder(h -> action.run());
	}
	
	/**
	 * Button entry<br>
	 * Displays a button in the GUI that can trigger an arbitrary action.<br>
	 * The action may receive the immediate parent of the entry as parameter.
	 */
	public static ButtonEntry.Builder button(Consumer<ISimpleConfigEntryHolder> action) {
		return new ButtonEntry.Builder(action);
	}
	
	/**
	 * Add a button to another entry.<br>
	 * Not persistent. Useful for GUI screen interaction.
	 */
	public static <V, Gui, Inner extends AbstractConfigEntry<V, ?, Gui, Inner> & IKeyEntry<?, Gui>>
	EntryButtonEntry.Builder<V, Gui, Inner> button(
	  AbstractConfigEntryBuilder<V, ?, Gui, Inner, ?> inner, Consumer<V> action
	) { return button(inner, (v, h) -> action.accept(v)); }
	
	
	/**
	 * Add a button to another entry.<br>
	 * Not persistent. Useful for GUI screen interaction.
	 */
	public static <V, Gui, Inner extends AbstractConfigEntry<V, ?, Gui, Inner> & IKeyEntry<?, Gui>>
	EntryButtonEntry.Builder<V, Gui, Inner> button(
	  AbstractConfigEntryBuilder<V, ?, Gui, Inner, ?> inner,
	  BiConsumer<V, ISimpleConfigEntryHolder> action
	) {
		return new EntryButtonEntry.Builder<>(inner, action);
	}
	
	/**
	 * Enum-like cycling button between a definite, finite amount of values.<br>
	 */
	@SuppressWarnings("unchecked") public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E> & IKeyEntry<C, G>>
	SelectorEntry.Builder<V, C, G, E> select(
	  AbstractConfigEntryBuilder<V, C, ?, E, ?> builder, V... values
	) {
		return new SelectorEntry.Builder<>(builder, values);
	}
	
	/**
	 * Enum-like cycling button between a definite, finite amount of values.
	 */
	public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E> & IKeyEntry<C, G>>
	SelectorEntry.Builder<V, C, G, E> select(
	  AbstractConfigEntryBuilder<V, C, G, E, ?> builder, List<V> values
	) {
		//noinspection unchecked
		return new SelectorEntry.Builder<>(builder, (V[]) values.toArray(new Object[0]));
	}
	
	/**
	 * An entry that lets users apply different presets to the entries, using global paths.<br>
	 * Create presets using {@link Builders#presets(PresetBuilder...)} and {@link Builders#preset(String)}
	 * or just create a map your way.
	 */
	public static PresetSwitcherEntry.Builder globalPresetSwitcher(
	  Map<String, Map<String, Object>> presets, String path
	) {
		return new PresetSwitcherEntry.Builder(presets, path, true);
	}
	
	
	/**
	 * An entry that lets users apply different presets to the entries,
	 * using local paths from the parent of this entry.<br>
	 * Create presets using {@link Builders#presets(PresetBuilder...)} and {@link Builders#preset(String)}
	 * or just create a map your way.
	 */
	public static PresetSwitcherEntry.Builder localPresetSwitcher(
	  Map<String, Map<String, Object>> presets, String path
	) {
		return new PresetSwitcherEntry.Builder(presets, path, false);
	}
	
	/**
	 * Create a preset map from a collection of preset builders
	 */
	public static Map<String, Map<String, Object>> presets(PresetBuilder... presets) {
		final Map<String, Map<String, Object>> map = new LinkedHashMap<>();
		Arrays.stream(presets).forEachOrdered(p -> map.put(p.name, p.build()));
		return Collections.unmodifiableMap(map);
	}
	
	/**
	 * Preset map builder
	 */
	public static PresetBuilder preset(String name) {
		return new PresetBuilder(name);
	}
	
	public static class PresetBuilder {
		protected final Map<String, Object> map = new LinkedHashMap<>();
		protected final String name;
		
		protected PresetBuilder(String presetName) {
			name = presetName;
		}
		
		/**
		 * Add an entry to the preset
		 */
		public <V> PresetBuilder add(String path, V value) {
			map.put(path, value);
			return this;
		}
		
		/**
		 * Add all entries from another preset, using the prefix name as
		 * prefix for the entries (separated with a dot).
		 */
		public PresetBuilder n(PresetBuilder nest) {
			for (Map.Entry<String, Object> e : nest.map.entrySet()) {
				final String k = e.getKey();
				map.put(nest.name + "." + k, nest.map.get(k));
			}
			return this;
		}
		
		protected Map<String, Object> build() {
			return Collections.unmodifiableMap(map);
		}
	}
	
	// Byte
	/**
	 * Unbound byte value
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ByteEntry.Builder number(byte value) {
		return new ByteEntry.Builder(value);
	}
	
	/**
	 * Non-negative byte between 0 and {@code max} (inclusive)
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ByteEntry.Builder number(byte value, byte max) {
		return number(value, (byte) 0, max);
	}
	/**
	 * Byte value between {@code min} and {@code max} (inclusive)
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ByteEntry.Builder number(byte value, byte min, byte max) {
		return new ByteEntry.Builder(value).range(min, max);
	}
	
	// Short
	/**
	 * Unbound short value
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ShortEntry.Builder number(short value) {
		return new ShortEntry.Builder(value);
	}
	/**
	 * Non-negative short between 0 and {@code max} (inclusive)
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ShortEntry.Builder number(short value, short max) {
		return number(value, (short) 0, max);
	}
	/**
	 * Short value between {@code min} and {@code max} (inclusive)
	 * @deprecated Use a bound int entry
	 */
	public static @Deprecated ShortEntry.Builder number(short value, short min, short max) {
		return new ShortEntry.Builder(value).range(min, max);
	}
	
	// Int
	/**
	 * Unbound integer value
	 */
	public static IntegerEntry.Builder number(int value) {
		return new IntegerEntry.Builder(value);
	}
	/**
	 * Non-negative integer between 0 and {@code max} (inclusive)
	 */
	public static IntegerEntry.Builder number(int value, int max) {
		return number(value, 0, max);
	}
	/**
	 * Integer value between {@code min} and {@code max} (inclusive)
	 */
	public static IntegerEntry.Builder number(int value, int min, int max) {
		return new IntegerEntry.Builder(value).range(min, max);
	}
	/**
	 * Integer percentage, between 0 and 100 (inclusive)<br>
	 * Displayed as a slider
	 */
	public static IntegerEntry.Builder percent(int value) {
		return number(value, 0, 100)
		  .slider("simple-config.format.slider.percentage");
	}
	
	// Long
	/**
	 * Unbound long value
	 */
	public static LongEntry.Builder number(long value) {
		return new LongEntry.Builder(value);
	}
	/**
	 * Non-negative long between 0 and {@code max} (inclusive)
	 */
	public static LongEntry.Builder number(long value, long max) {
		return number(value, 0L, max);
	}
	/**
	 * Long value between {@code min} and {@code max} (inclusive)
	 */
	public static LongEntry.Builder number(long value, long min, long max) {
		return new LongEntry.Builder(value).range(min, max);
	}
	
	// Float
	/**
	 * Unbound float value
	 */
	public static FloatEntry.Builder number(float value) {
		return new FloatEntry.Builder(value);
	}
	/**
	 * Non-negative float value between 0 and {@code max} (inclusive)
	 */
	public static FloatEntry.Builder number(float value, float max) {
		return number(value, 0F, max);
	}
	/**
	 * Float value between {@code min} and {@code max} inclusive
	 */
	public static FloatEntry.Builder number(float value, float min, float max) {
		return new FloatEntry.Builder(value).range(min, max);
	}
	/**
	 * Float percentage, between 0 and 100, but stored as a fraction
	 * between 0.0 and 1.0 in the backing field (not the config file).<br>
	 * Displayed as a slider
	 */
	public static FloatEntry.Builder percent(float value) {
		return number(value, 0F, 100F)
		  .slider("simple-config.format.slider.percentage.float")
		  .fieldScale(0.01F);
	}
	
	// Double
	/**
	 * Unbound double value
	 */
	public static DoubleEntry.Builder number(double value) {
		return new DoubleEntry.Builder(value);
	}
	/**
	 * Non-negative double value between 0 and {@code max} (inclusive)
	 */
	public static DoubleEntry.Builder number(double value, double max) {
		return number(value, 0D, max);
	}
	/**
	 * Double value between {@code min} and {@code max} inclusive
	 */
	public static DoubleEntry.Builder number(double value, double min, double max) {
		return new DoubleEntry.Builder(value).range(min, max);
	}
	/**
	 * Double percentage, between 0 and 100, but stored as a fraction
	 * between 0.0 and 1.0 in the backing field (not the config file).<br>
	 * Displayed as a slider
	 */
	public static DoubleEntry.Builder percent(double value) {
		return number(value, 0D, 100D)
		  .slider("simple-config.format.slider.percentage.float")
		  .fieldScale(0.01);
	}
	
	/**
	 * Float value between 0 and 1 (inclusive)
	 */
	public static FloatEntry.Builder fraction(float value) {
		if (0F > value || value > 1F)
			throw new IllegalArgumentException(
			  "Fraction values must be within [0, 1], passed " + value);
		return number(value).range(0, 1).slider();
	}
	
	/**
	 * Double value between 0 and 1 (inclusive)
	 */
	public static DoubleEntry.Builder fraction(double value) {
		if (0D > value || value > 1D)
			throw new IllegalArgumentException(
			  "Fraction values must be within [0, 1], passed " + value);
		return number(value).range(0, 1).slider();
	}
	
	/**
	 * Float entry between 0 and 1 (inclusive)<br>
	 * Displays a volume label in the slider instead of the usual "Value: %s"
	 */
	public static FloatEntry.Builder volume(float value) {
		return fraction(value).slider("simple-config.format.slider.volume");
	}
	
	/**
	 * Float entry between 0 and 1 (inclusive) with default value of 1.<br>
	 * Displays a volume label in the slider instead of the usual "Value: %s"
	 */
	public static FloatEntry.Builder volume() {
		return volume(1F);
	}
	
	/**
	 * Double entry between 0 and 1 (inclusive)<br>
	 * Displays a volume label in the slider instead of the usual "Value: %s"
	 */
	public static DoubleEntry.Builder volume(double value) {
		return fraction(value).slider("simple-config.format.slider.volume");
	}
	
	/**
	 * Color entry<br>
	 * Use {@link ColorEntry.Builder#alpha()} to allow alpha values
	 */
	public static ColorEntry.Builder color(Color value) {
		return new ColorEntry.Builder(value);
	}
	
	/**
	 * Regex Pattern entry<br>
	 * Will use the flags of the passed regex to compile user input<br>
	 */
	public static PatternEntry.Builder pattern(Pattern pattern) {
		return new PatternEntry.Builder(pattern);
	}
	
	/**
	 * Regex pattern entry with default flags
	 */
	public static PatternEntry.Builder pattern(String pattern) {
		return new PatternEntry.Builder(pattern);
	}
	
	/**
	 * Regex pattern
	 */
	public static PatternEntry.Builder pattern(String pattern, int flags) {
		return new PatternEntry.Builder(pattern, flags);
	}
	
	// String serializable entries
	
	/**
	 * Entry of a String serializable object
	 */
	public static <V> SerializableEntry.Builder<V> entry(
	  V value, Function<V, String> serializer, Function<String, Optional<V>> deserializer
	) {
		return new SerializableEntry.Builder<>(value, serializer, deserializer);
	}
	/**
	 * Entry of a String serializable object
	 */
	public static <V> SerializableEntry.Builder<V> entry(
	  V value, IConfigEntrySerializer<V> serializer
	) {
		return new SerializableEntry.Builder<>(value, serializer);
	}
	/**
	 * Entry of a String serializable object
	 */
	public static <V extends ISerializableConfigEntry<V>> SerializableEntry.Builder<V> entry(V value) {
		return new SerializableEntry.Builder<>(value, value.getConfigSerializer());
	}
	
	// Convenience Minecraft entries
	
	
	/**
	 * NBT entry that accepts any kind of NBT, either values or compounds
	 */
	public static INBTEntry.Builder nbtValue(INBT value) {
		return new INBTEntry.Builder(value);
	}
	
	/**
	 * NBT entry that accepts NBT compounds
	 */
	public static CompoundNBTEntry.Builder nbtTag(CompoundNBT value) {
		return new CompoundNBTEntry.Builder(value);
	}
	
	/**
	 * Generic resource location entry
	 */
	public static ResourceLocationEntry.Builder resource(String resourceName) {
		return new ResourceLocationEntry.Builder(new ResourceLocation(resourceName));
	}
	/**
	 * Generic resource location entry
	 */
	public static ResourceLocationEntry.Builder resource(ResourceLocation value) {
		return new ResourceLocationEntry.Builder(value);
	}
	
	/**
	 * Key binding entry. By default, accepts modifiers but not mouse keys.<br>
	 * This is because handling mouse keys requires extra code on your end,
	 * if you only ever handle keyCode and scanCode in keyPress events, you won't
	 * be able to detect mouse keys.<br><br>
	 * <b>Prefer registering regular {@link KeyBinding}s through
	 * {@link net.minecraftforge.fml.client.registry.ClientRegistry#registerKeyBinding(KeyBinding)}
	 * </b><br>
	 * <b>KeyBindings registered the proper way can be configured altogether with
	 * other vanilla/modded keybindings, with highlighted conflicts</b><br><br>
	 * The only encouraged use of KeyBind entries is when you need further
	 * flexibility, such as <em>a map of KeyBinds to actions of some kind</em>.
	 * Use wisely.
	 */
	public static KeyBindEntry.Builder key(ModifierKeyCode key) {
		return new KeyBindEntry.Builder(key);
	}
	
	/**
	 * Key binding entry. By default, accepts modifiers but not mouse keys.<br>
	 * This is because handling mouse keys requires extra code on your end,
	 * if you only ever handle keyCode and scanCode in keyPress events, you won't
	 * be able to detect mouse keys.<br><br>
	 * <b>Prefer registering regular {@link KeyBinding}s through
	 * {@link net.minecraftforge.fml.client.registry.ClientRegistry#registerKeyBinding(KeyBinding)}
	 * </b><br>
	 * <b>KeyBindings registered the proper way can be configured altogether with
	 * other vanilla/modded keybindings, with highlighted conflicts</b><br><br>
	 * The only encouraged use of KeyBind entries is when you need further
	 * flexibility, such as <em>a map of KeyBinds to actions of some kind</em>.
	 * Use wisely.
	 */
	public static Builder key(InputMappings.Input key) {
		return key(key, Modifier.none());
	}
	
	/**
	 * Key binding entry. By default, accepts modifiers but not mouse keys.<br>
	 * This is because handling mouse keys requires extra code on your end,
	 * if you only ever handle keyCode and scanCode in keyPress events, you won't
	 * be able to detect mouse keys.<br><br>
	 * <b>Prefer registering regular {@link KeyBinding}s through
	 * {@link net.minecraftforge.fml.client.registry.ClientRegistry#registerKeyBinding(KeyBinding)}
	 * </b><br>
	 * <b>KeyBindings registered the proper way can be configured altogether with
	 * other vanilla/modded keybindings, with highlighted conflicts</b><br><br>
	 * The only encouraged use of KeyBind entries is when you need further
	 * flexibility, such as <em>a map of KeyBinds to actions of some kind</em>.
	 * Use wisely.
	 */
	public static KeyBindEntry.Builder key(InputMappings.Input key, Modifier modifier) {
		return new KeyBindEntry.Builder(key, modifier);
	}
	
	/**
	 * Key binding entry. By default, accepts modifiers but not mouse keys.<br>
	 * This is because handling mouse keys requires extra code on your end,
	 * if you only ever handle keyCode and scanCode in keyPress events, you won't
	 * be able to detect mouse keys.<br><br>
	 * <b>Prefer registering regular {@link KeyBinding}s through
	 * {@link net.minecraftforge.fml.client.registry.ClientRegistry#registerKeyBinding(KeyBinding)}
	 * </b><br>
	 * <b>KeyBindings registered the proper way can be configured altogether with
	 * other vanilla/modded keybindings, with highlighted conflicts</b><br><br>
	 * The only encouraged use of KeyBind entries is when you need further
	 * flexibility, such as <em>a map of KeyBinds to actions of some kind</em>.
	 * Use wisely.
	 */
	public static KeyBindEntry.Builder key(String key) {
		return new KeyBindEntry.Builder(key);
	}
	
	/**
	 * Key binding entry. By default, accepts modifiers but not mouse keys.<br>
	 * This is because handling mouse keys requires extra code on your end,
	 * if you only ever handle keyCode and scanCode in keyPress events, you won't
	 * be able to detect mouse keys.<br><br>
	 * <b>Prefer registering regular {@link KeyBinding}s through
	 * {@link net.minecraftforge.fml.client.registry.ClientRegistry#registerKeyBinding(KeyBinding)}
	 * </b><br>
	 * <b>KeyBindings registered the proper way can be configured altogether with
	 * other vanilla/modded keybindings, with highlighted conflicts</b><br><br>
	 * The only encouraged use of KeyBind entries is when you need further
	 * flexibility, such as <em>a map of KeyBinds to actions of some kind</em>.
	 * Use wisely.
	 */
	public static KeyBindEntry.Builder key() {
		return new KeyBindEntry.Builder();
	}
	
	/**
	 * Item entry<br>
	 * Use {@link Builders#itemName} instead to use ResourceLocations as value,
	 * to allow unknown items.
	 */
	public static ItemEntry.Builder item(@Nullable Item value) {
		return new ItemEntry.Builder(value);
	}
	
	/**
	 * Item name entry<br>
	 * Use {@link Builders#item} instead to use Item objects as value.
	 */
	public static ItemNameEntry.Builder itemName(@Nullable ResourceLocation value) {
		return new ItemNameEntry.Builder(value);
	}
	
	/**
	 * Item name entry<br>
	 * Use {@link Builders#item} instead to use Item objects as value.
	 */
	public static ItemNameEntry.Builder itemName(Item value) {
		return itemName(value.getRegistryName());
	}
	
	/**
	 * Block entry<br>
	 * Use {@link Builders#blockName} instead to use ResourceLocations as value,
	 * to allow unknown blocks.
	 */
	public static BlockEntry.Builder block(@Nullable Block value) {
		return new BlockEntry.Builder(value);
	}
	
	/**
	 * Block name entry<br>
	 * Use {@link Builders#block} instead to use Block objects as value.
	 */
	public static BlockNameEntry.Builder blockName(@Nullable ResourceLocation value) {
		return new BlockNameEntry.Builder(value);
	}
	
	/**
	 * Block name entry<br>
	 * Use {@link Builders#block} instead to use Block objects as value.
	 */
	public static BlockNameEntry.Builder blockName(Block value) {
		return blockName(value.getRegistryName());
	}
	
	/**
	 * Fluid entry<br>
	 * Use {@link Builders#fluidName} instead to use ResourceLocations as value,
	 * to allow unknown fluids.
	 */
	public static FluidEntry.Builder fluid(@Nullable Fluid value) {
		return new FluidEntry.Builder(value);
	}
	
	/**
	 * Fluid name entry<br>
	 * Use {@link Builders#fluid} instead to use Fluid objects as value.
	 */
	public static FluidNameEntry.Builder fluidName(@Nullable ResourceLocation value) {
		return new FluidNameEntry.Builder(value);
	}
	
	/**
	 * Fluid name entry<br>
	 * Use {@link Builders#fluid} instead to use Fluid objects as value.
	 */
	public static FluidNameEntry.Builder fluidName(Fluid value) {
		return fluidName(value.getRegistryName());
	}
	
	// List entries
	
	/**
	 * String list
	 */
	public static StringListEntry.Builder stringList(java.util.List<String> value) {
		return new StringListEntry.Builder(value);
	}
	
	/**
	 * Byte list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 * @deprecated Use bound Integer lists
	 */
	@SuppressWarnings("DeprecatedIsStillUsed") @Deprecated
	public static ByteListEntry.Builder byteList(java.util.List<Byte> value) {
		return new ByteListEntry.Builder(value);
	}
	
	/**
	 * Short list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 * @deprecated Use bound Integer lists
	 */
	@SuppressWarnings("DeprecatedIsStillUsed") @Deprecated
	public static ShortListEntry.Builder shortList(java.util.List<Short> value) {
		return new ShortListEntry.Builder(value);
	}
	
	/**
	 * Integer list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 */
	public static IntegerListEntry.Builder intList(java.util.List<Integer> value) {
		return new IntegerListEntry.Builder(value);
	}
	/**
	 * Long list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 */
	public static LongListEntry.Builder longList(java.util.List<Long> value) {
		return new LongListEntry.Builder(value);
	}
	/**
	 * Float list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 */
	public static FloatListEntry.Builder floatList(java.util.List<Float> value) {
		return new FloatListEntry.Builder(value);
	}
	/**
	 * Double list with elements between {@code min} and {@code max} (inclusive)<br>
	 * Null bounds are unbound
	 */
	public static DoubleListEntry.Builder doubleList(java.util.List<Double> value) {
		return new DoubleListEntry.Builder(value);
	}
	
	// Caption
	
	/**
	 * Attach an entry as the caption of a list entry<br>
	 * Changes the value to a {@link Pair} of the caption's value and the list's value
	 */
	public static <V, C, G, E extends AbstractListEntry<V, C, G, E>,
	  B extends AbstractListEntry.Builder<V, C, G, E, B>,
	  CV, CC, CG, CE extends AbstractConfigEntry<CV, CC, CG, CE> & IKeyEntry<CC, CG>,
	  CB extends AbstractConfigEntryBuilder<CV, CC, CG, CE, CB>>
	DecoratedListEntry.Builder<V, C, G, E, B, CV, CC, CG, CE, CB> caption(
	  CB caption, B list
	) {
		//noinspection deprecation
		return new DecoratedListEntry.Builder<>(
		  Pair.of(getValue(caption), getValue(list)), list, caption);
	}
	
	/**
	 * Attach an entry as the caption of a map entry<br>
	 * Changes the value to a {@link Pair} of the caption's value and the map's value
	 */
	public static <K, V, KC, C, KG, G, E extends AbstractConfigEntry<V, C, G, E>,
	  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KC, KG>,
	  B extends AbstractConfigEntryBuilder<V, C, G, E, B>,
	  KB extends AbstractConfigEntryBuilder<K, KC, KG, KE, KB>,
	  MB extends EntryMapEntry.Builder<K, V, KC, C, KG, G, E, B, KE, KB>,
	  CV, CC, CG, CE extends AbstractConfigEntry<CV, CC, CG, CE> & IKeyEntry<CC, CG>,
	  CB extends AbstractConfigEntryBuilder<CV, CC, CG, CE, CB>>
	DecoratedMapEntry.Builder<K, V, KC, C, KG, G, E, B, KE, KB, MB, CV, CC, CG, CE, CB>
	caption(
	  CB caption, MB map
	) {
		//noinspection deprecation
		return new DecoratedMapEntry.Builder<>(
		  Pair.of(getValue(caption), getValue(map)), map, caption);
	}
	
	// List entry
	
	/**
	 * List of other entries. Defaults to an empty list<br>
	 * The nested entry may be other list entry, or even a map entry.<br>
	 * Non-persistent entries cannot be nested
	 */
	public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  Builder extends AbstractConfigEntryBuilder<V, C, G, E, Builder>>
	EntryListEntry.Builder<V, C, G, E, Builder> list(Builder entry) {
		return list(entry, Collections.emptyList());
	}
	
	/**
	 * List of other entries<br>
	 * The nested entry may be other list entry, or even a map entry.<br>
	 * Non-persistent entries cannot be nested
	 */
	public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  Builder extends AbstractConfigEntryBuilder<V, C, G, E, Builder>>
	EntryListEntry.Builder<V, C, G, E, Builder> list(Builder entry, List<V> value) {
		return new EntryListEntry.Builder<>(value, entry);
	}
	
	/**
	 * List of other entries<br>
	 * The nested entry may be other list entry, or even a map entry.<br>
	 * Non-persistent entries cannot be nested
	 */
	@SuppressWarnings("unchecked") public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  Builder extends AbstractConfigEntryBuilder<V, C, G, E, Builder>>
	EntryListEntry.Builder<V, C, G, E, Builder> list(Builder entry, V... values) {
		return list(entry, Arrays.stream(values).collect(Collectors.toList()));
	}
	
	// Map entry
	
	/**
	 * Map of other entries<br>
	 * The keys themselves are other entries, as long as they can
	 * serialize as strings. Non string-serializable key entries
	 * won't compile<br>
	 * By default the key is a string entry and the value is an empty map
	 *
	 * @param keyEntry The entry to be used as key
	 * @param entry    The entry to be used as value, which may be another map
	 *                 entry, or a list entry. Not persistent entries cannot be used
	 */
	public static <K, V, KC, C, KG, G, E extends AbstractConfigEntry<V, C, G, E>,
	  Builder extends AbstractConfigEntryBuilder<V, C, G, E, Builder>,
	  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KC, KG>,
	  KeyBuilder extends AbstractConfigEntryBuilder<K, KC, KG, KE, KeyBuilder>>
	EntryMapEntry.Builder<K, V, KC, C, KG, G, E, Builder, KE, KeyBuilder> map(
	  KeyBuilder keyEntry, Builder entry
	) { return map(keyEntry, entry, new LinkedHashMap<>()); }
	
	/**
	 * Map of other entries<br>
	 * The keys themselves are other entries, as long as they can
	 * serialize as strings. Non string-serializable key entries
	 * won't compile<br>
	 * By default the key is a string entry and the value is an empty map
	 *
	 * @param keyEntry The entry to be used as key
	 * @param entry    The entry to be used as value, which may be another map
	 *                 entry, or a list entry. Not persistent entries cannot be used
	 * @param value    Entry value
	 */
	public static <K, V, KC, C, KG, G, E extends AbstractConfigEntry<V, C, G, E>,
	  Builder extends AbstractConfigEntryBuilder<V, C, G, E, Builder>,
	  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KC, KG>,
	  KeyBuilder extends AbstractConfigEntryBuilder<K, KC, KG, KE, KeyBuilder>>
	EntryMapEntry.Builder<K, V, KC, C, KG, G, E, Builder, KE, KeyBuilder> map(
	  KeyBuilder keyEntry, Builder entry, Map<K, V> value
	) { return new EntryMapEntry.Builder<>(value, keyEntry, entry); }
	
	/**
	 * Map of other entries<br>
	 * The keys themselves are other entries, as long as they can
	 * serialize as strings. Non string-serializable key entries
	 * won't compile<br>
	 * By default the key is a string entry and the value is an empty map
	 * @param entry The entry to be used as value, which may be other
	 *              map entry, or a list entry. Not persistent entries cannot be used
	 */
	public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  Builder extends AbstractConfigEntryBuilder<V, C, G, E, Builder>>
	EntryMapEntry.Builder<String, V, String, C, String, G, E, Builder, StringEntry, StringEntry.Builder> map(
	  Builder entry
	) { return map(entry, new LinkedHashMap<>()); }
	
	/**
	 * Map of other entries<br>
	 * The keys themselves are other entries, as long as they can
	 * serialize as strings. Non string-serializable key entries
	 * won't compile<br>
	 * By default the key is a string entry and the value is an empty map
	 * @param entry The entry to be used as value, which may be other
	 *              map entry, or a list entry. Not persistent entries cannot be used
	 * @param value Entry value (default: empty map)
	 */
	public static <V, C, G, E extends AbstractConfigEntry<V, C, G, E>,
	  Builder extends AbstractConfigEntryBuilder<V, C, G, E, Builder>>
	EntryMapEntry.Builder<String, V, String, C, String, G, E, Builder, StringEntry, StringEntry.Builder> map(
	  Builder entry, Map<String, V> value
	) { return new EntryMapEntry.Builder<>(value, string(""), entry); }
	
	// Pair list
	
	/**
	 * List of pairs of other entries, like a linked map, but with duplicates<br>
	 * The keys themselves are other entries, as long as they can
	 * serialize as strings. Non string-serializable key entries
	 * won't compile<br>
	 * By default the value is an empty list
	 *
	 * @param keyEntry The entry to be used as key
	 * @param entry    The entry to be used as value, which may be another map
	 *                 entry, or a list entry. Not persistent entries cannot be used
	 */
	public static <K, V, KC, C, KG, G, E extends AbstractConfigEntry<V, C, G, E>,
	  Builder extends AbstractConfigEntryBuilder<V, C, G, E, Builder>,
	  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KC, KG>,
	  KeyBuilder extends AbstractConfigEntryBuilder<K, KC, KG, KE, KeyBuilder>>
	EntryPairListEntry.Builder<K, V, KC, C, KG, G, E, Builder, KE, KeyBuilder> pairList(
	  KeyBuilder keyEntry, Builder entry
	) { return pairList(keyEntry, entry, new ArrayList<>()); }
	
	/**
	 * List of pairs of other entries, like a linked map, but with duplicates<br>
	 * The keys themselves are other entries, as long as they can
	 * serialize as strings. Non string-serializable key entries
	 * won't compile<br>
	 * By default the value is an empty list
	 *
	 * @param keyEntry The entry to be used as key
	 * @param entry    The entry to be used as value, which may be another map
	 *                 entry, or a list entry. Not persistent entries cannot be used
	 * @param value    Entry value
	 */
	public static <K, V, KC, C, KG, G, E extends AbstractConfigEntry<V, C, G, E>,
	  Builder extends AbstractConfigEntryBuilder<V, C, G, E, Builder>,
	  KE extends AbstractConfigEntry<K, KC, KG, KE> & IKeyEntry<KC, KG>,
	  KeyBuilder extends AbstractConfigEntryBuilder<K, KC, KG, KE, KeyBuilder>>
	EntryPairListEntry.Builder<K, V, KC, C, KG, G, E, Builder, KE, KeyBuilder> pairList(
	  KeyBuilder keyEntry, Builder entry, List<Pair<K, V>> value
	) { return new EntryPairListEntry.Builder<>(value, keyEntry, entry); }
	
	// Register reflection field parsers ------------------------------------------------------------
	
	static {
		registerFieldParser(Entry.class, Boolean.class, (a, field, value) ->
		  bool(value != null ? value : false));
		registerFieldParser(Entry.class, String.class, (a, field, value) ->
		  string(value != null ? value : ""));
		registerFieldParser(Entry.class, Enum.class, (a, field, value) -> {
			if (value == null)
				//noinspection rawtypes
				value = (Enum) field.getType().getEnumConstants()[0];
			return enum_(value);
		});
		registerFieldParser(Entry.class, Byte.class, (a, field, value) ->
		  number(value, getMin(field).byteValue(), getMax(field).byteValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Short.class, (a, field, value) ->
		  number(value, getMin(field).shortValue(), getMax(field).shortValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Integer.class, (a, field, value) ->
		  number(value, getMin(field).intValue(), getMax(field).intValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Long.class, (a, field, value) ->
		  number(value, getMin(field).longValue(), getMax(field).longValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Float.class, (a, field, value) ->
		  number(value, getMin(field).floatValue(), getMax(field).floatValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Double.class, (a, field, value) ->
		  number(value, getMin(field).doubleValue(), getMax(field).doubleValue())
		    .slider(field.isAnnotationPresent(Slider.class)));
		registerFieldParser(Entry.class, Color.class, (a, field, value) ->
		  color(value).alpha(field.isAnnotationPresent(HasAlpha.class)));
		
		// Lists
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, String.class) ? null :
		  decorateListEntry(stringList((List<String>) value), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Integer.class) ? null :
		  decorateListEntry(
		    intList((List<Integer>) value)
		      .min(getMin(field).intValue()).max(getMax(field).intValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Long.class) ? null :
		  decorateListEntry(
		    longList((List<Long>) value)
		      .min(getMin(field).longValue()).max(getMax(field).longValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Float.class) ? null :
		  decorateListEntry(
		    floatList((List<Float>) value)
		      .min(getMin(field).floatValue()).max(getMax(field).floatValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Double.class) ? null :
		  decorateListEntry(
		    doubleList((List<Double>) value)
		      .min(getMin(field).doubleValue()).max(getMax(field).doubleValue()), field));
		
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Short.class) ? null :
		  decorateListEntry(
		    shortList((List<Short>) value)
		      .min(getMin(field).shortValue()).max(getMax(field).shortValue()), field));
		//noinspection unchecked
		registerFieldParser(Entry.class, List.class, (a, field, value) ->
		  !checkType(field, List.class, Byte.class) ? null :
		  decorateListEntry(
		    byteList((List<Byte>) value)
		      .min(getMin(field).byteValue()).max(getMax(field).byteValue()), field));
		
		// Minecraft entry types
		registerFieldParser(Entry.class, Item.class, (a, field, value) -> item(value));
		registerFieldParser(Entry.class, INBT.class, (a, field, value) -> nbtValue(value));
		registerFieldParser(Entry.class, CompoundNBT.class, (a, field, value) -> nbtTag(value));
		registerFieldParser(Entry.class, ResourceLocation.class, (a, field, value) -> resource(value));
	}
}
