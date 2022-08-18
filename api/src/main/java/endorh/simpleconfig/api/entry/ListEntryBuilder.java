package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ErrorEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface ListEntryBuilder<V, Config, Gui, Self extends ListEntryBuilder<V, Config, Gui, Self>>
  extends ConfigEntryBuilder<List<V>, List<Config>, List<Gui>, Self> {
	@Contract(pure=true) Self expand();
	
	@Contract(pure=true) Self expand(boolean expand);
	
	/**
	 * Set the minimum (inclusive) allowed list size.
	 *
	 * @param minSize Inclusive minimum size
	 */
	@Contract(pure=true) Self minSize(int minSize);
	
	/**
	 * Set the maximum (inclusive) allowed list size.
	 *
	 * @param maxSize Inclusive maximum size
	 */
	@Contract(pure=true) Self maxSize(int maxSize);
	
	/**
	 * Set an error message supplier for the elements of this list entry<br>
	 * You may also use {@link ErrorEntryBuilder#error(Function)} to check
	 * instead the whole list<br>
	 * If a single element is deemed invalid, the whole list is considered invalid.
	 *
	 * @param errorSupplier Error message supplier. Empty return values indicate
	 *   correct values
	 */
	@Contract(pure=true) Self elemError(Function<V, Optional<ITextComponent>> errorSupplier);
}
