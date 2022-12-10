package endorh.simpleconfig.api.annotation;

import endorh.simpleconfig.api.AbstractRange.DoubleRange;
import endorh.simpleconfig.api.AbstractRange.FloatRange;
import endorh.simpleconfig.api.AbstractRange.IntRange;
import endorh.simpleconfig.api.AbstractRange.LongRange;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Mark a <b>static</b> field as a config entry<br>
 * The type of the field must be one of the supported
 * <ul>
 *    <li>{@code boolean} (or {@code Boolean})</li>
 *    <li>{@code String}</li>
 *    <li>{@code Enum} (any {@code enum} type)</li>
 *    <li>{@code byte} (or {@code Byte}</li>
 *    <li>{@code short} (or {@code Short})</li>
 *    <li>{@code int} (or {@code Integer})</li>
 *    <li>{@code long} (or {@code Long})</li>
 *    <li>{@code float} (or {@code Float})</li>
 *    <li>{@code double} (or {@code Double})</li>
 *    <li>{@link IntRange}</li>
 *    <li>{@link LongRange}</li>
 *    <li>{@link FloatRange}</li>
 *    <li>{@link DoubleRange}</li>
 *    <li>{@link java.awt.Color}</li>
 *    <li>{@link Pattern}</li>
 *    <li>{@link Tag}</li>
 *    <li>{@link CompoundTag}</li>
 *    <li>{@link ResourceLocation}</li>
 *    <li>{@link KeyBindMapping}</li>
 *    <li>{@link Item}</li>
 *    <li>{@link Block}</li>
 *    <li>{@link Fluid}</li>
 * </ul>
 * Additionally, the following generic types are also supported,
 * given that their type arguments are also supported:
 * <ul>
 *    <li>{@link List}</li>
 *    <li>{@link Set}</li>
 *    <li>{@link Map} (keys must be atomic)</li>
 *    <li>{@link Pair} (values must be atomic)</li>
 *    <li>{@link Triple} (values must be atomic)</li>
 * </ul>
 * Furthermore, any class annotated as {@link Bean} is also supported.
 * Such classes should contain their own {@link Entry} annotated field
 * bean properties.<br><br>
 *
 * Other annotations in this package (such as {@link Min}, {@link Max} or
 * {@link Slider}) can be used to configure generated entries.<br>
 * In particular, the {@link Configure} annotation can be used to
 * achieve full parity with the builder API, by defining decorator
 * methods that get applied to entries annotated with your own
 * custom annotations.<br><br>
 *
 * Simple Config can also recognize what will be referred to as
 * 'sibling methods'. These are methods defined besides their entries,
 * sharing their name with a suffix (separated by '{@code $}').
 * The following sibling methods are supported:
 * <ul><li>
 *    {@code $error}, receiving a value of the same type as the entry,
 *    and returning either of the following types:
 *    <ul><li>
 *       {@code Optional<Component>}, an optional error message
 *    </li><li>
 *       {@code Component}, an error message or null
 *    </li><li>
 *       {@code String}, an error message translation key or null
 *    </li><li>
 *       {@code boolean}, {@code true} if an error is present (a generic error message will be used)
 *    </li></ul>
 * </li><li>
 *    {@code $tooltip}, receiving a value of the same type as the entry,
 *    and returning either of the following types:
 *    <ul><li>
 *       {@code List<Component>}, a tooltip for the value
 *    </li><li>
 *       {@code Component}, a single line tooltip for the value
 *    </li><li>
 *       {@code String}, a translation key for the tooltip for the value,
 *       which may contain new lines
 *    </li></ul>
 * </li></ul>
 *
 * <b>Keep in mind that entries get automatically mapped tooltip
 * translation keys</b>. Use dynamic tooltips only when
 * necessary<br><br>
 *
 * For generic entry types, sibling methods can be defined for the inner entries,
 * by appending the following suffixes after the entry name:
 * <ul>
 *    <li>{@code $v} for {@code List}s, {@code Set}s and {@code Map} values</li>
 *    <li>{@code $k} for {@code Map} keys</li>
 *    <li>{@code $l}, {@code $m} and {@code $r} for {@code Pair} and {@code Triple} values</li>
 *    <li>
 *       {@code $caption} and {@code $list}/{@code $set}/{@code $map} for captioned collections
 *    </li>
 * </ul>
 * @see Category
 * @see Group
 * @see Configure
 * @see Bind
 * @see Text
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entry {
	/**
	 * The preferred index where to add this entry, relative to its siblings,
	 * including config groups on the same level<br><br>
	 * Should only be necessary when dealing with nested groups in between
	 * entries, but the Java language specification won't let me guarantee that
	 */
	int value() default 0;
	
	/**
	 * Marks this entry as the caption of the following entry in the same class.<br><br>
	 *
	 * Only non-atomic entries can have a caption, and the caption must always be atomic.
	 */
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Caption {}
}
