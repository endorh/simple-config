package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.function.Supplier;

public interface StringEntryBuilder
  extends ConfigEntryBuilder<String, String, String, StringEntryBuilder>, KeyEntryBuilder<String> {
	/**
	 * Suggest possible values in a combo-box.<br>
	 * To restrict values to the suggestions, use {@link #restrict} instead.<br>
	 * For suggestions, it's possible to provide instead a suggestion supplier,
	 * to provide dynamic suggestions instead. This is not possible with restrictions.
	 */
	@Contract(pure=true) StringEntryBuilder suggest(String... suggestions);
	
	/**
	 * Suggest possible values in a combo-box.<br>
	 * To restrict values to the suggestions, use {@link #restrict} instead.<br>
	 * For suggestions, it's possible to provide instead a suggestion supplier,
	 * to provide dynamic suggestions instead. This is not possible with restrictions.
	 */
	@Contract(pure=true) StringEntryBuilder suggest(@NotNull List<String> suggestions);
	
	/**
	 * Suggest possible values in a combo-box dynamically.<br>
	 * To restrict values to the suggestions, use {@link #restrict},
	 * although this method can only supply a fixed set of choices.
	 */
	@Contract(pure=true) StringEntryBuilder suggest(Supplier<List<String>> suggestionSupplier);
	
	/**
	 * Restrict the values of this entry to a finite set
	 * of options, displayed in a combo box.<br>
	 * Unlike {@link #suggest}, this method does not accept
	 * a {@link Supplier} of choices, since delayed choice
	 * computation would result in the entry's value being reset
	 * before the choices can be determined. Consider using
	 * suggestions instead when they cannot be determined at
	 * start-up time
	 */
	@Contract(pure=true) StringEntryBuilder restrict(String first, String... choices);
	
	/**
	 * Restrict the values of this entry to a finite set
	 * of options, displayed in a combo box.<br>
	 * Unlike {@link #suggest}, this method does not accept
	 * a {@link Supplier} of choices, since delayed choice
	 * computation would result in the entry's value being reset
	 * before the choices can be determined. Consider using
	 * suggestions instead when they cannot be determined at
	 * start-up time
	 */
	@Contract(pure=true) StringEntryBuilder restrict(@NotNull List<String> choices);
	
	@Contract(pure=true) StringEntryBuilder maxLength(@Range(from=0, to=Integer.MAX_VALUE) int maxLength);
	
	@Contract(pure=true) StringEntryBuilder minLength(@Range(from=0, to=Integer.MAX_VALUE) int minLength);
	
	@Contract(pure=true) StringEntryBuilder notEmpty();
}
