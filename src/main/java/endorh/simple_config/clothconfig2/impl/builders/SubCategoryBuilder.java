package endorh.simple_config.clothconfig2.impl.builders;

import com.google.common.collect.Lists;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.gui.entries.SubCategoryListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class SubCategoryBuilder
  extends FieldBuilder<Object, SubCategoryListEntry>
  implements List<AbstractConfigListEntry<?>> {
	private final List<AbstractConfigListEntry<?>> entries;
	private Function<List<AbstractConfigListEntry<?>>, Optional<ITextComponent[]>> tooltipSupplier
	  = list -> Optional.empty();
	private boolean expanded = false;
	
	public SubCategoryBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey) {
		super(resetButtonKey, fieldNameKey);
		this.entries = Lists.newArrayList();
	}
	
	@Override
	public void requireRestart(boolean requireRestart) {
		throw new UnsupportedOperationException();
	}
	
	public SubCategoryBuilder setTooltipSupplier(
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = list -> tooltipSupplier.get();
		return this;
	}
	
	public SubCategoryBuilder setTooltipSupplier(
	  Function<List<AbstractConfigListEntry<?>>, Optional<ITextComponent[]>> tooltipSupplier
	) {
		this.tooltipSupplier = tooltipSupplier;
		return this;
	}
	
	public SubCategoryBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
		this.tooltipSupplier = list -> tooltip;
		return this;
	}
	
	public SubCategoryBuilder setTooltip(ITextComponent... tooltip) {
		this.tooltipSupplier = list -> Optional.ofNullable(tooltip);
		return this;
	}
	
	public SubCategoryBuilder setExpanded(boolean expanded) {
		this.expanded = expanded;
		return this;
	}
	
	@Deprecated
	@ApiStatus.ScheduledForRemoval
	public SubCategoryBuilder setExpended(boolean expanded) {
		return this.setExpanded(expanded);
	}
	
	@Override
	@NotNull
	public SubCategoryListEntry build() {
		SubCategoryListEntry entry =
		  new SubCategoryListEntry(this.getFieldNameKey(), this.entries, this.expanded);
		entry.setTooltipSupplier(() -> this.tooltipSupplier.apply(entry.getValue()));
		return entry;
	}
	
	@Override
	public int size() {
		return this.entries.size();
	}
	
	@Override
	public boolean isEmpty() {
		return this.entries.isEmpty();
	}
	
	@Override
	public boolean contains(Object o) {
		return this.entries.contains(o);
	}
	
	@Override
	@NotNull
	public Iterator<AbstractConfigListEntry<?>> iterator() {
		return this.entries.iterator();
	}
	
	@Override
	public Object[] toArray() {
		return this.entries.toArray();
	}
	
	@Override
	public <T> T[] toArray(T @NotNull [] a) {
		return this.entries.toArray(a);
	}
	
	@Override
	public boolean add(AbstractConfigListEntry abstractConfigListEntry) {
		return this.entries.add(abstractConfigListEntry);
	}
	
	@Override
	public boolean remove(Object o) {
		return this.entries.remove(o);
	}
	
	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		return this.entries.containsAll(c);
	}
	
	@Override
	public boolean addAll(@NotNull Collection<? extends AbstractConfigListEntry<?>> c) {
		return this.entries.addAll(c);
	}
	
	@Override
	public boolean addAll(int index, @NotNull Collection<? extends AbstractConfigListEntry<?>> c) {
		return this.entries.addAll(index, c);
	}
	
	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		return this.entries.removeAll(c);
	}
	
	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		return this.entries.retainAll(c);
	}
	
	@Override
	public void clear() {
		this.entries.clear();
	}
	
	@Override
	public AbstractConfigListEntry<?> get(int index) {
		return this.entries.get(index);
	}
	
	@Override
	public AbstractConfigListEntry<?> set(int index, AbstractConfigListEntry element) {
		return this.entries.set(index, element);
	}
	
	@Override
	public void add(int index, AbstractConfigListEntry element) {
		this.entries.add(index, element);
	}
	
	@Override
	public AbstractConfigListEntry<?> remove(int index) {
		return this.entries.remove(index);
	}
	
	@Override
	public int indexOf(Object o) {
		return this.entries.indexOf(o);
	}
	
	@Override
	public int lastIndexOf(Object o) {
		return this.entries.lastIndexOf(o);
	}
	
	@Override
	@NotNull
	public ListIterator<AbstractConfigListEntry<?>> listIterator() {
		return this.entries.listIterator();
	}
	
	@Override
	@NotNull
	public ListIterator<AbstractConfigListEntry<?>> listIterator(int index) {
		return this.entries.listIterator(index);
	}
	
	@Override
	@NotNull
	public List<AbstractConfigListEntry<?>> subList(int fromIndex, int toIndex) {
		return this.entries.subList(fromIndex, toIndex);
	}
}

