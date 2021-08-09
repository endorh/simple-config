package endorh.simple_config.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.core.EntrySetterUtil;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ReferenceProvider;
import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry.AbstractListCell;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Reimplementation of {@link me.shedaniel.clothconfig2.gui.entries.NestedListListEntry.NestedListCell}
 * for {@link NestedListEntry}
 * @param <T> Value held by the entry
 * @param <Inner> Cell type
 */
@SuppressWarnings("UnstableApiUsage")
public class NestedListCell<T, Inner extends AbstractConfigListEntry<T>>
  extends AbstractListCell<T, NestedListCell<T, Inner>, NestedListEntry<T, Inner>>
  implements ReferenceProvider<T> {
	protected final Inner nestedEntry;
	
	public NestedListCell(
	  @Nullable T value, NestedListEntry<T, Inner> listListEntry, Inner nestedEntry
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
		nestedEntry.setParent(((NestedListEntry) listListEntry).getParent());
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
		EntrySetterUtil.setValue(nestedEntry, value);
	}
	
	public void onAdd() {
		super.onAdd();
		listListEntry.onAdd(nestedEntry);
	}
	
	public void onDelete() {
		super.onDelete();
		listListEntry.onDelete(nestedEntry);
	}
}
