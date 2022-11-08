package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.function.Supplier;

public interface StringEntryBuilder
  extends ConfigEntryBuilder<@NotNull String, String, String, StringEntryBuilder>,
          AtomicEntryBuilder {
	/**
	 * Suggest possible values in a combo-box.<br>
	 * To restrict values to the suggestions, use {@link #restrict} instead.<br>
	 * For suggestions, it's possible to provide instead a suggestion supplier,
	 * to provide dynamic suggestions instead. This is not possible with restrictions.
	 */
	@Contract(pure=true) @NotNull StringEntryBuilder suggest(String... suggestions);
	
	/**
	 * Suggest possible values in a combo-box.<br>
	 * To restrict values to the suggestions, use {@link #restrict} instead.<br>
	 * For suggestions, it's possible to provide instead a suggestion supplier,
	 * to provide dynamic suggestions instead. This is not possible with restrictions.
	 */
	@Contract(pure=true) @NotNull StringEntryBuilder suggest(@NotNull List<String> suggestions);
	
	/**
	 * Suggest possible values in a combo-box dynamically.<br>
	 * To restrict values to the suggestions, use {@link #restrict},
	 * although this method can only supply a fixed set of choices.
	 */
	@Contract(pure=true) @NotNull StringEntryBuilder suggest(Supplier<List<String>> suggestionSupplier);
	
	/**
	 * Restrict the values of this entry to a finite set
	 * of options, displayed in a combo box.<br><br>
	 *
	 * This variadic override will automatically change the default value
	 * to the first option by calling {@link #withValue} if it's not
	 * included in the choices.<br><br>
	 *
	 * Unlike {@link #suggest}, this method does not accept
	 * a {@link Supplier} of choices, since delayed choice
	 * computation would result in the entry's value being reset
	 * before the choices can be determined. Consider using
	 * suggestions instead when they cannot be determined at
	 * start-up time
	 */
	@Contract(pure=true) @NotNull StringEntryBuilder restrict(String first, String... choices);
	
	/**
	 * Restrict the values of this entry to a finite set
	 * of options, displayed in a combo box.<br><br>
	 *
	 * Unlike the variadic override, this method will throw
	 * an {@link IllegalArgumentException} if the default value
	 * is not included in the choices.
	 * You may want to use {@link #withValue} to ensure the default
	 * value is included in the choice list.<br><br>
	 *
	 * Unlike {@link #suggest}, this method does not accept
	 * a {@link Supplier} of choices, since delayed choice
	 * computation would result in the entry's value being reset
	 * before the choices can be determined. Consider using
	 * suggestions instead when they cannot be determined at
	 * start-up time
	 */
	@Contract(pure=true) @NotNull StringEntryBuilder restrict(@NotNull List<String> choices);
	
	/**
	 * Set the maximum (inclusive) allowed length for this entry.<br>
	 * By default there is no limit.
	 */
	@Contract(pure=true) @NotNull StringEntryBuilder maxLength(@Range(from=0, to=Integer.MAX_VALUE) int maxLength);
	
	/**
	 * Set the minimum (inclusive) allowed length for this entry.<br>
	 * By default the limit is 0.<br>
	 * Set to 1 or use {@link #notEmpty()} to disallow empty strings.
	 */
	@Contract(pure=true) @NotNull StringEntryBuilder minLength(@Range(from=0, to=Integer.MAX_VALUE) int minLength);
	
	/**
	 * Dissallow empty strings.<br>
	 * Equivalent to {@code minLength(1)}
	 */
	@Contract(pure=true) @NotNull StringEntryBuilder notEmpty();
}
