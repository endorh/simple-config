package endorh.simpleconfig.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.clothconfig2.api.*;
import endorh.simpleconfig.clothconfig2.gui.AbstractConfigScreen;
import endorh.simpleconfig.clothconfig2.gui.INavigableTarget;
import endorh.simpleconfig.clothconfig2.gui.WidgetUtils;
import endorh.simpleconfig.clothconfig2.gui.widget.DynamicEntryListWidget;
import endorh.simpleconfig.clothconfig2.impl.ISeekableComponent;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import endorh.simpleconfig.clothconfig2.api.AbstractConfigEntry.EntryError;

@OnlyIn(Dist.CLIENT)
public class DecoratedListEntry<V, E extends AbstractListListEntry<V, ?, E>,
  C, CE extends AbstractConfigListEntry<C> & IChildListEntry>
  extends TooltipListEntry<Pair<C, List<V>>> implements IExpandable {
	protected E listEntry;
	protected CE captionEntry;
	protected List<IGuiEventListener> children;
	
	public DecoratedListEntry(ITextComponent fieldName, E listEntry, CE captionEntry) {
		super(fieldName);
		this.listEntry = listEntry;
		this.captionEntry = captionEntry;
		this.captionEntry.setChild(true);
		this.captionEntry.setListParent(listParent);
		this.listEntry.setNavigableParent(this);
		this.listEntry.setHeldEntry(this.captionEntry);
		this.listEntry.setExpandableParent(this);
		this.listEntry.setParent(getParentOrNull());
		this.listEntry.setScreen(getConfigScreenOrNull());
		this.listEntry.setListParent(listParent);
		this.listEntry.setParentEntry(parentEntry);
		this.listEntry.setName(name);
		children = Lists.newArrayList(listEntry);
	}
	
	@Override public void setParentEntry(AbstractConfigEntry<?> parentEntry) {
		super.setParentEntry(parentEntry);
		listEntry.setParentEntry(parentEntry);
	}
	
	@Override public void setCategory(ConfigCategory category) {
		super.setCategory(category);
		listEntry.setCategory(category);
	}
	
	@Override public void setName(String name) {
		super.setName(name);
		if (listEntry != null) listEntry.setName(name);
	}
	
	@Override public void setListParent(@Nullable BaseListEntry<?, ?, ?> listParent) {
		super.setListParent(listParent);
		listEntry.setListParent(listParent);
	}
	
	@Override protected void doExpandParents() {
		expandParents();
	}
	
	public E getListEntry() {
		return listEntry;
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight,
	  int mouseX, int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		     listEntry.render(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered,
		                  delta);
	}
	
	@Override public Pair<C, List<V>> getValue() {
		return Pair.of(captionEntry.getValue(), listEntry.getValue());
	}
	
	@Override public void setValue(Pair<C, List<V>> value) {
		if (!Objects.equals(captionEntry.getValue(), value.getKey()))
			captionEntry.setValue(value.getKey());
		if (!Objects.equals(listEntry.getValue(), value.getValue()))
			listEntry.setValue(value.getValue());
	}
	
	@Override public boolean isEditable() {
		return listEntry.isEditable();
	}
	
	@Override public void setEditable(boolean editable) {
		listEntry.setEditable(editable);
	}
	
	@Override public boolean isRequiresRestart() {
		return listEntry.isRequiresRestart();
	}
	
	@Override public void setRequiresRestart(boolean requiresRestart) {
		listEntry.setRequiresRestart(requiresRestart);
	}
	
	@Override public Rectangle getEntryArea(
	  int x, int y, int entryWidth, int entryHeight
	) {
		return listEntry.getEntryArea(x, y, entryWidth, entryHeight);
	}
	
	@Override public int getItemHeight() {
		return listEntry.getItemHeight();
	}
	
	@Override protected void acquireFocus() {
		setFocused(listEntry);
		listEntry.updateSelected(false);
		listEntry.setFocused(listEntry.label);
		WidgetUtils.forceFocus(listEntry.label);
	}
	
	@Override public void save() {
		super.save();
		listEntry.save();
	}
	
	@Override public Optional<EntryError> getError() {
		return listEntry.getError();
	}
	
	@Override public void updateSelected(boolean isSelected) {
		super.updateSelected(isSelected);
		listEntry.updateSelected(isSelected);
	}
	
	@Override public IGuiEventListener getFocused() {
		return listEntry;
	}
	
	@Override public void setScreen(AbstractConfigScreen screen) {
		super.setScreen(screen);
		listEntry.setScreen(screen);
	}
	
	@Override public void setParent(DynamicEntryListWidget<?> parent) {
		super.setParent(parent);
		listEntry.setParent(parent);
	}
	
	@Override public boolean isEdited() {
		return !ignoreEdits && listEntry.isEdited();
	}
	
	@Override public int getExtraScrollHeight() {
		return listEntry.getExtraScrollHeight();
	}
	
	@Override public boolean isResettable() {
		return listEntry.isResettable();
	}
	
	@Override public boolean isRestorable() {
		return listEntry.isRestorable();
	}
	
	@Override public void resetValue(boolean commit) {
		listEntry.resetValue(commit);
	}
	
	@Override public void restoreValue(Object storedValue) {
		listEntry.suppressRecords = true;
		super.restoreValue(storedValue);
		listEntry.suppressRecords = false;
	}
	
	@Override public String seekableText() {
		return "";
	}
	
	@Override public String seekableValueText() {
		return "";
	}
	
	@Override protected List<ISeekableComponent> seekableChildren() {
		return Lists.newArrayList(listEntry);
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> children() {
		return children;
	}
	
	@Override public boolean isExpanded() {
		return listEntry.isExpanded();
	}
	
	@Override public void setExpanded(boolean expanded, boolean recurse) {
		listEntry.setExpanded(expanded, recurse);
	}
	
	@Override public int getFocusedScroll() {
		return listEntry.getFocusedScroll();
	}
	
	@Override public int getFocusedHeight() {
		return listEntry.getFocusedHeight();
	}
	
	@Override protected boolean shouldProvideTooltip(
	  int mouseX, int mouseY, int x, int y, int width, int height
	) {
		return listEntry.shouldProvideTooltip(mouseX, mouseY, x, y, width, height);
	}
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		return listEntry.getTooltip(mouseX, mouseY);
	}
	
	@Override public Optional<ITextComponent[]> getTooltip() {
		return listEntry.getTooltip();
	}
	
	@Override public @Nullable Supplier<Optional<ITextComponent[]>> getTooltipSupplier() {
		return listEntry.getTooltipSupplier();
	}
	
	@Override public void setTooltipSupplier(
	  @Nullable Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		listEntry.setTooltipSupplier(tooltipSupplier);
	}
	
	@Override protected String seekableTooltipString() {
		return "";
	}
	
	@Override public List<INavigableTarget> getNavigableChildren() {
		final List<INavigableTarget> children = listEntry.getNavigableChildren();
		children.remove(listEntry);
		children.add(0, this);
		return children;
	}
}
