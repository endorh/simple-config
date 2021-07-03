package dnj.simple_config.core.entry;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class RangedListEntry
  <V extends Comparable<V>, Config, Gui, Self extends RangedListEntry<V, Config, Gui, Self>>
  extends ListEntry<V, Config, Gui, Self> {
	protected V min;
	protected V max;
	
	public RangedListEntry(
	  @Nullable List<V> value, @Nonnull V min, @Nonnull V max
	) {
		super(value);
		this.min = min;
		this.max = max;
	}
	
	/**
	 * Set the minimum allowed value for the elements of this list entry (inclusive)
	 */
	public Self min(@Nonnull V min) {
		this.min = min;
		elemError(clamp(validator, min, max));
		return self();
	}
	
	/**
	 * Set the maximum allowed value for the elements of this list entry (inclusive)
	 */
	public Self max(@Nonnull V max) {
		this.max = max;
		elemError(clamp(validator, min, max));
		return self();
	}
	
	/**
	 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
	 */
	public Self range(V min, V max) {
		this.min = min;
		this.max = max;
		elemError(clamp(validator, min, max));
		return self();
	}
	
	@Override
	public Self elemError(Function<V, Optional<ITextComponent>> validator) {
		return super.elemError(clamp(validator, min, max));
	}
	
	protected Function<V, Optional<ITextComponent>> clamp(
	  @Nullable Function<V, Optional<ITextComponent>> validator, V min, V max
	) {
		return t -> {
			if (t.compareTo(min) < 0)
				return Optional
				  .of(new TranslationTextComponent("text.cloth-config.error.too_small", min));
			if (t.compareTo(max) > 0)
				return Optional
				  .of(new TranslationTextComponent("text.cloth-config.error.too_large", max));
			return validator != null ? validator.apply(t) : Optional.empty();
		};
	}
}
