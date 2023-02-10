package endorh.simpleconfig.ui.gui.widget.treeview;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.ScissorsHandler;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.gui.widget.IPositionableRenderable;
import endorh.simpleconfig.ui.gui.widget.ToggleAnimator;
import endorh.simpleconfig.ui.gui.widget.treeview.DragBroadcastableControl.DragBroadcastableAction;
import endorh.simpleconfig.ui.gui.widget.treeview.DragBroadcastableControl.DragBroadcastableAction.WidgetDragBroadcastableAction;
import endorh.simpleconfig.ui.gui.widget.treeview.DragBroadcastableControl.DragBroadcastableWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.ui.gui.AbstractConfigScreen.drawBorderRect;
import static java.lang.Math.max;
import static java.lang.Math.min;

public abstract class ArrangeableTreeViewEntry<E extends ArrangeableTreeViewEntry<E>>
  extends AbstractContainerEventHandler {
	private static final boolean DEBUG_DRAG = false;
	private ArrangeableTreeView<E> tree;
	private E parent;
	private final Rectangle area = new Rectangle();
	protected List<E> subEntries = new ArrayList<>();
	protected final List<GuiEventListener> listeners = new ArrayList<>();
	private E focusedSubEntry;
	private boolean expanded;
	private boolean awaitingSelectionMouseRelease;
	private Point clickedPos = null;
	private final ToggleAnimator expandAnimator = new ToggleAnimator();
	private final ToggleAnimator xAnimator = new ToggleAnimator();
	private final ToggleAnimator yAnimator = new ToggleAnimator();
	
	public ArrangeableTreeViewEntry() {
		cancelAnimations();
	}
	
	public List<E> getSubEntries() {
		return subEntries;
	}
	
	public void addSubEntry(E entry) {
		addSubEntry(getSubEntries().size(), entry);
	}
	public void addSubEntry(int pos, E entry) {
		subEntries.add(pos, entry);
		// noinspection unchecked
		entry.tick(getTree(), (E) this);
	}
	public void removeSubEntry(E entry) {
		tree.setSelected(entry, false);
		subEntries.remove(entry);
	}
	
	public int getOwnHeight() {
		return 24;
	}
	
	public int getVerticalPadding() {
		return 2;
	}
	
	protected final void tick(ArrangeableTreeView<E> tree, @Nullable E parent) {
		this.tree = tree;
		this.parent = parent;
		tick();
		for (E e: getSubEntries()) // noinspection unchecked
			e.tick(tree, (E) this);
	}
	
	protected void tick() {}
	
	public ArrangeableTreeView<E> getTree() {
		return tree;
	}
	
	public E getParent() {
		return parent;
	}
	
	public Rectangle getArea() {
		return area;
	}
	
	public int getTotalHeight(boolean ignoreExpanded, boolean ignoreHidden) {
		ArrangeableTreeView<E> tree = getTree();
		if (!ignoreHidden && isHidden()) return 0;
		return getOwnHeight() + (
		  ignoreExpanded || expanded
		  ? getSubEntries().stream().mapToInt(e -> e.getTotalHeight(ignoreExpanded, ignoreHidden))
			 .sum()
		  : 0
		) + (tree.getDraggedOver() == this && !ignoreHidden? 20 : 0);
	}
	
	public int getHeight() {
		ArrangeableTreeView<E> tree = getTree();
		if (isHidden()) return 0;
		return getOwnHeight()
		       +
		       (expanded? subEntries.stream().mapToInt(ArrangeableTreeViewEntry::getHeight).sum() : 0)
		       + (tree.getDraggedOver() == this? 20 : 0);
	}
	
	public boolean isExpanded() {
		return expanded;
	}
	
	public void setExpanded(boolean expanded) {
		setExpanded(expanded, true);
	}
	
	public void setExpanded(boolean expanded, boolean animated) {
		if (getParent() == null) {
			expanded = true;
			animated = false;
		}
		this.expanded = expanded;
		if (animated) {
			expandAnimator.resetTarget(expanded);
			expandAnimator.setOutputRange(0F, 1F);
		} else {
			float p = expanded? 1F : 0F;
			expandAnimator.setOutputRange(p, p);
		}
	}
	
	public boolean isHidden() {
		ArrangeableTreeView<E> tree = getTree();
		return tree.isDraggingEntries() && tree.isSelected(this);
	}
	
	public boolean isSelectable() {
		return true;
	}
	
	/**
	 * Called before an entry is selected.<br>
	 * By default, it only returns true if the selection is empty.<br>
	 * Override this to enable multi-selection with your own rules.<br>
	 * <b>This method is ignored if {@link #isSelectable()} returns {@code false}.</b>
	 *
	 * @param selection The current selection
	 */
	public boolean canBeAddedToSelection(Set<E> selection) {
		return selection.isEmpty();
	}
	
	/**
	 * Called before a selection can be dropped into this entry as children.<br>
	 *
	 * @param selection The current selection
	 */
	public boolean canBeDroppedInto(int index, List<E> selection) {
		return selection.size() == 1
		       ? getSubEntries().contains(selection.get(0))
		       : new HashSet<>(getSubEntries()).containsAll(selection);
	}
	
	public E getFocusedSubEntry() {
		return focusedSubEntry;
	}
	
	public void expandParents() {
		E p = getParent();
		if (p != null) {
			p.setExpanded(true);
			p.expandParents();
		}
	}
	
	public int getRelY() {
		E p = getParent();
		if (p == null) return 0;
		int y = p.getRelY();
		if (p != tree.getRoot()) y += p.getOwnHeight();
		for (E e: p.getSubEntries()) {
			if (e == this) break;
			y += e.getTotalHeight(false, false);
		}
		return y;
	}
	
	protected Point interpolatePosition(int x, int y) {
		ArrangeableTreeView<E> tree = getTree();
		ArrangeableTreeViewScroller scroller = tree.scroller;
		int treeX = tree.getX(), treeY = (int) (scroller.getY() - scroller.scrollAmount);
		int relX = x - treeX, relY = y - treeY;
		if (relY != (int) yAnimator.getRangeMax()) {
			yAnimator.setOutputRange(
			  yAnimator.getRangeMin() == -1? relY : yAnimator.getEaseOut(), relY);
			yAnimator.resetTarget();
		}
		if (relX != (int) xAnimator.getRangeMax()) {
			xAnimator.setOutputRange(
			  xAnimator.getRangeMin() == -1? relX : xAnimator.getEaseOut(), relX);
			xAnimator.resetTarget();
		}
		return Point.of(
		  treeX + Math.round(xAnimator.getEaseOut()),
		  treeY + Math.round(yAnimator.getEaseOut()));
	}
	
	protected void cancelAnimations() {
		xAnimator.setOutputRange(-1, -1);
		yAnimator.setOutputRange(-1, -1);
		getSubEntries().forEach(ArrangeableTreeViewEntry::cancelAnimations);
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		boolean alt = Screen.hasAltDown();
		return alt && handleNavigationKey(keyCode, scanCode, modifiers)
		       || super.keyPressed(keyCode, scanCode, modifiers)
		       || !alt && handleNavigationKey(keyCode, scanCode, modifiers);
	}
	
	public boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		E focused = focusedSubEntry;
		if (focused != null) return focused.handleNavigationKey(keyCode, scanCode, modifiers);
		boolean shift = Screen.hasShiftDown();
		boolean ctrl = Screen.hasControlDown();
		boolean clear = !shift && !ctrl;
		boolean select = shift || !ctrl;
		if (keyCode == GLFW.GLFW_KEY_LEFT) {
			if (isExpanded()) {
				setExpanded(false);
			} else {
				E p = getParent();
				if (p != null) p.focusAndSelect(clear, select, shift, null);
			}
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
			if (!isExpanded()) {
				setExpanded(true);
			} else {
				Optional<E> first = getSubEntries().stream().findFirst();
				if (first.isPresent()) {
					first.get().focusAndSelect(clear, select, shift, null);
				} else handleNavigationKey(GLFW.GLFW_KEY_DOWN, scanCode, modifiers);
			}
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
			return handleArrowKeys(keyCode == GLFW.GLFW_KEY_DOWN, null);
		} else if (keyCode == GLFW.GLFW_KEY_SPACE && ctrl) {
			ArrangeableTreeView<E> tree = getTree();
			tree.setSelected(this, !tree.isSelected(this));
			return true;
		}
		return false;
	}
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		if (Screen.hasAltDown() && Screen.hasControlDown() && (codePoint == ' ' || codePoint == ' '))
			return true;
		return super.charTyped(codePoint, modifiers);
	}
	
	protected boolean handleArrowKeys(boolean forwards, ArrangeableTreeViewEntry<E> redirect) {
		boolean shift = Screen.hasShiftDown();
		boolean ctrl = Screen.hasControlDown();
		boolean clear = !shift && !ctrl;
		boolean select = shift || !ctrl;
		List<E> subEntries = getSubEntries();
		boolean hasChildren = isExpanded() && !subEntries.isEmpty();
		E p = getParent();
		if (p != null) {
			if (redirect != null && !forwards) {
				if (hasChildren) {
					subEntries.get(subEntries.size() - 1).handleArrowKeys(false, redirect);
				} else focusAndSelect(clear, select, shift, redirect);
				return true;
			}
			if (hasChildren && forwards && redirect == null) {
				subEntries.get(0).focusAndSelect(clear, select, shift, this);
				return true;
			}
			List<E> siblings = p.getSubEntries();
			int step = forwards? 1 : -1;
			// noinspection SuspiciousMethodCalls
			int index = siblings.indexOf(this);
			if (index == -1) return false;
			index += step;
			if (index < 0) {
				if (p.getParent() == null) return p.handleArrowKeys(false, this);
				p.focusAndSelect(clear, select, shift, this);
			} else if (index >= siblings.size()) {
				p.setFocused(null);
				p.handleArrowKeys(true, this);
			} else {
				E entry = siblings.get(index);
				if (forwards) {
					entry.focusAndSelect(clear, select, shift, redirect != null? redirect : this);
				} else entry.handleArrowKeys(false, this);
			}
		} else {
			if (forwards) {
				subEntries.get(0).focusAndSelect(clear, select, shift, redirect);
			} else subEntries.get(subEntries.size() - 1).handleArrowKeys(false, redirect);
		}
		return true;
	}
	
	@Override public void setFocused(@Nullable GuiEventListener listener) {
		setFocused(listener, true);
	}
	
	public void setFocused(@Nullable GuiEventListener listener, boolean scroll) {
		super.setFocused(listener);
		if (focusedSubEntry != null && focusedSubEntry != listener)
			focusedSubEntry.unFocus();
		// noinspection SuspiciousMethodCalls
		if (getSubEntries().contains(listener)) {
			// noinspection unchecked
			E entry = (E) listener;
			focusedSubEntry = entry;
			if (scroll && entry != null && entry.getFocusedSubEntry() == null) entry.ensureVisible();
		} else focusedSubEntry = null;
	}
	
	@Override public boolean changeFocus(boolean focus) {
		boolean res = super.changeFocus(focus);
		ArrangeableTreeView<E> tree = getTree();
		E focused = getFocusedSubEntry();
		if (res && focused != null && focused.getFocusedSubEntry() == null) {
			tree.clearSelection();
			tree.setSelected(focused, true);
		}
		return res;
	}
	
	protected void focusAndSelect(
	  boolean clearSelection, boolean select, boolean retract, @Nullable ArrangeableTreeViewEntry<E> remove
	) {
		focus();
		ArrangeableTreeView<E> tree = getTree();
		E parent = getParent();
		if (select && !isSelectable() && !parent.isSelectable()) select = false;
		// noinspection unchecked
		E self = (E) this;
		if (clearSelection) {
			tree.clearSelection();
		} else if (retract && remove != null && tree.getSelection().contains(self))
			tree.setSelected(remove, false);
		if (select) tree.setSelected(isSelectable()? this : parent, true);
	}
	
	protected void focus() {
		focus(true);
	}
	
	protected void focus(boolean scroll) {
		E parent = getParent();
		if (parent != null) {
			parent.focus(false);
			parent.setFocused(this, false);
		}
		List<GuiEventListener> listeners = children();
		GuiEventListener first = listeners.stream()
		  .findFirst().filter(l -> !(l instanceof ArrangeableTreeViewEntry)).orElse(null);
		setFocused(first);
		if (scroll) ensureVisible();
	}
	
	protected void ensureVisible() {
		int relY = getRelY();
		getTree().scroller.scrollToShow(relY, relY + getOwnHeight());
	}
	
	protected void unFocus() {
		GuiEventListener listener = getFocused();
		setFocused(null);
		if (listener instanceof ArrangeableTreeViewEntry) {
			((ArrangeableTreeViewEntry<?>) listener).unFocus();
		} else if (listener != null) WidgetUtils.forceUnFocus(listener);
	}
	
	public void updateDraggingPos(
	  int x, int y, int mouseX, int mouseY, int insertPos, boolean lastSibling
	) {
		ArrangeableTreeView<E> tree = getTree();
		if (tree.isSelected(this)) return;
		E p = getParent();
		int h = getOwnHeight();
		int yy = p != null? y + h : y;
		// noinspection unchecked
		E self = (E) this;
		int dragY = mouseY + tree.getDragOffsetY();
		int indent = tree.getIndent();
		int halfIndent = indent / 2;
		if (p == null || dragY >= y + h / 2) {
			tree.setDraggedOver(self);
			int dragX = mouseX + tree.getDragOffsetX();
			List<E> subEntries = getSubEntries();
			Optional<E> first = subEntries.stream().filter(e -> !tree.isSelected(e)).findFirst();
			boolean empty = first.isEmpty();
			List<E> draggedEntries = tree.getDraggedEntries();
			if (isExpanded() && (empty || dragY < yy + first.get().getOwnHeight() / 2)
			    && canBeDroppedInto(0, draggedEntries) &&
			    (dragX >= x + halfIndent || !empty)
			    || p == null) {
				tree.setDraggedOverParent(self);
			} else if (
			  p.canBeDroppedInto(insertPos, draggedEntries)
			  && (dragX >= x - halfIndent || !lastSibling)) {
				tree.setDraggedOverParent(p);
			}
			if (isExpanded()) {
				int inPos = 0;
				int lastY = yy;
				E last = null;
				boolean isLast = true;
				for (E sub: subEntries) {
					if (dragY >= yy + sub.getOwnHeight() / 2) {
						if (!tree.isSelected(sub)) {
							last = sub;
							inPos++;
							lastY = yy;
							yy += sub.getHeight();
						}
					} else if (!tree.isSelected(sub)) {
						isLast = false;
						break;
					} else yy += sub.getHeight();
				}
				if (last != null)
					last.updateDraggingPos(x + indent, lastY, mouseX, mouseY, inPos, isLast);
			}
		}
	}
	
	public void renderBackground(
	  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		ArrangeableTreeView<E> tree = getTree();
		if (tree.isDraggingEntries() && tree.isSelected(this)) {
			ArrangeableTreeView.fill(mStack, x, y, x + w, y + h, 0x64646480);
			return;
		}
		E parent = getParent();
		int l = tree.getX();
		int r = tree.getX() + tree.getWidth();
		int b = y + h;
		if (tree.isSelected(this))
			fill(mStack, l, y, r, b, 0x64646480);
		if (parent != null && parent.getFocusedSubEntry() == this && getFocusedSubEntry() == null)
			drawBorderRect(mStack, l, y, r, b, 1, 0x80808080, 0);
		if (DEBUG_DRAG) {
			if (tree.getDraggedOver() == this)
				drawBorderRect(mStack, l - 1, y - 1, r + 1, b + 1, 1, 0x8000FF00, 0);
			if (tree.getDraggedOverParent() == this && parent != null)
				drawBorderRect(mStack, l - 2, y - 2, r + 2, b + 2, 1, 0x80FFFF00, 0);
		}
	}
	
	public void render(
	  PoseStack mStack, int x, int y, int width, int mouseX, int mouseY, float delta
	) {
		Point pos = interpolatePosition(x, y);
		int ix = pos.getX();
		int iy = pos.getY();
		int height = getOwnHeight();
		renderBackground(mStack, ix, iy, width, height, mouseX, mouseY, delta);
		if (!subEntries.isEmpty() && height < 10) height = 10;
		area.setBounds(ix, iy, width, height);
		
		E p = getParent();
		int yy = y;
		if (p != null) {
			if (!subEntries.isEmpty() || isForceRenderAsGroup()) SimpleConfigIcons.Widgets.TREE_ARROW.renderCentered(
			  mStack, ix, iy, 16, height, isExpanded()? 1 : 0);
			if (isSelectable()) SimpleConfigIcons.Widgets.TREE_DRAG_HANDLE.renderCentered(
			  mStack, ix + 16, iy, 8, height);
			int padding = getVerticalPadding();
			renderContent(
			  mStack, ix + 24, iy + padding, width - 24, height - 2 * padding,
			  mouseX, mouseY, delta);
			yy += height;
		}
		ArrangeableTreeView<E> tree = getTree();
		int indent = tree.getIndent();
		boolean hasDragPreview = tree.isDraggingEntries() && tree.getDraggedOver() == this;
		if (isExpanded() && !subEntries.isEmpty()) {
			float e = expandAnimator.getEaseOut();
			if (e < 1F) ScissorsHandler.INSTANCE.pushScissor(new Rectangle(
			  tree.getX(), y, tree.getWidth(), (int) (e * getTotalHeight(false, false))));
			{
				if (hasDragPreview && tree.getDraggedOverParent() == this) {
					renderDragPreview(mStack, yy);
					yy += 20;
					hasDragPreview = false;
				}
				for (ArrangeableTreeViewEntry<E> subCell: subEntries) {
					if (!subCell.isHidden() || isHidden()) {
						subCell.render(mStack, x + indent, yy, width - indent, mouseX, mouseY, delta);
						yy += subCell.getTotalHeight(false, false);
					}
				}
			}
			if (e < 1F) ScissorsHandler.INSTANCE.popScissor();
		}
		if (hasDragPreview) renderDragPreview(mStack, yy);
	}
	
	protected void renderDragPreview(PoseStack mStack, int y) {
		ArrangeableTreeView<E> tree = getTree();
		int destX = tree.getDraggedOverParent().getArea().x + tree.getIndent();
		drawBorderRect(mStack, destX, y, tree.area.getMaxX(), y + 20, 1, 0x8080A0FF, 0x646480FF);
		SimpleConfigIcons.Widgets.TREE_DRAG_HANDLE.renderCentered(
		  mStack, destX + 16, y, 8, 20);
	}
	
	public abstract void renderContent(
	  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta);
	
	public boolean isForceRenderAsGroup() {
		return false;
	}
	
	public boolean isMouseOverSelf(double mouseX, double mouseY) {
		ArrangeableTreeView<E> tree = getTree();
		int x = tree.getX();
		return x <= mouseX && mouseX < x + tree.getWidth() && area.y <= mouseY && mouseY < area.getMaxY();
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		ArrangeableTreeView<E> tree = getTree();
		int x = tree.getX();
		return x <= mouseX && mouseX < x + tree.getWidth() && area.y <= mouseY && mouseY < area.y + getHeight();
	}
	
	public boolean isMouseOverArrow(double mouseX, double mouseY) {
		return area.contains(mouseX, mouseY) && mouseX < area.x + 16;
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		ArrangeableTreeView<E> tree = getTree();
		E p = getParent();
		clickedPos = Point.of(mouseX, mouseY);
		if (p != null && area.contains(mouseX, mouseY) && isSelectable()) {
			E focused = p.getFocusedSubEntry();
			boolean isSelected = tree.isSelected(this);
			if (Screen.hasShiftDown() && focused != null && focused != this) {
				List<E> siblings = p.getSubEntries();
				// noinspection unchecked
				int selfPos = siblings.indexOf((E) this);
				int focPos = siblings.indexOf(focused);
				tree.setSelected(this, true);
				for (int i = min(selfPos, focPos); i <= max(selfPos, focPos); i++)
					tree.setSelected(siblings.get(i), true);
				return true;
			} else if (
			  Screen.hasControlDown()
			  && (isSelected || canBeAddedToSelection(tree.getSelection()))
			) {
				tree.setSelected(this, !isSelected);
				return true;
			} else if (!isSelected) {
				tree.clearSelection();
				tree.setSelected(this, true);
			} else awaitingSelectionMouseRelease = true;
		}
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		if (button == 0 && isMouseOverArrow(mouseX, mouseY)) {
			setExpanded(!isExpanded());
			tree.startExpandDrag(isExpanded());
			return true;
		}
		setFocused(null);
		return area.contains(mouseX, mouseY);
	}
	
	@Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
		ArrangeableTreeView<E> tree = getTree();
		if (awaitingSelectionMouseRelease) {
			tree.clearSelection();
			tree.setSelected(this, true);
			awaitingSelectionMouseRelease = false;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}
	
	@Override public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
		ArrangeableTreeView<E> tree = getTree();
		if (!tree.isDraggingEntries()
		    && tree.isSelected(this)
		    && (clickedPos == null || clickedPos.distance(mouseX, mouseY) > 8)) {
			awaitingSelectionMouseRelease = false;
			tree.startDrag((int) (area.x - mouseX), (int) (area.y - mouseY), button);
			return false;
		}
		return false;
	}
	
	@Override public @NotNull List<GuiEventListener> children() {
		if (this == tree.getRoot()) return Lists.newArrayList(subEntries);
		return isExpanded()? Stream.concat(
		  listeners.stream(), subEntries.stream()
		).collect(Collectors.toList()) : listeners;
	}
	
	public <W extends IPositionableRenderable> DragBroadcastableControl<W> draggable(
	  DragBroadcastableAction<W> action, W widget
	) {
		return new DragBroadcastableControl<>(this::getTree, action, widget);
	}
	
	public <W extends AbstractWidget> DragBroadcastableWidget<W> draggable(
	  WidgetDragBroadcastableAction<W> action, W widget
	) {
		return new DragBroadcastableWidget<>(this::getTree, action, widget);
	}
}
