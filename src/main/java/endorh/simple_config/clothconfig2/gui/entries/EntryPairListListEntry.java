package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.api.*;
import endorh.simple_config.clothconfig2.gui.AbstractConfigScreen;
import endorh.simple_config.clothconfig2.gui.entries.EntryPairListListEntry.EntryPairCell;
import endorh.simple_config.clothconfig2.gui.widget.DynamicEntryListWidget;
import endorh.simple_config.clothconfig2.gui.widget.DynamicEntryListWidget.INavigableTarget;
import endorh.simple_config.clothconfig2.impl.ISeekableComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class EntryPairListListEntry<K, V, KE extends AbstractConfigListEntry<K> & IChildListEntry, E extends AbstractConfigListEntry<V>>
  extends AbstractListListEntry<Pair<K, V>, EntryPairCell<K, V, KE, E>, EntryPairListListEntry<K, V, KE, E>> {
	// protected final List<ReferenceProvider> referencableEntries = Lists.newArrayList();
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
		// for (EntryPairCell<K, V, KE, E> cell : cells)
		// 	referencableEntries.add(cell.valueEntry);
		// setReferenceProviderEntries(referencableEntries);
	}
	
	@Override
	public void updateSelected(boolean isSelected) {
		super.updateSelected(isSelected);
		for (EntryPairCell<K, V, KE, E> cell : cells)
			cell.updateSelected(isSelected && getListener() == cell && expanded);
	}
	
	@Override public boolean isRequiresRestart() {
		return super.isRequiresRestart();
	}
	
	@Override public boolean isEdited() {
		if (ignoreEdits) return false;
		final List<Pair<K, V>> original = getOriginal();
		return !ignoreOrder ? super.isEdited() :
		       getConfigError().isPresent()
		       || heldEntry != null && heldEntry.isEdited()
		       || original == null
		       || !cells.stream().map(EntryPairCell::getValue).collect(Collectors.toMap(
			        Pair::getKey, Pair::getValue, (a, b) -> b))
		         .equals(original.stream().collect(Collectors.toMap(
		           Pair::getKey, Pair::getValue, (a, b) -> b)));
	}
	
	@Internal @Override public Optional<ITextComponent> getError() {
		// This is preferable to just displaying "Multiple issues!" without further info
		// The names already turn red on each error anyways
		final List<ITextComponent> errors = cells.stream().map(BaseListCell::getConfigError)
		  .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
		if (!errors.isEmpty())
			return Optional.ofNullable(errors.get(0));
		return Optional.empty();
	}
	
	@Override public boolean preserveState() {
		if (cells.isEmpty() || !cells.get(0).isExpandable || heldEntry != null && getListener() == heldEntry)
			return super.preserveState();
		else if (preservedState != null) savePreservedState();
		return false;
	}
	
	@Internal public List<Pair<K, E>> getEntries() {
		return cells.stream().map(c -> Pair.of(c.keyEntry.getValue(), c.valueEntry)).collect(Collectors.toList());
	}
	
	@Override public void setParent(DynamicEntryListWidget<?> parent) {
		super.setParent(parent);
		for (EntryPairCell<K, V, KE, E> cell : cells) {
			cell.keyEntry.setParent(parent);
			cell.valueEntry.setParent(parent);
		}
	}
	
	@Override public void setScreen(AbstractConfigScreen screen) {
		super.setScreen(screen);
		for (EntryPairCell<K, V, KE, E> cell : cells) {
			cell.keyEntry.setScreen(screen);
			cell.valueEntry.setScreen(screen);
		}
	}
	
	@Override public String seekableValueText() {
		return "";
	}
	
	public static class EntryPairCell<
	  K, V, KE extends AbstractConfigListEntry<K> & IChildListEntry, E extends AbstractConfigListEntry<V>>
	  extends AbstractListListEntry.AbstractListCell<
	  Pair<K, V>, EntryPairCell<K, V, KE, E>, EntryPairListListEntry<K, V, KE, E>>
	  implements IExpandable, ReferenceProvider {
		protected final KE keyEntry;
		protected final E valueEntry;
		protected final List<IGuiEventListener> widgets;
		protected int keyOffset = 0;
		protected final boolean isExpandable;
		
		protected int noFilter = 0x00000000;
		protected int errorFilter = 0x64E04242;
		protected int keyOverlayColor = 0x00000000;
		
		public EntryPairCell(
		  EntryPairListListEntry<K, V, KE, E> listEntry,
		  KE keyEntry, E valueEntry
		) {
			super(listEntry);
			this.keyEntry = keyEntry;
			this.keyEntry.setChild(true);
			this.valueEntry = valueEntry;
			if (listEntry.areCaptionControlsEnabled())
				keyOffset = 24;
			isExpandable = valueEntry instanceof BaseListEntry;
			widgets = Lists.newArrayList(this.keyEntry, this.valueEntry);
			final DynamicEntryListWidget<?> parent = listEntry.getParentOrNull();
			final AbstractConfigScreen screen = listEntry.getConfigScreenOrNull();
			keyEntry.setParent(parent);
			valueEntry.setParent(parent);
			keyEntry.setScreen(screen);
			valueEntry.setScreen(screen);
			keyEntry.setExpandableParent(listEntry);
			valueEntry.setExpandableParent(listEntry);
			keyEntry.setNavigableParent(this);
			valueEntry.setNavigableParent(this);
			keyEntry.setListParent(listEntry);
			valueEntry.setListParent(listEntry);
		}
		
		@Override public Pair<K, V> getValue() {
			return Pair.of(keyEntry.getValue(), valueEntry.getValue());
		}
		
		@Override public int getCellHeight() {
			return valueEntry.getItemHeight();
		}
		
		@Override public void updateSelected(boolean isSelected) {
			super.updateSelected(isSelected);
			keyEntry.updateSelected(isSelected && getListener() == keyEntry);
			valueEntry.updateSelected(isSelected && getListener() == valueEntry);
			if (isSelected) getListEntry().selectKey = getListener() == keyEntry;
		}
		
		@Override public @NotNull AbstractConfigEntry<?> provideReferenceEntry() {
			return valueEntry;
		}
		
		public Optional<ITextComponent> getError() {
			Optional<ITextComponent> e = valueEntry.getError();
			if (!e.isPresent())
				e = getListEntry().cellErrorSupplier.apply(getValue());
			return e;
		}
		
		@Override public void doSetValue(Pair<K, V> value) {
			keyEntry.setValue(value.getKey());
			valueEntry.setValue(value.getValue());
		}
		
		@Override public void setOriginal(Pair<K, V> value) {
			keyEntry.setOriginal(value.getKey());
			valueEntry.setOriginal(value.getValue());
		}
		
		@Override public boolean isExpanded() {
			return isExpandable && ((IExpandable) valueEntry).isExpanded();
		}
		
		@Override public void setExpanded(boolean expanded, boolean recursive) {
			if (isExpandable)
				((IExpandable) valueEntry).setExpanded(expanded, recursive);
		}
		
		@Override public int getFocusedScroll() {
			return isExpandable ? ((IExpandable) valueEntry).getFocusedScroll() : 0;
		}
		
		@Override public int getFocusedHeight() {
			return isExpandable ? ((IExpandable) valueEntry).getFocusedHeight() : getCellHeight();
		}
		
		@Override
		public void render(
		  MatrixStack mStack, int index, int y, int x, int entryWidth,
		  int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta
		) {
			super.render(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
			keyOverlayColor = getConfigError().isPresent() ? errorFilter : noFilter;
			final FontRenderer fr = Minecraft.getInstance().fontRenderer;
			int keyX = fr.getBidiFlag() ? x + entryWidth - 150 - keyOffset : x + keyOffset;
			valueEntry.render(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
			keyEntry.renderChild(mStack, keyX, y, 120, 20, mouseX, mouseY, delta);
			fill(mStack, keyX - 2, y, keyX + 124, y + 20, keyOverlayColor);
		}
		
		@Override public boolean drawsLine(int mouseX, int mouseY) {
			return isExpandable && ((BaseListEntry<?, ?, ?>) valueEntry).expanded && mouseY > 18;
		}
		
		@Override public @Nonnull List<? extends IGuiEventListener> getEventListeners() {
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
		
		@Override public void onNavigate() {
			if (getListEntry().selectKey)
				keyEntry.onNavigate();
			else valueEntry.onNavigate();
		}
		
		@Override public @Nullable INavigableTarget getNavigableParent() {
			return getListEntry();
		}
		
		@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
			if (keyCode == 263 && getListener() == valueEntry) {
				setListener(keyEntry);
				keyEntry.changeFocus(true);
				return true;
			} else if (keyCode == 262 && getListener() == keyEntry) {
				setListener(valueEntry);
				valueEntry.changeFocus(true);
				return true;
			}
			if (Screen.hasAltDown() && getListEntry().getSelectedIndex() != -1 && (keyCode == 257 || keyCode == 260))
				getListEntry().selectKey = true;
			return super.handleNavigationKey(keyCode, scanCode, modifiers);
		}
		
		// Modified tab order
		@Override public boolean changeFocus(boolean focus) {
			IGuiEventListener listener = getListener();
			boolean hasListener = listener != null;
			BaseListEntry<?, ?, ?> subList = isExpandable? (BaseListEntry<?, ?, ?>) valueEntry : null;
			if (focus && isExpandable && listener == valueEntry && subList.getListener() == subList.label) {
				subList.changeFocus(false);
				if (!keyEntry.changeFocus(true)) keyEntry.changeFocus(true);
				setListener(keyEntry);
				return true;
			} else if (
			  !focus && isExpandable && listener == valueEntry && subList.getEventListeners().indexOf(subList.getListener()) == 1
			) {
				subList.changeFocus(false);
				subList.changeFocus(false);
				if (!keyEntry.changeFocus(true)) keyEntry.changeFocus(true);
				setListener(keyEntry);
				return true;
			}
			if (hasListener && listener.changeFocus(focus)) return true;
			
			if (isExpandable) {
				if (listener == keyEntry) {
					final List<? extends IGuiEventListener> subListeners = subList.getEventListeners();
					final IGuiEventListener l = focus ? subListeners.get(1) : subListeners.get(0);
					forceUnFocus(l);
					setListener(valueEntry);
					valueEntry.setListener(l);
					if (valueEntry.changeFocus(focus))
						return true;
				}
				
				if (!hasListener && focus) {
					setListener(valueEntry);
					valueEntry.setListener(subList.label);
					return true;
				}
				
				if (listener == valueEntry && valueEntry.getListener() == subList.label && !focus) {
					valueEntry.changeFocus(false);
					setListener(null);
					return false;
				}
			}
			
			List<? extends IGuiEventListener> list = getEventListeners();
			int index = list.indexOf(listener);
			int target = hasListener && index >= 0 ? index + (focus ? 1 : 0)
			                                       : focus ? 0 : list.size();
			ListIterator<? extends IGuiEventListener> l = list.listIterator(target);
			BooleanSupplier hasNext = focus ? l::hasNext : l::hasPrevious;
			Supplier<? extends IGuiEventListener> supplier = focus ? l::next : l::previous;
			
			while (hasNext.getAsBoolean()) {
				IGuiEventListener next = supplier.get();
				if (next.changeFocus(focus)) {
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
