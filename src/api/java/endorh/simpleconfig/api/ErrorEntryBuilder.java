package endorh.simpleconfig.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Interface for {@link ConfigEntryBuilder}s providing methods to set
 * the error supplier accepting broader signatures
 * @param <V> The type of the entry
 * @param <Config> The config type of the entry
 * @param <Gui> The GUI type of the entry
 * @param <Self> The actual entry subtype to be returned by builder-like methods
 */
public interface ErrorEntryBuilder<V, Config, Gui, Self extends TooltipEntryBuilder<V, Gui, Self>> {
	
	/**
	 * Provide error messages for invalid values<br>
	 * Subsequent calls to this and any other error methods <b>add more error checks</b> to the
	 * entry, <b>rather than replacing</b> them.<br>
	 * Use {@link #withoutError()} to remove all error checks.<br><br>
	 * You may also use {@link #guiErrorNullable(Function)} to
	 * use a function returning nullable values instead of {@link Optional}
	 * @param guiErrorSupplier Error message supplier. Empty return values indicate
	 *                         correct values
	 */
	@Contract(pure=true) @NotNull Self guiError(Function<Gui, Optional<Component>> guiErrorSupplier);
	
	/**
	 * Provide error messages for invalid values<br>
	 * Subsequent calls to this and any other error methods <b>add more error checks</b> to the
	 * entry, <b>rather than replacing</b> them.<br>
	 * Use {@link #withoutError()} to remove all error checks.<br><br>
	 * You may also use {@link #guiError(Function)} to
	 * use a function returning {@link Optional} instead of nullable values
	 * @param errorSupplier Error message supplier. null return values
	 *                         indicate correct values
	 */
	@Contract(pure=true) default @NotNull Self guiErrorNullable(Function<Gui, Component> errorSupplier) {
		return guiError(v -> Optional.ofNullable(errorSupplier.apply(v)));
	}
	
	/**
	 * Provide error messages for invalid values<br>
	 * Subsequent calls to this and any other error methods <b>add more error checks</b> to the
	 * entry, <b>rather than replacing</b> them.<br>
	 * Use {@link #withoutError()} to remove all error checks.<br><br>
	 * You may also use {@link #configError(Function)}to
	 * use a function returning {@link Optional} instead of nullable values
	 *
	 * @param errorSupplier Error message supplier. null return values
	 *   indicate correct values
	 */
	@Contract(pure=true) default @NotNull Self configErrorNullable(Function<Gui, Component> errorSupplier) {
		return guiError(v -> Optional.ofNullable(errorSupplier.apply(v)));
	}
	
	/**
	 * Provide error messages for invalid values<br>
	 * Subsequent calls to this and any other error methods <b>add more error checks</b> to the
	 * entry, <b>rather than replacing</b> them.<br>
	 * Use {@link #withoutError()} to remove all error checks.<br><br>
	 * You may also use {@link #errorNullable(Function)} to
	 * use a function returning nullable values instead of {@link Optional}
	 * @param errorSupplier Error message supplier. Empty return values indicate correct values
	 */
	@Contract(pure=true) @NotNull Self error(Function<V, Optional<Component>> errorSupplier);
	
	/**
	 * Provide error messages for invalid values.<br>
	 * Subsequent calls to this and any other error methods <b>add more error checks</b> to the
	 * entry, <b>rather than replacing</b> them.<br>
	 * Use {@link #withoutError()} to remove all error checks.<br><br>
	 * You may also use {@link #configErrorNullable(Function)} to use a function returning nullable
	 * values instead of {@link Optional}
	 * @param errorSupplier Error message supplier. Empty return values indicate correct values
	 */
	@Contract(pure=true) @NotNull Self configError(Function<Config, Optional<Component>> errorSupplier);
	
	/**
	 * Remove error checks from this entry previously added with
	 * {@link #guiError(Function)}, {@link #error(Function)}, {@link #configError(Function)}
	 * or their variants.
	 */
	@Contract(pure=true) @NotNull Self withoutError();
	
	/**
	 * Restrict the values of this entry<br>
	 * Subsequent calls to this and any other error methods <b>add more error checks</b> to the
	 * entry, <b>rather than replacing</b> them.<br>
	 * Use {@link #withoutError()} to remove all error checks.<br><br>
	 * You may also use {@link #error(Function)}
	 * to provide users with an explicative error message
	 * @param validator Should return true for all valid elements
	 * @deprecated Use {@link #error(Function)}
	 *             to provide users with clearer error messages
	 */
	@Contract(pure=true) @Deprecated default @NotNull Self check(Predicate<V> validator) {
		return error(v -> validator.test(v)? Optional.empty() : Optional.of(
		  Component.translatable("simpleconfig.config.error.invalid_value_generic")));
	}
	
	/**
	 * Provide error messages for invalid values<br>
	 * Subsequent calls to this and any other error methods <b>add more error checks</b> to the
	 * entry, <b>rather than replacing</b> them.<br>
	 * Use {@link #withoutError()} to remove all error checks.<br><br>
	 * You may also use {@link #error(Function)} to use
	 * a function returning {@link Optional} instead of nullable values
	 * @param errorSupplier Error message supplier. null return values indicate
	 *                      correct values
	 */
	@Contract(pure=true) default @NotNull Self errorNullable(Function<V, Component> errorSupplier) {
		return error(v -> Optional.ofNullable(errorSupplier.apply(v)));
	}
}
