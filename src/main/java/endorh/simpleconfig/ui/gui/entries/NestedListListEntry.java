package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.gui.entries.NestedListListEntry.NestedListCell;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@OnlyIn(value = Dist.CLIENT)
public class NestedListListEntry<T, Inner extends AbstractConfigListEntry<T>>
  extends AbstractListListEntry<T, NestedListCell<T, Inner>, NestedListListEntry<T, Inner>>
  implements IEntryHolder {
	protected boolean ignoreOrder;
	
	public NestedListListEntry(
	  Component fieldName, List<T> value,
	  Function<NestedListListEntry<T, Inner>, Inner> createInner,
	  boolean ignoreOrder
	) {
		super(fieldName, value,
		      l -> new NestedListListEntry.NestedListCell<>(l, createInner.apply(l)));
		this.ignoreOrder = ignoreOrder;
	}
	
	@Override public boolean areEqual(List<T> value, List<T> other) {
		if (value == null || other == null) return value == other;
		if (value.isEmpty() && other.isEmpty()) return true;
		if (value.size() != other.size()) return false;
		Inner dummy =
		  !cells.isEmpty()? cells.get(0).nestedEntry : createCellWithValue(value.get(0)).nestedEntry;
		if (ignoreOrder) {
			// We cannot convert to set directly, because we must use the `areEquals` method of the
			//   inner entry rather than the `equals` method.
			// Creating a container class that wraps the value and implements `equals` is not a
			//   solution, since we'd have no way to implement `hashCode`.
			List<T> o = new ArrayList<>(other);
			check:for (T t: value) {
				Iterator<T> iter = o.iterator();
				while (iter.hasNext()) {
					T e = iter.next();
					if (dummy.areEqual(t, e)) {
						iter.remove();
						continue check;
					}
				}
				return false;
			}
		} else {
			final Iterator<T> iter = other.iterator();
			for (T t: value) if (!dummy.areEqual(t, iter.next())) return false;
		}
		return true;
	}
	
	@Internal public List<Inner> getEntries() {
		return cells.stream().map(c -> c.nestedEntry).collect(Collectors.toList());
	}
	
	@Override public List<AbstractConfigField<?>> getHeldEntries() {
		return cells.stream()
		  .map(c -> c.nestedEntry)
		  .collect(Collectors.toList());
	}
	
	@Override public String providePath(AbstractConfigField<?> child) {
		final String prefix = getCatPath() + ".";
		int i = 0;
		for (NestedListCell<T, Inner> cell : cells) {
			if (cell.nestedEntry == child) return prefix + i;
			i++;
		}
		return prefix + "?";
	}
	
	@Override public @Nullable AbstractConfigField<?> getEntry(String path) {
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
	  > {
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
		
		@Override public void tick() {
			nestedEntry.tick();
			super.tick();
		}
		
		@Override public T getValue() {
			return nestedEntry.getDisplayedValue();
		}
		
		@Override protected List<EntryError> computeErrors() {
			List<EntryError> errors = super.computeErrors();
			errors.addAll(
			  nestedEntry.getEntryErrors().stream()
				 .filter(e -> !errors.contains(e)).toList());
			return errors;
		}
		
		@Override public Optional<Component> getErrorMessage() {
			return Optional.empty();
		}
		
		@Override public int getCellHeight() {
			return nestedEntry.getItemHeight();
		}
		
		@Override public Rectangle getSelectionArea() {
			return nestedEntry.getSelectionArea();
		}
		
		@Override public boolean drawsLine(int mouseX, int mouseY) {
			if (!isExpandable) return false;
			if (nestedEntry instanceof IExpandable)
				return ((IExpandable) nestedEntry).isExpanded() && mouseY > 18;
			return false;
		}
		
		@Override public void renderCell(
		  PoseStack mStack, int index, int x, int y, int cellWidth, int cellHeight,
		  int mouseX, int mouseY, boolean isSelected, float delta
		) {
			super.renderCell(mStack, index, x, y, cellWidth, cellHeight, mouseX, mouseY, isSelected, delta);
			nestedEntry.render(mStack, index, x, y, cellWidth, cellHeight, mouseX, mouseY, isSelected, delta);
		}
		
		@Override public void renderLabel(
		  PoseStack mStack, Component label, int textX, int index, int x, int y, int cellWidth,
		  int cellHeight, int mouseX, int mouseY, boolean isSelected, float delta
		) {
			super.renderLabel(
			  mStack, label, textX, index, x, y,
			  cellWidth, nestedEntry.getCaptionHeight(), mouseX, mouseY, isSelected, delta);
		}
		
		@Override public @NotNull List<? extends GuiEventListener> children() {
			return Collections.singletonList(nestedEntry);
		}
		
		@Override public boolean isRequiresRestart() {
			return nestedEntry.isRequiresRestart();
		}
		
		@Override public void updateSelected(boolean isSelected) {
			super.updateSelected(isSelected);
			nestedEntry.updateFocused(isSelected);
		}
		
		@Override protected boolean computeIsEdited() {
			return super.computeIsEdited() || nestedEntry.isEdited();
		}
		
		public Inner getNestedEntry() {
			return nestedEntry;
		}
		
		@Override public void doSetValue(T value) {
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
		
		@Override public List<INavigableTarget> getNavigableSubTargets() {
			List<INavigableTarget> subTargets = nestedEntry.getNavigableSubTargets();
			return subTargets.isEmpty()? Lists.newArrayList(nestedEntry) : subTargets;
		}
		
		@Override public @Nullable INavigableTarget getNavigableParent() {
			return getListEntry();
		}
		
		@Override public List<INavigableTarget> getNavigableChildren(boolean onlyVisible) {
			if (!onlyVisible) {
				return Stream.concat(
				  Stream.of(nestedEntry), nestedEntry.getNavigableChildren(false).stream()
				).collect(Collectors.toList());
			}
			return nestedEntry.getNavigableChildren(true);
		}
		
		@Override public void navigate() {
			nestedEntry.navigate();
		}
		
		@Override public Rectangle getRowArea() {
			return nestedEntry.getRowArea();
		}
	}
	
	@Override public boolean preserveState() {
		return false;
	}
	
	@Override public void setFocused(GuiEventListener listener) {
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
