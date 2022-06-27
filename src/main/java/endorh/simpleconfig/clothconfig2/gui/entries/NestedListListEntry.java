package endorh.simpleconfig.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.clothconfig2.api.AbstractConfigEntry;
import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.IEntryHolder;
import endorh.simpleconfig.clothconfig2.api.IExpandable;
import endorh.simpleconfig.clothconfig2.gui.INavigableTarget;
import endorh.simpleconfig.clothconfig2.gui.entries.NestedListListEntry.NestedListCell;
import endorh.simpleconfig.clothconfig2.impl.ISeekableComponent;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@OnlyIn(value = Dist.CLIENT)
public class NestedListListEntry<T, Inner extends AbstractConfigListEntry<T>>
  extends AbstractListListEntry<T, NestedListCell<T, Inner>, NestedListListEntry<T, Inner>>
  implements IEntryHolder {
	// protected final List<ReferenceProvider> referencableEntries = Lists.newArrayList();
	
	public NestedListListEntry(
	  ITextComponent fieldName, List<T> value,
	  Function<NestedListListEntry<T, Inner>, Inner> createInner
	) {
		super(fieldName, value,
		      l -> new NestedListListEntry.NestedListCell<>(l, createInner.apply(l)));
	}
	
	@Override public boolean areEqual(List<T> value, List<T> other) {
		if (value.isEmpty() && other.isEmpty()) return true;
		if (value.size() != other.size()) return false;
		Inner dummy = !cells.isEmpty() ? cells.get(0).nestedEntry
		                               : createCellWithValue(value.get(0)).nestedEntry;
		final Iterator<T> iter = other.iterator();
		for (T t : value) if (!dummy.areEqual(t, iter.next())) return false;
		return true;
	}
	
	@Internal public List<Inner> getEntries() {
		return cells.stream().map(c -> c.nestedEntry).collect(Collectors.toList());
	}
	
	@Override public List<AbstractConfigEntry<?>> getHeldEntries() {
		return cells.stream()
		  .map(c -> c.nestedEntry)
		  .collect(Collectors.toList());
	}
	
	@Override public String providePath(AbstractConfigEntry<?> child) {
		final String prefix = getPath() + ".";
		int i = 0;
		for (NestedListCell<T, Inner> cell : cells) {
			if (cell.nestedEntry == child) return prefix + i;
			i++;
		}
		return prefix + "?";
	}
	
	@Override public @Nullable AbstractConfigEntry<?> getEntry(String path) {
		String[] split = DOT.split(path, 2);
		if ("?".equals(split[0])) return null;
		try {
			int i = Integer.parseInt(split[0]);
			if (i >= 0 && i < cells.size()) {
				Inner nestedEntry = cells.get(i).nestedEntry;
				if (nestedEntry instanceof IEntryHolder && split.length == 2)
					return ((IEntryHolder) nestedEntry).getEntry(split[1]);
				return nestedEntry;
			}
		} catch (NumberFormatException ignored) {}
		return null;
	}
	
	public static class NestedListCell<T, Inner extends AbstractConfigListEntry<T>>
	  extends AbstractListListEntry.AbstractListCell<
	    T, NestedListListEntry.NestedListCell<T, Inner>, NestedListListEntry<T, Inner>
	  > /*implements IExpandable*/ {
		protected final Inner nestedEntry;
		protected final boolean isExpandable;
		
		public NestedListCell(
		  NestedListListEntry<T, Inner> listEntry, Inner nestedEntry
		) {
			super(listEntry);
			this.nestedEntry = nestedEntry;
			this.isExpandable = nestedEntry instanceof IExpandable;
			nestedEntry.setSubEntry(true);
			if (!(nestedEntry instanceof IExpandable))
				nestedEntry.setHeadless(true);
			nestedEntry.setParentEntry(listEntry);
			nestedEntry.setNavigableParent(this);
		}
		
		public T getValue() {
			return nestedEntry.getDisplayedValue();
		}
		
		@Override public List<EntryError> getErrors() {
			List<EntryError> errors = super.getErrors();
			errors.addAll(
			  nestedEntry.getErrors().stream()
			    .filter(e -> !errors.contains(e))
			    .collect(Collectors.toList()));
			return errors;
		}
		
		public Optional<ITextComponent> getErrorMessage() {
			return Optional.empty();
		}
		
		public int getCellHeight() {
			return nestedEntry.getItemHeight();
		}
		
		@Override public Rectangle getSelectionArea() {
			return nestedEntry.getSelectionArea();
		}
		
		// @Override public boolean isExpanded() {
		// 	return isExpandable && ((IExpandable) nestedEntry).isExpanded();
		// }
		//
		// @Override public void setExpanded(boolean expanded, boolean recursive) {
		// 	if (isExpandable)
		// 		((IExpandable) nestedEntry).setExpanded(expanded, recursive);
		// }
		//
		// @Override public int getFocusedScroll() {
		// 	return isExpandable ? ((IExpandable) nestedEntry).getFocusedScroll() : 0;
		// }
		//
		// @Override public int getFocusedHeight() {
		// 	return isExpandable ? ((IExpandable) nestedEntry).getFocusedHeight() : getCellHeight();
		// }
		
		@Override public boolean drawsLine(int mouseX, int mouseY) {
			if (!isExpandable) return false;
			if (nestedEntry instanceof IExpandable)
				return ((IExpandable) nestedEntry).isExpanded() && mouseY > 18;
			return false;
		}
		
		public void renderCell(
		  MatrixStack mStack, int index, int x, int y, int cellWidth, int cellHeight,
		  int mouseX, int mouseY, boolean isSelected, float delta
		) {
			super.renderCell(mStack, index, x, y, cellWidth, cellHeight, mouseX, mouseY, isSelected, delta);
			nestedEntry.render(mStack, index, x, y, cellWidth, cellHeight, mouseX, mouseY, isSelected, delta);
		}
		
		@Override public void renderLabel(
		  MatrixStack mStack, ITextComponent label, int textX, int index, int x, int y, int cellWidth,
		  int cellHeight, int mouseX, int mouseY, boolean isSelected, float delta
		) {
			super.renderLabel(
			  mStack, label, textX, index, x, y,
			  cellWidth, nestedEntry.getCaptionHeight(), mouseX, mouseY, isSelected, delta);
		}
		
		public @NotNull List<? extends IGuiEventListener> children() {
			return Collections.singletonList(nestedEntry);
		}
		
		public boolean isRequiresRestart() {
			return nestedEntry.isRequiresRestart();
		}
		
		public void updateSelected(boolean isSelected) {
			super.updateSelected(isSelected);
			nestedEntry.updateFocused(isSelected);
		}
		
		public boolean isEdited() {
			return super.isEdited() || nestedEntry.isEdited();
		}
		
		public Inner getNestedEntry() {
			return nestedEntry;
		}
		
		public void doSetValue(T value) {
			nestedEntry.setDisplayedValue(value);
			nestedEntry.setValue(value);
		}
		
		@Override public void setOriginal(T value) {
			nestedEntry.setOriginal(value);
		}
		
		@Override public boolean areEqual(T left, T right) {
			return nestedEntry.areEqual(left, right);
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
		
		@Override public List<INavigableTarget> getNavigableSubTargets() {
			List<INavigableTarget> subTargets = nestedEntry.getNavigableSubTargets();
			return subTargets.isEmpty()? Lists.newArrayList(nestedEntry) : subTargets;
		}
		
		@Override public @Nullable INavigableTarget getNavigableParent() {
			return getListEntry();
		}
		
		@Override public List<INavigableTarget> getNavigableChildren(boolean onlyVisible) {
			return nestedEntry.getNavigableChildren(onlyVisible);
		}
		
		@Override public void navigate() {
			nestedEntry.navigate();
		}
		
		@Override public Rectangle getRowArea() {
			return nestedEntry.getRowArea();
		}
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
	
	@Override public boolean preserveState() {
		return false;
	}
	
	@Override public void setFocused(IGuiEventListener listener) {
		if (children().contains(listener)) {
			super.setFocused(listener);
		} else {
			for (NestedListCell<T, Inner> cell : cells) {
				if (cell.nestedEntry == listener) {
					super.setFocused(cell);
					cell.setFocused(listener);
					break;
				}
			}
		}
	}
}
