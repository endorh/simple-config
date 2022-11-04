package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.gui.entries.BaseListEntry.ListCaptionWidget;
import endorh.simpleconfig.ui.gui.widget.ResetButton;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class CaptionedListListEntry<
  V, E extends AbstractListListEntry<V, ?, E>,
  C, CE extends AbstractConfigListEntry<C> & IChildListEntry
> extends TooltipListEntry<Pair<C, List<V>>> implements IExpandable, IEntryHolder {
	protected final ListCaptionWidget label;
	private final E listEntry;
	private final CE captionEntry;
	private final List<AbstractConfigField<?>> heldEntries;
	private final List<ISeekableComponent> seekableChildren;
	protected List<GuiEventListener> children;
	
	public CaptionedListListEntry(Component fieldName, final E listEntry, final CE captionEntry) {
		super(fieldName);
		this.listEntry = listEntry;
		this.captionEntry = captionEntry;
		listEntry.setName("list");
		captionEntry.setName("caption");
		listEntry.setParentEntry(this);
		captionEntry.setParentEntry(this);
		captionEntry.setChildSubEntry(true);
		listEntry.setSubEntry(true);
		listEntry.setHeadless(true);
		label = new ListCaptionWidget(listEntry);
		heldEntries = Lists.newArrayList(captionEntry, listEntry);
		seekableChildren = Lists.newArrayList(captionEntry, listEntry);
		children = Lists.newArrayList(label, captionEntry, sideButtonReference, listEntry);
	}
	
	public E getListEntry() {
		return listEntry;
	}
	
	@Override public void renderEntry(
	  PoseStack mStack, int index, int x, int y, int entryWidth, int entryHeight,
	  int mouseX, int mouseY, boolean isHovered, float delta
	) {
		listEntry.render(mStack, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		super.renderEntry(mStack, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
	}
	
	@Override protected void renderField(
	  PoseStack mStack, int fieldX, int fieldY, int fieldWidth, int fieldHeight, int x, int y,
	  int entryWidth, int entryHeight, int index, int mouseX, int mouseY, float delta
	) {
		captionEntry.renderChild(mStack, fieldX, fieldY, fieldWidth, fieldHeight, mouseX, mouseY, delta);
		label.setFocused(isFocused() && getFocused() == label);
		label.area.setBounds(x - 24, y, entryWidth - fieldWidth - 5, 20);
		label.render(mStack, mouseX, mouseY, delta);
		final ResetButton resetButton = getResetButton();
		if (resetButton != null) resetButton.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override protected void doExpandParents(AbstractConfigField<?> entry) {
		boolean expanded = isExpanded();
		super.doExpandParents(entry);
		if (entry == captionEntry || captionEntry.getNavigableSubTargets().contains(entry))
			setExpanded(expanded);
	}
	
	@Override public Pair<C, List<V>> getDisplayedValue() {
		return Pair.of(captionEntry.getDisplayedValue(), listEntry.getDisplayedValue());
	}
	
	@Override public void setDisplayedValue(Pair<C, List<V>> value) {
		captionEntry.setDisplayedValue(value.getKey());
		listEntry.setDisplayedValue(value.getValue());
	}
	
	@Override public List<EntryError> getEntryErrors() {
		List<EntryError> errors = super.getEntryErrors();
		errors.addAll(captionEntry.getEntryErrors());
		errors.addAll(listEntry.getEntryErrors());
		return errors;
	}
	
	@Override public boolean areEqual(Pair<C, List<V>> value, Pair<C, List<V>> other) {
		if (value == null || other == null) return value == other;
		return captionEntry.areEqual(value.getLeft(), other.getLeft())
		       && listEntry.areEqual(value.getRight(), other.getRight());
	}
	
	@Override public boolean isResettable() {
		return super.isResettable();
	}
	
	@Override public boolean isRestorable() {
		return super.isRestorable();
	}
	
	@Override public boolean canResetGroup() {
		Pair<C, List<V>> defValue = getDefaultValue(), value = getDisplayedValue();
		return !listEntry.areEqual(value.getValue(), defValue != null ? defValue.getValue() : null);
	}
	
	@Override public boolean canRestoreGroup() {
		Pair<C, List<V>> original = getOriginal(), value = getDisplayedValue();
		return !listEntry.areEqual(value.getValue(), original != null ? original.getValue() : null);
	}
	
	@Override public @Nullable AbstractConfigField<?> getSingleResettableEntry() {
		final Pair<C, List<V>> defValue = getDefaultValue();
		if (!captionEntry.areEqual(getDisplayedValue().getKey(), defValue != null ? defValue.getKey() : null))
			return captionEntry;
		return null;
	}
	
	@Override public @Nullable AbstractConfigField<?> getSingleRestorableEntry() {
		final Pair<C, List<V>> original = getOriginal();
		if (!captionEntry.areEqual(getDisplayedValue().getKey(), original != null ? original.getKey() : null))
			return captionEntry;
		return null;
	}
	
	@Override public void resetSingleEntry(AbstractConfigField<?> entry) {
		Pair<C, List<V>> defValue = getDefaultValue(), value = getDisplayedValue();
		setValueTransparently(Pair.of(defValue != null? defValue.getLeft() : null, value.getRight()));
	}
	
	@Override public void restoreSingleEntry(AbstractConfigField<?> entry) {
		Pair<C, List<V>> original = getOriginal(), value = getDisplayedValue();
		setValueTransparently(Pair.of(original != null? original.getLeft() : null, value.getRight()));
	}
	
	@Override public Rectangle getSelectionArea() {
		return listEntry.getSelectionArea();
	}
	
	@Override public int getItemHeight() {
		return listEntry.getItemHeight();
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		listEntry.updateFocused(isFocused && getFocused() == listEntry);
		captionEntry.updateFocused(isFocused && getFocused() == captionEntry);
	}
	
	@Override public int getExtraScrollHeight() {
		return listEntry.getExtraScrollHeight();
	}
	
	@Override public String seekableText() {
		return "";
	}
	
	@Override public String seekableValueText() {
		return "";
	}
	
	@Override protected List<ISeekableComponent> seekableChildren() {
		return seekableChildren;
	}
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return children;
	}
	
	@Override public boolean isExpanded() {
		return listEntry.isExpanded();
	}
	
	@Override public void setExpanded(boolean expanded, boolean recurse) {
		listEntry.setExpanded(expanded, recurse);
	}
	
	@Override public int getFocusedScroll() {
		return getFocused() == listEntry? listEntry.getFocusedScroll() : 0;
	}
	@Override public int getFocusedHeight() {
		return getFocused() == listEntry? listEntry.getFocusedHeight() : getCaptionHeight();
	}
	@Override protected String seekableTooltipString() {
		return "";
	}
	
	@Override public List<AbstractConfigField<?>> getHeldEntries() {
		return heldEntries;
	}
	
	@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		if (getFocused() == label && keyCode == GLFW.GLFW_KEY_LEFT && isExpanded()) {
			setExpanded(false, Screen.hasShiftDown());
			playFeedbackTap(0.4F);
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_RIGHT && !isExpanded()) {
			setExpanded(true, Screen.hasShiftDown());
			playFeedbackTap(0.4F);
			return true;
		}
		return super.handleNavigationKey(keyCode, scanCode, modifiers);
	}
	
	@Override public Rectangle getNavigableArea() {
		return label.area;
	}
	
	@Override public List<INavigableTarget> getNavigableChildren(boolean onlyVisible) {
		if (!onlyVisible || isExpanded())
			return listEntry.getNavigableChildren(onlyVisible);
		return super.getNavigableChildren(true);
	}
	
	@Override public List<INavigableTarget> getNavigableSubTargets() {
		List<INavigableTarget> captionSubTargets = captionEntry.getNavigableSubTargets();
		return captionSubTargets.isEmpty()
		       ? Lists.newArrayList(this, captionEntry)
		       : Stream.concat(
					Stream.of(this), captionEntry.getNavigableSubTargets().stream()
		       ).collect(Collectors.toList());
	}
}
