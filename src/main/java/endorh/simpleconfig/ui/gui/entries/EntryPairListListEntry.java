package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.entries.EntryPairListListEntry.EntryPairCell;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class EntryPairListListEntry<
    K, V, KE extends AbstractConfigListEntry<K> & IChildListEntry,
    E extends AbstractConfigListEntry<V>
  > extends AbstractListListEntry<
    Pair<K, V>, EntryPairCell<K, V, KE, E>, EntryPairListListEntry<K, V, KE, E>
  > implements IEntryHolder {
	protected boolean ignoreOrder;
	protected boolean selectKey;
	
	public EntryPairListListEntry(
	  ITextComponent fieldName, List<Pair<K, V>> value,
	  Function<EntryPairListListEntry<K, V, KE, E>, Pair<KE, E>> cellFactory,
	  boolean ignoreOrder
	) {
		super(
		  fieldName, value,
		  l -> {
			  final Pair<KE, E> pair = cellFactory.apply(l);
			  return new EntryPairCell<>(l, pair.getKey(), pair.getValue());
		  });
		this.ignoreOrder = ignoreOrder;
	}
	
	@Override public boolean preserveState() {
		return false;
	}
	
	@Override
	public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		for (EntryPairCell<K, V, KE, E> cell : cells)
			cell.updateSelected(isFocused && getListener() == cell && expanded);
	}
	
	@Override protected boolean isFieldFullWidth() {
		return true;
	}
	
	@Override public boolean areEqual(List<Pair<K, V>> value, List<Pair<K, V>> other) {
		if (value.isEmpty() && other.isEmpty()) return true;
		if (value.size() != other.size()) return false;
		EntryPairCell<K, V, KE, E> dummy = !cells.isEmpty() ? cells.get(0)
		                                                    : createCellWithValue(value.get(0));
		if (ignoreOrder) {
			// Keys are actually compared using Object::equals, but keys can only be String
			//   serializable objects anyway
			final Map<K, V> vMap = value.stream().collect(
			  HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
			final Map<K, V> oMap = other.stream().collect(
			  HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
			for (K key : vMap.keySet())
				if (!dummy.valueEntry.areEqual(vMap.get(key), oMap.get(key))) return false;
		} else {
			final Iterator<Pair<K, V>> iter = other.iterator();
			for (Pair<K, V> t : value)
				if (!dummy.areEqual(t, iter.next())) return false;
		}
		return true;
	}
	
	@Override public boolean isEdited() {
		return super.isEdited();
	}
	
	@Override public boolean isResettable() {
		return super.isResettable();
	}
	
	// @Override public boolean preserveState() {
	// 	if (cells.isEmpty() || !(cells.get(0).valueEntry instanceof BaseListEntry<?, ?, ?>))
	// 		return super.preserveState();
	// 	else if (preservedState != null) savePreservedState();
	// 	return false;
	// }
	
	@Internal public List<Pair<K, E>> getEntries() {
		return cells.stream().map(c -> Pair.of(c.keyEntry.getDisplayedValue(), c.valueEntry)).collect(Collectors.toList());
	}
	
	@Override public List<AbstractConfigEntry<?>> getHeldEntries() {
		return cells.stream()
		  .flatMap(c -> Stream.of(c.keyEntry, c.valueEntry))
		  .collect(Collectors.toList());
	}
	
	@Override public String providePath(AbstractConfigEntry<?> child) {
		String prefix = getRelPath() + ".";
		int i = 0;
		for (EntryPairCell<K, V, KE, E> cell : cells) {
			if (cell.keyEntry == child) return prefix + "key." + i;
			if (cell.valueEntry == child) return prefix + "val." + i;
			i++;
		}
		return prefix + "?";
	}
	
	@Override public @Nullable AbstractConfigEntry<?> getEntry(String path) {
		String[] split = DOT.split(path, 3);
		boolean isKey = "key".equals(split[0]);
		if (!isKey && !"val".equals(split[0]) || split.length < 2) return null;
		try {
			int i = Integer.parseInt(split[1]);
			if (i >= 0 && i < cells.size()) {
				EntryPairCell<K, V, KE, E> cell = cells.get(i);
				AbstractConfigEntry<?> entry = isKey? cell.keyEntry : cell.valueEntry;
				if (entry instanceof IEntryHolder && split.length == 3)
					return ((IEntryHolder) entry).getEntry(split[2]);
				return entry;
			}
		} catch (NumberFormatException ignored) {}
		return null;
	}
	
	public static class EntryPairCell<
	    K, V, KE extends AbstractConfigListEntry<K> & IChildListEntry,
	    E extends AbstractConfigListEntry<V>
	  > extends AbstractListListEntry.AbstractListCell<
	    Pair<K, V>, EntryPairCell<K, V, KE, E>, EntryPairListListEntry<K, V, KE, E>
	  > /*implements IExpandable*/ {
		protected final KE keyEntry;
		protected final E valueEntry;
		protected final List<IGuiEventListener> widgets;
		protected int keyOffset = 0;
		protected final boolean isExpandable;
		
		protected int noFilter = 0x00000000;
		protected int errorFilter = 0x64E04242;
		protected int keyOverlayColor = 0x00000000;
		
		public EntryPairCell(
		  final EntryPairListListEntry<K, V, KE, E> listEntry, final KE keyEntry, final E valueEntry
		) {
			super(listEntry);
			this.keyEntry = keyEntry;
			this.valueEntry = valueEntry;
			if (listEntry.areCaptionControlsEnabled()) keyOffset = 24;
			
			keyEntry.setChildSubEntry(true);
			keyEntry.setSubEntry(true);
			valueEntry.setSubEntry(true);
			keyEntry.setParentEntry(listEntry);
			valueEntry.setParentEntry(listEntry);
			keyEntry.setNavigableParent(this);
			valueEntry.setNavigableParent(this);
			
			isExpandable = valueEntry instanceof BaseListEntry;
			widgets = Lists.newArrayList(keyEntry, valueEntry);
		}
		
		@Override public Pair<K, V> getValue() {
			return Pair.of(keyEntry.getDisplayedValue(), valueEntry.getDisplayedValue());
		}
		
		@Override public Rectangle getSelectionArea() {
			return valueEntry.getSelectionArea();
		}
		
		@Override public int getCellHeight() {
			return valueEntry.getItemHeight();
		}
		
		@Override public void updateSelected(boolean isSelected) {
			super.updateSelected(isSelected);
			keyEntry.updateFocused(isSelected && getListener() == keyEntry);
			valueEntry.updateFocused(isSelected && getListener() == valueEntry);
			if (isSelected) getListEntry().selectKey = getListener() == keyEntry;
		}
		
		@Override public List<EntryError> getErrors() {
			List<EntryError> errors = super.getErrors();
			errors.addAll(
			  Stream.concat(keyEntry.getEntryErrors().stream(), valueEntry.getEntryErrors().stream())
			    .filter(e -> !errors.contains(e))
			    .collect(Collectors.toList()));
			return errors;
		}
		
		@Override public Optional<ITextComponent> getErrorMessage() {
			return Optional.empty();
		}
		
		@Override public void doSetValue(Pair<K, V> value) {
			keyEntry.setDisplayedValue(value.getKey());
			keyEntry.setValue(value.getKey());
			valueEntry.setDisplayedValue(value.getValue());
			valueEntry.setValue(value.getValue());
		}
		
		@Override public void setOriginal(Pair<K, V> value) {
			keyEntry.setOriginal(value.getKey());
			valueEntry.setOriginal(value.getValue());
		}
		
		@Override public boolean areEqual(Pair<K, V> left, Pair<K, V> right) {
			return keyEntry.areEqual(left.getKey(), right.getKey())
			  && valueEntry.areEqual(left.getValue(), right.getValue());
		}
		
		// @Override public boolean isExpanded() {
		// 	return isExpandable && ((IExpandable) valueEntry).isExpanded();
		// }
		//
		// @Override public void setExpanded(boolean expanded, boolean recursive) {
		// 	if (isExpandable)
		// 		((IExpandable) valueEntry).setExpanded(expanded, recursive);
		// }
		//
		// @Override public int getFocusedScroll() {
		// 	return isExpandable ? ((IExpandable) valueEntry).getFocusedScroll() : 0;
		// }
		//
		// @Override public int getFocusedHeight() {
		// 	return isExpandable ? ((IExpandable) valueEntry).getFocusedHeight() : getCellHeight();
		// }
		
		@Override
		public void renderCell(
		  MatrixStack mStack, int index, int x, int y, int cellWidth,
		  int cellHeight, int mouseX, int mouseY, boolean isHovered, float delta
		) {
			super.renderCell(mStack, index, x, y, cellWidth, cellHeight, mouseX, mouseY, isHovered, delta);
			keyOverlayColor = hasError() ? errorFilter : noFilter;
			final FontRenderer fr = Minecraft.getInstance().fontRenderer;
			int keyX = fr.getBidiFlag() ? x + cellWidth - 150 - keyOffset : x + keyOffset;
			valueEntry.render(mStack, index, x, y, cellWidth, cellHeight, mouseX, mouseY, isHovered, delta);
			final EntryPairListListEntry<K, V, KE, E> listEntry = getListEntry();
			keyEntry.renderChild(mStack, keyX, y, listEntry.getKeyFieldWidth(), 20, mouseX, mouseY, delta);
			fill(mStack, keyX, y, keyX + listEntry.getKeyFieldWidth(), y + 20, keyOverlayColor);
		}
		
		@Override public void renderLabel(
		  MatrixStack mStack, ITextComponent label, int textX, int index, int x, int y, int cellWidth,
		  int cellHeight, int mouseX, int mouseY, boolean isSelected, float delta
		) {}
		
		@Override public ITextComponent getLabel() {
			return StringTextComponent.EMPTY;
		}
		
		@Override public boolean drawsLine(int mouseX, int mouseY) {
			return isExpandable && ((BaseListEntry<?, ?, ?>) valueEntry).expanded && mouseY > 18;
		}
		
		@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
			return widgets;
		}
		
		@Override protected List<ISeekableComponent> seekableComponents() {
			return Lists.newArrayList(keyEntry, valueEntry);
		}
		
		@Override protected String seekableText() {
			return "";
		}
		
		@Override public List<INavigableTarget> getNavigableChildren() {
			final List<INavigableTarget> children = valueEntry.getNavigableChildren();
			children.remove(valueEntry);
			children.add(0, this);
			return children;
		}
		
		@Override public List<INavigableTarget> getNavigableSubTargets() {
			List<INavigableTarget> subTargets = new ArrayList<>();
			AbstractConfigEntry<?>[] arr = {isExpandable? valueEntry : keyEntry,
			                                isExpandable? keyEntry : valueEntry};
			for (AbstractConfigEntry<?> entry : arr) {
				List<INavigableTarget> entryTargets = entry.getNavigableSubTargets();
				if (entryTargets.isEmpty()) subTargets.add(entry);
				else subTargets.addAll(entryTargets);
			}
			return subTargets;
		}
		
		@Override public void navigate() {
			if (getListEntry().selectKey)
				keyEntry.navigate();
			else valueEntry.navigate();
		}
		
		@Override public Rectangle getRowArea() {
			return valueEntry.getRowArea();
		}
		
		@Override public @Nullable INavigableTarget getNavigableParent() {
			return getListEntry();
		}
		
		@Override public List<INavigableTarget> getNavigableChildren(boolean onlyVisible) {
			if (!onlyVisible) return Stream.concat(
			  Stream.of(keyEntry, valueEntry), valueEntry.getNavigableChildren().stream()
			).collect(Collectors.toList());
			return valueEntry.getNavigableChildren(true);
		}
		
		// Modified tab order
		@Override public boolean changeFocus(boolean forward) {
			IGuiEventListener listener = getListener();
			boolean hasListener = listener != null;
			BaseListEntry<?, ?, ?> subList = isExpandable? (BaseListEntry<?, ?, ?>) valueEntry : null;
			if (forward && isExpandable && listener == valueEntry && subList.getListener() == subList.labelReference) {
				subList.changeFocus(false);
				if (!keyEntry.changeFocus(true)) keyEntry.changeFocus(true);
				setListener(keyEntry);
				return true;
			} else if (
			  !forward && isExpandable && listener == valueEntry && subList.getEventListeners().indexOf(subList.getListener()) == 1
			) {
				subList.changeFocus(false);
				subList.changeFocus(false);
				if (!keyEntry.changeFocus(true)) keyEntry.changeFocus(true);
				setListener(keyEntry);
				return true;
			}
			if (hasListener && listener.changeFocus(forward)) return true;
			
			if (isExpandable) {
				if (listener == keyEntry) {
					final List<? extends IGuiEventListener> subListeners = subList.getEventListeners();
					final IGuiEventListener l = forward ? subListeners.get(1) : subListeners.get(0);
					WidgetUtils.forceUnFocus(l);
					setListener(valueEntry);
					valueEntry.setListener(l);
					if (valueEntry.changeFocus(forward))
						return true;
				}
				
				if (!hasListener && forward) {
					setListener(valueEntry);
					valueEntry.setListener(subList.labelReference);
					return true;
				}
				
				if (listener == valueEntry && valueEntry.getListener() == subList.labelReference && !forward) {
					valueEntry.changeFocus(false);
					setListener(null);
					return false;
				}
			}
			
			List<? extends IGuiEventListener> list = getEventListeners();
			int index = list.indexOf(listener);
			int target = hasListener && index >= 0 ? index + (forward ? 1 : 0)
			                                       : forward ? 0 : list.size();
			ListIterator<? extends IGuiEventListener> l = list.listIterator(target);
			BooleanSupplier hasNext = forward ? l::hasNext : l::hasPrevious;
			Supplier<? extends IGuiEventListener> supplier = forward ? l::next : l::previous;
			
			while (hasNext.getAsBoolean()) {
				IGuiEventListener next = supplier.get();
				if (next.changeFocus(forward)) {
					setListener(next);
					return true;
				}
			}
			
			setListener(null);
			return false;
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
	
	@Override public void setListener(IGuiEventListener listener) {
		if (getEventListeners().contains(listener)) {
			super.setListener(listener);
		} else {
			for (EntryPairCell<K, V, KE, E> cell : cells) {
				if (cell.keyEntry == listener || cell.valueEntry == listener) {
					super.setListener(cell);
					cell.setListener(listener);
					break;
				}
			}
		}
	}
}
