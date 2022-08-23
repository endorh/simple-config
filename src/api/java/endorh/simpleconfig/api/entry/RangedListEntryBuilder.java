package endorh.simpleconfig.api.entry;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;

import java.util.Optional;
import java.util.function.Function;

public interface RangedListEntryBuilder<
  V extends Comparable<V>, Config, Gui extends Comparable<Gui>,
  Self extends RangedListEntryBuilder<V, Config, Gui, Self>
> extends ListEntryBuilder<V, Config, Gui, Self> {
	/**
	 * Set the minimum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) Self min(V min);
	
	/**
	 * Set the maximum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) Self max(V max);
	
	/**
	 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) Self range(V min, V max);
	
	@Contract(pure=true)
	@Override Self elemError(Function<V, Optional<Component>> errorSupplier);
}
