package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.config.ClientConfig.advanced;
import endorh.simpleconfig.ui.api.*;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen.ListWidget;
import endorh.simpleconfig.ui.gui.SimpleConfigScreen.ListWidget.EntryDragAction.ExpandedDragAction;
import endorh.simpleconfig.ui.gui.widget.DynamicEntryListWidget;
import endorh.simpleconfig.ui.gui.widget.ResetButton;
import endorh.simpleconfig.ui.gui.widget.ToggleAnimator;
import endorh.simpleconfig.ui.impl.EditHistory.EditRecord;
import endorh.simpleconfig.ui.impl.EditHistory.EditRecord.ListEditRecord;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.*;

@OnlyIn(value = Dist.CLIENT)
public abstract class BaseListEntry<T, C extends BaseListCell<T>, Self extends BaseListEntry<T, C, Self>>
  extends TooltipListEntry<List<T>> implements IExpandable {
	@NotNull protected final List<C> cells;
	@NotNull protected List<GuiEventListener> widgets;
	@NotNull protected List<GuiEventListener> unexpandedWidgets;
	@NotNull protected List<GuiEventListener> expandedEmptyWidgets;
	
	protected boolean expanded;
	protected boolean deleteButtonEnabled;
	protected boolean insertInFront;
	/** Marks the insert position */
	protected int caret = -1;
	/** Marks the delete/move position */
	protected int cursor = -1;
	/** Marks the hovered cell or -1 */
	protected int hoverCursor = -1;
	/** Marks the dragged cell or -1 */
	protected int dragCursor = -1;
	/** Marks the relative mouse Y inside the dragged/hovered cell */
	protected int draggingOffset = -1;
	protected int cellControlsY = -1;
	protected ListCaptionWidget label;
	protected RedirectGuiEventListener labelReference;
	protected RedirectGuiEventListener sideButtonReferenceReference;
	protected EmptyPlaceholderWidget placeholder;
	@NotNull protected Function<Self, C> cellFactory;
	@Nullable protected Component[] addTooltip = new Component[] {
	  new TranslatableComponent("simpleconfig.help.list.insert"),
	  new TranslatableComponent("simpleconfig.help.list.insert:key")
	};
	@Nullable protected Component[] removeTooltip = new Component[] {
	  new TranslatableComponent("simpleconfig.help.list.remove"),
	  new TranslatableComponent("simpleconfig.help.list.remove:key")
	};
	protected Component[] moveTooltip = new Component[]{
	  new TranslatableComponent("simpleconfig.help.list.move"),
	  new TranslatableComponent("simpleconfig.help.list.move:drag"),
	  new TranslatableComponent("simpleconfig.help.list.move:key")
	};
	
	protected int lastSelected = -1;
	
	protected Rectangle dragOverlayRectangle = new Rectangle();
	// When the mouse is over an overlay, components below receive mouse coordinates
	//   outside the screen to prevent false hover states
	protected int dragMouseX;
	protected int dragMouseY;
	
	protected ToggleAnimator expandAnimator = new ToggleAnimator();
	protected ToggleAnimator heightAnimator = new ToggleAnimator();
	protected int lastHeight = -1;
	
	protected boolean captionControlsEnabled = false;
	protected int dragPlaceHolderColor = 0x42848484;
	protected int dragOverlayBackground = 0x64242424;
	
	public BaseListEntry(
	  @NotNull Component fieldName, @NotNull Function<Self, C> cellFactory
	) {
		super(fieldName);
		setValue(Lists.newArrayList());
		cells = Lists.newArrayList();
		label = new ListCaptionWidget(this);
		labelReference = new RedirectGuiEventListener(label);
		sideButtonReferenceReference = new RedirectGuiEventListener(sideButtonReference);
		widgets = Lists.newArrayList(labelReference, sideButtonReferenceReference);
		placeholder = new EmptyPlaceholderWidget(this);
		unexpandedWidgets = Lists.newArrayList(labelReference, sideButtonReferenceReference);
		expandedEmptyWidgets = Util.make(new ArrayList<>(widgets), l -> l.add(placeholder));
		this.cellFactory = cellFactory;
		acceptButton.setDefaultIcon(SimpleConfigIcons.Buttons.MERGE_ACCEPT_GROUP);
	}
	
	@Override public boolean isExpanded() {
		return expanded;
	}
	
	@Override public void setExpanded(boolean expanded, boolean recursive, boolean animate) {
		if (this.expanded != expanded) {
			if (animate) {
				expandAnimator.setLength(min(250L, cells.size() * 25L));
				expandAnimator.setEaseOutTarget(expanded);
			} else expandAnimator.stopAndSet(expanded);
			cells.forEach(expanded? BaseListCell::onShown : BaseListCell::onHidden);
		}
		this.expanded = expanded;
		if (recursive)
			cells.stream().filter(c -> c instanceof IExpandable)
			  .forEach(c -> ((IExpandable) c).setExpanded(expanded, true));
	}
	
	public boolean areCaptionControlsEnabled() {
		return captionControlsEnabled && isEditable();
	}
	
	public void setCaptionControlsEnabled(boolean enabled) {
		captionControlsEnabled = enabled;
	}
	
	@Override public void setHeadless(boolean headless) {
		super.setHeadless(headless);
		if (headless) {
			labelReference.setTarget(null);
			sideButtonReferenceReference.setTarget(null);
		} else {
			labelReference.setTarget(label);
			sideButtonReferenceReference.setTarget(sideButtonReference);
		}
	}
	
	@Override public void setValue(List<T> value) {
		if (!Objects.equals(getValue(), value))
			super.setValue(new ArrayList<>(value));
	}
	
	@Override public void setValueTransparently(List<T> value) {
		getScreen().getHistory().preserveState(EditRecord.of(this));
		setValue(value);
		if (isDisplayingValue())
			setDisplayedValue(value);
	}
	
	@Override public void setOriginal(List<T> value) {
		super.setOriginal(new ArrayList<>(value));
	}
	
	@Override public void setExternalValue(@Nullable List<T> value) {
		super.setExternalValue(value != null? new ArrayList<>(value) : null);
	}
	
	@Override public List<T> getDisplayedValue() {
		return cells.stream().map(BaseListCell::getValue)
		  .collect(Collectors.toList());
	}
	
	@Override public void setDisplayedValue(List<T> value) {
		cancelDrag();
		for (int i = 0; i < cells.size() && i < value.size(); i++)
			set(i, value.get(i));
		while (cells.size() > value.size())
			remove(cells.size() - 1);
		while (cells.size() < value.size())
			add(cells.size(), value.get(cells.size()));
	}
	
	@Override public boolean isGroup() {
		return true;
	}
	
	public Self self() {
		//noinspection unchecked
		return (Self) this;
	}
	
	public boolean isDeleteButtonEnabled() {
		return deleteButtonEnabled && isEditable();
	}
	
	public void setDeleteButtonEnabled(boolean enabled) {
		this.deleteButtonEnabled = enabled;
	}
	
	protected abstract C createCellWithValue(T var1);
	
	@NotNull public Function<Self, C> getCellFactory() {
		return cellFactory;
	}
	
	public void setCellFactory(@NotNull Function<Self, C> cellFactory) {
		this.cellFactory = cellFactory;
	}
	
	@Nullable public Component[] getAddTooltip() {
		return addTooltip;
	}
	
	public void setAddTooltip(@Nullable Component[] addTooltip) {
		this.addTooltip = addTooltip;
	}
	
	@Nullable public Component[] getRemoveTooltip() {
		return removeTooltip;
	}
	
	public void setRemoveTooltip(@Nullable Component[] removeTooltip) {
		this.removeTooltip = removeTooltip;
	}
	
	@Override public int getItemHeight() {
		int c = getCaptionHeight() + 4;
		if (expanded || expandAnimator.isInProgress()) {
			int h = c + (
			  cells.isEmpty()? placeholder.h :
			  cells.stream().mapToInt(BaseListCell::getCellHeight).sum());
			if (h != lastHeight) {
				heightAnimator.setOutputRange(lastHeight == -1? h : lastHeight, h);
				heightAnimator.resetTarget();
				lastHeight = h;
			}
			return round(expandAnimator.getEaseOut() * (heightAnimator.getEaseOut() - c)) + c;
		}
		return c;
	}
	
	@Override protected int getPreviewCaptionOffset() {
		return 0;
	}
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return expanded ? cells.isEmpty() ? expandedEmptyWidgets : widgets : unexpandedWidgets;
	}
	
	@Override public List<EntryError> getEntryErrors() {
		List<EntryError> errors = super.getEntryErrors();
		cells.stream().flatMap(c -> c.getErrors().stream()).forEach(errors::add);
		return errors;
	}
	
	public void addTransparently() {
		addTransparently(cells.size());
	}
	
	public void addTransparently(T element) {
		addTransparently(cells.size(), element);
	}
	
	public void addTransparently(int index) {
		addTransparently(index, null);
	}
	
	public void addTransparently(int index, T element) {
		if (index < 0 || index > cells.size()) throw new IndexOutOfBoundsException(
		  "Cannot add element at position " + index + ", size: " + cells.size());
		getScreen().getHistory().add(ListEditRecord.remove(
		  this, Util.make(new ArrayList<>(), l -> l.add(index))));
		add(index, element);
		preserveState();
	}
	
	public void add(int index, T element) {
		if (index < 0 || index > cells.size()) throw new IndexOutOfBoundsException(
		  "Cannot add element at position " + index + ", size: " + cells.size());
		final C cell = element == null? cellFactory.apply(self()) : createCellWithValue(element);
		if (index < cells.size()) {
			final int targetWidget = widgets.indexOf(cells.get(index));
			cells.add(index, cell);
			widgets.add(targetWidget, cell);
		} else {
			cells.add(cell);
			widgets.add(cell);
		}
		cell.onAdd();
	}
	
	public void remove(T element) {
		final int index = getDisplayedValue().indexOf(element);
		if (index >= 0) removeTransparently(index);
	}
	
	public void removeTransparently(int index) {
		final C cell = cells.get(index);
		getScreen().getHistory().add(ListEditRecord.add(
		  this, Util.make(new Int2ObjectArrayMap<>(), m -> m.put(index, cell.getValue()))));
		remove(index);
		preserveState();
	}
	
	public void remove(int index) {
		if (index == dragCursor) cancelDrag();
		final int prev = getSelectedIndex();
		final C cell = cells.get(index);
		cell.onDelete();
		cells.remove(cell);
		widgets.remove(cell);
		if (prev != -1 && !cells.isEmpty())
			setFocused(cells.get(index == 0? 0 : index - 1));
		if (index < cells.size())
			cells.subList(index, cells.size()).forEach(BaseListCell::onMove);
	}
	
	public void setTransparently(int index, T element) {
		final C cell = cells.get(index);
		getScreen().getHistory().add(ListEditRecord.modify(
		  this, Util.make(new Int2ObjectArrayMap<>(), m -> m.put(index, cell.getValue()))));
		set(index, element);
		preserveState();
	}
	
	public void set(int index, T element) {
		cells.get(index).setValue(element);
	}
	
	public void markRestoredCell(int index, boolean forInsertion, boolean forRemoval) {
		if (index >= cells.size()) return;
		final C c = cells.get(index);
		if (forInsertion) {
			c.applyFocusHighlight(c.historyInsertColor);
			c.navigate();
		} else if (forRemoval) {
			c.applyFocusHighlight(c.historyRemoveColor);
		} else {
			c.applyFocusHighlight(c.historyApplyColor);
			c.navigate();
		}
	}
	
	@Override public Rectangle getSelectionArea() {
		final DynamicEntryListWidget<?> parent = getEntryList();
		return new Rectangle(parent.left, entryArea.y, parent.right - parent.left, 20);
	}
	
	protected boolean isInsideCreateNew(double mouseX, double mouseY) {
		return areCaptionControlsEnabled()
		       && mouseX >= entryArea.x - 3
		       && mouseY >= entryArea.y + 3
		       && mouseX <= entryArea.x + 6
		       && mouseY <= entryArea.y + 14;
	}
	
	protected boolean isInsideDelete(double mouseX, double mouseY) {
		return areCaptionControlsEnabled() && isDeleteButtonEnabled()
		       && mouseX >= entryArea.x + 10
		       && mouseY >= entryArea.y + 3
		       && mouseX <= entryArea.x + 19
		       && mouseY <= entryArea.y + 14;
	}
	
	protected boolean isInsideInsert(double mouseX, double mouseY) {
		return caret != -1
		       && mouseX >= entryArea.x - 26
		       && mouseX < entryArea.x - 14
		       && mouseY >= cellControlsY
		       && mouseY < cellControlsY + 9;
	}
	
	protected boolean isInsideRemove(double mouseX, double mouseY) {
		return cursor != -1
		       && mouseX >= entryArea.x - 26
		       && mouseX < entryArea.x - 14
		       && mouseY >= cellControlsY
		       && mouseY < cellControlsY + 9;
	}
	
	protected boolean isInsideUp(double mouseX, double mouseY) {
		return cursor != -1
		       && mouseX >= entryArea.x - 10
		       && mouseX < entryArea.x - 3
		       && mouseY >= cellControlsY
		       && mouseY < cellControlsY + 4;
	}
	
	protected boolean isInsideDown(double mouseX, double mouseY) {
		return cursor != -1
		       && mouseX >= entryArea.x - 10
		       && mouseX < entryArea.x - 3
		       && mouseY >= cellControlsY + 5
		       && mouseY < cellControlsY + 9;
	}
	
	@Override protected boolean shouldProvideTooltip(
	  int mouseX, int mouseY, int x, int y, int width, int height
	) {
		return super.shouldProvideTooltip(mouseX, mouseY, x, y, width, height) ||
		       !getScreen().isDragging() && advanced.show_ui_tips &&
		       (isInsideInsert(mouseX, mouseY) || isInsideCreateNew(mouseX, mouseY) ||
		        isInsideDelete(mouseX, mouseY) || isInsideRemove(mouseX, mouseY) ||
		        isInsideUp(mouseX, mouseY) || isInsideDown(mouseX, mouseY));
	}
	
	@Override public Optional<Component[]> getTooltip(int mouseX, int mouseY) {
		if (isEditable() && advanced.show_ui_tips) {
			if (addTooltip != null && (isInsideInsert(mouseX, mouseY) || isInsideCreateNew(mouseX, mouseY)))
				return Optional.of(addTooltip);
			if (removeTooltip != null && (isInsideRemove(mouseX, mouseY) || isInsideDelete(mouseX, mouseY)))
				return Optional.of(removeTooltip);
			if (isInsideUp(mouseX, mouseY) || isInsideDown(mouseX, mouseY))
				return Optional.of(moveTooltip);
		}
		if (mouseY < entryArea.y + 24)
			return super.getTooltip(mouseX, mouseY);
		return Optional.empty();
	}
	
	public List<BaseListCell<T>> getCells() {
		return Collections.unmodifiableList(cells);
	}
	
	@Override protected void renderTitle(
	  PoseStack mStack, Component title, float textX, int index, int x, int y, int entryWidth,
	  int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta
	) {
		super.renderTitle(
		  mStack, title, textX + (areCaptionControlsEnabled()? isDeleteButtonEnabled()? 26 : 13 : 0),
		  index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
	}
	
	@Override public void tick() {
		updateValue(false);
		cells.forEach(BaseListCell::tick);
		super.tick();
		label.setFocused(
		  !isHeadless() && isFocused() && getFocused() == labelReference && dragCursor == -1
		  && (getListParent() == null || getListParent().dragCursor == -1));
	}
	
	@Override public void renderEntry(
	  PoseStack mStack, int index, int x, int y, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		if (isDragging() && !isEditable()) endDrag(mouseX, mouseY, -1);
		if (!isHeadless())
			label.area.setBounds(x - 24, y, entryWidth - 2, 20);
		if (dragCursor != -1) {
			mouseX = dragMouseX;
			mouseY = dragMouseY;
		}
		super.renderEntry(mStack, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		//noinspection unchecked
		BaseListCell<T> focused =
		  !expanded || getFocused() == null ||
		  !(getFocused() instanceof BaseListCell)
		  ? null : (BaseListCell<T>) getFocused();
		boolean insideCreateNew = isInsideCreateNew(mouseX, mouseY);
		boolean insideDelete = isInsideDelete(mouseX, mouseY);
		SimpleConfigIcons.Lists.EXPAND.renderCentered(
		  mStack, x - 15, y + 5, 9, 9,
		  (label.isMouseOver(mouseX, mouseY) && !insideCreateNew && !insideDelete
		   ? 2 : 0) + (expanded ? 1 : 0));
		if (areCaptionControlsEnabled()) {
			SimpleConfigIcons.Lists.ADD.renderCentered(mStack, x - 15 + 13, y + 5, 9, 9, insideCreateNew? 2 : 1);
			if (isDeleteButtonEnabled())
				SimpleConfigIcons.Lists.REMOVE.renderCentered(mStack, x - 15 + 26, y + 5, 9, 9, focused == null ? 0 : insideDelete ? 2 : 1);
		}
		caret = cursor = cellControlsY = hoverCursor = -1;
		final boolean animating = expandAnimator.isInProgress();
		if (expanded || animating) {
			if (animating) ScissorsHandler.INSTANCE.pushScissor(
			  new Rectangle(entryArea.x, entryArea.y, entryArea.width, getItemHeight()));
			if (cells.isEmpty())
				placeholder.render(mStack, mouseX, mouseY, delta);
			int i = 0;
			int yy = y + 24;
			int o = cells.isEmpty()? 0 : cells.get(0).getCellAreaOffset();
			int draggedHeight = dragCursor >= 0 && dragCursor < cells.size()
			                    ? cells.get(dragCursor).getCellHeight() : 0;
			if (mouseY < yy && (mouseY >= yy - 6 || dragCursor != -1)) {
				if (dragCursor != -1) {
					renderDragPlaceHolder(mStack, x, yy + o, entryWidth, draggedHeight - 4);
					yy += draggedHeight;
				}
				if (dragCursor != 0) {
					caret = 0;
					cellControlsY = yy - 7 + (cells.isEmpty() ? 0 : cells.get(0).getCellAreaOffset());
				}
			}
			final DynamicEntryListWidget<?> entryList = getEntryList();
			boolean childDrawsLine = false;
			for (BaseListCell<T> cell : cells) {
				final int ch = cell.getCellHeight();
				o = cell.getCellAreaOffset();
				int yo = 0;
				if (mouseY >= yy + o && mouseY < yy + o + ch) {
					hoverCursor = i;
					if (entryList instanceof ListWidget)
						((ListWidget<?>) entryList).thisTimeTarget = cell.getSelectionArea();
					final int mY = mouseY - yy;
					if (dragCursor == -1) { // Not dragging
						draggingOffset = mY;
						if (mY < o + 4) { // Insert before
							caret = i;
							cellControlsY = yy + o - 7;
						} else if (mY < o + ch - 8) { // Remove/move
							cursor = i;
							cellControlsY = (ch >= 24 ? yy + o + 5 : yy + o + (ch - 2) / 2 - 5);
						} else { // Insert after
							caret = i + 1;
							cellControlsY = yy + o + ch - 7;
						}
					} else if (i != dragCursor) {
						if (mY - draggingOffset < o + ch / 2) {
							caret = i;
							renderDragPlaceHolder(mStack, x, yy + o, entryWidth, draggedHeight - 4);
							yy += draggedHeight;
							cellControlsY = yy + o - 7;
						} else if (mY - draggingOffset + draggedHeight > o + ch / 2) {
							caret = i + 1;
							renderDragPlaceHolder(mStack, x, yy + o + ch, entryWidth, draggedHeight - 4);
							yo += draggedHeight;
							cellControlsY = yy + o + ch - 7;
						}
					}
					childDrawsLine = cell.drawsLine(mouseX - 14, mouseY - yy);
				}
				if (dragCursor == i) {
					int overlayY = Mth.clamp(mouseY - draggingOffset, y + 12, y + entryHeight - ch + 12);
					dragOverlayRectangle.setBounds(
					  x, overlayY + min(0, o),
					  entryWidth, ch + abs(o));
				} else {
					cell.render(
					  mStack, i, x + 14, yy, yy - y, entryWidth - 14, ch, mouseX, mouseY,
					  isFocused() && getFocused() == cell, delta);
					yy += ch + yo;
				}
				i++;
			}
			if (dragCursor != -1) {
				if (mouseY >= yy) {
					renderDragPlaceHolder(mStack, x, yy, entryWidth, draggedHeight - 4);
					if (dragCursor < cells.size() - 1) {
						caret = i;
						cellControlsY = yy + o - 7;
					}
				}
			} else if (isEditable() && mouseX >= x - 32 && mouseX < x + 80
			           && (getScreen().getDragged() == null
			               || getScreen().getDragged().getLeft() != 2)) {
				if (caret != -1) {
					SimpleConfigIcons.Lists.INSERT_ARROW.renderCentered(mStack, x - 26, cellControlsY, 12, 9, isInsideInsert(mouseX, mouseY) ? 1 : 0);
					if (!childDrawsLine) {
						fill(mStack, x - 14, cellControlsY + 3, x - 1 + entryWidth + 1, cellControlsY + 6, 0x24FFFFFF);
						fill(mStack, x - 14, cellControlsY + 4, x - 1 + entryWidth + 1, cellControlsY + 5, 0x64FFFFFF);
					}
				} else if (cursor != -1) {
					SimpleConfigIcons.Lists.DELETE_ARROW.renderCentered(mStack, x - 26, cellControlsY, 12, 9, isInsideRemove(mouseX, mouseY) ? 1 : 0);
					if (cursor > 0)
						SimpleConfigIcons.Lists.UP_ARROW.renderCentered(mStack, x - 10, cellControlsY, 7, 4, isInsideUp(mouseX, mouseY) ? 1 : 0);
					if (cursor < cells.size() - 1)
						SimpleConfigIcons.Lists.DOWN_ARROW.renderCentered(mStack, x - 10, cellControlsY + 5, 7, 4, isInsideDown(mouseX, mouseY) ? 1 : 0);
				}
			}
			if (animating)
				ScissorsHandler.INSTANCE.popScissor();
		}
		if (!isHeadless()) label.render(mStack, mouseX, mouseY, delta);
	}
	
	protected void renderDragPlaceHolder(PoseStack mStack, int x, int y, int width, int height) {
		fill(mStack, x, y, x + width, y + height, dragPlaceHolderColor);
	}
	
	@Override public boolean renderOverlay(
	  PoseStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (area != dragOverlayRectangle)
			return super.renderOverlay(mStack, area, mouseX, mouseY, delta);
		if (dragCursor < 0 || dragCursor >= cells.size()) return false;
		dragMouseX = mouseX;
		dragMouseY = mouseY;
		final C cell = cells.get(dragCursor);
		final int cellHeight = cell.getCellHeight();
		final int o = cell.getCellAreaOffset();
		final int decorationY = area.y + max(0, o);
		fill(mStack, area.x, decorationY, area.getMaxX(),
		     decorationY + cellHeight - 4, dragOverlayBackground);
		cell.render(
		  mStack, dragCursor, area.x + 14, area.y + max(0, -o), -1,
		  area.width - 14, cellHeight, mouseX, mouseY,
		  getEntryList().getFocusedItem() != null && getEntryList().getFocusedItem().equals(this)
		  && getFocused() != null && getFocused().equals(cell), delta);
		return true;
	}
	
	@Override public boolean preserveState() {
		updateValue(true);
		final List<T> value = getValue();
		int i = getSelectedIndex();
		if (i < 0 || i > value.size()) return false;
		
		final ListEditRecord record = ListEditRecord.modify(
		  this, Util.make(new Int2ObjectArrayMap<>(), m -> m.put(i, value.get(i))));
		getScreen().getHistory().preserveState(record);
		return true;
	}
	
	@Range(from = -1, to = Integer.MAX_VALUE)
	protected int getSelectedIndex() {
		//noinspection SuspiciousMethodCalls
		return cells.indexOf(getFocused());
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		for (BaseListCell<T> cell : cells)
			cell.updateSelected(isFocused && expanded && getFocused() == cell);
		int selected = isFocused? getSelectedIndex() : -1;
		if (selected != lastSelected) {
			if (selected != -1) preserveState();
			lastSelected = selected;
		}
	}
	
	@Override public void save() {
		super.save();
	}
	
	public boolean insertInFront() {
		return insertInFront;
	}
	public void setInsertInFront(boolean insertInFront) {
		this.insertInFront = insertInFront;
	}
	
	public void moveTransparently(int from, int to) {
		if (from < 0 || to < 0 || from >= cells.size() || to >= cells.size())
			throw new IndexOutOfBoundsException("Cannot move " + from + " to " + to + ", size: " + cells.size());
		if (from != to) {
			getScreen().getHistory().add(ListEditRecord.move(this, Util.make(
			  new Int2IntArrayMap(1), m -> m.put(to, from))));
		}
		move(from, to);
	}
	
	public void move(int from, int to) {
		if (from < 0 || to < 0 || from >= cells.size() || to >= cells.size())
			throw new IndexOutOfBoundsException("Cannot move " + from + " to " + to + ", size: " + cells.size());
		final C cell = cells.get(from);
		final int widgetTarget = widgets.indexOf(cells.get(to));
		cell.onDelete();
		cells.remove(cell);
		widgets.remove(cell);
		cells.add(to, cell);
		widgets.add(widgetTarget, cell);
		cell.onAdd();
		cells.subList(min(from, to), max(from, to) + 1).forEach(BaseListCell::onMove);
	}
	
	@Override public boolean onMouseClicked(double mouseX, double mouseY, int button) {
		if (isEditable() && !(Screen.hasAltDown() && button == 0 || button == 2)) {
			if (isInsideInsert(mouseX, mouseY)) {
				addTransparently(caret);
				setFocused(cells.get(caret));
				caret = -1;
				playFeedbackClick(1F);
				return true;
			}
			if (isInsideRemove(mouseX, mouseY)) {
				if (getSelectedIndex() == cursor)
					setFocused(cells.isEmpty()? isHeadless()? null : labelReference
					                          : cells.get(max(0, cursor - 1)));
				removeTransparently(cursor);
				cursor = -1;
				playFeedbackClick(1F);
				return true;
			}
			if (isInsideUp(mouseX, mouseY) && cursor > 0) {
				final int target = cursor - 1;
				moveTransparently(cursor, target);
				cursor = -1;
				playFeedbackClick(1F);
				return true;
			}
			if (isInsideDown(mouseX, mouseY) && cursor < cells.size() - 1) {
				int target = cursor + 1;
				moveTransparently(cursor, target);
				cursor = -1;
				playFeedbackClick(1F);
				return true;
			}
		}
		if (super.onMouseClicked(mouseX, mouseY, button))
			return true;
		if (isEditable() && (button == 2 || button == 0 && Screen.hasAltDown())
		    && hoverCursor != -1 && !isDragging()) {
			dragMouseX = (int) mouseX;
			dragMouseY = (int) mouseY;
			dragCursor = hoverCursor;
			getScreen().addOverlay(dragOverlayRectangle, this, -1);
			playFeedbackTap(1F);
			return true;
		}
		return false;
	}
	
	public void cancelDrag() {
		if (dragCursor >= 0 && dragCursor < cells.size()) {
			final C dragged = cells.get(dragCursor);
			dragged.onDragged(dragOverlayRectangle.y - entryArea.y);
			dragCursor = -1;
			dragOverlayRectangle.setBounds(0, 0, 0, 0);
		}
	}
	
	@Override public void endDrag(double mouseX, double mouseY, int button) {
		if (dragCursor >= 0 && dragCursor < cells.size()) {
			final C dragged = cells.get(dragCursor);
			int target = caret > dragCursor ? caret - 1 : caret;
			if (isEditable() && target >= 0 && target < cells.size()) {
				moveTransparently(dragCursor, target);
				dragged.onDragged(dragOverlayRectangle.y - entryArea.y);
			}
			dragCursor = -1;
			dragOverlayRectangle.setBounds(0, 0, 0, 0);
			playFeedbackTap(1F);
		} else super.endDrag(mouseX, mouseY, button);
	}
	
	@Override public List<INavigableTarget> getNavigableChildren(boolean onlyVisible) {
		if (!onlyVisible || isExpanded())
			return Lists.newArrayList(cells);
		return super.getNavigableChildren(true);
	}
	
	// This only catches keys when the caption is selected
	// The rest are caught by AbstractListCell
	@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		if (!isSubEntry() && isEditable() && Screen.hasAltDown() && getSelectedIndex() == -1) {
			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_INSERT) {
				addTransparently(0);
				cells.get(0).navigate();
				playFeedbackTap(1F);
				return true;
			} else if (!cells.isEmpty() && (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE)) {
				removeTransparently(0);
				playFeedbackTap(1F);
				return true;
			}
		}
		return super.handleNavigationKey(keyCode, scanCode, modifiers);
	}
	
	@Override public void navigate() {
		if (isHeadless()) {
			INavigableTarget parent = getNavigableParent();
			if (parent != null && !(parent instanceof BaseListCell)) {
				parent.navigate();
				return;
			}
		}
		super.navigate();
	}
	
	public static class ListCaptionWidget extends CaptionedSubCategoryListEntry.CaptionWidget<BaseListEntry<?, ?, ?>> {
		protected ListCaptionWidget(BaseListEntry<?, ?, ?> expandable) {
			super(expandable);
		}
		
		@Override protected BaseListEntry<?, ?, ?> getParent() {
			return super.getParent();
		}
		
		@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (button == 0 && Screen.hasAltDown() || button == 2) // Ignore drag start
				return false;
			final BaseListEntry<?, ?, ?> parent = getParent();
			final ResetButton resetButton = parent.getResetButton();
			if (resetButton != null && resetButton.isMouseOver(mouseX, mouseY))
				return false;
			if (parent.isEditable() && button == 0) {
				if (parent.isInsideCreateNew(mouseX, mouseY)) {
					parent.setExpanded(true);
					final int idx = parent.insertInFront() ? 0 : parent.cells.size();
					parent.addTransparently(idx);
					parent.setFocused(parent.cells.get(idx));
					playFeedbackClick(1F);
					return true;
				}
				if (parent.isDeleteButtonEnabled() && parent.isInsideDelete(mouseX, mouseY)) {
					GuiEventListener focused = parent.getFocused();
					if (parent.isExpanded() && focused instanceof BaseListCell) {
						final int index = parent.cells.indexOf(focused);
						if (index >= 0)
							parent.removeTransparently(index);
						playFeedbackClick(1F);
					}
					return true;
				}
				if (area.contains(mouseX, mouseY)) {
					final boolean recurse = Screen.hasShiftDown();
					parent.setExpanded(!parent.isExpanded(), recurse);
					DynamicEntryListWidget<?> list = parent.getEntryList();
					if (!recurse && list instanceof ListWidget) {
						((ListWidget<?>) list).startDragAction(new ExpandedDragAction(parent.isExpanded()));
					}
					playFeedbackClick(1F);
					return true;
				}
			}
			return super.mouseClicked(mouseX, mouseY, button);
		}
	}
	
	public static class EmptyPlaceholderWidget implements GuiEventListener {
		protected boolean focused;
		protected BaseListEntry<?, ?, ?> listEntry;
		public int x, y, w, h;
		protected Component text = new TranslatableComponent("simpleconfig.help.list.insert");
		protected int textColor = 0xffa0ffa0;
		protected int borderColor = 0xff80ff80;
		protected int hoveredBgColor = 0x8080f080;
		protected int hoveredTextColor = 0xff80ff80;
		
		public EmptyPlaceholderWidget(BaseListEntry<?, ?, ?> listEntry) {
			this.listEntry = listEntry;
		}
		
		public void render(PoseStack mStack, int mouseX, int mouseY, float delta) {
			final BaseListEntry<?, ?, ?> listEntry = getListEntry();
			final Rectangle area = listEntry.entryArea;
			x = area.x + 16;
			y = area.y + 24;
			w = area.width - 16;
			h = 12;
			final Font font = Minecraft.getInstance().font;
			final List<FormattedCharSequence> lines = font.split(text, w);
			int tw = font.width(lines.get(0)) + 4;
			if (isMouseInside(mouseX, mouseY))
				fill(mStack, x + 1, y + 1, x + tw - 1, y + h - 1, hoveredBgColor);
			if (focused) {
				fill(mStack, x, y, x + tw, y + 1, borderColor);
				fill(mStack, x, y + 1, x + 1, y + h - 1, borderColor);
				fill(mStack, x + tw - 1, y + 1, x + tw, y + h - 1, borderColor);
				fill(mStack, x, y + h - 1, x + tw, y + h, borderColor);
			}
			if (!lines.isEmpty()) {
				font.drawShadow(
				  mStack, lines.get(0), x + 2, y + 2,
				  isMouseInside(mouseX, mouseY)? hoveredTextColor : textColor);
			}
		}
		
		public boolean isMouseInside(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		}
		
		protected BaseListEntry<?, ?, ?> getListEntry() {
			return listEntry;
		}
		
		public Component getText() {
			return text;
		}
		
		public void setText(Component text) {
			this.text = text;
		}
		
		@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (getListEntry().isEditable() && isMouseInside(mouseX, mouseY)) {
				getListEntry().addTransparently();
				return true;
			}
			return false;
		}
		
		@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (getListEntry().isEditable() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_INSERT)) {
				getListEntry().addTransparently();
				return true;
			}
			return GuiEventListener.super.keyPressed(keyCode, scanCode, modifiers);
		}
		
		@Override public boolean changeFocus(boolean forward) {
			return focused = !focused;
		}
	}
	
	@Override public int getFocusedScroll() {
		final GuiEventListener listener = getFocused();
		//noinspection SuspiciousMethodCalls
		if (!cells.contains(listener)) return 0;
		int y = 24;
		//noinspection SuspiciousMethodCalls
		for (C entry : cells.subList(0, cells.indexOf(listener)))
			y += entry.getCellHeight();
		if (listener instanceof IExpandable)
			y += ((IExpandable) listener).getFocusedScroll();
		return y;
	}
	
	@Override public int getFocusedHeight() {
		final GuiEventListener listener = getFocused();
		if (listener instanceof IExpandable)
			return ((IExpandable) listener).getFocusedHeight();
		else if (listener instanceof BaseListCell)
			return ((BaseListCell<?>) listener).getCellHeight();
		else return 20;
	}
	
	@Override public String seekableText() {
		if (isHeadless()) return "";
		return super.seekableText();
	}
	
	@Override public String seekableValueText() {
		return "";
	}
	
	@Override protected List<ISeekableComponent> seekableChildren() {
		return Lists.newArrayList(cells);
	}
	
	@Override public void setFocused(GuiEventListener listener) {
		super.setFocused(listener);
	}
}

