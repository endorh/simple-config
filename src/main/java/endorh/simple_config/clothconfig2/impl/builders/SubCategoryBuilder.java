package endorh.simple_config.clothconfig2.impl.builders;

import com.google.common.collect.Lists;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.gui.entries.SubCategoryListEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class SubCategoryBuilder extends FieldBuilder<Object, SubCategoryListEntry> implements List<AbstractConfigListEntry<?>> {
   private final List<AbstractConfigListEntry<?>> entries = Lists.newArrayList();
   private Function<List<AbstractConfigListEntry<?>>, Optional<ITextComponent[]>> tooltipSupplier =
     (list) -> Optional.empty();
   private boolean expanded = false;

   public SubCategoryBuilder(ITextComponent resetButtonKey, ITextComponent fieldNameKey) {
      super(resetButtonKey, fieldNameKey);
   }

   public void requireRestart(boolean requireRestart) {
      throw new UnsupportedOperationException();
   }

   public SubCategoryBuilder setTooltipSupplier(Supplier<Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = (list) -> tooltipSupplier.get();
      return this;
   }

   public SubCategoryBuilder setTooltipSupplier(Function<List<AbstractConfigListEntry<?>>, Optional<ITextComponent[]>> tooltipSupplier) {
      this.tooltipSupplier = tooltipSupplier;
      return this;
   }

   public SubCategoryBuilder setTooltip(Optional<ITextComponent[]> tooltip) {
      this.tooltipSupplier = (list) -> tooltip;
      return this;
   }

   public SubCategoryBuilder setTooltip(ITextComponent... tooltip) {
      this.tooltipSupplier = (list) -> Optional.ofNullable(tooltip);
      return this;
   }

   public SubCategoryBuilder setExpanded(boolean expanded) {
      this.expanded = expanded;
      return this;
   }

   /** @deprecated */
   @Deprecated
   @ScheduledForRemoval
   public SubCategoryBuilder setExpended(boolean expanded) {
      return this.setExpanded(expanded);
   }

   @NotNull
   public SubCategoryListEntry build() {
      SubCategoryListEntry entry = new SubCategoryListEntry(this.getFieldNameKey(), this.entries, this.expanded);
      entry.setTooltipSupplier(() -> this.tooltipSupplier.apply(entry.getValue()));
      return entry;
   }

   public int size() {
      return this.entries.size();
   }

   public boolean isEmpty() {
      return this.entries.isEmpty();
   }

   public boolean contains(Object o) {
      return this.entries.contains(o);
   }

   @NotNull
   public Iterator<AbstractConfigListEntry<?>> iterator() {
      return this.entries.iterator();
   }

   public Object[] toArray() {
      return this.entries.toArray();
   }

   public <T> T[] toArray(T[] a) {
      return this.entries.toArray(a);
   }

   public boolean add(AbstractConfigListEntry abstractConfigListEntry) {
      return this.entries.add(abstractConfigListEntry);
   }

   public boolean remove(Object o) {
      return this.entries.remove(o);
   }

   public boolean containsAll(@NotNull Collection<?> c) {
      return this.entries.containsAll(c);
   }

   public boolean addAll(@NotNull Collection<? extends AbstractConfigListEntry<?>> c) {
      return this.entries.addAll(c);
   }

   public boolean addAll(int index, @NotNull Collection<? extends AbstractConfigListEntry<?>> c) {
      return this.entries.addAll(index, c);
   }

   public boolean removeAll(Collection<?> c) {
      return this.entries.removeAll(c);
   }

   public boolean retainAll(Collection<?> c) {
      return this.entries.retainAll(c);
   }

   public void clear() {
      this.entries.clear();
   }

   public AbstractConfigListEntry<?> get(int index) {
      return this.entries.get(index);
   }

   public AbstractConfigListEntry<?> set(int index, AbstractConfigListEntry element) {
      return this.entries.set(index, element);
   }

   public void add(int index, AbstractConfigListEntry element) {
      this.entries.add(index, element);
   }

   public AbstractConfigListEntry<?> remove(int index) {
      return this.entries.remove(index);
   }

   public int indexOf(Object o) {
      return this.entries.indexOf(o);
   }

   public int lastIndexOf(Object o) {
      return this.entries.lastIndexOf(o);
   }

   @NotNull
   public ListIterator<AbstractConfigListEntry<?>> listIterator() {
      return this.entries.listIterator();
   }

   @NotNull
   public ListIterator<AbstractConfigListEntry<?>> listIterator(int index) {
      return this.entries.listIterator(index);
   }

   @NotNull
   public List<AbstractConfigListEntry<?>> subList(int fromIndex, int toIndex) {
      return this.entries.subList(fromIndex, toIndex);
   }
}
