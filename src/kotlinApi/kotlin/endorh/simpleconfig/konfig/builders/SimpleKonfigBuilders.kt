package endorh.simpleconfig.konfig.builders

import endorh.simpleconfig.api.*
import endorh.simpleconfig.api.ConfigBuilderFactory.PresetBuilder
import endorh.simpleconfig.api.entry.*
import endorh.simpleconfig.api.range.AbstractRange.*
import endorh.simpleconfig.api.range.DoubleRange
import endorh.simpleconfig.api.range.FloatRange
import endorh.simpleconfig.api.range.IntRange
import endorh.simpleconfig.api.range.LongRange
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBind
import endorh.simpleconfig.api.ui.hotkey.ExtendedKeyBindProvider
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping
import endorh.simpleconfig.konfig.commons.toCommonsPair
import endorh.simpleconfig.konfig.commons.toCommonsTriple
import endorh.simpleconfig.konfig.entry.DataClassEntryBuilder
import net.minecraft.client.KeyMapping
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.material.Fluid
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import org.apache.commons.lang3.tuple.Pair
import org.intellij.lang.annotations.Language
import java.awt.Color
import java.util.*
import java.util.regex.Pattern
import endorh.simpleconfig.api.ConfigBuilderFactoryProxy as P
import endorh.simpleconfig.konfig.entry.KotlinEntryBuilders as K
import kotlin.ranges.IntRange as KIntRange
import kotlin.ranges.LongRange as KLongRange

private typealias KDoubleRange = ClosedFloatingPointRange<Double>
private typealias KFloatRange = ClosedFloatingPointRange<Float>

// Basic types

/**
 * Boolean entry
 *
 * You may change the text that appears in the button using
 * [BooleanEntryBuilder.text]
 */
fun bool(value: Boolean = false) = P.bool(value)

/**
 * Boolean entry
 *
 * Uses the labels "Yes" and "No" instead of the usual "true" and "false"
 *
 * You may also provide your own labels using [BooleanEntryBuilder.text]
 */
fun yesNo(value: Boolean = false) = P.yesNo(value)

/**
 * Boolean entry
 *
 * Uses the labels "Enabled" and "Disabled" instead of the usual "true" and "false"
 *
 * You may also provide your own labels using [BooleanEntryBuilder.text]
 */
fun enable(value: Boolean = false) = P.enable(value)

/**
 * Boolean entry
 *
 * Uses the labels "ON" and "OFF" instead of the usual "true" and "false"
 *
 * You may also provide your own labels using [BooleanEntryBuilder.text]
 */
fun onOff(value: Boolean = false) = P.onOff(value)

/**
 * String entry
 */
fun string(value: String = "") = P.string(value)

/**
 * Enum entry
 */
fun <E : Enum<E>> enum(value: E) = option(value)

/**
 * Enum entry
 */
fun <E : Enum<E>> option(value: E) = P.option(value)

/**
 * Option entry, like an enum but for arbitrary types.
 *
 * Supports variable allowed options, mainly intended to support
 * environment dependent options (that don't change for a given
 * installation).
 *
 * The set of allowed values is only checked upon modifications,
 * so it's not reliable to modify it during a game session.
 * @see [option]
 */
fun <T> option(value: T, options: () -> List<T>) = P.option(value, options)

/**
 * Option entry, like an enum but for arbitrary types.
 *
 * Can also support variable allowed options.
 * @see [option]
 */
fun <T> option(value: T, options: List<T>?) = P.option(value, options)

/**
 * Option entry, like an enum but for arbitrary types.
 *
 * Can also support variable allowed options.
 * @see [option]
 */
fun <T> option(value: T, vararg options: T) = P.option(value, *options)

/**
 * Enum entry, defaults to the first value in the enum.
 */
inline fun <reified E: Enum<E>> enum() = option(enumValues<E>()[0])

/**
 * Enum entry, defaults to the first value in the enum.
 */
inline fun <reified E: Enum<E>> option() = option(enumValues<E>()[0])

// Buttons

/**
 * Button entry
 *
 * Displays a button in the GUI that can trigger an arbitrary action.
 *
 * The action may receive the immediate parent of the entry as parameter.
 */
fun button(action: () -> Unit) = P.button(action)

/**
 * Button entry
 *
 * Displays a button in the GUI that can trigger an arbitrary action.
 *
 * The action may receive the immediate parent of the entry as parameter.
 */
fun button(action: ConfigEntryHolder.() -> Unit) = P.button(action)

/**
 * Add a button to another entry.
 *
 * Not persistent. Useful for GUI screen interaction.
 */
fun <V, Gui, B> button(
  inner: B, action: ConfigEntryHolder.(V) -> Unit
) where B : ConfigEntryBuilder<V, *, Gui, B>, B : AtomicEntryBuilder =
  P.button(inner) { v, holder -> holder.action(v) }

// Presets

/**
 * An entry that lets users apply different presets to the entries, using global paths.
 *
 * Create presets using [SimpleKonfigBuilders.presets] and
 * [preset]
 * or just create a map your way.
 */
fun globalPresetSwitcher(
  presets: Map<String, Map<String, Any>>, path: String
) = P.globalPresetSwitcher(presets, path)

/**
 * An entry that lets users apply different presets to the entries,
 * using local paths from the parent of this entry.
 *
 * Create presets using [SimpleKonfigBuilders.presets] and
 * [preset]
 * or just create a map your way.
 */
fun localPresetSwitcher(
  presets: Map<String, Map<String, Any>>, path: String
) = P.localPresetSwitcher(presets, path)

/**
 * Create a preset map from a collection of preset builders
 */
fun presets(vararg presets: PresetBuilder) = P.presets(*presets)

/**
 * Preset map builder
 */
fun preset(name: String) = P.preset(name)

// Numbers

/**
 * Unbound byte value.
 *
 * You should probably be using bound [int] entries instead.
 */
fun byte(value: Byte = 0) = number(value)

/**
 * Unbound byte value
 */
@Deprecated("Use a bound int entry", ReplaceWith("number(value.toInt())"))
fun number(value: Byte) = P.number(value)

/**
 * Non-negative byte between 0 and `max` (inclusive)
 */
@Deprecated("Use a bound int entry", ReplaceWith("number(value.toInt(), max)"))
fun number(value: Byte, max: Byte) = P.number(value, max)

/**
 * Byte value between `min` and `max` (inclusive)
 */
@Deprecated("Use a bound int entry", ReplaceWith("number(value.toInt(), min, max)"))
fun number(value: Byte, min: Byte, max: Byte) = P.number(value, min, max)

/**
 * Unbound short value
 *
 * You should probably be using bound [int] entries instead.
 */
fun short(value: Short = 0) = number(value)

/**
 * Unbound short value
 */
@Deprecated("Use a bound int entry", ReplaceWith("number(value.toInt())"))
fun number(value: Short) = P.number(value)

/**
 * Non-negative short between 0 and `max` (inclusive)
 */
@Deprecated("Use a bound int entry", ReplaceWith("number(value.toInt(), max)"))
fun number(value: Short, max: Short) = P.number(value, max)

/**
 * Short value between `min` and `max` (inclusive)
 */
@Deprecated("Use a bound int entry", ReplaceWith("number(value.toInt(), min, max)"))
fun number(value: Short, min: Short, max: Short) = P.number(value, min, max)

/**
 * Unbound integer value
 */
fun int(value: Int = 0) = number(value)

/**
 * Unbound integer value
 */
fun number(value: Int = 0) = P.number(value)

/**
 * Non-negative integer between 0 and `max` (inclusive)
 */
fun number(value: Int, max: Int) = P.number(value, max)

/**
 * Integer value between `min` and `max` (inclusive)
 */
fun number(value: Int, min: Int, max: Int) = P.number(value, min, max)

/**
 * Integer percentage, between 0 and 100 (inclusive)
 *
 * Displayed as a slider, equivalent to
 * `int(value, 0, 100).slider("simpleconfig.format.slider.percentage")`
 */
fun percent(value: Int = 0) = P.percent(value)

/**
 * Unbound long value
 */
fun long(value: Long = 0) = number(value)

/**
 * Unbound long value
 */
fun number(value: Long) = P.number(value)

/**
 * Non-negative long between 0 and `max` (inclusive)
 */
fun number(value: Long, max: Long) = P.number(value, max)

/**
 * Long value between `min` and `max` (inclusive)
 */
fun number(value: Long, min: Long, max: Long) = P.number(value, min, max)

/**
 * Unbound float value
 */
fun float(value: Float = 0f) = number(value)

/**
 * Unbound float value
 */
fun number(value: Float) = P.number(value)

/**
 * Non-negative float value between 0 and `max` (inclusive)
 */
fun number(value: Float, max: Float) = P.number(value, max)

/**
 * Float value between `min` and `max` inclusive
 */
fun number(value: Float, min: Float, max: Float) = P.number(value, min, max)

/**
 * Float percentage, between 0 and 100, but stored as a fraction
 * between 0.0 and 1.0 in the backing field (not the config file).
 *
 * Displayed as a slider, equivalent to
 * `float(value, 0F, 1F).slider("simpleconfig.format.slider.percentage")`
 */
fun percent(value: Float) = P.percent(value)

/**
 * Unbound double value
 */
fun double(value: Double = 0.0) = number(value)

/**
 * Unbound double value
 */
fun number(value: Double) = P.number(value)

/**
 * Non-negative double value between 0 and `max` (inclusive)
 */
fun number(value: Double, max: Double) = P.number(value, max)

/**
 * Double value between `min` and `max` inclusive
 */
fun number(value: Double, min: Double, max: Double) = P.number(value, min, max)

/**
 * Double percentage, between 0 and 100, but stored as a fraction
 * between 0.0 and 1.0 in the backing field (not the config file).
 *
 * Displayed as a slider, equivalent to
 * `double(value, 0.0, 1.0).slider("simpleconfig.format.slider.percentage")`
 */
fun percent(value: Double) = P.percent(value)

/**
 * Float value between 0 and 1 (inclusive)
 *
 * Equivalent to `float(value).range(0F, 1F).slider()`
 */
fun fraction(value: Float) = P.fraction(value)

/**
 * Double value between 0 and 1 (inclusive)
 *
 * Equivalent to `double(value).range(0.0, 1.0).slider()`
 */
fun fraction(value: Double = 0.0) = P.fraction(value)

/**
 * Float entry between 0 and 1 (inclusive)
 *
 * Displays a volume label in the slider instead of the usual "Value: %s"
 *
 * Equivalent to `fraction(value).slider("simpleconfig.format.slider.volume")`
 */
fun volume(value: Float) = P.volume(value)

/**
 * Double entry between 0 and 1 (inclusive)
 *
 * Displays a volume label in the slider instead of the usual "Value: %s"
 *
 * Equivalent to `fraction(value).slider("simpleconfig.format.slider.volume")`
 */
fun volume(value: Double = 1.0) = P.volume(value)

// Ranges

/**
 * Double range, which defines a min and max values, optionally exclusive.
 *
 * You may allow users to change the exclusiveness of the bounds.
 * @see DoubleRange
 */
fun range(range: KDoubleRange) = P.range(
    DoubleRange.inclusive(range.start, range.endInclusive))

/**
 * Double range entry, which defines a min and max values, optionally exclusive.
 *
 * You may allow users to change the exclusiveness of the bounds.
 *
 * @see DoubleRange
 */
fun range(range: DoubleRange = DoubleRange.UNIT) = P.range(range)

/**
 * Double range entry, which defines a min and max values, inclusive by default.
 *
 * You may allow users to change the exclusiveness of the bounds.
 *
 * @see DoubleRange
 */
fun range(min: Double, max: Double) = P.range(min, max)

/**
 * Float range, which defines a min and max values, optionally exclusive.
 *
 * You may allow users to change the exclusiveness of the bounds.
 * @see FloatRange
 */
fun range(range: KFloatRange) = P.range(
    FloatRange.inclusive(range.start, range.endInclusive))

/**
 * Float range entry, which defines a min and max values, optionally exclusive.
 *
 * You may allow users to change the exclusiveness of the bounds.
 *
 * @see FloatRange
 */
fun range(range: FloatRange) = P.range(range)

/**
 * Float range entry, which defines a min and max values, inclusive by default.
 *
 * You may allow users to change the exclusiveness of the bounds.
 *
 * @see FloatRange
 */
fun range(min: Float, max: Float) = P.range(min, max)

/**
 * Long range, which defines a min and max values, optionally exclusive.
 *
 * You may allow users to change the exclusiveness of the bounds.
 * @see LongRange
 */
fun range(range: KLongRange) = P.range(
    LongRange.inclusive(range.first, range.last))

/**
 * Long range entry, which defines a min and max values, optionally exclusive.
 *
 * You may allow users to change the exclusiveness of the bounds.
 *
 * @see LongRange
 */
fun range(range: LongRange) = P.range(range)

/**
 * Long range entry, which defines a min and max values, inclusive by default.
 *
 * You may allow users to change the exclusiveness of the bounds.
 *
 * @see LongRange
 */
fun range(min: Long, max: Long) = P.range(min, max)

/**
 * Integer range, which defines a min and max values, optionally exclusive.
 *
 * You may allow users to change the exclusiveness of the bounds.
 * @see IntRange
 */
fun range(range: KIntRange) = P.range(
    IntRange.inclusive(range.first, range.last))

/**
 * Integer range entry, which defines a min and max values, optionally exclusive.
 *
 * You may allow users to change the exclusiveness of the bounds.
 *
 * @see IntRange
 */
fun range(range: IntRange) = P.range(range)

/**
 * Integer range entry, which defines a min and max values, inclusive by default.
 *
 * You may allow users to change the exclusiveness of the bounds.
 *
 * @see IntRange
 */
fun range(min: Int, max: Int) = P.range(min, max)

// Serializable types

/**
 * Color entry
 *
 * Use [ColorEntryBuilder.alpha] to allow alpha values
 */
fun color(value: Color = Color.DARK_GRAY) = P.color(value)

/**
 * Color entry
 *
 * Use [ColorEntryBuilder.alpha] to allow alpha values
 */
fun color(value: Long) = color(Color(value.toInt(), true)).also {
    if (value > Int.MAX_VALUE || value < Int.MIN_VALUE) throw IllegalArgumentException(
        "Invalid color value: $value, must be an hexadecimal number between 0x00000000 and 0xFFFFFFFF")
}

/**
 * Regex entry
 *
 * Will use the flags of the passed regex to compile user input
 */
fun regex(regex: Regex = Regex("")) = K.regex(regex)

/**
 * Regex entry
 *
 * The passed flags will be used to compile user input
 */
fun regex(@Language("RegExp") regex: String, vararg options: RegexOption) = K.regex(regex, *options)

/**
 * Java Regex pattern entry
 *
 * Will use the pattern's flags to compile user input
 */
@Deprecated("Use regex(Regex) instead", ReplaceWith("regex(pattern)"))
fun pattern(pattern: Pattern = Pattern.compile("")) = P.pattern(pattern)

/**
 * Java Regex pattern entry
 *
 * The passed flags will be used to compile user input
 */
@Deprecated("Use Kotlin Regex entries instead", ReplaceWith("regex(pattern)"))
fun pattern(@Language("RegExp") pattern: String, flags: Int = 0) = P.pattern(pattern, flags)

/**
 * Entry of a String serializable object
 */
fun <V: Any> entry(
  value: V, serializer: (V) -> String, deserializer: (String) -> V?
) = P.entry(value, serializer) { Optional.ofNullable(deserializer(it)) }

/**
 * Entry of a String serializable object
 */
fun <V: Any> entry(value: V, serializer: ConfigEntrySerializer<V>) =
  P.entry(value, serializer)

/**
 * Entry of a String serializable object
 */
fun <V : ISerializableConfigEntry<V>> entry(value: V) = P.entry(value)

/**
 * Data class entry
 *
 * You can bind each property to an entry inside the [configure] block by using
 * [DataClassEntryBuilder.bind] and mapping property references to entry builders
 * using [DataClassEntryBuilder.by] or [DataClassEntryBuilder.caption]
 *
 * The [configure] block should only contain a call to [DataClassEntryBuilder.bind],
 * which should contain the property bindings.
 * Once [multiple context receivers](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md)
 * become stable, the call to [DataClassEntryBuilder.bind] will be unnecessary and deprecated.
 *
 * Example usage:
 * ```
 * object ClientKonfig(Type.CLIENT) {
 *     // ...
 *     data class Data(val name: String, val age: Int)
 *     val data = data(Data("Steve", 20)) { bind {
 *         ::name caption string()
 *         ::age by number()
 *     }}
 * }
 * ```
 * The default values from the entry builders bound to the properties are ignored,
 * as the passed data class instance is used as the default value. As such, you should
 * avoid passing them where possible.
 *
 * This API ensures type safety of the property bindings, and doesn't use string literals
 * to map property names
 *
 * As Java Bean entries, these entries are most useful within list or map entries, rather than
 * as singleton entries, since a config group could serve the same purpose.
 */
fun <B: Any> data(value: B, configure: DataClassEntryBuilder<B>.() -> Unit) = K.data(value, configure)

/**
 * Java Bean entry. **Does NOT support Kotlin data classes**, instead use [data].
 *
 * Use the builder's methods to define entries to edit the bean's properties.
 *
 * Most useful for bean lists or maps, rather than as singleton entries.
 * @param <B> Class of the bean. Must conform to the `JavaBean` specification.
</B> */
@Deprecated("Use data() with data classes instead", ReplaceWith("data(value) { bindProperties { \n// ...\n }}"))
fun <B> bean(value: B) = P.bean(value)

// Minecraft specific types

/**
 * NBT entry that accepts any kind of NBT, either values or compounds
 */
fun tag(value: Tag) = P.tag(value)

/**
 * NBT entry that accepts NBT compounds
 */
fun compoundTag(value: CompoundTag = CompoundTag()) = P.compoundTag(value)

/**
 * Generic resource location entry
 */
fun resource(resourceName: String = "") = P.resource(resourceName)

/**
 * Generic resource location entry
 */
fun resource(value: ResourceLocation = ResourceLocation("")) = P.resource(value)

/**
 * Key binding entry. Supports advanced key combinations, and other advanced
 * settings such as exclusivity, order sensitivity, activation on release/repeat/toggle.
 *
 * Register extended keybinds by registering an [ExtendedKeyBindProvider] for them
 * using [ExtendedKeyBindProvider.registerProvider]
 *
 * **Consider registering regular [KeyMapping]s through [RegisterKeyMappingsEvent]**
 */
@OnlyIn(Dist.CLIENT)
fun key(keyBind: ExtendedKeyBind) = P.key(keyBind)

/**
 * Key binding entry. Supports advanced key combinations, and other advanced
 * settings such as exclusivity, order sensitivity, activation on release/repeat/toggle.
 *
 * **If you're using this entry as a static keybind for your mod, prefer using
 * [key], as it'll provide better overlap detection.**
 *
 * Register extended keybinds by registering an [ExtendedKeyBindProvider] for them
 * using [ExtendedKeyBindProvider.registerProvider]
 *
 * **Consider registering regular [KeyMapping]s through [RegisterKeyMappingsEvent]**
 */
@OnlyIn(Dist.CLIENT)
fun key(key: KeyBindMapping = KeyBindMapping.unset()) = P.key(key)

/**
 * Key binding entry. Supports advanced key combinations, and other advanced
 * settings such as exclusivity, order sensitivity, activation on release/repeat/toggle.
 *
 * **If you're using this entry as a static keybind for your mod, prefer using
 * [key], as it'll provide better overlap detection.**
 *
 * Register extended keybinds by registering an [ExtendedKeyBindProvider] for them
 * using [ExtendedKeyBindProvider.registerProvider]
 *
 * **Consider registering regular [KeyMapping]s through [RegisterKeyMappingsEvent]**
 */
@OnlyIn(Dist.CLIENT)
fun key(key: String) = P.key(key)

/**
 * Key binding entry. Supports advanced key combinations, and other advanced
 * settings such as exclusivity, order sensitivity, activation on release/repeat/toggle.
 *
 * **If you're using this entry as a static keybind for your mod, prefer using
 * [key], as it'll provide better overlap detection.**
 *
 * Register extended keybinds by registering an [ExtendedKeyBindProvider] for them
 * using [ExtendedKeyBindProvider.registerProvider]
 *
 * **Consider registering regular [KeyMapping]s through [RegisterKeyMappingsEvent]**
 */
@OnlyIn(Dist.CLIENT)
fun key() = P.key()

/**
 * Item entry
 *
 * Use [itemName] instead to use ResourceLocations as value,
 * to allow unknown items.
 */
fun item(value: Item) = P.item(value)

/**
 * Item name entry
 *
 * Use [item] instead to use Item objects as value.
 */
fun itemName(value: ResourceLocation = ResourceLocation("")) = P.itemName(value)

/**
 * Item name entry
 *
 * Use [item] instead to use Item objects as value.
 */
fun itemName(value: Item) = P.itemName(value)

/**
 * Block entry
 *
 * Use [blockName] instead to use ResourceLocations as value,
 * to allow unknown blocks.
 */
fun block(value: Block) = P.block(value)

/**
 * Block name entry
 *
 * Use [block] instead to use Block objects as value.
 */
fun blockName(value: ResourceLocation = ResourceLocation("")) = P.blockName(value)

/**
 * Block name entry
 *
 * Use [block] instead to use Block objects as value.
 */
fun blockName(value: Block) = P.blockName(value)

/**
 * Fluid entry
 *
 * Use [fluidName] instead to use ResourceLocations as value,
 * to allow unknown fluids.
 */
fun fluid(value: Fluid) = P.fluid(value)

/**
 * Fluid name entry
 *
 * Use [fluid] instead to use Fluid objects as value.
 */
fun fluidName(value: ResourceLocation = ResourceLocation("")) = P.fluidName(value)

/**
 * Fluid name entry
 *
 * Use [fluid] instead to use Fluid objects as value.
 */
fun fluidName(value: Fluid) = P.fluidName(value)

// Specific lists (for `String`s and `Number` types)

/**
 * String list
 * 
 * You should use instead general [list]s with a [string] entry,
 * which provides better control over allowed values.
 */
fun stringList(value: List<String> = emptyList()) = P.stringList(value)

/**
 * Byte list
 * 
 * You should use instead general [list]s with a byte/int [number] entry,
 * which provides better control over allowed values.
 */
@Deprecated("Use bound int lists", ReplaceWith("intList(value.map { it.toInt() }).min(0).max(256)"))
fun byteList(value: List<Byte> = emptyList()) = P.byteList(value)

/**
 * Short list
 * 
 * You should use instead general [list]s with a short/int [number] entry,
 * which provides better control over allowed values.
 */
@Deprecated("Use bound int lists", ReplaceWith("intList(value.map { it.toInt() }).min(0).max(65536)"))
fun shortList(value: List<Short> = emptyList()) = P.shortList(value)

/**
 * Integer list
 * 
 * You should use instead general [list]s with an int [number] entry,
 * which provides better control over allowed values.
 */
fun intList(value: List<Int> = emptyList()) = P.intList(value)

/**
 * Long list
 * 
 * You should use instead general [list]s with a long [number] entry,
 * which provides better control over allowed values.
 */
fun longList(value: List<Long> = emptyList()) = P.longList(value)

/**
 * Float list
 * 
 * You should use instead general [list]s with a float [number] entry,
 * which provides better control over allowed values.
 */
fun floatList(value: List<Float> = emptyList()) = P.floatList(value)

/**
 * Double list
 * 
 * You should use instead general [list]s with a double [number] entry,
 * which provides better control over allowed values.
 */
fun doubleList(value: List<Double> = emptyList()) = P.doubleList(value)

// General lists

/**
 * List of other entries
 *
 * The nested entry may be other list entry, or even a map entry.
 *
 * Non-persistent entries cannot be nested
 */
fun <V, C, G, Builder : ConfigEntryBuilder<V, C, G, Builder>> list(
  entry: Builder, value: List<V> = emptyList()
) = P.list(entry, value)

/**
 * List of other entries
 *
 * The nested entry may be other list entry, or even a map entry.
 *
 * Non-persistent entries cannot be nested
 */
fun <V, C, G, Builder : ConfigEntryBuilder<V, C, G, Builder>> list(
  entry: Builder, vararg values: V
) = P.list(entry, *values)

// General sets

/**
 * Set of other entries
 *
 * The nested entry may be other list/set entry, or even a map entry.
 *
 * Non-persistent entries cannot be nested
 */
fun <V, C, G, Builder : ConfigEntryBuilder<V, C, G, Builder>> set(
  entry: Builder, value: Set<V> = emptySet()
) = P.set(entry, value)

/**
 * Set of other entries
 *
 * The nested entry may be other list/set entry, or even a map entry.
 *
 * Non-persistent entries cannot be nested
 */
fun <V, C, G, Builder : ConfigEntryBuilder<V, C, G, Builder>> set(
  entry: Builder, vararg values: V
) = P.set(entry, *values)

// General maps

/**
 * Map of other entries
 *
 * The keys can be any arbitrary non-collection entry (implementing [AtomicEntryBuilder]).
 * This only excludes using maps, lists and beans/data classes as map keys.
 *
 * Any entry can be used as values, just like in lists.
 *
 * By default, the key is a string entry and the value is an empty map
 *
 * @param keyEntry The entry to be used as key
 * @param entry The entry to be used as value, which may be another map
 * entry, or a list entry. Not persistent entries cannot be used
 * @param value Entry value
 */
fun <K, V, KC, C, KG, G, B : ConfigEntryBuilder<V, C, G, B>, KB> map(
  keyEntry: KB, entry: B, value: Map<K, V> = emptyMap()
) where KB : ConfigEntryBuilder<K, KC, KG, KB>, KB : AtomicEntryBuilder =
  P.map(keyEntry, entry, value)

/**
 * Map of other entries
 *
 * The keys can be any arbitrary non-collection entry (implementing [AtomicEntryBuilder]).
 * This only excludes using maps, lists and beans/data classes as map keys.
 *
 * Any entry can be used as values, just like in lists.
 *
 * By default, the key is a string entry and the value is an empty map
 *
 * @param entry The entry to be used as value, which may be other
 * map entry, or a list entry. Not persistent entries cannot be used
 * @param value Entry value (default: empty map)
 */
fun <V, C, G, Builder : ConfigEntryBuilder<V, C, G, Builder>> map(
  entry: Builder, value: Map<String, V> = emptyMap()
) = P.map(entry, value)

// Linked maps

/**
 * Linked [map] of entries. Equivalent to calling `map(entry, value).linked()`.
 *
 * The keys can be any arbitrary non-collection entry (implementing [AtomicEntryBuilder]).
 * This only excludes using maps, lists and beans/data classes as map keys.
 *
 * Any entry can be used as values, just like in lists.
 *
 * By default, the key is a string entry and the value is an empty map
 */
fun <V, C, G, Builder: ConfigEntryBuilder<V, C, G, Builder>> linkedMap(
  entry: Builder, value: Map<String, V> = emptyMap()
) = map(entry, value).linked()

/**
 * Linked [map] of entries. Equivalent to calling `map(keyEntry, entry, value).linked()`.
 *
 * The keys can be any arbitrary non-collection entry (implementing [AtomicEntryBuilder]).
 * This only excludes using maps, lists and beans/data classes as map keys.
 *
 * Any entry can be used as values, just like in lists.
 *
 * By default, the key is a string entry and the value is an empty map
 */
fun <K, V, KC, C, KG, G, B : ConfigEntryBuilder<V, C, G, B>, KB> linkedMap(
  keyEntry: KB, entry: B, value: Map<K, V> = emptyMap()
) where KB : ConfigEntryBuilder<K, KC, KG, KB>, KB : AtomicEntryBuilder =
  map(keyEntry, entry, value).linked()

// Non unique maps (lists of pairs)

/**
 * List of pairs of other entries, like a linked map, but allowing duplicates
 *
 * The keys can be any arbitrary non-collection entry (implementing [AtomicEntryBuilder]).
 * This only excludes using maps, lists and beans/data classes as map keys.
 *
 * Any entry can be used as values, just like in lists.
 *
 * By default, the value is an empty list
 *
 * @param keyEntry The entry to be used as key
 * @param entry The entry to be used as value, which may be another map
 * entry, or a list entry. Not persistent entries cannot be used
 */
fun <K, V, KC, C, KG, G, B : ConfigEntryBuilder<V, C, G, B>, KB> pairList(
  keyEntry: KB, entry: B
) where KB : ConfigEntryBuilder<K, KC, KG, KB>, KB : AtomicEntryBuilder =
  P.pairList(keyEntry, entry)

/**
 * List of pairs of other entries, like a linked map, but allowing duplicates
 *
 * The keys can be any arbitrary non-collection entry (implementing [AtomicEntryBuilder]).
 * This only excludes using maps, lists and beans/data classes as map keys.
 *
 * Any entry can be used as values, just like in lists.
 *
 * By default, the value is an empty list
 *
 * @param keyEntry The entry to be used as key
 * @param entry The entry to be used as value, which may be another map
 * entry, or a list entry. Not persistent entries cannot be used
 * @param value Entry value
 */
fun <K, V, KC, C, KG, G, B : ConfigEntryBuilder<V, C, G, B>, KB> pairList(
  keyEntry: KB, entry: B, value: List<kotlin.Pair<K, V>> = emptyList()
) where KB : ConfigEntryBuilder<K, KC, KG, KB>, KB : AtomicEntryBuilder =
  P.pairList(keyEntry, entry, value.map { it.toCommonsPair() })

// Collection captions

/**
 * Attach an entry as the caption of a list entry<br></br>
 * Changes the value to a [Pair] of the caption's value and the list's value
 */
@Deprecated("Use as method from the list builder", ReplaceWith("list.caption(caption)"))
fun <V, C, G, B : ListEntryBuilder<V, C, G, B>, CV, CC, CG, CB> caption(
  caption: CB, list: B
) where CB : ConfigEntryBuilder<CV, CC, CG, CB>?, CB : AtomicEntryBuilder =
  list.caption(caption)

/**
 * Attach an entry as the caption of a list entry<br></br>
 * Changes the value to a [Pair] of the caption's value and the list's value
 */
fun <V, C, G, B : ListEntryBuilder<V, C, G, B>, CV, CC, CG, CB> B.caption(caption: CB):
  CaptionedCollectionEntryBuilder<List<V>, List<C>, G, B, CV, CC, CG, CB>
where CB : ConfigEntryBuilder<CV, CC, CG, CB>, CB : AtomicEntryBuilder = P.caption(caption, this)

/**
 * Attach an entry as the caption of a set entry<br></br>
 * Changes the value to a [Pair] of the caption's value and the set's value
 */
@Deprecated("Use as method from the set builder", ReplaceWith("set.caption(caption)"))
fun <V, C, G, B : ConfigEntryBuilder<V, C, G, B>, CV, CC, CG, CB> caption(
  caption: CB, set: EntrySetEntryBuilder<V, C, G, B>
) where CB : ConfigEntryBuilder<CV, CC, CG, CB>?, CB : AtomicEntryBuilder = set.caption(caption)

/**
 * Attach an entry as the caption of a set entry<br></br>
 * Changes the value to a [Pair] of the caption's value and the set's value
 */
fun <V, C, G, B : ConfigEntryBuilder<V, C, G, B>, CV, CC, CG, CB>
  EntrySetEntryBuilder<V, C, G, B>.caption(caption: CB):
  CaptionedCollectionEntryBuilder<Set<V>, Set<C>, G, EntrySetEntryBuilder<V, C, G, B>, CV, CC, CG, CB>
where CB : ConfigEntryBuilder<CV, CC, CG, CB>?, CB : AtomicEntryBuilder = P.caption(caption, this)

/**
 * Attach an entry as the caption of a map entry<br></br>
 * Changes the value to a [Pair] of the caption's value and the map's value
 */
@Deprecated("Use as method from the map builder", ReplaceWith("map.caption(caption)"))
fun <K, V, KC, C, KG, G, KB, B : ConfigEntryBuilder<V, C, G, B>, CV, CC, CG, CB> caption(
  caption: CB, map: EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB>
) where KB : ConfigEntryBuilder<K, KC, KG, KB>, KB : AtomicEntryBuilder,
        CB : ConfigEntryBuilder<CV, CC, CG, CB>, CB : AtomicEntryBuilder = map.caption(caption)

/**
 * Attach an entry as the caption of a map entry<br></br>
 * Changes the value to a [Pair] of the caption's value and the map's value
 */
fun <K, V, KC, C, KG, G, KB, B : ConfigEntryBuilder<V, C, G, B>, CV, CC, CG, CB>
  EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB>.caption(caption: CB):
  CaptionedCollectionEntryBuilder<
    Map<K, V>, Map<KC, C>, Pair<KG, G>, EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB>,
    CV, CC, CG, CB
  > where KB : ConfigEntryBuilder<K, KC, KG, KB>, KB : AtomicEntryBuilder,
          CB : ConfigEntryBuilder<CV, CC, CG, CB>, CB : AtomicEntryBuilder = P.caption(caption, this)

/**
 * Attach an entry as the caption of a pairList entry<br></br>
 * Changes the value to a [Pair] of the caption's value and the pairList's value
 */
@Deprecated("Use as method from the pairList builder", ReplaceWith("pairList.caption(caption)"))
fun <K, V, KC, C, KG, G, KB, B : ConfigEntryBuilder<V, C, G, B>, CV, CC, CG, CB> caption(
  caption: CB, pairList: EntryPairListEntryBuilder<K, V, KC, C, KG, G, B, KB>
) where KB : ConfigEntryBuilder<K, KC, KG, KB>, KB : AtomicEntryBuilder,
        CB : ConfigEntryBuilder<CV, CC, CG, CB>, CB : AtomicEntryBuilder = pairList.caption(caption)

/**
 * Attach an entry as the caption of a pairList entry<br></br>
 * Changes the value to a [Pair] of the caption's value and the pairList's value
 */
fun <K, V, KC, C, KG, G, KB, B : ConfigEntryBuilder<V, C, G, B>, CV, CC, CG, CB>
  EntryPairListEntryBuilder<K, V, KC, C, KG, G, B, KB>.caption(caption: CB):
  CaptionedCollectionEntryBuilder<
    List<Pair<K, V>>, List<Pair<KC, C>>, Pair<KG, G>,
    EntryPairListEntryBuilder<K, V, KC, C, KG, G, B, KB>,
    CV, CC, CG, CB
  > where KB : ConfigEntryBuilder<K, KC, KG, KB>, KB : AtomicEntryBuilder,
          CB : ConfigEntryBuilder<CV, CC, CG, CB>, CB : AtomicEntryBuilder = P.caption(caption, this)

// General pairs

/**
 * Pair of two arbitrary non-collection entries (implementing [AtomicEntryBuilder]).
 *
 * If a default value is not specified, the default value of
 * each entry will be used to define it.
 *
 * @param leftEntry The entry for the left
 * @param rightEntry The entry for the right
 */
fun <L, R, LC, RC, LG, RG, LB, RB> pair(
  leftEntry: LB, rightEntry: RB
) where LB : ConfigEntryBuilder<L, LC, LG, LB>, LB : AtomicEntryBuilder,
        RB : ConfigEntryBuilder<R, RC, RG, RB>, RB : AtomicEntryBuilder =
  P.pair(leftEntry, rightEntry)

/**
 * Pair of two arbitrary non-collection entries (implementing [AtomicEntryBuilder]).
 *
 * If the default value is not specified, the default value of
 * each entry will be used to define it.
 *
 * @param leftEntry The entry for the left
 * @param rightEntry The entry for the right
 * @param value Entry value (if omitted, it's inferred from the values of the entries)
 */
fun <L, R, LC, RC, LG, RG, LB, RB> pair(
  leftEntry: LB, rightEntry: RB, value: kotlin.Pair<L, R>
) where LB : ConfigEntryBuilder<L, LC, LG, LB>, LB : AtomicEntryBuilder,
        RB : ConfigEntryBuilder<R, RC, RG, RB>, RB : AtomicEntryBuilder =
  P.pair(leftEntry, rightEntry, value.toCommonsPair())

// General triples

/**
 * Triple of three arbitrary non-collection entries (implementing [AtomicEntryBuilder]).
 *
 * If the default value is not specified, the default value of
 * each entry will be used to define it.
 *
 * @param leftEntry The entry for the left
 * @param middleEntry The entry for the middle
 * @param rightEntry The entry for the right
 */
fun <L, M, R, LC, MC, RC, LG, MG, RG, LB, MB, RB> triple(
  leftEntry: LB, middleEntry: MB, rightEntry: RB
) where LB : ConfigEntryBuilder<L, LC, LG, LB>, LB : AtomicEntryBuilder,
        MB : ConfigEntryBuilder<M, MC, MG, MB>, MB : AtomicEntryBuilder,
        RB : ConfigEntryBuilder<R, RC, RG, RB>, RB : AtomicEntryBuilder =
  P.triple(leftEntry, middleEntry, rightEntry)

/**
 * Triple of three arbitrary non-collection entries (implementing [AtomicEntryBuilder]).
 *
 * If the default value is not specified, the default value of
 * each entry will be used to define it.
 *
 * @param leftEntry The entry for the left
 * @param middleEntry The entry for the middle
 * @param rightEntry The entry for the right
 * @param value Entry value (if omitted, it's inferred from the values of the entries)
 */
fun <L, M, R, LC, MC, RC, LG, MG, RG, LB, MB, RB> triple(
  leftEntry: LB, middleEntry: MB, rightEntry: RB, value: kotlin.Triple<L, M, R>
) where LB : ConfigEntryBuilder<L, LC, LG, LB>, LB : AtomicEntryBuilder,
        MB : ConfigEntryBuilder<M, MC, MG, MB>, MB : AtomicEntryBuilder,
        RB : ConfigEntryBuilder<R, RC, RG, RB>, RB : AtomicEntryBuilder =
  P.triple(leftEntry, middleEntry, rightEntry, value.toCommonsTriple())
