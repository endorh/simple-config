package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.api.AbstractConfigEntry;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.IExpandable;
import endorh.simple_config.clothconfig2.api.ReferenceProvider;
import endorh.simple_config.clothconfig2.gui.AbstractConfigScreen;
import endorh.simple_config.clothconfig2.gui.INavigableTarget;
import endorh.simple_config.clothconfig2.gui.entries.NestedListListEntry.NestedListCell;
import endorh.simple_config.clothconfig2.gui.widget.DynamicEntryListWidget;
import endorh.simple_config.clothconfig2.impl.ISeekableComponent;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@OnlyIn(value = Dist.CLIENT)
public class NestedListListEntry<T, Inner extends AbstractConfigListEntry<T>>
  extends AbstractListListEntry<T, NestedListCell<T, Inner>, NestedListListEntry<T, Inner>> {
	// protected final List<ReferenceProvider> referencableEntries = Lists.newArrayList();
	
	public NestedListListEntry(
	  ITextComponent fieldName, List<T> value,
	  Function<NestedListListEntry<T, Inner>, Inner> createInner
	) {
		super(fieldName, value,
		      l -> new NestedListListEntry.NestedListCell<>(l, createInner.apply(l)));
	}
	
	@Override public boolean preserveState() {
		if (cells.isEmpty() || !cells.get(0).isExpandable || heldEntry != null && getListener() == heldEntry)
			return super.preserveState();
		else if (preservedState != null) savePreservedState();
		return false;
	}
	
	@Override public void setParent(DynamicEntryListWidget<?> parent) {
		super.setParent(parent);
		for (NestedListCell<T, Inner> c : cells) c.nestedEntry.setParent(parent);
	}
	
	@Override public void setScreen(AbstractConfigScreen screen) {
		super.setScreen(screen);
		for (NestedListCell<T, Inner> c : cells) c.nestedEntry.setScreen(screen);
	}
	
	@Internal public List<Inner> getEntries() {
		return cells.stream().map(c -> c.nestedEntry).collect(Collectors.toList());
	}
	
	public static class NestedListCell<T, Inner extends AbstractConfigListEntry<T>>
	  extends AbstractListListEntry.AbstractListCell<T, NestedListListEntry.NestedListCell<T,
	  Inner>, NestedListListEntry<T, Inner>>
	  implements ReferenceProvider, IExpandable {
		protected final Inner nestedEntry;
		protected final boolean isExpandable;
		
		public NestedListCell(
		  NestedListListEntry<T, Inner> listEntry, Inner nestedEntry
		) {
			super(listEntry);
			this.nestedEntry = nestedEntry;
			this.isExpandable = nestedEntry instanceof IExpandable;
			nestedEntry.setParent(listEntry.getParentOrNull());
			nestedEntry.setScreen(listEntry.getConfigScreenOrNull());
			nestedEntry.setExpandableParent(listEntry);
			nestedEntry.setNavigableParent(this);
			nestedEntry.setListParent(listEntry);
		}
		
		@NotNull public AbstractConfigEntry<?> provideReferenceEntry() {
			return nestedEntry;
		}
		
		public T getValue() {
			return nestedEntry.getValue();
		}
		
		public Optional<ITextComponent> getErrorMessage() {
			return nestedEntry.getErrorMessage();
		}
		
		public int getCellHeight() {
			return nestedEntry.getItemHeight();
		}
		
		@Override public boolean isExpanded() {
			return isExpandable && ((IExpandable) nestedEntry).isExpanded();
		}
		
		@Override public void setExpanded(boolean expanded, boolean recursive) {
			if (isExpandable)
				((IExpandable) nestedEntry).setExpanded(expanded, recursive);
		}
		
		@Override public int getFocusedScroll() {
			return isExpandable ? ((IExpandable) nestedEntry).getFocusedScroll() : 0;
		}
		
		@Override public int getFocusedHeight() {
			return isExpandable ? ((IExpandable) nestedEntry).getFocusedHeight() : getCellHeight();
		}
		
		@Override public boolean drawsLine(int mouseX, int mouseY) {
			if (!isExpandable) return false;
			if (nestedEntry instanceof IExpandable)
				return ((IExpandable) nestedEntry).isExpanded() && mouseY > 18;
			return false;
		}
		
		public void render(
		  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight,
		  int mouseX, int mouseY, boolean isSelected, float delta
		) {
			super.render(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
			nestedEntry.render(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
		}
		
		public @NotNull List<? extends IGuiEventListener> getEventListeners() {
			return Collections.singletonList(nestedEntry);
		}
		
		public boolean isRequiresRestart() {
			return nestedEntry.isRequiresRestart();
		}
		
		public void updateSelected(boolean isSelected) {
			super.updateSelected(isSelected);
			nestedEntry.updateSelected(isSelected);
		}
		
		public boolean isEdited() {
			return super.isEdited() || nestedEntry.isEdited();
		}
		
		public Inner getNestedEntry() {
			return nestedEntry;
		}
		
		public void doSetValue(T value) {
			nestedEntry.setValue(value);
		}
		
		@Override public void setOriginal(T value) {
			nestedEntry.setOriginal(value);
		}
		
		@Override protected String seekableText() {
			return "";
		}
		
		@Override protected List<ISeekableComponent> seekableComponents() {
			return Lists.newArrayList(nestedEntry);
		}
		
		@Override public List<INavigableTarget> getNavigableChildren() {
			final List<INavigableTarget> children = nestedEntry.getNavigableChildren();
			children.remove(nestedEntry);
			children.add(0, this);
			return children;
		}
		
		@Override public @Nullable INavigableTarget getNavigableParent() {
			return getListEntry();
		}
		
		@Override public void onNavigate() {
			nestedEntry.onNavigate();
		}
	}
	
	@Override public String seekableValueText() {
		return "";
	}
	
	@Override public List<INavigableTarget> getNavigableChildren() {
		if (expanded) {
			final List<INavigableTarget> targets = cells.stream()
			  .flatMap(c -> c.getNavigableChildren().stream()).collect(Collectors.toList());
			targets.add(0, this);
			return targets;
		}
		return super.getNavigableChildren();
	}
	
	@Override public void setListener(IGuiEventListener listener) {
		if (getEventListeners().contains(listener)) {
			super.setListener(listener);
		} else {
			for (NestedListCell<T, Inner> cell : cells) {
				if (cell.nestedEntry == listener) {
					super.setListener(cell);
					cell.setListener(listener);
					break;
				}
			}
		}
	}
}
