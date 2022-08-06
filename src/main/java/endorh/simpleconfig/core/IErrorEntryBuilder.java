package endorh.simpleconfig.core;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.Contract;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Interface for {@link AbstractConfigEntry} providing methods to set
 * the error supplier accepting broader signatures
 * @param <V> The type of the entry
 * @param <Config> The config type of the entry
 * @param <Gui> The GUI type of the entry
 * @param <Self> The actual entry subtype to be returned by builder-like methods
 */
public interface IErrorEntryBuilder<V, Config, Gui, Self extends ITooltipEntryBuilder<V, Gui, Self>> {
	
	/**
	 * Provide error messages for invalid values<br>
	 * You may also use {@link #guiErrorNullable(Function)} to
	 * use a function returning nullable values instead of {@link Optional}
	 * @param guiErrorSupplier Error message supplier. Empty return values indicate
	 *                         correct values
	 */
	@Contract(pure=true) Self guiError(Function<Gui, Optional<ITextComponent>> guiErrorSupplier);
	
	/**
	 * Provide error messages for invalid values<br>
	 * You may also use {@link #guiError(Function)} to
	 * use a function returning {@link Optional} instead of nullable values
	 * @param errorSupplier Error message supplier. null return values
	 *                         indicate correct values
	 */
	@Contract(pure=true) default Self guiErrorNullable(Function<Gui, ITextComponent> errorSupplier) {
		return guiError(v -> Optional.ofNullable(errorSupplier.apply(v)));
	}
	
	/**
	 * Provide error messages for invalid values<br>
	 * You may also use {@link #configError(Function)}to
	 * use a function returning {@link Optional} instead of nullable values
	 *
	 * @param errorSupplier Error message supplier. null return values
	 *   indicate correct values
	 */
	@Contract(pure=true) default Self configErrorNullable(Function<Gui, ITextComponent> errorSupplier) {
		return guiError(v -> Optional.ofNullable(errorSupplier.apply(v)));
	}
	
	/**
	 * Provide error messages for invalid values<br>
	 * You may also use {@link #errorNullable(Function)} to
	 * use a function returning nullable values instead of {@link Optional}
	 * @param errorSupplier Error message supplier. Empty return values indicate correct values
	 */
	@Contract(pure=true) Self error(Function<V, Optional<ITextComponent>> errorSupplier);
	
	/**
	 * Provide error messages for invalid values<br>
	 * You may also use {@link #configErrorNullable(Function)} to use a function returning nullable
	 * values instead of {@link Optional}
	 * @param errorSupplier Error message supplier. Empty return values indicate correct values
	 */
	@Contract(pure=true) Self configError(Function<Config, Optional<ITextComponent>> errorSupplier);
	
	/**
	 * Restrict the values of this entry<br>
	 * You may also use {@link #error(Function)}
	 * to provide users with an explicative error message
	 * @param validator Should return true for all valid elements
	 * @deprecated Use {@link #error(Function)}
	 *             to provide users with clearer error messages
	 */
	@Contract(pure=true) @Deprecated default Self check(Predicate<V> validator) {
		return error(v -> validator.test(v)? Optional.empty() : Optional.of(
		  new TranslationTextComponent(
		    "simpleconfig.config.error.invalid_value_generic")));
	}
	
	/**
	 * Provide error messages for invalid values<br>
	 * You may also use {@link #error(Function)} to use
	 * a function returning {@link Optional} instead of nullable values
	 * @param errorSupplier Error message supplier. null return values indicate
	 *                      correct values
	 */
	@Contract(pure=true) default Self errorNullable(Function<V, ITextComponent> errorSupplier) {
		return error(v -> Optional.ofNullable(errorSupplier.apply(v)));
	}
}
