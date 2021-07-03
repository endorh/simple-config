package endorh.simple_config.core;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Interface for {@link AbstractConfigEntry} providing methods to set
 * the error supplier accepting broader signatures
 * @param <V> The type of the entry
 * @param <Gui> The GUI type of the entry
 * @param <Self> The actual entry subtype to be returned by builder-like methods
 */
public interface IErrorEntry<V, Gui, Self extends ITooltipEntry<V, Gui, Self>> {
	
	/**
	 * Provide error messages for invalid values<br>
	 * You may also use {@link IErrorEntry#guiErrorNullable(Function)} to
	 * use a function returning nullable values instead of {@link Optional}
	 * @param guiErrorSupplier Error message supplier. Empty return values indicate
	 *                         correct values
	 */
	Self guiError(Function<Gui, Optional<ITextComponent>> guiErrorSupplier);
	
	/**
	 * Provide error messages for invalid values<br>
	 * You may also use {@link IErrorEntry#guiError(Function)} to
	 * use a function returning {@link Optional} instead of nullable values
	 * @param guiErrorSupplier Error message supplier. null return values
	 *                         indicate correct values
	 */
	default Self guiErrorNullable(Function<Gui, ITextComponent> guiErrorSupplier) {
		return guiError(v -> Optional.ofNullable(guiErrorSupplier.apply(v)));
	}
	
	/**
	 * Provide error messages for invalid values<br>
	 * You may also use {@link IErrorEntry#errorNullable(Function)} to
	 * use a function returning nullable values instead of {@link Optional}
	 * @param errorSupplier Error message supplier. Empty return values indicate correct values
	 */
	Self error(Function<V, Optional<ITextComponent>> errorSupplier);
	
	/**
	 * Restrict the values of this entry<br>
	 * You may also use {@link IErrorEntry#error(Function)}
	 * to provide users with an explicative error message
	 * @param validator Should return true for all valid elements
	 * @deprecated Use {@link IErrorEntry#error(Function)}
	 *             to provide users with clearer error messages
	 */
	@Deprecated default Self check(Predicate<V> validator) {
		return error(v -> validator.test(v)? Optional.empty() : Optional.of(
		  new TranslationTextComponent(
		    "simple-config.config.error.invalid_value_generic")));
	}
	
	/**
	 * Provide error messages for invalid values<br>
	 * You may also use {@link IErrorEntry#error(Function)} to use
	 * a function returning {@link Optional} instead of nullable values
	 * @param errorSupplier Error message supplier. null return values indicate
	 *                      correct values
	 */
	default Self errorNullable(Function<V, ITextComponent> errorSupplier) {
		return error(v -> Optional.ofNullable(errorSupplier.apply(v)));
	}
}
