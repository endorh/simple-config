package endorh.simpleconfig.ui.impl.builders;

import com.google.common.collect.Lists;
import endorh.simpleconfig.ui.api.AbstractConfigEntry;
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

@OnlyIn(value = Dist.CLIENT)
public class CaptionedSubCategoryBuilder<T, HE extends AbstractConfigEntry<T> & IChildListEntry>
  extends FieldBuilder<T, CaptionedSubCategoryListEntry<T, HE>, CaptionedSubCategoryBuilder<T, HE>>
  implements List<AbstractConfigListEntry<?>> {
	protected List<AbstractConfigListEntry<?>> entries = Lists.newArrayList();
	protected boolean expanded = false;
	protected @Nullable HE captionEntry;
	
	public CaptionedSubCategoryBuilder(
	  ConfigEntryBuilder builder, ITextComponent name, @Nullable HE captionEntry
	) {
		super(builder, name, captionEntry != null? captionEntry.getValue() : null);
		this.captionEntry = captionEntry;
	}
	
	public CaptionedSubCategoryBuilder<T, HE> setExpanded(boolean expanded) {
		this.expanded = expanded;
		return this;
	}
	
	protected @Nullable HE getCaptionEntry() {
		return captionEntry;
	}
	
	@Override @NotNull protected CaptionedSubCategoryListEntry<T, HE> buildEntry() {
		return new CaptionedSubCategoryListEntry<>(fieldNameKey, entries, captionEntry);
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
	
	@Override @NotNull public Iterator<AbstractConfigListEntry<?>> iterator() {
		return entries.iterator();
	}
	
	@Override public Object[] toArray() {
		return entries.toArray();
	}
	
	@Override public <A> A[] toArray(A @NotNull [] a) {
		//noinspection SuspiciousToArrayCall
		return entries.toArray(a);
	}
	
	@Override public boolean add(AbstractConfigListEntry entry) {
		return entries.add(entry);
	}
	
	@Override public boolean remove(Object o) {
		return entries.remove(o);
	}
	
	@SuppressWarnings("SlowListContainsAll")
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

