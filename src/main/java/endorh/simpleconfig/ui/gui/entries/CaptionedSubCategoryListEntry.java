package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen.ListWidget;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen.ListWidget.EntryDragAction.ExpandedDragAction;
import endorh.simpleconfig.ui.gui.widget.DynamicEntryListWidget;
import endorh.simpleconfig.ui.gui.widget.ToggleAnimator;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Math.min;
import static java.lang.Math.round;

@OnlyIn(value = Dist.CLIENT)
public class CaptionedSubCategoryListEntry<
  T, CE extends AbstractConfigField<T> & IChildListEntry
> extends TooltipListEntry<T> implements IExpandable, IEntryHolder {
	
	protected final CaptionWidget<CaptionedSubCategoryListEntry<T, CE>> label;
	protected final List<AbstractConfigField<?>> heldEntries;
	protected final List<AbstractConfigListEntry<?>> entries;
	protected final List<GuiEventListener> children;
	protected final List<GuiEventListener> expandedChildren;
	protected final Rectangle captionEntryArea = new Rectangle();
	protected @Nullable CE captionEntry;
	protected boolean expanded;
	protected ToggleAnimator expandAnimator = new ToggleAnimator();
	
	@Internal public CaptionedSubCategoryListEntry(
	  Component title, List<AbstractConfigListEntry<?>> entries, @Nullable CE captionEntry
	) {
		super(title);
		this.entries = Lists.newArrayList(entries);
		heldEntries = Lists.newArrayList(entries);
		this.captionEntry = captionEntry;
		entries.forEach(e -> e.setParentEntry(this));
		label = new CaptionWidget<>(this);
		if (captionEntry != null) {
			captionEntry.setChildSubEntry(true);
			captionEntry.setName("$caption");
			captionEntry.setParentEntry(this);
			setValue(captionEntry.getValue());
			heldEntries.add(0, captionEntry);
			children = Lists.newArrayList(label, captionEntry, sideButtonReference);
			expandedChildren = Lists.newArrayList(label, captionEntry, sideButtonReference);
			expandedChildren.addAll(entries);
		} else {
			children = Lists.newArrayList(label, sideButtonReference);
			expandedChildren = Lists.newArrayList(label, sideButtonReference);
			expandedChildren.addAll(entries);
		}
		acceptButton.setDefaultIcon(SimpleConfigIcons.Buttons.MERGE_ACCEPT_GROUP);
	}
	
	@Override public boolean isExpanded() {
		return expanded;
	}
	
	@Override public void setExpanded(boolean expanded, boolean recursive, boolean animate) {
		if (this.expanded != expanded) {
			if (animate) {
				expandAnimator.setLength(min(250L, entries.size() * 25L));
				expandAnimator.setEaseOutTarget(expanded);
			} else expandAnimator.stopAndSet(expanded);
		}
		this.expanded = expanded;
		if (recursive) entries.stream().filter(e -> e instanceof IExpandable)
		  .forEach(e -> ((IExpandable) e).setExpanded(expanded, true));
	}
	
	@Override public T getDisplayedValue() {
		return captionEntry != null? captionEntry.getDisplayedValue() : null;
	}
	
	@Override public void setDisplayedValue(T value) {
		if (captionEntry != null) captionEntry.setDisplayedValue(value);
	}
	
	@Override public boolean isShown() {
		return super.isShown() || entries.stream().anyMatch(AbstractConfigListEntry::isShown);
	}
	
	@Override public List<AbstractConfigField<?>> getHeldEntries() {
		return heldEntries;
	}
	
	@Override public Rectangle getNavigableArea() {
		return label.area;
	}
	
	@Override public List<INavigableTarget> getNavigableSubTargets() {
		return captionEntry != null? Lists.newArrayList(this, captionEntry) :
		       super.getNavigableSubTargets();
	}
	
	@Override public void resetValue() {
		getScreen().runAtomicTransparentAction(this, () ->
		  entries.forEach(AbstractConfigField::resetValue));
	}
	
	@Override public void restoreValue() {
		getScreen().runAtomicTransparentAction(this, () ->
		  entries.forEach(AbstractConfigField::restoreValue));
	}
	
	@Override protected List<EntryError> computeErrors() {
		List<EntryError> errors = super.computeErrors();
		errors.addAll(IEntryHolder.super.getErrors());
		return errors;
	}
	
	@Override public Optional<Component[]> getTooltip(int mouseX, int mouseY) {
		if (isHeldEntryHovered(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override public void renderEntry(
      GuiGraphics gg, int index, int x, int y, int entryWidth, int entryHeight, int mouseX,
      int mouseY, boolean isHovered, float delta
	) {
		label.setFocused(isFocused() && getFocused() == label);
		super.renderEntry(gg, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		SimpleConfigIcons.Entries.EXPAND.renderCentered(
		  gg, x - 15, y + 5, 9, 9,
		  (label.area.contains(mouseX, mouseY) ? 2 : 0) + (isExpanded() ? 1 : 0));
		final boolean animating = expandAnimator.isInProgress();
		if (isExpanded() || animating) {
			if (animating) {
				DynamicEntryListWidget<?> parent = getEntryList();
				ScissorsHandler.INSTANCE.pushScissor(new Rectangle(
				  parent.left, entryArea.y, parent.right - parent.left, getItemHeight()));
			}
			int yy = y + 24;
			for (AbstractConfigListEntry<?> entry : entries) {
				if (entry.isShown()) {
					entry.render(
					  gg, -1, x + 14, yy, entryWidth - 14, entry.getItemHeight(),
					  mouseX, mouseY, isHovered && getFocused() == entry, delta);
					yy += entry.getItemHeight();
				}
			}
			if (animating) ScissorsHandler.INSTANCE.popScissor();
		}
		label.render(gg, mouseX, mouseY, delta);
	}
	
	@Override protected void renderField(
      GuiGraphics gg, int fieldX, int fieldY, int fieldWidth, int fieldHeight, int x, int y,
      int entryWidth, int entryHeight, int index, int mouseX, int mouseY, float delta
	) {
		super.renderField(gg, fieldX, fieldY, fieldWidth, fieldHeight, x, y, entryWidth, entryHeight, index, mouseX, mouseY, delta);
		label.area.setBounds(x - 24, y, captionEntry != null ? entryWidth - fieldWidth - 5 : entryWidth - 2, 20);
		if (captionEntry != null) {
			captionEntry.renderChild(
			  gg, fieldX, fieldY, fieldWidth, fieldHeight, mouseX, mouseY, delta);
			captionEntryArea.setBounds(fieldX, fieldY, fieldWidth, fieldHeight);
		} else captionEntryArea.setBounds(0, 0, 0, 0);
	}
	
	@Override public boolean isSelected() {
		return isSelectable() && isShown() && entries.stream()
		  .filter(e -> e.isSelectable() && e.isShown())
		  .allMatch(AbstractConfigField::isSelected)
		  && entries.stream()
		         .filter(e -> e.isSelectable() && e.isShown())
		         .anyMatch(e -> true);
	}
	
	@Override public void setSelected(boolean isSelected) {
		if (!isSelectable()) return;
		entries.stream()
		  .filter(e -> e.isSelectable() && e.isShown())
		  .forEach(e -> e.setSelected(isSelected));
	}
	
	@Override protected void doExpandParents(AbstractConfigField<?> entry) {
		boolean expanded = isExpanded();
		super.doExpandParents(entry);
		if (entry == captionEntry) setExpanded(expanded);
	}
	
	@Override public boolean isSelectable() {
		return super.isSelectable() && entries.stream().anyMatch(AbstractConfigField::isSelectable);
	}
	
	public boolean isHeldEntryHovered(int mouseX, int mouseY) {
		return captionEntry != null && captionEntryArea.contains(mouseX, mouseY);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (captionEntry != null) {
			final boolean captionEntrySelected = isFocused && getFocused() == captionEntry;
			final boolean prevSelected = captionEntry.isFocused();
			captionEntry.updateFocused(captionEntrySelected);
			if (!prevSelected && captionEntrySelected) getScreen().getHistory().preserveState(
			  captionEntry);
		}
		for (AbstractConfigListEntry<?> entry : entries)
			entry.updateFocused(isExpanded() && isFocused && getFocused() == entry);
	}
	
	@Override public boolean isEditable() {
		if (isEditingHotKeyAction())
			return getScreen().isEditable();
		return super.isEditable();
	}
	
	@Override protected boolean computeIsEdited() {
		return super.computeIsEdited()
		       || !isIgnoreEdits() && !isSubEntry() && isEditable()
		          && entries.stream().anyMatch(AbstractConfigField::isEdited);
	}
	
	@Override public boolean areEqual(T value, T other) {
		return captionEntry != null? captionEntry.areEqual(value, other) : super.areEqual(value, other);
	}
	
	@Override public @Nullable AbstractConfigField<?> getSingleResettableEntry() {
		return super.isResettable()? captionEntry : null;
	}
	
	@Override public @Nullable AbstractConfigField<?> getSingleRestorableEntry() {
		return super.isRestorable()? captionEntry : null;
	}
	
	@Override public void resetSingleEntry(AbstractConfigField<?> entry) {
		super.resetValue();
	}
	
	@Override public void restoreSingleEntry(AbstractConfigField<?> entry) {
		super.restoreValue();
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
		return entries.stream().anyMatch(e -> e.isResettable() || e.canResetGroup());
	}
	
	@Override public boolean canRestoreGroup() {
		return entries.stream().anyMatch(e -> e.isRestorable() || e.canRestoreGroup());
	}
	
	@Override public int getExtraScrollHeight() {
		ArrayList<Integer> list = new ArrayList<>();
		int i = 24;
		if (captionEntry != null)
			list.add(i + captionEntry.getExtraScrollHeight());
		if (isExpanded()) {
			for (AbstractConfigListEntry<?> entry : entries) {
				i += entry.getItemHeight();
				if (entry.getExtraScrollHeight() < 0) continue;
				list.add(i + entry.getExtraScrollHeight());
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
			for (AbstractConfigListEntry<?> entry : entries)
				if (entry.isShown()) i += entry.getItemHeight();
			return round(expandAnimator.getEaseOut() * (i - 24)) + 24;
		}
		return 24;
	}
	
	@Override public void tick() {
		super.tick();
		entries.forEach(AbstractConfigField::tick);
	}
	
	@Override public void save() {
		super.save();
		entries.forEach(AbstractConfigField::save);
	}
	
	@Override protected int getPreviewCaptionOffset() {
		return captionEntry != null ? super.getPreviewCaptionOffset() : 0;
	}
	
	@Override public void setHeadless(boolean headless) {
		throw new UnsupportedOperationException();
	}
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return isExpanded() ? expandedChildren : children;
	}
	
	@Override public int getFocusedScroll() {
		final GuiEventListener listener = getFocused();
		//noinspection SuspiciousMethodCalls
		if (!entries.contains(listener))
			return 0;
		int y = 24;
		//noinspection SuspiciousMethodCalls
		final int index = entries.indexOf(listener);
		if (index >= 0) {
			for (AbstractConfigListEntry<?> entry : entries.subList(0, index))
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
		if (path.startsWith("$caption")) {
			String[] split = DOT.split(path, 2);
			if ("$caption".equals(split[0])) {
				if (captionEntry instanceof IEntryHolder && split.length == 2)
					return ((IEntryHolder) captionEntry).getEntry(split[1]);
				return captionEntry;
			}
		}
		return IEntryHolder.super.getEntry(path);
	}
	
	@Override protected List<ISeekableComponent> seekableChildren() {
		final List<ISeekableComponent> children =
		  entries.stream().map(e -> (ISeekableComponent) e).collect(Collectors.toList());
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
	
	public @Nullable AbstractConfigField<?> getCaptionEntry() {
		return captionEntry;
	}
	
	@Override public List<INavigableTarget> getNavigableChildren(boolean onlyVisible) {
		return onlyVisible ? isExpanded() ? entries.stream()
		  .filter(AbstractConfigField::isNavigable)
		  .collect(Collectors.toList()) : Collections.emptyList() : Lists.newArrayList(entries);
	}
	
	// Handles the caption clicks and key-presses, but not the rendering
	public static class CaptionWidget<E extends AbstractConfigField<?> & IExpandable> implements GuiEventListener {
		protected final Rectangle area = new Rectangle();
		protected boolean focused = false;
		protected E expandable;
		protected int focusedColor = 0x80E0E0E0;
		
		protected CaptionWidget(E expandable) {
			this.expandable = expandable;
		}
		
		protected E getParent() {
			return expandable;
		}
		
		public void render(GuiGraphics gg, int mouseX, int mouseY, float delta) {
			if (focused && !expandable.isPreviewingExternal())
				drawBorder(gg, area.x + 4, area.y, area.width, area.height, 1, focusedColor);
		}
		
		@Override public boolean isMouseOver(double mouseX, double mouseY) {
			return area.contains(mouseX, mouseY);
		}
		
		@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (area.contains(mouseX, mouseY)) {
				final E parent = getParent();
				final boolean recurse = Screen.hasShiftDown();
				parent.setExpanded(!parent.isExpanded(), recurse);
				final DynamicEntryListWidget<?> list = parent.getEntryList();
				if (!recurse && list instanceof ListWidget) {
					((ListWidget<?>) list).startDragAction(new ExpandedDragAction(
					  parent.isExpanded()));
				}
				playFeedbackClick(1.0f);
				return true;
			}
			return false;
		}
		
		@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			final IExpandable parent = getParent();
			switch (keyCode) {
				case GLFW.GLFW_KEY_RIGHT:
					if (!parent.isExpanded()) {
						parent.setExpanded(true, Screen.hasShiftDown());
						playFeedbackTap(0.4F);
						return true;
					}
					break;
				case GLFW.GLFW_KEY_LEFT:
					if (parent.isExpanded()) {
						parent.setExpanded(false, Screen.hasShiftDown());
						playFeedbackTap(0.4F);
						return true;
					}
					break;
			}
			return GuiEventListener.super.keyPressed(keyCode, scanCode, modifiers);
		}
		
		@Override public boolean isFocused() {
			return focused;
		}
		
		@Override public void setFocused(boolean focused) {
			this.focused = focused;
		}

		@Override public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent e) {
			return isFocused()? null : ComponentPath.leaf(this);
		}

		@Override public @Nullable ComponentPath getCurrentFocusPath() {
			return GuiEventListener.super.getCurrentFocusPath();
		}
	}
}
