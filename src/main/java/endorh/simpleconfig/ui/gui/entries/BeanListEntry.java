package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.core.entry.BeanEntry.ConfigBeanAccessException;
import endorh.simpleconfig.core.entry.BeanProxy;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.gui.entries.CaptionedSubCategoryListEntry.CaptionWidget;
import endorh.simpleconfig.ui.gui.widget.DynamicEntryListWidget;
import endorh.simpleconfig.ui.gui.widget.ToggleAnimator;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.min;
import static java.lang.Math.round;

public class BeanListEntry<B> extends TooltipListEntry<B> implements IExpandable, IEntryHolder {
	CaptionWidget<BeanListEntry<B>> label;
	protected final BeanProxy<B> proxy;
	protected final List<AbstractConfigField<?>> heldEntries;
	protected final List<BeanPropertyCell> cells;
	protected final Map<String, AbstractConfigListEntry<?>> entries;
	protected final List<GuiEventListener> children;
	protected final List<GuiEventListener> expandedChildren;
	protected @Nullable AbstractConfigListEntry<?> captionEntry;
	protected final Rectangle captionEntryArea = new Rectangle();
	protected @Nullable Function<B, Icon> iconProvider;
	protected @Nullable Icon icon;
	
	protected ToggleAnimator expandAnimator = new ToggleAnimator();
	private boolean expanded;
	
	public BeanListEntry(
	  Component fieldName, B value, BeanProxy<B> proxy,
	  Map<String, AbstractConfigListEntry<?>> entries,
	  @Nullable String caption, @Nullable Function<B, Icon> iconProvider
	) {
		super(fieldName);
		this.proxy = proxy;
		this.entries = entries;
		heldEntries = Lists.newArrayList(entries.values());
		cells = Lists.newArrayList();
		label = new CaptionWidget<>(this);
		if (caption != null && entries.get(caption) instanceof IChildListEntry) {
			captionEntry = entries.get(caption);
			captionEntry.setChildSubEntry(true);
			captionEntry.setParentEntry(this);
			heldEntries.remove(captionEntry);
			children = Lists.newArrayList(label, captionEntry, sideButtonReference);
			expandedChildren = Lists.newArrayList(label, captionEntry, sideButtonReference);
			expandedChildren.addAll(heldEntries);
			heldEntries.add(0, captionEntry);
		} else {
			children = Lists.newArrayList(label, sideButtonReference);
			expandedChildren = Lists.newArrayList(label, sideButtonReference);
			expandedChildren.addAll(heldEntries);
		}
		entries.forEach((n, e) -> {
			e.setParentEntry(this);
			e.setSubEntry(true);
			e.setName(n);
			if (e != captionEntry) {
				BeanPropertyCell cell = new BeanPropertyCell(this, e);
				e.setNavigableParent(cell);
				cells.add(cell);
			}
		});
		this.iconProvider = iconProvider;
		setValue(value);
		setDisplayedValue(value);
	}
	
	@Override public B getDisplayedValue() {
		return proxy.createFromGUI(getDefaultValue(), Maps.transformValues(
		  entries, AbstractConfigField::getDisplayedValue));
	}
	
	@Override public void setDisplayedValue(B value) {
		entries.forEach((name, entry) -> {
			try {
				//noinspection unchecked
				AbstractConfigListEntry<Object> e = (AbstractConfigListEntry<Object>) entry;
				e.setDisplayedValue(proxy.getGUI(value, name));
			} catch (ClassCastException e) {
				throw new ConfigBeanAccessException(
				  "Invalid Bean property type: " + proxy.getPropertyName(name), e);
			}
		});
	}
	
	@Override public void setOriginal(@Nullable B original) {
		super.setOriginal(original);
		if (original != null) entries.forEach((name, entry) -> {
			try {
				//noinspection unchecked
				AbstractConfigListEntry<Object> e = (AbstractConfigListEntry<Object>) entry;
				e.setOriginal(proxy.getGUI(original, name));
			} catch (ClassCastException e) {
				throw new ConfigBeanAccessException(
				  "Invalid Bean property type: " + proxy.getPropertyName(name), e);
			}
		});
	}
	
	@Override public void renderEntry(
	  PoseStack mStack, int index, int x, int y, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		label.setFocused(isFocused() && getFocused() == label);
		super.renderEntry(
		  mStack, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		Icon icon = this.icon != null? this.icon : SimpleConfigIcons.Entries.EXPAND;
		icon.renderCentered(
		  mStack, x - 20, y, 18, 18,
		  this.icon != null? 0 : (label.area.contains(mouseX, mouseY)? 2 : 0) + (isExpanded()? 1 : 0));
		final boolean animating = expandAnimator.isInProgress();
		if (isExpanded() || animating) {
			if (animating) {
				DynamicEntryListWidget<?> parent = getEntryList();
				ScissorsHandler.INSTANCE.pushScissor(new Rectangle(
				  parent.left, entryArea.y, parent.right - parent.left, getItemHeight()));
			}
			int yy = y + 24;
			for (AbstractConfigListEntry<?> entry: entries.values()) {
				if (entry.isShown() && entry != captionEntry) {
					entry.render(
					  mStack, -1, x + 14, yy, entryWidth - 14, entry.getItemHeight(),
					  mouseX, mouseY, isHovered && getFocused() == entry, delta);
					yy += entry.getItemHeight();
				}
			}
			if (animating) ScissorsHandler.INSTANCE.popScissor();
		}
		label.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override protected void renderField(
	  PoseStack mStack, int fieldX, int fieldY, int fieldWidth, int fieldHeight, int x, int y,
	  int entryWidth, int entryHeight, int index, int mouseX, int mouseY, float delta
	) {
		super.renderField(
		  mStack, fieldX, fieldY, fieldWidth, fieldHeight, x, y, entryWidth, entryHeight, index,
		  mouseX, mouseY, delta);
		label.area.setBounds(
		  x - 24, y, captionEntry != null? entryWidth - fieldWidth - 5 : entryWidth - 2, 20);
		if (captionEntry != null) {
			((IChildListEntry) captionEntry).renderChild(
			  mStack, fieldX, fieldY, fieldWidth, fieldHeight, mouseX, mouseY, delta);
			captionEntryArea.setBounds(fieldX, fieldY, fieldWidth, fieldHeight);
		} else captionEntryArea.setBounds(0, 0, 0, 0);
	}
	
	@Override public List<AbstractConfigField<?>> getHeldEntries() {
		return heldEntries;
	}
	
	@Override public void resetValue() {
		getScreen().runAtomicTransparentAction(this, () ->
		  entries.values().forEach(AbstractConfigField::resetValue));
	}
	
	@Override public void restoreValue() {
		getScreen().runAtomicTransparentAction(this, () ->
		  entries.values().forEach(AbstractConfigField::restoreValue));
	}
	
	@Override protected List<EntryError> computeErrors() {
		List<EntryError> errors = super.computeErrors();
		errors.addAll(IEntryHolder.super.getErrors());
		return errors;
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		for (AbstractConfigField<?> entry: heldEntries)
			entry.updateFocused(
			  (isExpanded() || entry == captionEntry)
			  && isFocused && getFocused() == entry);
	}
	
	@Override public boolean isExpanded() {
		return expanded;
	}
	
	@Override public void setExpanded(boolean expanded, boolean recurse) {
		if (this.expanded != expanded) {
			expandAnimator.setLength(min(250L, entries.size() * 25L));
			expandAnimator.setEaseOutTarget(expanded);
		}
		this.expanded = expanded;
		if (recurse) heldEntries.stream().filter(e -> e instanceof IExpandable)
		  .forEach(e -> ((IExpandable) e).setExpanded(expanded, true));
	}
	
	@Override protected void doExpandParents(AbstractConfigField<?> entry) {
		boolean expanded = isExpanded();
		super.doExpandParents(entry);
		if (entry == captionEntry) setExpanded(expanded);
	}
	
	@Override public int getFocusedScroll() {
		final GuiEventListener listener = getFocused();
		//noinspection SuspiciousMethodCalls
		if (!heldEntries.contains(listener))
			return 0;
		int y = 24;
		//noinspection SuspiciousMethodCalls
		final int index = heldEntries.indexOf(listener);
		if (index >= 0) {
			for (AbstractConfigField<?> entry: heldEntries.subList(0, index))
				y += entry.getItemHeight();
		}
		if (listener instanceof IExpandable)
			y += ((IExpandable) listener).getFocusedScroll();
		return y;
	}
	
	@Override public int getFocusedHeight() {
		final GuiEventListener listener = getFocused();
		if (listener instanceof IExpandable)
			return ((IExpandable) listener).getFocusedHeight();
		if (listener instanceof AbstractConfigListEntry<?>)
			return ((AbstractConfigListEntry<?>) listener).getItemHeight();
		return 20;
	}
	
	@Override public @Nullable AbstractConfigField<?> getEntry(String path) {
		String[] split = DOT.split(path, 2);
		AbstractConfigListEntry<?> entry = entries.get(split[0]);
		if (split.length == 1) return entry;
		if (entry instanceof IEntryHolder)
			return ((IEntryHolder) entry).getEntry(split[1]);
		return null;
	}
	
	@Override public @Nullable AbstractConfigField<?> getSingleResettableEntry() {
		return captionEntry;
	}
	
	@Override public @Nullable AbstractConfigField<?> getSingleRestorableEntry() {
		return captionEntry;
	}
	
	@Override public void resetSingleEntry(AbstractConfigField<?> entry) {
		if (captionEntry != null) captionEntry.resetValue();
	}
	
	@Override public void restoreSingleEntry(AbstractConfigField<?> entry) {
		if (captionEntry != null) captionEntry.restoreValue();
	}
	
	@Override public boolean isResettable() {
		if (!isEditable() || isSubEntry()) return false;
		return captionEntry != null && captionEntry.isResettable();
	}
	
	@Override public boolean isRestorable() {
		if (!isEditable() || isSubEntry()) return false;
		return captionEntry != null && captionEntry.isRestorable();
	}
	
	@Override public boolean canResetGroup() {
		return entries.values().stream().anyMatch(
		  e -> e != captionEntry && e.isResettable());
	}
	
	@Override public boolean canRestoreGroup() {
		return entries.values().stream().anyMatch(
		  e -> e != captionEntry && e.isRestorable());
	}
	
	@Override public void tick() {
		super.tick();
		entries.values().forEach(AbstractConfigField::tick);
		if (iconProvider != null) {
			icon = iconProvider.apply(getDisplayedValue());
		} else icon = null;
	}
	
	@Override protected int getPreviewCaptionOffset() {
		return captionEntry != null? super.getPreviewCaptionOffset() : 0;
	}
	
	@Override public int getExtraScrollHeight() {
		List<Integer> list = new ArrayList<>();
		int i = 24;
		if (captionEntry != null)
			list.add(i + captionEntry.getExtraScrollHeight());
		if (isExpanded()) {
			for (AbstractConfigListEntry<?> entry: entries.values()) {
				if (entry != captionEntry) {
					i += entry.getItemHeight();
					if (entry.getExtraScrollHeight() <= 0) continue;
					list.add(i + entry.getExtraScrollHeight());
				}
			}
			list.add(i);
		}
		return list.stream().max(Integer::compare).orElse(0) - i;
	}
	
	@Override public Rectangle getSelectionArea() {
		final DynamicEntryListWidget<?> parent = getEntryList();
		return new Rectangle(parent.left, entryArea.y, parent.right - parent.left, 20);
	}
	
	@Override public int getItemHeight() {
		if (isExpanded() || expandAnimator.isInProgress()) {
			int i = 24;
			for (AbstractConfigListEntry<?> entry: entries.values())
				if (entry.isShown() && entry != captionEntry)
					i += entry.getItemHeight();
			return round(expandAnimator.getEaseOut() * (i - 24)) + 24;
		}
		return 24;
	}
	
	@Override protected List<ISeekableComponent> seekableChildren() {
		final List<ISeekableComponent> children =
		  entries.values().stream().map(e -> (ISeekableComponent) e).collect(Collectors.toList());
		if (captionEntry != null)
			children.add(0, captionEntry);
		return children;
	}
	
	@Override public String seekableValueText() {
		return "";
	}
	
	@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		if (getFocused() == label && keyCode == GLFW.GLFW_KEY_LEFT && isExpanded()) {
			setExpanded(false, Screen.hasShiftDown());
			playFeedbackTap(0.4F);
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_RIGHT && !isExpanded()) { // Right
			setExpanded(true, Screen.hasShiftDown());
			playFeedbackTap(0.4F);
			return true;
		}
		return super.handleNavigationKey(keyCode, scanCode, modifiers);
	}
	
	@Override public Rectangle getNavigableArea() {
		return label.area;
	}
	
	@Override public List<INavigableTarget> getNavigableSubTargets() {
		return captionEntry != null? Lists.newArrayList(this, captionEntry) :
		       super.getNavigableSubTargets();
	}
	
	@Override public List<INavigableTarget> getNavigableChildren(boolean onlyVisible) {
		return !onlyVisible || isExpanded()? cells.stream()
		  .filter(c -> c.getEntry() != captionEntry)
		  .filter(c -> c.getEntry().isNavigable())
		  .collect(Collectors.toList()) : Collections.emptyList();
	}
	
	protected static class BeanPropertyCell implements INavigableTarget {
		private final INavigableTarget parent;
		private final AbstractConfigField<?> entry;
		private final List<GuiEventListener> listeners = new ArrayList<>(1);
		
		public BeanPropertyCell(INavigableTarget parent, AbstractConfigField<?> entry) {
			this.parent = parent;
			this.entry = entry;
			listeners.add(entry);
		}
		
		@Override public @Nullable INavigableTarget getNavigableParent() {
			return parent;
		}
		
		public AbstractConfigField<?> getEntry() {
			return entry;
		}
		
		@Override public void navigate() {
			entry.navigate();
		}
		
		@Override public @Nullable INavigableTarget getLastSelectedNavigableSubTarget() {
			return entry.getLastSelectedNavigableSubTarget();
		}
		@Override public void setLastSelectedNavigableSubTarget(@Nullable INavigableTarget target) {
			entry.setLastSelectedNavigableSubTarget(target);
		}
		
		@Override public List<INavigableTarget> getNavigableSubTargets() {
			return entry.getNavigableSubTargets();
		}
		
		@Override public List<INavigableTarget> getNavigableChildren(boolean onlyVisible) {
			return entry.getNavigableChildren(onlyVisible);
		}
		
		@Override public Rectangle getNavigableArea() {
			return entry.getNavigableArea();
		}
		
		@Override public Rectangle getRowArea() {
			return entry.getRowArea();
		}
		
		@Override public void applyFocusHighlight(int color, int length) {
			entry.applyFocusHighlight(color, length);
		}
		
		@Override public @NotNull List<? extends GuiEventListener> children() {
			return listeners;
		}
		
		@Override public boolean isDragging() {
			return entry.isDragging();
		}
		@Override public void setDragging(boolean dragging) {
			entry.setDragging(dragging);
		}
		
		@Nullable @Override public GuiEventListener getFocused() {
			return entry;
		}
		@Override public void setFocused(@Nullable GuiEventListener listener) {}
	}
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return isExpanded()? expandedChildren : children;
	}
}
