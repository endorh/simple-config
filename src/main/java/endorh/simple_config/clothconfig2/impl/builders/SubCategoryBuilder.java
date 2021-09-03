package endorh.simple_config.clothconfig2.impl.builders;

import com.google.common.collect.Lists;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.api.IChildListEntry;
import endorh.simple_config.clothconfig2.gui.entries.SubCategoryListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@OnlyIn(value = Dist.CLIENT)
public class SubCategoryBuilder
  extends FieldBuilder<Void, SubCategoryListEntry, SubCategoryBuilder>
  implements List<AbstractConfigListEntry<?>> {
	protected List<AbstractConfigListEntry<?>> entries = Lists.newArrayList();
	protected boolean expanded = false;
	protected @Nullable AbstractConfigListEntry<?> heldEntry = null;
	
	public SubCategoryBuilder(ConfigEntryBuilder builder, ITextComponent name) {
		super(builder, name, null);
	}
	
	public SubCategoryBuilder setExpanded(boolean expanded) {
		this.expanded = expanded;
		return this;
	}
	
	public <H, E extends AbstractConfigListEntry<H> & IChildListEntry> SubCategoryBuilder setHeldEntry(
	  E heldEntry
	) {
		this.heldEntry = heldEntry;
		return this;
	}
	
	protected <E extends AbstractConfigListEntry<?> & IChildListEntry> E getHeldEntry() {
		//noinspection unchecked
		return (E) heldEntry;
	}
	
	@Override @NotNull protected SubCategoryListEntry buildEntry() {
		final SubCategoryListEntry entry =
		  new SubCategoryListEntry(fieldNameKey, entries);
		entry.setExpanded(expanded);
		if (heldEntry != null)
			entry.setHeldEntry(getHeldEntry());
		return entry;
	}
	
	// List interface
	
	@Override public int size() {
		return entries.size();
	}
	
	@Override public boolean isEmpty() {
		return entries.isEmpty();
	}
	
	@Override public boolean contains(Object o) {
		return entries.contains(o);
	}
	
	@Override @NotNull public Iterator<AbstractConfigListEntry<?>> iterator() {
		return entries.iterator();
	}
	
	@Override public Object[] toArray() {
		return entries.toArray();
	}
	
	@Override public <T> T[] toArray(T @NotNull [] a) {
		//noinspection SuspiciousToArrayCall
		return entries.toArray(a);
	}
	
	@Override public boolean add(AbstractConfigListEntry entry) {
		return entries.add(entry);
	}
	
	@Override public boolean remove(Object o) {
		return entries.remove(o);
	}
	
	@Override public boolean containsAll(@NotNull Collection<?> c) {
		return entries.containsAll(c);
	}
	
	@Override public boolean addAll(@NotNull Collection<? extends AbstractConfigListEntry<?>> c) {
		return entries.addAll(c);
	}
	
	@Override public boolean addAll(int index, @NotNull Collection<? extends AbstractConfigListEntry<?>> c) {
		return entries.addAll(index, c);
	}
	
	@Override public boolean removeAll(@NotNull Collection<?> c) {
		return entries.removeAll(c);
	}
	
	@Override public boolean retainAll(@NotNull Collection<?> c) {
		return entries.retainAll(c);
	}
	
	@Override public void clear() {
		entries.clear();
	}
	
	@Override public AbstractConfigListEntry<?> get(int index) {
		return entries.get(index);
	}
	
	@Override public AbstractConfigListEntry<?> set(int index, AbstractConfigListEntry element) {
		return entries.set(index, element);
	}
	
	@Override public void add(int index, AbstractConfigListEntry element) {
		entries.add(index, element);
	}
	
	@Override public AbstractConfigListEntry<?> remove(int index) {
		return entries.remove(index);
	}
	
	@Override public int indexOf(Object o) {
		return entries.indexOf(o);
	}
	
	@Override public int lastIndexOf(Object o) {
		return entries.lastIndexOf(o);
	}
	
	@Override @NotNull public ListIterator<AbstractConfigListEntry<?>> listIterator() {
		return entries.listIterator();
	}
	
	@Override @NotNull public ListIterator<AbstractConfigListEntry<?>> listIterator(int index) {
		return entries.listIterator(index);
	}
	
	@Override @NotNull public List<AbstractConfigListEntry<?>> subList(int fromIndex, int toIndex) {
		return entries.subList(fromIndex, toIndex);
	}
}

