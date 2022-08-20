package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.icon.Icon;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

public class PairListEntry<
  L, R, LE extends AbstractConfigField<L> & IChildListEntry,
  RE extends AbstractConfigField<R> & IChildListEntry
> extends TooltipListEntry<Pair<L, R>> implements IChildListEntry, IEntryHolder {
	private final LE leftEntry;
	private final RE rightEntry;
	protected @Nullable Icon middleIcon = null;
	protected float splitPos = 0.5F;
	protected List<IGuiEventListener> listeners;
	protected List<AbstractConfigField<?>> heldEntries;
	protected List<ISeekableComponent> seekableChildren;
	
	public PairListEntry(
	  ITextComponent fieldName, Pair<L, R> value, LE leftEntry, RE rightEntry
	) {
		super(fieldName);
		this.leftEntry = leftEntry;
		this.rightEntry = rightEntry;
		leftEntry.setChildSubEntry(true);
		rightEntry.setChildSubEntry(true);
		leftEntry.setParentEntry(this);
		rightEntry.setParentEntry(this);
		leftEntry.setName("left");
		rightEntry.setName("right");
		setValue(value);
		setDisplayedValue(value);
		listeners = Lists.newArrayList(leftEntry, rightEntry, sideButtonReference);
		heldEntries = Lists.newArrayList(leftEntry, rightEntry);
		seekableChildren = Lists.newArrayList(leftEntry, rightEntry);
	}
	
	@Override public Pair<L, R> getDisplayedValue() {
		final L left = leftEntry.getDisplayedValue();
		final R right = rightEntry.getDisplayedValue();
		return left == null && right == null? null : Pair.of(left, right);
	}
	
	@Override public void setDisplayedValue(Pair<L, R> value) {
		leftEntry.setDisplayedValue(value == null? null : value.getLeft());
		rightEntry.setDisplayedValue(value == null? null : value.getRight());
	}
	
	@Override public boolean areEqual(Pair<L, R> value, Pair<L, R> other) {
		if (value == null || other == null) return value == other;
		return leftEntry.areEqual(value.getLeft(), other.getLeft())
		       && rightEntry.areEqual(value.getRight(), other.getRight());
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		leftEntry.updateFocused(isFocused && getListener() == leftEntry);
		rightEntry.updateFocused(isFocused && getListener() == rightEntry);
	}
	
	@Override public boolean isGroup() {
		return false;
	}
	
	@Override public List<EntryError> getEntryErrors() {
		List<EntryError> errors = super.getEntryErrors();
		errors.addAll(IEntryHolder.super.getErrors());
		return errors;
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		int iconWidth = middleIcon != null? middleIcon.w : 4;
		int leftWidth = (int) ((w - iconWidth) * splitPos);
		if (middleIcon != null)
			middleIcon.renderCentered(mStack, x + leftWidth, y, middleIcon.w, h);
		leftEntry.renderChild(
		  mStack, x, y, leftWidth, h, mouseX, mouseY, delta);
		rightEntry.renderChild(
		  mStack, x + leftWidth + iconWidth, y, w - iconWidth - leftWidth, h,
		  mouseX, mouseY, delta);
	}
	
	@Override public int getExtraScrollHeight() {
		return max(leftEntry.getExtraScrollHeight(), rightEntry.getExtraScrollHeight());
	}
	
	@Override public List<INavigableTarget> getNavigableSubTargets() {
		List<INavigableTarget> targets = new ArrayList<>();
		List<INavigableTarget> leftTargets = leftEntry.getNavigableSubTargets();
		if (!leftTargets.isEmpty()) targets.addAll(leftTargets);
		else targets.add(leftEntry);
		List<INavigableTarget> rightTargets = rightEntry.getNavigableSubTargets();
		if (!rightTargets.isEmpty()) targets.addAll(rightTargets);
		else targets.add(rightEntry);
		return targets;
	}
	
	@Override protected List<ISeekableComponent> seekableChildren() {
		return seekableChildren;
	}
	
	@Override protected @NotNull List<? extends IGuiEventListener> getEntryListeners() {
		return listeners;
	}
	
	@Override public List<AbstractConfigField<?>> getHeldEntries() {
		return heldEntries;
	}
	
	public LE getLeftEntry() {
		return leftEntry;
	}
	public RE getRightEntry() {
		return rightEntry;
	}
	
	public @Nullable Icon getMiddleIcon() {
		return middleIcon;
	}
	public void setMiddleIcon(@Nullable Icon middleIcon) {
		this.middleIcon = middleIcon;
	}
	
	public float getSplitPos() {
		return splitPos;
	}
	public void setSplitPos(float splitPos) {
		this.splitPos = splitPos;
	}
}
