package endorh.simpleconfig.ui.impl.builders;

import com.google.common.collect.Lists;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.api.IChildListEntry;
import endorh.simpleconfig.ui.gui.entries.CaptionedSubCategoryListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

@OnlyIn(value = Dist.CLIENT)
public class CaptionedSubCategoryBuilder<
  T, HE extends AbstractConfigListEntry<T> & IChildListEntry,
  HEB extends FieldBuilder<T, HE, HEB>
> extends FieldBuilder<T, CaptionedSubCategoryListEntry<T, HE>, CaptionedSubCategoryBuilder<T, HE, HEB>>
  implements List<FieldBuilder<?, ?, ?>> {
	protected List<FieldBuilder<?, ?, ?>> entries = Lists.newArrayList();
	protected boolean expanded = false;
	protected @Nullable HEB captionEntry;
	
	public CaptionedSubCategoryBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, @Nullable HEB captionEntry
	) {
		super(CaptionedSubCategoryListEntry.class, builder, name,
		      captionEntry != null? captionEntry.value : null);
		this.captionEntry = captionEntry;
	}
	
	public CaptionedSubCategoryBuilder<T, HE, HEB> setExpanded(boolean expanded) {
		this.expanded = expanded;
		return this;
	}
	
	protected @Nullable HEB getCaptionEntry() {
		return captionEntry;
	}
	
	@Override @NotNull protected CaptionedSubCategoryListEntry<T, HE> buildEntry() {
		List<AbstractConfigListEntry<?>> builtEntries = entries.stream()
		  .map(FieldBuilder::build)
		  .collect(Collectors.toList());
		return new CaptionedSubCategoryListEntry<>(
		  fieldNameKey, builtEntries, captionEntry != null? captionEntry.build() : null);
	}
	
	@Override public @NotNull CaptionedSubCategoryListEntry<T, HE> build() {
		final CaptionedSubCategoryListEntry<T, HE> entry = super.build();
		entry.setExpanded(expanded);
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
	
	@Override @NotNull public Iterator<FieldBuilder<?, ?, ?>> iterator() {
		return entries.iterator();
	}
	
	@Override public Object[] toArray() {
		return entries.toArray();
	}
	
	@Override public <A> A[] toArray(A @NotNull [] a) {
		//noinspection SuspiciousToArrayCall
		return entries.toArray(a);
	}
	
	@Override public boolean add(FieldBuilder<?, ?, ?> entry) {
		return entries.add(entry);
	}
	
	@Override public boolean remove(Object o) {
		return entries.remove(o);
	}
	
	@SuppressWarnings("SlowListContainsAll")
	@Override public boolean containsAll(@NotNull Collection<?> c) {
		return entries.containsAll(c);
	}
	
	@Override public boolean addAll(@NotNull Collection<? extends FieldBuilder<?, ?, ?>> c) {
		return entries.addAll(c);
	}
	
	@Override public boolean addAll(int index, @NotNull Collection<? extends FieldBuilder<?, ?, ?>> c) {
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
	
	@Override public FieldBuilder<?, ?, ?> get(int index) {
		return entries.get(index);
	}
	
	@Override public FieldBuilder<?, ?, ?> set(int index, FieldBuilder<?, ?, ?> element) {
		return entries.set(index, element);
	}
	
	@Override public void add(int index, FieldBuilder<?, ?, ?> element) {
		entries.add(index, element);
	}
	
	@Override public FieldBuilder<?, ?, ?> remove(int index) {
		return entries.remove(index);
	}
	
	@Override public int indexOf(Object o) {
		return entries.indexOf(o);
	}
	
	@Override public int lastIndexOf(Object o) {
		return entries.lastIndexOf(o);
	}
	
	@Override @NotNull public ListIterator<FieldBuilder<?, ?, ?>> listIterator() {
		return entries.listIterator();
	}
	
	@Override @NotNull public ListIterator<FieldBuilder<?, ?, ?>> listIterator(int index) {
		return entries.listIterator(index);
	}
	
	@Override @NotNull public List<FieldBuilder<?, ?, ?>> subList(int fromIndex, int toIndex) {
		return entries.subList(fromIndex, toIndex);
	}
}

