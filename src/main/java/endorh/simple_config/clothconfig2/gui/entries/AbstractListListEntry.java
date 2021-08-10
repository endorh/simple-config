package endorh.simple_config.clothconfig2.gui.entries;

import endorh.simple_config.clothconfig2.gui.entries.AbstractListListEntry.AbstractListCell;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@OnlyIn(value = Dist.CLIENT)
public abstract class AbstractListListEntry<T, C extends AbstractListCell<T, C, SELF>, SELF extends AbstractListListEntry<T, C, SELF>>
  extends BaseListEntry<T, C, SELF> {
	protected final BiFunction<T, SELF, C> createNewCell;
	protected Function<T, Optional<ITextComponent>> cellErrorSupplier;
	protected List<T> original;
	
	@ApiStatus.Internal
	public AbstractListListEntry(
	  ITextComponent fieldName, List<T> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<T>> saveConsumer,
	  Supplier<List<T>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart,
	  boolean deleteButtonEnabled, boolean insertInFront, BiFunction<T, SELF, C> createNewCell
	) {
		super(
		  fieldName, tooltipSupplier, defaultValue,
		  abstractListListEntry -> createNewCell.apply(null, abstractListListEntry), saveConsumer,
		  resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront);
		this.createNewCell = createNewCell;
		this.original = new ArrayList<>(value);
		for (T f : value) {
			this.cells.add(createNewCell.apply(f, this.self()));
		}
		this.widgets.addAll(this.cells);
		this.setExpanded(defaultExpanded);
	}
	
	public Function<T, Optional<ITextComponent>> getCellErrorSupplier() {
		return this.cellErrorSupplier;
	}
	
	public void setCellErrorSupplier(Function<T, Optional<ITextComponent>> cellErrorSupplier) {
		this.cellErrorSupplier = cellErrorSupplier;
	}
	
	@Override public List<T> getValue() {
		return this.cells.stream().map(AbstractListCell::getValue).collect(Collectors.toList());
	}
	
	@Override public void setValue(List<T> value) {
		for (int i = 0; i < cells.size() && i < value.size(); i++)
			cells.get(i).setValue(value.get(i));
		while (cells.size() > value.size())
			remove(cells.size() - 1);
		while (cells.size() < value.size())
			add(value.get(cells.size()));
	}
	
	@Override protected C cellWithValue(T value) {
		return this.createNewCell.apply(value, this.self());
	}
	
	@Override
	public boolean isEdited() {
		if (super.isEdited())
			return true;
		final List<T> value = this.getValue();
		if (value.size() != this.original.size())
			return true;
		for (int i = 0; i < value.size(); ++i) {
			if (Objects.equals(value.get(i), this.original.get(i))) continue;
			return true;
		}
		return false;
	}
	
	public void add(T element) {
		final C cell = createNewCell.apply(element, self());
		cells.add(cell);
		widgets.add(cell);
		cell.onAdd();
	}
	
	public void add(int index, T element) {
		final C cell = createNewCell.apply(element, self());
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
		final C cell = cells.get(index);
		cell.onDelete();
		cells.remove(cell);
		widgets.remove(cell);
	}
	
	@Internal
	public static abstract class AbstractListCell<T, SELF extends AbstractListCell<T, SELF, OUTER_SELF>, OUTER_SELF extends AbstractListListEntry<T, SELF, OUTER_SELF>>
	  extends BaseListCell {
		protected final OUTER_SELF listListEntry;
		
		public AbstractListCell(@Nullable T value, OUTER_SELF listListEntry) {
			this.listListEntry = listListEntry;
			this.setErrorSupplier(() -> Optional.ofNullable(listListEntry.cellErrorSupplier)
			  .flatMap(cellErrorFn -> cellErrorFn.apply(this.getValue())));
		}
		
		public abstract T getValue();
		public abstract void setValue(T value);
	}
}

