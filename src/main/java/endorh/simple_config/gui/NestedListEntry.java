package endorh.simple_config.gui;

import com.google.common.collect.Lists;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ReferenceProvider;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Reimplementation of {@link me.shedaniel.clothconfig2.gui.entries.NestedListListEntry},
 * which not only is marked as internal, but also is not functional, since
 * it passes null as its saveConsumer to its super.
 * @param <T> Type held by the entry
 * @param <Inner> Type of the cells within the entry
 */
@SuppressWarnings("UnstableApiUsage")
@OnlyIn(Dist.CLIENT)
public class NestedListEntry<T, Inner extends AbstractConfigListEntry<T>>
  extends AbstractListListEntry<T, NestedListCell<T, Inner>, NestedListEntry<T, Inner>> {
	protected final List<ReferenceProvider<?>> referencableEntries = Lists.newArrayList();
	protected final Consumer<Integer> deleteConsumer;
	
	public NestedListEntry(
	  ITextComponent fieldName, List<T> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<T>> saveConsumer,
	  Supplier<List<T>> defaultValue, ITextComponent resetButtonKey, boolean deleteButtonEnabled,
	  boolean insertInFront, BiFunction<T, NestedListEntry<T, Inner>, Inner> createNewCell,
	  Consumer<Integer> deleteConsumer
	) {
		super(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, false, deleteButtonEnabled, insertInFront,
		  (t, nestedListEntry) -> new NestedListCell<>(
		    t, nestedListEntry, createNewCell.apply(t, nestedListEntry)));
		
		this.deleteConsumer = deleteConsumer;
		for (NestedListCell<T, Inner> cell : cells)
			referencableEntries.add(cell.nestedEntry);
		setReferenceProviderEntries(referencableEntries);
	}
	
	public NestedListEntry<T, Inner> self() {
		return this;
	}
	
	public void add(Inner inner) {
		referencableEntries.add(inner);
		requestReferenceRebuilding();
	}
	
	public void delete(Inner inner) {
		final int i = referencableEntries.indexOf(inner);
		deleteConsumer.accept(i);
		referencableEntries.remove(i);
		requestReferenceRebuilding();
	}
	
	/*@Override public Optional<ITextComponent[]> getTooltip() {
		return super.getTooltip();
	}*/
}