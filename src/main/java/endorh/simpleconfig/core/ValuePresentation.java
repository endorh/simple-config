package endorh.simpleconfig.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@FunctionalInterface
public interface ValuePresentation<V, P> extends Function<V, P> {
	static <V, P> ValuePresentation<V, P> of(@NotNull Function<@NotNull V, P> present) {
		return present::apply;
	}
	
	static <V, P> ValuePresentation<V, P> of(
	  @NotNull Function<@NotNull V, P> present, @Nullable Function<P, @Nullable V> invert
	) {
		if (invert == null) return present::apply;
		return new ValuePresentation<V, P>() {
			@Override public P apply(@NotNull V v) {
				return present.apply(v);
			}
			
			@Override public boolean isInvertible() {
				return true;
			}
			
			@Override public @Nullable V recover(P p) {
				return invert.apply(p);
			}
		};
	}
	
	@Override P apply(@NotNull V v);
	
	default boolean isInvertible() {
		return false;
	}
	
	default @Nullable V recover(P p) {
		return null;
	}
}
