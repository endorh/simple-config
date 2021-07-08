package endorh.simple_config.gui;

import com.google.common.collect.Lists;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ReferenceProvider;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import me.shedaniel.clothconfig2.gui.entries.BaseListCell;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class StringPairListEntry<T, Inner extends AbstractConfigListEntry<T>>
  extends AbstractListListEntry<Pair<String, T>, StringPairCell<T, Inner>, StringPairListEntry<T, Inner>> {
	protected final List<ReferenceProvider<?>> referencableEntries = Lists.newArrayList();
	protected final Supplier<Pair<String, T>> defaultEntrySupplier;
	protected Function<List<Pair<String, T>>, Optional<ITextComponent>> errorSupplier;
	protected Function<Pair<String, T>, Optional<ITextComponent>> entryErrorSupplier;
	protected boolean ignoreOrder;
	
	public StringPairListEntry(
	  ITextComponent fieldName, List<Pair<String, T>> value,
	  boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier,
	  Consumer<List<Pair<String, T>>> saveConsumer, Supplier<List<Pair<String, T>>> defaultValue,
	  Supplier<Pair<String, T>> defaultEntrySupplier,
	  Function<List<Pair<String, T>>, Optional<ITextComponent>> errorSupplier,
	  Function<Pair<String, T>, Optional<ITextComponent>> entryErrorSupplier,
	  ITextComponent resetButtonKey, boolean deleteButtonEnabled, boolean insertInFront,
	  BiFunction<Pair<String, T>, StringPairListEntry<T, Inner>, Inner> createNewCell,
	  boolean ignoreOrder
	) {
		super(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, false, deleteButtonEnabled, insertInFront,
		  (t, nestedListEntry) -> {
		  	  if (t == null) t = defaultEntrySupplier.get();
		  	  return new StringPairCell<>(t, nestedListEntry, createNewCell.apply(t, nestedListEntry));
		  });
		
		this.defaultEntrySupplier = defaultEntrySupplier;
		this.errorSupplier = errorSupplier;
		this.entryErrorSupplier = entryErrorSupplier;
		this.ignoreOrder = ignoreOrder;
		for (StringPairCell<T, Inner> cell : cells)
			referencableEntries.add(cell.nestedEntry);
		setReferenceProviderEntries(referencableEntries);
	}
	
	@Override
	public List<Pair<String, T>> getValue() {
		return super.getValue();
	}
	
	@Override
	public void updateSelected(boolean isSelected) {
		for (StringPairCell<T, Inner> cell : this.cells) {
			cell.updateSelected(isSelected && this.getListener() == cell && this.expanded);
		}
	}
	
	@Override
	public StringPairListEntry<T, Inner> self() {
		return this;
	}
	
	protected Map<String, T> toMap(Stream<Pair<String, T>> stream) {
		return stream.collect(Collectors.toMap(Pair::getKey, Pair::getValue, (a, b) -> b));
	}
	
	@Override
	public boolean isRequiresRestart() {
		return super.isRequiresRestart();
	}
	
	@Override
	public boolean isEdited() {
		return !ignoreOrder ? super.isEdited() :
		       !cells.stream().map(c -> c.getValue().getKey()).collect(Collectors.toSet())
		         .equals(original.stream().map(Pair::getKey).collect(Collectors.toSet()))
		       || cells.stream().anyMatch(StringPairCell::isEdited);
		       // !cells.stream().map(StringPairCell::getValue).collect(
		       //   Collectors.toSet()).equals(new HashSet<>(original));
	}
	
	@Override
	public Optional<ITextComponent> getError() {
		// This is preferable to just displaying "Multiple issues!" without further info
		// The names already turn red on each error anyways
		final Optional<ITextComponent> e = this.cells.stream().map(BaseListCell::getConfigError)
		  .filter(Optional::isPresent).map(Optional::get).findFirst();
		if (e.isPresent())
			return e;
		return errorSupplier.apply(getValue());
	}
}
