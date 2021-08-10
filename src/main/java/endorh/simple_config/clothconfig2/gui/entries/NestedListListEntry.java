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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public final class NestedListListEntry<T, INNER extends AbstractConfigListEntry<T>>
  extends AbstractListListEntry<T, NestedListCell<T, INNER>, NestedListListEntry<T, INNER>> {
	private final List<ReferenceProvider<?>> referencableEntries = Lists.newArrayList();
	
	@ApiStatus.Internal
	public NestedListListEntry(
	  ITextComponent fieldName, List<T> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<T>> saveConsumer,
	  Supplier<List<T>> defaultValue, ITextComponent resetButtonKey, boolean deleteButtonEnabled,
	  boolean insertInFront, BiFunction<T, NestedListListEntry<T, INNER>, INNER> createNewCell
	) {
		super(fieldName, value, defaultExpanded, null, null, defaultValue, resetButtonKey, false,
		      deleteButtonEnabled, insertInFront,
		      (t, nestedListListEntry) -> new NestedListCell<>(
		        t, nestedListListEntry, createNewCell.apply(t, nestedListListEntry)));
		for (NestedListCell<T, INNER> cell : this.cells) {
			this.referencableEntries.add(cell.nestedEntry);
		}
		this.setReferenceProviderEntries(this.referencableEntries);
	}
	
	@Override
	public NestedListListEntry<T, INNER> self() {
		return this;
	}
	
	public static class NestedListCell<T, INNER extends AbstractConfigListEntry<T>>
	  extends
	  AbstractListListEntry.AbstractListCell<T, NestedListCell<T, INNER>, NestedListListEntry<T, INNER>>
	  implements ReferenceProvider<T> {
		private final INNER nestedEntry;
		
		@ApiStatus.Internal
		public NestedListCell(
		  @Nullable T value, NestedListListEntry<T, INNER> listListEntry, INNER nestedEntry
		) {
			super(value, listListEntry);
			this.nestedEntry = nestedEntry;
		}
		
		@Override
		@NotNull
		public AbstractConfigEntry<T> provideReferenceEntry() {
			return this.nestedEntry;
		}
		
		@Override
		public T getValue() {
			return this.nestedEntry.getValue();
		}
		
		@Override
		public Optional<ITextComponent> getError() {
			return this.nestedEntry.getError();
		}
		
		@Override
		public int getCellHeight() {
			return this.nestedEntry.getItemHeight();
		}
		
		@Override
		public void render(
		  MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
		  int mouseY, boolean isSelected, float delta
		) {
			this.nestedEntry.setParent(((NestedListListEntry) this.listListEntry).getParent());
			this.nestedEntry.setScreen(this.listListEntry.getConfigScreen());
			this.nestedEntry.render(
			  matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
		}
		
		public @NotNull List<? extends IGuiEventListener> getEventListeners() {
			return Collections.singletonList(this.nestedEntry);
		}
		
		@Override
		public boolean isRequiresRestart() {
			return this.nestedEntry.isRequiresRestart();
		}
		
		@Override
		public void updateSelected(boolean isSelected) {
			this.nestedEntry.updateSelected(isSelected);
		}
		
		@Override
		public boolean isEdited() {
			return super.isEdited() || this.nestedEntry.isEdited();
		}
		
		@Override
		public void onAdd() {
			super.onAdd();
			this.listListEntry.referencableEntries.add(this.nestedEntry);
			this.listListEntry.requestReferenceRebuilding();
		}
		
		@Override
		public void onDelete() {
			super.onDelete();
			this.listListEntry.referencableEntries.remove(this.nestedEntry);
			this.listListEntry.requestReferenceRebuilding();
		}
	}
}
