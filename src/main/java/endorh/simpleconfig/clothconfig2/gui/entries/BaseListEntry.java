package endorh.simpleconfig.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import endorh.simpleconfig.clothconfig2.api.IExpandable;
import endorh.simpleconfig.clothconfig2.api.ScissorsHandler;
import endorh.simpleconfig.clothconfig2.gui.AbstractConfigScreen;
import endorh.simpleconfig.clothconfig2.gui.IOverlayCapableScreen.IOverlayRenderer;
import endorh.simpleconfig.clothconfig2.gui.WidgetUtils;
import endorh.simpleconfig.clothconfig2.gui.entries.SubCategoryListEntry.ToggleAnimator;
import endorh.simpleconfig.clothconfig2.gui.widget.DynamicEntryListWidget;
import endorh.simpleconfig.clothconfig2.gui.widget.ResetButton;
import endorh.simpleconfig.clothconfig2.impl.EditHistory;
import endorh.simpleconfig.clothconfig2.impl.EditHistory.EditRecord.ListEditRecord;
import endorh.simpleconfig.clothconfig2.impl.ISeekableComponent;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.*;

@OnlyIn(value = Dist.CLIENT)
public abstract class BaseListEntry<T, C extends BaseListCell<T>, Self extends BaseListEntry<T, C, Self>>
  extends TooltipListEntry<List<T>>
  implements IExpandable, IOverlayRenderer, IEntryHoldingListEntry {
	@NotNull protected final List<C> cells;
	@NotNull protected List<IGuiEventListener> widgets;
	@NotNull protected List<IGuiEventListener> unexpandedWidgets;
	@NotNull protected List<IGuiEventListener> expandedEmptyWidgets;
	@Nullable protected AbstractConfigListEntry<?> heldEntry;
	
	protected boolean expanded;
	protected ToggleAnimator expandAnimator = new ToggleAnimator();
	protected boolean deleteButtonEnabled;
	protected boolean insertInFront;
	protected int caret = -1;
	protected int cursor = -1;
	protected int hoverCursor = -1;
	protected int dragCursor = -1;
	protected int draggingOffset = -1;
	protected int marginWidgetY = -1;
	protected ListCaptionWidget label;
	protected ResetButton resetButton;
	protected EmptyPlaceholderWidget placeholder;
	@NotNull protected Function<Self, C> cellFactory;
	@Nullable protected ITextComponent addTooltip =
	  new TranslationTextComponent("text.cloth-config.list.add");
	@Nullable protected ITextComponent removeTooltip =
	  new TranslationTextComponent("text.cloth-config.list.remove");
	protected ITextComponent[] moveTooltip = new ITextComponent[]{
	  new TranslationTextComponent("simpleconfig.help.list.move"),
	  new TranslationTextComponent("simpleconfig.help.list.move.drag")
	};
	
	protected boolean suppressRecords = false;
	protected Pair<Integer, Object> preservedState;
	
	protected Rectangle dragOverlayRectangle = new Rectangle();
	// When the mouse is over an overlay, components below receive mouse coordinates
	//   outside the screen to prevent false hover states
	protected int dragMouseX;
	protected int dragMouseY;
	
	protected boolean captionControlsEnabled = false;
	
	public BaseListEntry(
	  @NotNull ITextComponent fieldName, @NotNull Function<Self, C> cellFactory
	) {
		super(fieldName);
		cells = Lists.newArrayList();
		label = new ListCaptionWidget(this);
		resetButton = new ResetButton(this);
		widgets = Lists.newArrayList(label, resetButton);
		placeholder = new EmptyPlaceholderWidget(this);
		unexpandedWidgets = Lists.newArrayList(label, resetButton);
		expandedEmptyWidgets = Util.make(new LinkedList<>(widgets), l -> l.add(placeholder));
		this.cellFactory = cellFactory;
	}
	
	@Override public boolean isExpanded() {
		return expanded;
	}
	
	@Override public void setExpanded(boolean expanded, boolean recursive) {
		if (this.expanded != expanded) {
			expandAnimator.setLength(min(250L, cells.size() * 25L));
			expandAnimator.setEaseOutTarget(expanded);
		}
		this.expanded = expanded;
		if (recursive)
			cells.stream().filter(c -> c instanceof IExpandable)
			  .forEach(c -> ((IExpandable) c).setExpanded(expanded, true));
	}
	
	public boolean isSuppressRecords() {
		return suppressRecords;
	}
	
	public void setSuppressRecords(boolean suppressRecords) {
		this.suppressRecords = suppressRecords;
		if (suppressRecords) preservedState = null;
	}
	
	public boolean areCaptionControlsEnabled() {
		return captionControlsEnabled;
	}
	
	public void setCaptionControlsEnabled(boolean enabled) {
		captionControlsEnabled = enabled;
	}
	
	@Override public void resetValue(boolean commit) {
		suppressRecords = true;
		ListEditRecord record = null;
		if (commit) record = ListEditRecord.full(this);
		super.resetValue(commit);
		if (commit) {
			record.flatten(getConfigScreen());
			if (!record.isEmpty()) addRecord(record);
		}
		suppressRecords = false;
	}
	
	@Override public void restoreValue(boolean commit) {
		suppressRecords = true;
		ListEditRecord record = null;
		if (commit) record = ListEditRecord.full(this);
		super.restoreValue(commit);
		if (commit) {
			record.flatten(getConfigScreen());
			if (!record.isEmpty()) addRecord(record);
		}
		suppressRecords = false;
	}
	
	@Override public List<T> getValue() {
		return this.cells.stream().map(BaseListCell::getValue).collect(Collectors.toList());
	}
	
	@Override public void setValue(List<T> value) {
		boolean commit = false; // !suppressRecords;
		suppressRecords = true;
		ListEditRecord record = null;
		if (commit) record = ListEditRecord.full(this);
		for (int i = 0; i < cells.size() && i < value.size(); i++)
			cells.get(i).setValue(value.get(i));
		while (cells.size() > value.size())
			remove(cells.size() - 1);
		while (cells.size() < value.size())
			add(value.get(cells.size()));
		if (commit) {
			record.flatten(getConfigScreen());
			if (!record.isEmpty()) addRecord(record);
		}
		suppressRecords = !commit;
	}
	
	@Override public void restoreValue(Object storedValue) {
		suppressRecords = true;
		super.restoreValue(storedValue);
		suppressRecords = false;
	}
	
	@Override public boolean isEdited() {
		return !ignoreEdits && (heldEntry != null && heldEntry.isEdited() || super.isEdited());
	}
	
	@Override public boolean isRequiresRestart() {
		return heldEntry != null && heldEntry.isRequiresRestart() || cells.stream().anyMatch(BaseListCell::isRequiresRestart);
	}
	
	@Override public boolean isEditable() {
		return super.isEditable();
	}
	
	@Override public void setEditable(boolean editable) {
		super.setEditable(editable);
		if (heldEntry != null)
			heldEntry.setEditable(editable);
	}
	
	public Self self() {
		//noinspection unchecked
		return (Self) this;
	}
	
	public boolean isDeleteButtonEnabled() {
		return deleteButtonEnabled;
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
	
	@Nullable public ITextComponent getAddTooltip() {
		return addTooltip;
	}
	
	public void setAddTooltip(@Nullable ITextComponent addTooltip) {
		this.addTooltip = addTooltip;
	}
	
	@Nullable public ITextComponent getRemoveTooltip() {
		return removeTooltip;
	}
	
	public void setRemoveTooltip(@Nullable ITextComponent removeTooltip) {
		this.removeTooltip = removeTooltip;
	}
	
	@Override public int getItemHeight() {
		if (expanded || expandAnimator.isInProgress()) {
			if (cells.isEmpty()) return 24 + placeholder.h;
			int i = 24;
			for (BaseListCell<T> entry : cells)
				i += entry.getCellHeight();
			return round(expandAnimator.getEaseOut() * (i - 24)) + 24;
		}
		return 24;
	}
	
	public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return expanded ? cells.isEmpty() ? expandedEmptyWidgets : widgets : unexpandedWidgets;
	}
	
	@Internal @Override public Optional<ITextComponent> getErrorMessage() {
		return Optional.empty();
	}
	
	public void add() {
		add(cells.size());
	}
	
	public void add(T element) {
		add(cells.size(), element);
	}
	
	public void add(int index) {
		add(index, null);
	}
	
	public void add(int index, T element) {
		if (index < 0 || index > cells.size())
			throw new IndexOutOfBoundsException(
			  "Cannot add element at position " + index + ", size: " + cells.size());
		if (!suppressRecords && getConfigScreenOrNull() != null) {
			savePreservedState();
			addRecord(ListEditRecord.remove(
			  this, Util.make(new ArrayList<>(), l -> l.add(index))));
			preservedState = Pair.of(index, null);
		}
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
		final int index = getValue().indexOf(element);
		if (index >= 0)
			remove(index);
	}
	
	public void remove(int index) {
		final int prev = getSelectedIndex();
		final C cell = cells.get(index);
		if (!suppressRecords) {
			savePreservedState();
			addRecord(ListEditRecord.add(
			  this, Util.make(new Int2ObjectArrayMap<>(), m -> m.put(index, cell.getValue()))));
		}
		cell.onDelete();
		cells.remove(cell);
		widgets.remove(cell);
		if (prev != -1 && !cells.isEmpty())
			setListener(cells.get(index == 0? 0 : index - 1));
	}
	
	public void set(int index, T element) {
		cells.get(index).setValue(element);
	}
	
	public void markRestoredCell(int index, boolean forRemoval) {
		if (index >= cells.size()) return;
		final C c = cells.get(index);
		if (forRemoval) {
			c.lastHistoryTime = System.currentTimeMillis();
			c.lastHistoryRemove = true;
		} else {
			c.lastHistoryTime = System.currentTimeMillis();
			c.lastHistoryRemove = false;
			c.onNavigate();
		}
	}
	
	@Override public Rectangle getEntryArea(int x, int y, int entryWidth, int entryHeight) {
		return new Rectangle(
		  getParent().left, y, getParent().right - getParent().left, 20);
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
		       && mouseY >= marginWidgetY
		       && mouseY < marginWidgetY + 9;
	}
	
	protected boolean isInsideRemove(double mouseX, double mouseY) {
		return cursor != -1
		       && mouseX >= entryArea.x - 26
		       && mouseX < entryArea.x - 14
		       && mouseY >= marginWidgetY
		       && mouseY < marginWidgetY + 9;
	}
	
	protected boolean isInsideUp(double mouseX, double mouseY) {
		return cursor != -1
		       && mouseX >= entryArea.x - 10
		       && mouseX < entryArea.x - 3
		       && mouseY >= marginWidgetY
		       && mouseY < marginWidgetY + 4;
	}
	
	protected boolean isInsideDown(double mouseX, double mouseY) {
		return cursor != -1
		       && mouseX >= entryArea.x - 10
		       && mouseX < entryArea.x - 3
		       && mouseY >= marginWidgetY + 5
		       && mouseY < marginWidgetY + 9;
	}
	
	public boolean isHeldEntryHovered(int mouseX, int mouseY) {
		return mouseX >= entryArea.getMaxX() - 148
		       && mouseX < entryArea.getMaxX() - 4 - resetButton.getWidth()
		       && mouseY >= entryArea.y && mouseY < entryArea.getMaxY();
	}
	
	@Override protected boolean shouldProvideTooltip(
	  int mouseX, int mouseY, int x, int y, int width, int height
	) {
		return super.shouldProvideTooltip(mouseX, mouseY, x, y, width, height) ||
		       baseShouldProvideTooltip() &&
		       (isInsideInsert(mouseX, mouseY) || isInsideCreateNew(mouseX, mouseY) ||
		        isInsideDelete(mouseX, mouseY) || isInsideRemove(mouseX, mouseY) ||
		        isInsideUp(mouseX, mouseY) || isInsideDown(mouseX, mouseY));
	}
	
	@Override public Optional<ITextComponent[]> getTooltip(int mouseX, int mouseY) {
		if (addTooltip != null && (isInsideInsert(mouseX, mouseY) || isInsideCreateNew(mouseX, mouseY)))
			return Optional.of(new ITextComponent[]{addTooltip});
		if (removeTooltip != null && (isInsideRemove(mouseX, mouseY) || isInsideDelete(mouseX, mouseY)))
			return Optional.of(new ITextComponent[]{removeTooltip});
		if (isInsideUp(mouseX, mouseY) || isInsideDown(mouseX, mouseY))
			return Optional.of(moveTooltip);
		if (resetButton.isMouseOver(mouseX, mouseY))
			return resetButton.getTooltip(mouseX, mouseY);
		if (isHeldEntryHovered(mouseX, mouseY))
			return Optional.empty();
		if (mouseY < entryArea.y + 24)
			return super.getTooltip(mouseX, mouseY);
		return Optional.empty();
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		if (dragCursor != -1) {
			mouseX = dragMouseX;
			mouseY = dragMouseY;
		}
		if (!isEditable() && isExpanded()) setExpanded(false);
		label.area.setBounds(x - 24, y, heldEntry != null? entryWidth - 132 : entryWidth + 17 - resetButton.getWidth(), 20);
		WidgetUtils.forceSetFocus(label, isSelected && getListener() == label && dragCursor == -1
		                                 && (getListParent() == null || getListParent().dragCursor == -1));
		super.renderEntry(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		bindTexture();
		//noinspection unchecked
		BaseListCell<T> focused =
		  !expanded || getListener() == null ||
		  !(getListener() instanceof BaseListCell)
		  ? null : (BaseListCell<T>) getListener();
		boolean insideCreateNew = isInsideCreateNew(mouseX, mouseY);
		boolean insideDelete = isInsideDelete(mouseX, mouseY);
		blit(mStack, x - 15, y + 5, 33,
		     (label.isMouseOver(mouseX, mouseY) && !insideCreateNew && !insideDelete
		      ? 18 : 0) + (expanded ? 9 : 0), 9, 9);
		int labelX = x + 2;
		if (captionControlsEnabled) {
			blit(mStack, x - 15 + 13, y + 5, 42, insideCreateNew ? 9 : 0, 9, 9);
			labelX += 13;
			if (isDeleteButtonEnabled()) {
				blit(
				  mStack, x - 15 + 26, y + 5, 51, focused == null ? 0 : (insideDelete ? 18 : 9), 9, 9);
				labelX += 13;
			}
		}
		resetButton.x = x + entryWidth - resetButton.getWidth();
		resetButton.y = y;
		resetButton.render(mStack, mouseX, mouseY, delta);
		Minecraft.getInstance().fontRenderer.func_238407_a_(
		  mStack, getDisplayedFieldName().func_241878_f(), (float) labelX, (float) (y + 6),
		  label.isMouseOver(mouseX, mouseY) && !resetButton.isMouseOver(
			 mouseX, mouseY) && !insideDelete && !insideCreateNew
		  ? 0xffe6fe16 : getPreferredTextColor());
		caret = cursor = marginWidgetY = hoverCursor = -1;
		final boolean animating = expandAnimator.isInProgress();
		if (expanded || animating) {
			if (animating) ScissorsHandler.INSTANCE.scissor(
			  new Rectangle(entryArea.x, entryArea.y, entryArea.width, getItemHeight()));
			if (cells.isEmpty())
				placeholder.render(mStack, mouseX, mouseY, delta);
			int i = 0;
			int yy = y + 24;
			if (mouseY < yy && (mouseY >= yy - 6 || dragCursor != -1)) {
				if (dragCursor != 0) {
					caret = 0;
					marginWidgetY =
					  yy - 7 + (cells.isEmpty() ? 0 : cells.get(0).getCellDecorationOffset());
				}
			}
			boolean childDrawsLine = false;
			for (BaseListCell<T> cell : cells) {
				final int cellHeight = cell.getCellHeight();
				if (mouseY >= yy && mouseY < yy + cellHeight) {
					hoverCursor = i;
					final int mY = mouseY - yy;
					final int offset = cell.getCellDecorationOffset();
					if (dragCursor == -1) {
						draggingOffset = mY;
						if (mY < 4 + offset) {
							caret = i;
							marginWidgetY = yy - 7 + offset;
						} else if (mY < cellHeight - 8 + offset) {
							cursor = i;
							marginWidgetY =
							  (cellHeight >= 24 ? yy + 5 : yy + (cellHeight - 2) / 2 - 5) + offset;
						} else {
							caret = i + 1;
							marginWidgetY = yy + cellHeight - 7 + offset;
						}
					} else {
						if (mY < cellHeight / 2 - 2 + offset) {
							if (i != dragCursor && i != dragCursor + 1) {
								caret = i;
								marginWidgetY = yy - 7 + offset;
							}
						} else {
							if (i != dragCursor - 1 && i != dragCursor) {
								caret = i + 1;
								marginWidgetY = yy + cellHeight - 7 + offset;
							}
						}
					}
					childDrawsLine = cell.drawsLine(mouseX - 14, mouseY - yy);
				}
				if (dragCursor == i) {
					final int offset = cell.getCellDecorationOffset();
					fill(
					  mStack, x, yy + offset, x + entryWidth, yy + cellHeight - 4 + offset, 0x64848484);
					dragOverlayRectangle.setBounds(
					  x, max(y + 12, min(y + entryHeight - cellHeight + 12, mouseY - draggingOffset)) +
					     min(0, offset) - 1,
					  entryWidth, cellHeight + max(0, offset) + 2);
				} else {
					cell.render(
					  mStack, i, yy, x + 14, entryWidth - 14, cellHeight, mouseX, mouseY,
					  getParent().getFocusedItem() != null && getParent().getFocusedItem()
						 .equals(this) && getListener() != null && getListener().equals(cell), delta);
				}
				i++;
				yy += cellHeight;
			}
			if (dragCursor != -1) {
				if (mouseY >= yy && dragCursor < cells.size() - 1) {
					caret = i;
					marginWidgetY =
					  yy - 7 + (cells.isEmpty() ? 0 : cells.get(0).getCellDecorationOffset());
				}
				if (caret != -1) {
					bindTexture();
					blit(mStack, x - 10, marginWidgetY, 93, 0, 9, 9);
					fill(mStack, x, marginWidgetY + 3, x - 1 + entryWidth + 1, marginWidgetY + 6, 0x24FFFFFF);
					fill(mStack, x, marginWidgetY + 4, x - 1 + entryWidth + 1, marginWidgetY + 5, 0x64FFFFFF);
				}
			} else if (mouseX >= x - 32 && mouseX < x + 80
			           && (getConfigScreen().getDragged() == null
			               || getConfigScreen().getDragged().getLeft() != 2)) {
				if (caret != -1) {
					bindTexture();
					blit(mStack, x - 26, marginWidgetY, 60, isInsideInsert(mouseX, mouseY) ? 9 : 0, 12, 9);
					if (!childDrawsLine) {
						fill(mStack, x - 14, marginWidgetY + 3, x - 1 + entryWidth + 1, marginWidgetY + 6, 0x24FFFFFF);
						fill(mStack, x - 14, marginWidgetY + 4, x - 1 + entryWidth + 1, marginWidgetY + 5, 0x64FFFFFF);
					}
				} else if (cursor != -1) {
					bindTexture();
					blit(
					  mStack, x - 26, marginWidgetY, 72, isInsideRemove(mouseX, mouseY) ? 9 : 0, 12, 9);
					if (cursor > 0)
						blit(mStack, x - 10, marginWidgetY, 85, isInsideUp(mouseX, mouseY) ? 9 : 0, 7, 4);
					if (cursor < cells.size() - 1)
						blit(mStack, x - 10, marginWidgetY + 5, 85, isInsideDown(mouseX, mouseY) ? 14 : 5, 7, 4);
				}
			}
			if (animating)
				ScissorsHandler.INSTANCE.removeLastScissor();
		}
		label.render(mStack, mouseX, mouseY, delta);
		if (heldEntry != null)
			((IChildListEntry) heldEntry).renderChild(
			  mStack, x + entryWidth - 148, y, 144 - resetButton.getWidth(), 20, mouseX, mouseY, delta);
	}
	
	@Override public boolean renderOverlay(
	  MatrixStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (dragCursor < 0 || dragCursor >= cells.size())
			return false;
		dragMouseX = mouseX;
		dragMouseY = mouseY;
		final C cell = cells.get(dragCursor);
		final int cellHeight = cell.getCellHeight();
		final int offset = cell.getCellDecorationOffset();
		int draggedY = area.y - min(0, offset) + 1;
		// new Rectangle(draggedX - 14, draggedY + min(0, offset) - 1, draggedWidth + 14, cellHeight + max(0, offset) + 2)
		fill(mStack, area.x, draggedY + offset, area.getMaxX(), draggedY + cellHeight + offset - 3, 0x64242424);
		fill(mStack, area.x, draggedY + offset, area.getMaxX(), draggedY + cellHeight + offset - 3, 0x42848484);
		cell.render(
		  mStack, dragCursor, draggedY, area.x + 14, area.width - 14, cellHeight, mouseX, mouseY,
		  getParent().getFocusedItem() != null && getParent().getFocusedItem().equals(this)
		  && getListener() != null && getListener().equals(cell), delta);
		return true;
	}
	
	protected void addRecord(ListEditRecord record) {
		record = wrap(record);
		if (!record.isEmpty())
			getConfigScreen().getHistory().addRecord(record);
	}
	
	public boolean preserveState() {
		int i = getSelectedIndex();
		if (getListener() == heldEntry && heldEntry != null) i = -2;
		if (preservedState != null) {
			if (preservedState.getKey() != i)
				savePreservedState();
			else return false;
		}
		if (i != -1 && !suppressRecords)
			preservedState = Pair.of(i, i == -2? heldEntry.getValue() : getValue().get(i));
		final EditHistory history = getConfigScreen().getHistory();
		history.setOnHistory(() -> {
			savePreservedState();
			history.setOnHistory(null);
			history.setPeek(null);
		});
		history.setPeek(() -> {
			if (preservedState == null) {
				history.setPeek(null);
				return false;
			}
			if (preservedState.getKey() == -2) {
				return heldEntry != null && !Objects.equals(heldEntry.getValue(), preservedState.getValue());
			} else return !Objects.equals(getValue().get(preservedState.getKey()), preservedState.getValue());
		});
		return !suppressRecords;
	}
	
	protected void savePreservedState() {
		if (preservedState != null && !suppressRecords) {
			if (preservedState.getKey() == -2) {
				if (heldEntry != null && !Objects.equals(heldEntry.getValue(), preservedState.getValue()))
					addRecord(ListEditRecord.caption(this, preservedState.getValue()));
			} else if (preservedState.getValue() != null
			    && !Objects.equals(getValue().get(preservedState.getKey()), preservedState.getValue())) {
				addRecord(ListEditRecord.modify(this, Util.make(
				  new Int2ObjectArrayMap<>(1),
				  m -> m.put(preservedState.getKey(), preservedState.getValue()))));
			}
			preservedState = null;
		}
	}
	
	@Range(from = -1, to = Integer.MAX_VALUE)
	protected int getSelectedIndex() {
		//noinspection SuspiciousMethodCalls
		return cells.indexOf(getListener());
	}
	
	protected ListEditRecord wrap(ListEditRecord record) {
		final BaseListEntry<?, ?, ?> listParent = getListParent();
		if (listParent != null) {
			if (listParent.suppressRecords)
				return ListEditRecord.EMPTY;
			ListEditRecord w = ListEditRecord.sub(
			  listParent, Util.make(new Int2ObjectArrayMap<>(1), m -> m.put(listParent.getSelectedIndex(), record)));
			return listParent.wrap(w);
		}
		return record;
	}
	
	@Override public void updateSelected(boolean isSelected) {
		if (this.isSelected && !isSelected) {
			savePreservedState();
		} else if (isSelected) {
			if (getSelectedIndex() == -1 && getListener() != heldEntry)
				savePreservedState();
			else preserveState();
		}
		super.updateSelected(isSelected);
		if (isSelected)
			getConfigScreen().getHistory().discardPreservedState();
		if (heldEntry != null)
			heldEntry.updateSelected(isSelected && getListener() == heldEntry);
		for (BaseListCell<T> cell : cells)
			cell.updateSelected(isSelected && expanded && getListener() == cell);
		if (!isSelected)
			WidgetUtils.forceUnFocus(resetButton);
	}
	
	@Override public void save() {
		super.save();
		if (heldEntry != null) heldEntry.save();
	}
	
	@Override public int getInitialReferenceOffset() {
		return 24;
	}
	
	public boolean insertInFront() {
		return insertInFront;
	}
	
	public void setInsertInFront(boolean insertInFront) {
		this.insertInFront = insertInFront;
	}
	
	public void move(int from, int to) {
		if (!suppressRecords) {
			savePreservedState();
			addRecord(ListEditRecord.move(this, Util.make(
			  new Int2IntArrayMap(1), m -> m.put(to, from))));
		}
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
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!(Screen.hasAltDown() && button == 0 || button == 2)) {
			if (isInsideInsert(mouseX, mouseY)) {
				add(caret);
				setListener(cells.get(caret));
				caret = -1;
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1F));
				return true;
			}
			if (isInsideRemove(mouseX, mouseY)) {
				if (getSelectedIndex() == cursor)
					setListener(cells.isEmpty()? label : cells.get(max(0, cursor - 1)));
				remove(cursor);
				cursor = -1;
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1F));
				return true;
			}
			if (isInsideUp(mouseX, mouseY) && cursor > 0) {
				final int target = cursor - 1;
				move(cursor, target);
				cursor = -1;
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1F));
				return true;
			}
			if (isInsideDown(mouseX, mouseY) && cursor < cells.size() - 1) {
				int target = cursor + 1;
				move(cursor, target);
				cursor = -1;
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1F));
				return true;
			}
		}
		if (super.mouseClicked(mouseX, mouseY, button))
			return true;
		if ((button == 2 || button == 0 && Screen.hasAltDown())
		    && hoverCursor != -1 && !isDragging()) {
			dragMouseX = (int) mouseX;
			dragMouseY = (int) mouseY;
			dragCursor = hoverCursor;
			getConfigScreen().claimRectangle(dragOverlayRectangle, this, -1);
			Minecraft.getInstance().getSoundHandler().play(
			  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
			return true;
		}
		return false;
	}
	
	@Override public void endDrag(double mouseX, double mouseY, int button) {
		if (dragCursor >= 0 && dragCursor < cells.size()) {
			int target = caret > dragCursor ? caret - 1 : caret;
			if (target >= 0 && target < cells.size())
				move(dragCursor, target);
			dragCursor = -1;
			dragOverlayRectangle.setBounds(0, 0, 0, 0);
			Minecraft.getInstance().getSoundHandler().play(
			  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
		} else super.endDrag(mouseX, mouseY, button);
	}
	
	// This only catches keys when the caption is selected
	// The rest are caught by AbstractListCell
	@Override public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		if (Screen.hasAltDown() && getSelectedIndex() == -1) {
			if (keyCode == 257 || keyCode == 260) { // Enter || Insert
				add(0);
				cells.get(0).onNavigate();
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
				return true;
			} else if (!cells.isEmpty() && (keyCode == 259 || keyCode == 261)) { // Backspace || Delete
				remove(0);
				Minecraft.getInstance().getSoundHandler().play(
				  SimpleSound.master(SimpleConfigMod.UI_TAP, 1F));
				return true;
			}
		}
		return super.handleNavigationKey(keyCode, scanCode, modifiers);
	}
	
	public static class ListCaptionWidget extends SubCategoryListEntry.CaptionWidget {
		protected ListCaptionWidget(BaseListEntry<?, ?, ?> expandable) {
			super(expandable);
		}
		
		@Override protected BaseListEntry<?, ?, ?> getParent() {
			return (BaseListEntry<?, ?, ?>) super.getParent();
		}
		
		@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (button == 0 && Screen.hasAltDown() || button == 2) // Ignore drag start
				return false;
			final BaseListEntry<?, ?, ?> parent = getParent();
			if (parent.resetButton.isMouseOver(mouseX, mouseY))
				return false;
			if (button == 0) {
				if (parent.isInsideCreateNew(mouseX, mouseY)) {
					parent.setExpanded(true);
					final int idx = parent.insertInFront() ? 0 : parent.cells.size();
					parent.add(idx);
					parent.setListener(parent.cells.get(idx));
					Minecraft.getInstance().getSoundHandler().play(
					  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1F));
					return true;
				}
				if (parent.isDeleteButtonEnabled() && parent.isInsideDelete(mouseX, mouseY)) {
					IGuiEventListener focused = parent.getListener();
					if (parent.isExpanded() && focused instanceof BaseListCell) {
						final int index = parent.cells.indexOf(focused);
						if (index >= 0)
							parent.remove(index);
						Minecraft.getInstance().getSoundHandler().play(
						  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1F));
					}
					return true;
				}
				if (area.contains(mouseX, mouseY)) {
					parent.setExpanded(!parent.isExpanded(), Screen.hasShiftDown());
					Minecraft.getInstance().getSoundHandler().play(
					  SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1F));
					return true;
				}
			}
			return super.mouseClicked(mouseX, mouseY, button);
		}
	}
	
	public static class EmptyPlaceholderWidget implements IGuiEventListener {
		protected boolean focused;
		protected WeakReference<BaseListEntry<?, ?, ?>> listEntry;
		public int x;
		public int y;
		public int w;
		public int h;
		protected ITextComponent text = new TranslationTextComponent("text.cloth-config.list.add");
		protected int textColor = 0xffa0ffa0;
		protected int hoveredTextColor = 0xff42ff42;
		
		public EmptyPlaceholderWidget(BaseListEntry<?, ?, ?> listEntry) {
			this.listEntry = new WeakReference<>(listEntry);
		}
		
		public void render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
			final BaseListEntry<?, ?, ?> listEntry = getListEntry();
			final Rectangle area = listEntry.entryArea;
			x = area.x + 16;
			y = area.y + 24;
			w = area.width - 16;
			h = 12;
			final FontRenderer font = Minecraft.getInstance().fontRenderer;
			final List<IReorderingProcessor> lines = font.trimStringToWidth(text, w);
			if (!lines.isEmpty()) {
				font.func_238407_a_(
				  mStack, lines.get(0), x + 2, y + 2,
				  isMouseInside(mouseX, mouseY)? hoveredTextColor : textColor);
			}
		}
		
		public boolean isMouseInside(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		}
		
		protected BaseListEntry<?, ?, ?> getListEntry() {
			return listEntry.get();
		}
		
		public ITextComponent getText() {
			return text;
		}
		
		public void setText(ITextComponent text) {
			this.text = text;
		}
		
		@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (isMouseInside(mouseX, mouseY)) {
				getListEntry().add();
				return true;
			}
			return false;
		}
		
		@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (keyCode == 257 || keyCode == 32 || keyCode == 260) {
				getListEntry().add();
				return true;
			}
			return IGuiEventListener.super.keyPressed(keyCode, scanCode, modifiers);
		}
		
		@Override public boolean changeFocus(boolean forward) {
			return focused = !focused;
		}
	}
	
	@Override public void setParent(DynamicEntryListWidget<?> parent) {
		super.setParent(parent);
		if (heldEntry != null)
			heldEntry.setParent(parent);
	}
	
	@Override public void setScreen(AbstractConfigScreen screen) {
		super.setScreen(screen);
		if (heldEntry != null)
			heldEntry.setScreen(screen);
	}
	
	@Override public @Nullable <E extends AbstractConfigListEntry<?> & IChildListEntry> E getHeldEntry() {
		//noinspection unchecked
		return (E) heldEntry;
	}
	
	@Override public <E extends AbstractConfigListEntry<?> & IChildListEntry> void setHeldEntry(
	  E entry
	) {
		if (heldEntry != null) {
			widgets.remove(heldEntry);
			unexpandedWidgets.remove(heldEntry);
		}
		heldEntry = entry;
		heldEntry.setParentEntry(this);
		heldEntry.setListParent(this);
		entry.setChild(true);
		widgets.add(1, heldEntry);
		expandedEmptyWidgets.add(1, heldEntry);
		unexpandedWidgets.add(1, heldEntry);
		heldEntry.setParent(getParentOrNull());
		heldEntry.setScreen(getConfigScreenOrNull());
	}
	
	@Override public int getFocusedScroll() {
		final IGuiEventListener listener = getListener();
		//noinspection SuspiciousMethodCalls
		if (!cells.contains(listener))
			return 0;
		int y = 24;
		//noinspection SuspiciousMethodCalls
		for (C entry : cells.subList(0, cells.indexOf(listener)))
			y += entry.getCellHeight();
		if (listener instanceof IExpandable)
			y += ((IExpandable) listener).getFocusedScroll();
		return y;
	}
	
	@Override public int getFocusedHeight() {
		final IGuiEventListener listener = getListener();
		if (listener instanceof IExpandable)
			return ((IExpandable) listener).getFocusedHeight();
		else if (listener instanceof BaseListCell)
			return ((BaseListCell<?>) listener).getCellHeight();
		else return 20;
	}
	
	@Override protected List<ISeekableComponent> seekableChildren() {
		return cells.stream().map(c -> ((ISeekableComponent) c)).collect(Collectors.toList());
	}
	
	@Override public void setListener(IGuiEventListener listener) {
		super.setListener(listener);
	}
}

