package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.api.AbstractConfigEntry;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ReferenceProvider;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public final class NestedListListEntry<T, INNER extends AbstractConfigListEntry<T>> extends AbstractListListEntry<T, NestedListListEntry.NestedListCell<T, INNER>, NestedListListEntry<T, INNER>> {
   private final List<ReferenceProvider<?>> referencableEntries = Lists.newArrayList();

   @Internal
   public NestedListListEntry(ITextComponent fieldName, List<T> value, boolean defaultExpanded, Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<T>> saveConsumer, Supplier<List<T>> defaultValue, ITextComponent resetButtonKey, boolean deleteButtonEnabled, boolean insertInFront, BiFunction<T, NestedListListEntry<T, INNER>, INNER> createNewCell) {
      super(fieldName, value, defaultExpanded, null, null, defaultValue, resetButtonKey, false, deleteButtonEnabled, insertInFront, (t, nestedListListEntry) -> {
         return new NestedListListEntry.NestedListCell(t, nestedListListEntry,
                                                       createNewCell.apply(t, nestedListListEntry));
      });
      Iterator var11 = this.cells.iterator();

      while(var11.hasNext()) {
         NestedListListEntry.NestedListCell<T, INNER> cell = (NestedListListEntry.NestedListCell)var11.next();
         this.referencableEntries.add(cell.nestedEntry);
      }

      this.setReferenceProviderEntries(this.referencableEntries);
   }

   public NestedListListEntry<T, INNER> self() {
      return this;
   }

   public static class NestedListCell<T, INNER extends AbstractConfigListEntry<T>> extends AbstractListListEntry.AbstractListCell<T, NestedListListEntry.NestedListCell<T, INNER>, NestedListListEntry<T, INNER>> implements ReferenceProvider<T> {
      private final INNER nestedEntry;

      @Internal
      public NestedListCell(@Nullable T value, NestedListListEntry<T, INNER> listListEntry, INNER nestedEntry) {
         super(value, listListEntry);
         this.nestedEntry = nestedEntry;
      }

      @NotNull
      public AbstractConfigEntry<T> provideReferenceEntry() {
         return this.nestedEntry;
      }

      public T getValue() {
         return this.nestedEntry.getValue();
      }

      public Optional<ITextComponent> getError() {
         return this.nestedEntry.getError();
      }

      public int getCellHeight() {
         return this.nestedEntry.getItemHeight();
      }

      public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
         this.nestedEntry.setParent(((NestedListListEntry)this.listListEntry).getParent());
         this.nestedEntry.setScreen(this.listListEntry.getConfigScreen());
         this.nestedEntry.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
      }

      public List<? extends IGuiEventListener> getEventListeners() {
         return Collections.singletonList(this.nestedEntry);
      }

      public boolean isRequiresRestart() {
         return this.nestedEntry.isRequiresRestart();
      }

      public void updateSelected(boolean isSelected) {
         this.nestedEntry.updateSelected(isSelected);
      }

      public boolean isEdited() {
         return super.isEdited() || this.nestedEntry.isEdited();
      }

      public void onAdd() {
         super.onAdd();
         this.listListEntry.referencableEntries.add(this.nestedEntry);
         this.listListEntry.requestReferenceRebuilding();
      }

      public void onDelete() {
         super.onDelete();
         this.listListEntry.referencableEntries.remove(this.nestedEntry);
         this.listListEntry.requestReferenceRebuilding();
      }
   }
}
