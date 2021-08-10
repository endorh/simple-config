package endorh.simple_config.clothconfig2.gui.entries;

import endorh.simple_config.clothconfig2.gui.entries.AbstractListListEntry.AbstractListCell;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
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
	
	@Override
	public List<T> getValue() {
		return this.cells.stream().map(AbstractListCell::getValue).collect(Collectors.toList());
	}
	
	@Override
	protected C getFromValue(T value) {
		return this.createNewCell.apply(value, this.self());
	}
	
	@Override
	public boolean isEdited() {
		if (super.isEdited()) {
			return true;
		}
		final List<T> value = this.getValue();
		if (value.size() != this.original.size()) {
			return true;
		}
		for (int i = 0; i < value.size(); ++i) {
			if (Objects.equals(value.get(i), this.original.get(i))) continue;
			return true;
		}
		return false;
	}
	
	@ApiStatus.Internal
	public static abstract class AbstractListCell<T, SELF extends AbstractListCell<T, SELF, OUTER_SELF>, OUTER_SELF extends AbstractListListEntry<T, SELF, OUTER_SELF>>
	  extends BaseListCell {
		protected final OUTER_SELF listListEntry;
		
		public AbstractListCell(@Nullable T value, OUTER_SELF listListEntry) {
			this.listListEntry = listListEntry;
			this.setErrorSupplier(() -> Optional.ofNullable(listListEntry.cellErrorSupplier)
			  .flatMap(cellErrorFn -> cellErrorFn.apply(this.getValue())));
		}
		
		public abstract T getValue();
	}
}

