package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.api.AbstractConfigEntry;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ReferenceProvider;
import endorh.simple_config.clothconfig2.gui.entries.NestedListListEntry.NestedListCell;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class NestedListListEntry<T, Inner extends AbstractConfigListEntry<T>>
  extends AbstractListListEntry<T, NestedListCell<T, Inner>, NestedListListEntry<T, Inner>> {
	protected final List<ReferenceProvider<?>> referencableEntries = Lists.newArrayList();
	
	public NestedListListEntry(
	  ITextComponent fieldName, List<T> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<T>> saveConsumer,
	  Supplier<List<T>> defaultValue, ITextComponent resetButtonKey, boolean deleteButtonEnabled,
	  boolean insertInFront, BiFunction<T, NestedListListEntry<T, Inner>, Inner> createNewCell
	) {
		super(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, false, deleteButtonEnabled, insertInFront,
		  (t, NestedListListEntry) -> new NestedListListEntry.NestedListCell<>(
			 t, NestedListListEntry, createNewCell.apply(t, NestedListListEntry)));
		
		for (NestedListCell<T, Inner> cell : cells)
			referencableEntries.add(cell.nestedEntry);
		setReferenceProviderEntries(referencableEntries);
	}
	
	@Override public boolean isEdited() {
		return !getValue().equals(original);
		// return super.isEdited();
	}
	
	public static class NestedListCell<T, Inner extends AbstractConfigListEntry<T>>
	  extends AbstractListListEntry.AbstractListCell<T, NestedListListEntry.NestedListCell<T,
	  Inner>, NestedListListEntry<T, Inner>>
	  implements ReferenceProvider<T> {
		protected final Inner nestedEntry;
		
		public NestedListCell(
		  @Nullable T value, NestedListListEntry<T, Inner> listListEntry, Inner nestedEntry
		) {
			super(value, listListEntry);
			this.nestedEntry = nestedEntry;
		}
		
		@NotNull public AbstractConfigEntry<T> provideReferenceEntry() {
			return nestedEntry;
		}
		
		public T getValue() {
			return nestedEntry.getValue();
		}
		
		public Optional<ITextComponent> getError() {
			return nestedEntry.getError();
		}
		
		public int getCellHeight() {
			return nestedEntry.getItemHeight();
		}
		
		@SuppressWarnings({"unchecked", "rawtypes"}) public void render(
		  MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight,
		  int mouseX, int mouseY, boolean isSelected, float delta
		) {
			nestedEntry.setParent(((NestedListListEntry) listListEntry).getParent());
			nestedEntry.setScreen(listListEntry.getConfigScreen());
			
			nestedEntry.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
		}
		
		public @NotNull List<? extends IGuiEventListener> getEventListeners() {
			return Collections.singletonList(nestedEntry);
		}
		
		public boolean isRequiresRestart() {
			return nestedEntry.isRequiresRestart();
		}
		
		public void updateSelected(boolean isSelected) {
			nestedEntry.updateSelected(isSelected);
		}
		
		public boolean isEdited() {
			return super.isEdited() || nestedEntry.isEdited();
		}
		
		public Inner getNestedEntry() {
			return nestedEntry;
		}
		
		public void setValue(T value) {
			nestedEntry.setValue(value);
		}
		
		public void onAdd() {
			super.onAdd();
			listListEntry.referencableEntries.add(nestedEntry);
			listListEntry.requestReferenceRebuilding();
		}
		
		public void onDelete() {
			super.onDelete();
			listListEntry.referencableEntries.remove(nestedEntry);
			listListEntry.requestReferenceRebuilding();
		}
	}
}