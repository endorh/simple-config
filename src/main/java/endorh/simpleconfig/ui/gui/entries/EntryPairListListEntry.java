package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.entries.EntryPairListListEntry.EntryPairCell;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
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
	  Component fieldName, List<Pair<K, V>> value,
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
			cell.updateSelected(isFocused && getFocused() == cell && expanded);
	}
	
	@Override protected boolean isFieldFullWidth() {
		return true;
	}
	
	@Override public boolean areEqual(List<Pair<K, V>> value, List<Pair<K, V>> other) {
		if (value.isEmpty() && other.isEmpty()) return true;
		if (value.size() != other.size()) return false;
		EntryPairCell<K, V, KE, E> dummy =
		  !cells.isEmpty()? cells.get(0) : createCellWithValue(value.get(0));
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
	
	@Internal public List<Pair<K, E>> getEntries() {
		return cells.stream().map(c -> Pair.of(c.keyEntry.getDisplayedValue(), c.valueEntry)).collect(Collectors.toList());
	}
	
	@Override public List<AbstractConfigField<?>> getHeldEntries() {
		return cells.stream()
		  .flatMap(c -> Stream.of(c.keyEntry, c.valueEntry))
		  .collect(Collectors.toList());
	}
	
	@Override public String providePath(AbstractConfigField<?> child) {
		String prefix = getCatPath() + ".";
		int i = 0;
		for (EntryPairCell<K, V, KE, E> cell : cells) {
			if (cell.keyEntry == child) return prefix + "key." + i;
			if (cell.valueEntry == child) return prefix + "val." + i;
			i++;
		}
		return prefix + "?";
	}
	
	@Override public @Nullable AbstractConfigField<?> getEntry(String path) {
		String[] split = DOT.split(path, 3);
		boolean isKey = "key".equals(split[0]);
		if (!isKey && !"val".equals(split[0]) || split.length < 2) return null;
		try {
			int i = Integer.parseInt(split[1]);
			if (i >= 0 && i < cells.size()) {
				EntryPairCell<K, V, KE, E> cell = cells.get(i);
				AbstractConfigField<?> entry = isKey? cell.keyEntry : cell.valueEntry;
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
	  > {
		protected final KE keyEntry;
		protected final E valueEntry;
		protected final List<GuiEventListener> widgets;
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
		
		@Override public void tick() {
			keyEntry.tick();
			valueEntry.tick();
			super.tick();
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
			keyEntry.updateFocused(isSelected && getFocused() == keyEntry);
			valueEntry.updateFocused(isSelected && getFocused() == valueEntry);
			if (isSelected) getListEntry().selectKey = getFocused() == keyEntry;
		}
		
		@Override protected List<EntryError> computeErrors() {
			List<EntryError> errors = super.computeErrors();
			errors.addAll(
			  Stream.concat(keyEntry.getEntryErrors().stream(), valueEntry.getEntryErrors().stream())
				 .filter(e -> !errors.contains(e)).toList());
			return errors;
		}
		
		@Override public Optional<Component> getErrorMessage() {
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
		
		@Override
		public void renderCell(
		  PoseStack mStack, int index, int x, int y, int cellWidth,
		  int cellHeight, int mouseX, int mouseY, boolean isHovered, float delta
		) {
			super.renderCell(mStack, index, x, y, cellWidth, cellHeight, mouseX, mouseY, isHovered, delta);
			keyOverlayColor = hasError() ? errorFilter : noFilter;
			final Font fr = Minecraft.getInstance().font;
			int keyX = fr.isBidirectional() ? x + cellWidth - 150 - keyOffset : x + keyOffset;
			valueEntry.render(mStack, index, x, y, cellWidth, cellHeight, mouseX, mouseY, isHovered, delta);
			final EntryPairListListEntry<K, V, KE, E> listEntry = getListEntry();
			keyEntry.renderChild(mStack, keyX, y, listEntry.getKeyFieldWidth(), 20, mouseX, mouseY, delta);
			fill(mStack, keyX, y, keyX + listEntry.getKeyFieldWidth(), y + 20, keyOverlayColor);
		}
		
		@Override public void renderLabel(
		  PoseStack mStack, Component label, int textX, int index, int x, int y, int cellWidth,
		  int cellHeight, int mouseX, int mouseY, boolean isSelected, float delta
		) {}
		
		@Override public Component getLabel() {
			return Component.empty();
		}
		
		@Override public boolean drawsLine(int mouseX, int mouseY) {
			return isExpandable && ((BaseListEntry<?, ?, ?>) valueEntry).expanded && mouseY > 18;
		}
		
		@Override public @NotNull List<? extends GuiEventListener> children() {
			return widgets;
		}
		
		@Override protected List<ISeekableComponent> seekableComponents() {
			return Lists.newArrayList(keyEntry, valueEntry);
		}
		
		@Override protected String seekableText() {
			return "";
		}
		
		@Override public List<INavigableTarget> getNavigableSubTargets() {
			List<INavigableTarget> subTargets = new ArrayList<>();
			AbstractConfigField<?>[] arr = {isExpandable? valueEntry : keyEntry,
			                                isExpandable? keyEntry : valueEntry};
			for (AbstractConfigField<?> entry : arr) {
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
			  Stream.of(keyEntry, valueEntry), valueEntry.getNavigableChildren(false).stream()
			).collect(Collectors.toList());
			return valueEntry.getNavigableChildren(true);
		}
		
		// Modified tab order
		@Override public boolean changeFocus(boolean forward) {
			GuiEventListener listener = getFocused();
			boolean hasListener = listener != null;
			BaseListEntry<?, ?, ?> subList = isExpandable? (BaseListEntry<?, ?, ?>) valueEntry : null;
			if (forward && isExpandable && listener == valueEntry && subList.getFocused() == subList.labelReference) {
				subList.changeFocus(false);
				if (!keyEntry.changeFocus(true)) keyEntry.changeFocus(true);
				setFocused(keyEntry);
				return true;
			} else if (
			  !forward && isExpandable && listener == valueEntry && subList.children().indexOf(subList.getFocused()) == 1
			) {
				subList.changeFocus(false);
				subList.changeFocus(false);
				if (!keyEntry.changeFocus(true)) keyEntry.changeFocus(true);
				setFocused(keyEntry);
				return true;
			}
			if (hasListener && listener.changeFocus(forward)) return true;
			
			if (isExpandable) {
				if (listener == keyEntry) {
					final List<? extends GuiEventListener> subListeners = subList.children();
					final GuiEventListener l = forward ? subListeners.get(1) : subListeners.get(0);
					WidgetUtils.forceUnFocus(l);
					setFocused(valueEntry);
					valueEntry.setFocused(l);
					if (valueEntry.changeFocus(forward))
						return true;
				}
				
				if (!hasListener && forward) {
					setFocused(valueEntry);
					valueEntry.setFocused(subList.labelReference);
					return true;
				}
				
				if (listener == valueEntry && valueEntry.getFocused() == subList.labelReference && !forward) {
					valueEntry.changeFocus(false);
					setFocused(null);
					return false;
				}
			}
			
			List<? extends GuiEventListener> list = children();
			int index = list.indexOf(listener);
			int target = hasListener && index >= 0 ? index + (forward ? 1 : 0)
			                                       : forward ? 0 : list.size();
			ListIterator<? extends GuiEventListener> l = list.listIterator(target);
			BooleanSupplier hasNext = forward ? l::hasNext : l::hasPrevious;
			Supplier<? extends GuiEventListener> supplier = forward ? l::next : l::previous;
			
			while (hasNext.getAsBoolean()) {
				GuiEventListener next = supplier.get();
				if (next.changeFocus(forward)) {
					setFocused(next);
					return true;
				}
			}
			
			setFocused(null);
			return false;
		}
	}
	
	@Override public void setFocused(GuiEventListener listener) {
		if (children().contains(listener)) {
			super.setFocused(listener);
		} else {
			for (EntryPairCell<K, V, KE, E> cell : cells) {
				if (cell.keyEntry == listener || cell.valueEntry == listener) {
					super.setFocused(cell);
					cell.setFocused(listener);
					break;
				}
			}
		}
	}
}
