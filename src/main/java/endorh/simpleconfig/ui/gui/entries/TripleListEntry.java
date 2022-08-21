package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.icon.Icon;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class TripleListEntry<
  L, M, R, LE extends AbstractConfigField<L> & IChildListEntry,
  ME extends AbstractConfigField<M> & IChildListEntry,
  RE extends AbstractConfigField<R> & IChildListEntry
> extends TooltipListEntry<Triple<L, M, R>> implements IChildListEntry, IEntryHolder {
	private final LE leftEntry;
	private final ME middleEntry;
	private final RE rightEntry;
	protected @Nullable Icon leftIcon;
	protected @Nullable Icon rightIcon;
	protected float leftWeight = 0.333F;
	protected float rightWeight = 0.333F;
	
	protected List<IGuiEventListener> listeners;
	protected List<AbstractConfigField<?>> heldEntries;
	protected List<ISeekableComponent> seekableChildren;
	
	public TripleListEntry(
	  ITextComponent fieldName, Triple<L, M, R> value,
	  LE leftEntry, ME middleEntry, RE rightEntry
	) {
		super(fieldName);
		this.leftEntry = leftEntry;
		this.middleEntry = middleEntry;
		this.rightEntry = rightEntry;
		leftEntry.setChildSubEntry(true);
		middleEntry.setChildSubEntry(true);
		rightEntry.setChildSubEntry(true);
		leftEntry.setParentEntry(this);
		middleEntry.setParentEntry(this);
		rightEntry.setParentEntry(this);
		leftEntry.setName("left");
		middleEntry.setName("middle");
		rightEntry.setName("right");
		setValue(value);
		setDisplayedValue(value);
		listeners = Lists.newArrayList(leftEntry, middleEntry, rightEntry, sideButtonReference);
		heldEntries = Lists.newArrayList(leftEntry, middleEntry, rightEntry);
		seekableChildren = Lists.newArrayList(leftEntry, middleEntry, rightEntry);
	}
	
	@Override public Triple<L, M, R> getDisplayedValue() {
		final L left = leftEntry.getDisplayedValue();
		final M middle = middleEntry.getDisplayedValue();
		final R right = rightEntry.getDisplayedValue();
		return left == null && middle == null && right == null ? null :
		       Triple.of(left, middle, right);
	}
	
	@Override public void setDisplayedValue(Triple<L, M, R> value) {
		leftEntry.setDisplayedValue(value == null? null : value.getLeft());
		middleEntry.setDisplayedValue(value == null? null : value.getMiddle());
		rightEntry.setDisplayedValue(value == null? null : value.getRight());
	}
	
	@Override public boolean areEqual(Triple<L, M, R> value, Triple<L, M, R> other) {
		if (value == null || other == null) return value == other;
		return leftEntry.areEqual(value.getLeft(), other.getLeft())
		       && middleEntry.areEqual(value.getMiddle(), other.getMiddle())
		       && rightEntry.areEqual(value.getRight(), other.getRight());
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		leftEntry.updateFocused(isFocused && getFocused() == leftEntry);
		middleEntry.updateFocused(isFocused && getFocused() == middleEntry);
		rightEntry.updateFocused(isFocused && getFocused() == rightEntry);
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
		int leftIconWidth = leftIcon != null? leftIcon.w : 4;
		int rightIconWidth = rightIcon != null? rightIcon.w : 4;
		final int entriesWidth = w - leftIconWidth - rightIconWidth;
		int leftWidth = (int) (entriesWidth * leftWeight);
		int rightWidth = (int) (entriesWidth * min(rightWeight, 1F - leftWeight));
		int middleWidth = (int) (entriesWidth * max(0F, 1F - leftWeight - rightWeight));
		if (leftIcon != null)
			leftIcon.renderCentered(mStack, x + leftWidth, y, leftIcon.w, h);
		if (rightIcon != null)
			rightIcon.renderCentered(mStack, x + leftWidth, y, rightIcon.w, h);
		leftEntry.renderChild(
		  mStack, x, y, leftWidth, h, mouseX, mouseY, delta);
		middleEntry.renderChild(
		  mStack, x + leftWidth + leftIconWidth, y, middleWidth, h, mouseX, mouseY, delta);
		rightEntry.renderChild(
		  mStack, x + leftWidth + leftIconWidth + middleWidth + rightIconWidth, y,
		  rightWidth, h, mouseX, mouseY, delta);
	}
	
	@Override public int getExtraScrollHeight() {
		return max(leftEntry.getExtraScrollHeight(),
		           max(middleEntry.getExtraScrollHeight(), rightEntry.getExtraScrollHeight()));
	}
	
	@Override public List<INavigableTarget> getNavigableSubTargets() {
		List<INavigableTarget> targets = new ArrayList<>();
		List<INavigableTarget> leftTargets = leftEntry.getNavigableSubTargets();
		if (!leftTargets.isEmpty()) targets.addAll(leftTargets);
		else targets.add(leftEntry);
		List<INavigableTarget> middleTargets = middleEntry.getNavigableSubTargets();
		if (!middleTargets.isEmpty()) targets.addAll(middleTargets);
		else targets.add(middleEntry);
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
	
	public ME getMiddleEntry() {
		return middleEntry;
	}
	
	public RE getRightEntry() {
		return rightEntry;
	}
	
	public @Nullable Icon getLeftIcon() {
		return leftIcon;
	}
	
	public void setLeftIcon(@Nullable Icon leftIcon) {
		this.leftIcon = leftIcon;
	}
	
	public @Nullable Icon getRightIcon() {
		return rightIcon;
	}
	
	public void setRightIcon(@Nullable Icon rightIcon) {
		this.rightIcon = rightIcon;
	}
	
	public float getLeftWeight() {
		return leftWeight;
	}
	
	public void setLeftWeight(float leftWeight) {
		this.leftWeight = leftWeight;
	}
	
	public float getRightWeight() {
		return rightWeight;
	}
	
	public void setRightWeight(float rightWeight) {
		this.rightWeight = rightWeight;
	}
}

