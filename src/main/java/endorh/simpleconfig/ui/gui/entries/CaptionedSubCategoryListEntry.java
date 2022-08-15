package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen.ListWidget;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen.ListWidget.EntryDragAction.ExpandedDragAction;
import endorh.simpleconfig.ui.gui.widget.DynamicEntryListWidget;
import endorh.simpleconfig.ui.gui.widget.ToggleAnimator;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
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
  T, HE extends AbstractConfigEntry<T> & IChildListEntry
> extends TooltipListEntry<T> implements IExpandable, IEntryHolder {
	
	protected final CaptionWidget<CaptionedSubCategoryListEntry<T, HE>> label;
	protected final List<AbstractConfigEntry<?>> heldEntries;
	protected final List<AbstractConfigListEntry<?>> entries;
	protected final List<IGuiEventListener> children;
	protected final List<IGuiEventListener> expandedChildren;
	protected final Rectangle heldEntryArea = new Rectangle();
	protected @Nullable HE captionEntry;
	protected boolean expanded;
	protected ToggleAnimator expandAnimator = new ToggleAnimator();
	
	@Internal public CaptionedSubCategoryListEntry(
	  ITextComponent title, List<AbstractConfigListEntry<?>> entries, @Nullable HE heldEntry
	) {
		super(title);
		this.entries = Lists.newArrayList(entries);
		heldEntries = Lists.newArrayList(entries);
		captionEntry = heldEntry;
		entries.forEach(e -> e.setParentEntry(this));
		label = new CaptionWidget<>(this);
		if (heldEntry != null) {
			heldEntry.setChildSubEntry(true);
			heldEntry.setName("$caption");
			heldEntry.setParentEntry(this);
			setValue(heldEntry.getValue());
			heldEntries.add(0, heldEntry);
			children = Lists.newArrayList(label, heldEntry, sideButtonReference);
			expandedChildren = Lists.newArrayList(label, heldEntry, sideButtonReference);
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
	
	@Override public void setExpanded(boolean expanded, boolean recursive) {
		if (this.expanded != expanded) {
			expandAnimator.setLength(min(250L, entries.size() * 25L));
			expandAnimator.setEaseOutTarget(expanded);
		}
		this.expanded = expanded;
		if (recursive)
			entries.stream().filter(e -> e instanceof IExpandable)
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
	
	@Override public List<AbstractConfigEntry<?>> getHeldEntries() {
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
		  entries.forEach(AbstractConfigEntry::resetValue));
	}
	
	@Override public void restoreValue() {
		getScreen().runAtomicTransparentAction(this, () ->
		  entries.forEach(AbstractConfigEntry::restoreValue));
	}
	
	@Override public List<EntryError> getErrors() {
		List<EntryError> errors = super.getErrors();
		errors.addAll(IEntryHolder.super.getErrors());
		return errors;
	}
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		if (isHeldEntryHovered(mouseX, mouseY))
			return Optional.empty();
		return super.getTooltip(mouseX, mouseY);
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int x, int y, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		label.setFocused(isFocused() && getListener() == label);
		super.renderEntry(mStack, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
		SimpleConfigIcons.Entries.EXPAND.renderCentered(
		  mStack, x - 15, y + 5, 9, 9,
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
					  mStack, -1, x + 14, yy, entryWidth - 14, entry.getItemHeight(),
					  mouseX, mouseY, isHovered && getListener() == entry, delta);
					yy += entry.getItemHeight();
				}
			}
			if (animating) ScissorsHandler.INSTANCE.popScissor();
		}
		label.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override protected void renderField(
	  MatrixStack mStack, int fieldX, int fieldY, int fieldWidth, int fieldHeight, int x, int y,
	  int entryWidth, int entryHeight, int index, int mouseX, int mouseY, float delta
	) {
		super.renderField(mStack, fieldX, fieldY, fieldWidth, fieldHeight, x, y, entryWidth, entryHeight, index, mouseX, mouseY, delta);
		label.area.setBounds(x - 24, y, captionEntry != null ? entryWidth - fieldWidth - 5 : entryWidth - 2, 20);
		if (captionEntry != null) {
			captionEntry.renderChild(
			  mStack, fieldX, fieldY, fieldWidth, fieldHeight, mouseX, mouseY, delta);
			heldEntryArea.setBounds(fieldX, fieldY, fieldWidth, fieldHeight);
		} else heldEntryArea.setBounds(0, 0, 0, 0);
	}
	
	@Override public boolean isSelected() {
		return isSelectable() && isShown() && entries.stream()
		  .filter(e -> e.isSelectable() && e.isShown())
		  .allMatch(AbstractConfigEntry::isSelected)
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
	
	@Override protected void doExpandParents(AbstractConfigEntry<?> entry) {
		boolean expanded = isExpanded();
		super.doExpandParents(entry);
		if (entry == captionEntry) setExpanded(expanded);
	}
	
	@Override public boolean isSelectable() {
		return super.isSelectable() && entries.stream().anyMatch(AbstractConfigEntry::isSelectable);
	}
	
	public boolean isHeldEntryHovered(int mouseX, int mouseY) {
		return captionEntry != null && heldEntryArea.contains(mouseX, mouseY);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (captionEntry != null) {
			final boolean heldEntrySelected = isFocused && getListener() == captionEntry;
			final boolean prevSelected = captionEntry.isFocused();
			captionEntry.updateFocused(heldEntrySelected);
			if (!prevSelected && heldEntrySelected) getScreen().getHistory().preserveState(
			  captionEntry);
		}
		for (AbstractConfigListEntry<?> entry : entries)
			entry.updateFocused(isExpanded() && isFocused && getListener() == entry);
	}
	
	@Override public boolean isEditable() {
		if (isEditingHotKeyAction())
			return getScreen().isEditable();
		return super.isEditable();
	}
	
	@Override public boolean isEdited() {
		return super.isEdited()
		       || !isIgnoreEdits() && !isSubEntry() && isEditable()
		          && entries.stream().anyMatch(AbstractConfigEntry::isEdited);
	}
	
	@Override public boolean areEqual(T value, T other) {
		return captionEntry != null? captionEntry.areEqual(value, other) : super.areEqual(value, other);
	}
	
	@Override public @Nullable AbstractConfigEntry<?> getSingleResettableEntry() {
		return super.isResettable()? captionEntry : null;
	}
	
	@Override public @Nullable AbstractConfigEntry<?> getSingleRestorableEntry() {
		return super.isRestorable()? captionEntry : null;
	}
	
	@Override public void resetSingleEntry(AbstractConfigEntry<?> entry) {
		super.resetValue();
	}
	
	@Override public void restoreSingleEntry(AbstractConfigEntry<?> entry) {
		super.restoreValue();
	}
	
	@Override public boolean isResettable() {
		if (!isEditable() || isSubEntry()) return false;
		return entries.stream().anyMatch(AbstractConfigEntry::isResettable);
	}
	
	@Override public boolean isRestorable() {
		if (!isEditable() || isSubEntry()) return false;
		return entries.stream().anyMatch(AbstractConfigEntry::isRestorable);
	}
	
	@Override public boolean canResetGroup() {
		return entries.stream().anyMatch(AbstractConfigEntry::isResettable);
	}
	
	@Override public boolean canRestoreGroup() {
		return entries.stream().anyMatch(AbstractConfigEntry::isRestorable);
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
		entries.forEach(AbstractConfigEntry::tick);
	}
	
	@Override public void save() {
		super.save();
		entries.forEach(AbstractConfigEntry::save);
	}
	
	@Override protected int getPreviewCaptionOffset() {
		return captionEntry != null ? super.getPreviewCaptionOffset() : 0;
	}
	
	@Override public void setHeadless(boolean headless) {
		throw new UnsupportedOperationException();
	}
	
	@Override protected @NotNull List<? extends IGuiEventListener> getEntryListeners() {
		return isExpanded() ? expandedChildren : children;
	}
	
	@Override public int getFocusedScroll() {
		final IGuiEventListener listener = getListener();
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
		final IGuiEventListener listener = getListener();
		if (listener instanceof IExpandable)
			return ((IExpandable) listener).getFocusedHeight();
		if (listener instanceof AbstractConfigListEntry<?>)
			return ((AbstractConfigListEntry<?>) listener).getItemHeight();
		return 20;
	}
	
	@Override public @Nullable AbstractConfigEntry<?> getEntry(String path) {
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
		  entries.stream().map(e -> ((ISeekableComponent) e)).collect(Collectors.toList());
		if (captionEntry != null)
			children.add(0, captionEntry);
		return children;
	}
	
	@Override public String seekableValueText() {
		return "";
	}
	
	@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		if (getListener() == label && keyCode == GLFW.GLFW_KEY_LEFT && isExpanded()) {
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
	
	public @Nullable AbstractConfigEntry<?> getCaptionEntry() {
		return captionEntry;
	}
	
	@Override public List<INavigableTarget> getNavigableChildren(boolean onlyVisible) {
		return onlyVisible ? isExpanded() ? entries.stream()
		  .filter(AbstractConfigEntry::isNavigable)
		  .collect(Collectors.toList()) : Collections.emptyList() : Lists.newArrayList(entries);
	}
	
	@Override public List<INavigableTarget> getNavigableChildren() {
		if (isExpanded()) {
			final List<INavigableTarget> targets =
			  entries.stream().flatMap(e -> e.getNavigableChildren().stream())
				 .collect(Collectors.toList());
			targets.add(0, this);
			return targets;
		}
		return super.getNavigableChildren();
	}
	
	// Handles the caption clicks and key-presses, but not the rendering
	public static class CaptionWidget<E extends AbstractConfigEntry<?> & IExpandable> implements IGuiEventListener {
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
		
		public void render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
			if (focused && !expandable.isPreviewingExternal())
				drawBorder(mStack, area.x + 4, area.y, area.width, area.height, 1, focusedColor);
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
			return IGuiEventListener.super.keyPressed(keyCode, scanCode, modifiers);
		}
		
		@Override public boolean changeFocus(boolean focus) {
			return focused = !focused;
		}
		
		public boolean isFocused() {
			return focused;
		}
		
		public void setFocused(boolean focused) {
			this.focused = focused;
		}
	}
}
