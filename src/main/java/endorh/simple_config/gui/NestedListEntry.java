package endorh.simple_config.gui;

import com.google.common.collect.Lists;
import endorh.simple_config.core.EntrySetterUtil;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ReferenceProvider;
import endorh.simple_config.clothconfig2.gui.entries.AbstractListListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Reimplementation of {@link endorh.simple_config.clothconfig2.gui.entries.NestedListListEntry},
 * which not only is marked as internal, but also is not functional, since
 * it passes null as its saveConsumer to its super.
 * @param <T> Type held by the entry
 * @param <Inner> Type of the cells within the entry
 */
@SuppressWarnings("UnstableApiUsage")
@OnlyIn(Dist.CLIENT)
public class NestedListEntry<T, Inner extends AbstractConfigListEntry<T>>
  extends AbstractListListEntry<T, NestedListCell<T, Inner>, NestedListEntry<T, Inner>>
  implements ISettableConfigListEntry<List<T>> {
	protected final List<ReferenceProvider<?>> referencableEntries = Lists.newArrayList();
	
	public NestedListEntry(
	  ITextComponent fieldName, List<T> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<T>> saveConsumer,
	  Supplier<List<T>> defaultValue, ITextComponent resetButtonKey, boolean deleteButtonEnabled,
	  boolean insertInFront, BiFunction<T, NestedListEntry<T, Inner>, Inner> createNewCell
	) {
		super(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, false, deleteButtonEnabled, insertInFront,
		  (t, nestedListEntry) -> new NestedListCell<>(
		    t, nestedListEntry, createNewCell.apply(t, nestedListEntry)));
		
		for (NestedListCell<T, Inner> cell : cells)
			referencableEntries.add(cell.nestedEntry);
		setReferenceProviderEntries(referencableEntries);
	}
	
	public NestedListEntry<T, Inner> self() {
		return this;
	}
	
	protected void onAdd(Inner inner) {
		referencableEntries.add(inner);
		requestReferenceRebuilding();
	}
	
	protected void onDelete(Inner inner) {
		referencableEntries.remove(inner);
		requestReferenceRebuilding();
	}
	
	@Override public void setValue(List<T> value) {
		for (int i = 0; i < cells.size() && i < value.size(); i++)
			cells.get(i).setValue(value.get(i));
		while (cells.size() > value.size())
			remove(cells.size() - 1);
		while (cells.size() < value.size())
			add(value.get(cells.size()));
	}
	
	public void add(T element) {
		NestedListCell<T, Inner> cell = createNewCell.apply(element, this);
		cells.add(cell);
		widgets.add(cell);
		cell.onAdd();
	}
	
	public void add(int index, T element) {
		NestedListCell<T, Inner> cell = createNewCell.apply(element, this);
		cells.add(index, cell);
		widgets.add(index, cell);
		cell.onAdd();
	}
	
	public void remove(T element) {
		final int index = getValue().indexOf(element);
		if (index >= 0)
			remove(index);
	}
	
	public void remove(int index) {
		final NestedListCell<T, Inner> cell = cells.get(index);
		cell.onDelete();
		cells.remove(cell);
		widgets.remove(cell);
	}
}