package endorh.simpleconfig.ui.gui.widget.treeview;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.api.RedirectGuiEventListener;
import endorh.simpleconfig.ui.gui.widget.IPositionableRenderable;
import endorh.simpleconfig.ui.gui.widget.IPositionableRenderable.IRectanglePositionableRenderable;
import endorh.simpleconfig.ui.gui.widget.treeview.DragBroadcastableControl.DragBroadcastableAction;
import endorh.simpleconfig.ui.hotkey.ConfigHotKeyTreeView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static endorh.simpleconfig.ui.gui.AbstractConfigScreen.drawBorderRect;
import static java.lang.Math.max;

/**
 * Arrangeable Tree View widget.<br>
 * Subclass {@link ArrangeableTreeViewEntry} to specify how cells are rendered,
 * and how can they be selected and rearranged.
 * @param <E> Type of the entries. Usually you'd create a base class for the different kind
 *            of entries your tree supports. You may use multiple subclasses with different roles,
 *            as it's done in {@link ConfigHotKeyTreeView}.
 */
public class ArrangeableTreeView<E extends ArrangeableTreeViewEntry<E>> extends AbstractContainerEventHandler
  implements IOverlayRenderer, IRectanglePositionableRenderable, IDragBroadcastableControlContainer {
	private final IOverlayCapableContainer overlayContainer;
	private final E root;
	private @Nullable ArrangeableTreeViewCaption<E> caption = null;
	private final RedirectGuiEventListener captionReference = new RedirectGuiEventListener(null);
	private int indent = 12;
	private Component placeHolder = Component.translatable("simpleconfig.ui.nothing_here_yet");
	
	protected final Rectangle area = new Rectangle();
	protected final Rectangle overlayArea = new Rectangle();
	protected final ArrangeableTreeViewScroller scroller;
	protected List<GuiEventListener> listeners;
	
	private boolean transparent = false;
	private int borderColor = 0x80808080;
	private int fillColor = 0x32242424;
	
	private Pair<Integer, GuiEventListener> dragged;
	// Entries can only be added to selection if they return true from canBeAddedToSelection(Set<E>)
	private final Set<E> selection = new HashSet<>();
	// Multiple entries can be dragged at once if they were selected
	// The selection can only be dropped if the destination parent entry returns true from
	//   canBeDroppedInto(int, List<E>)
	private List<E> draggedEntries;
	private long lastAutoDrag = 0L;
	private int dragOffsetX;
	private int dragOffsetY;
	// The closest entry where the dragged entries can be dropped below
	private E draggedOver;
	// The destination parent entry for the dragged entries
	// In some positions of the tree, there may be multiple possibilities, which are disambiguated
	//   by the horizontal position of the dragged entries
	private E draggedOverParent = (null);
	private DragBroadcastableAction<?> dragBroadcastableAction = null;
	private DragBroadcastableControl<?> dragBroadcastableSource = null;
	private @Nullable Boolean draggedExpandState = null;
	
	public ArrangeableTreeView(IOverlayCapableContainer overlayContainer, E root) {
		this.overlayContainer = overlayContainer;
		// this.scroller = new ScrollingContainerWidget();
		this.root = root;
		root.setExpanded(true);
		tick();
		scroller = new ArrangeableTreeViewScroller(this);
		listeners = Lists.newArrayList(captionReference, scroller);
	}
	
	public void tick() {
		getRoot().tick(this, null);
	}
	
	public E getRoot() {
		return root;
	}
	
	public @Nullable ArrangeableTreeViewCaption<E> getCaption() {
		return caption;
	}
	public void setCaption(@Nullable ArrangeableTreeViewCaption<E> caption) {
		this.caption = caption;
		captionReference.setTarget(caption);
	}
	
	public Component getPlaceHolder() {
		return placeHolder;
	}
	public void setPlaceHolder(Component placeHolder) {
		this.placeHolder = placeHolder;
	}
	
	protected boolean isFocusedCaption() {
		return getFocused() == captionReference;
	}
	
	// Layout properties
	
	public int getIndent() {
		return indent;
	}
	public void setIndent(int indent) {
		this.indent = indent;
	}
	
	@Override public Rectangle getArea() {
		return area;
	}
	
	public int getPlaceHolderHeight() {
		Component placeHolder = getPlaceHolder();
		if (placeHolder != null) {
			Font font = Minecraft.getInstance().font;
			return font.split(placeHolder, getWidth() - 8).size() * font.lineHeight;
		}
		return 0;
	}
	public int getInnerHeight() {
		E root = getRoot();
		int placeHolderHeight = 0;
		if (root.getSubEntries().isEmpty())
			placeHolderHeight = getPlaceHolderHeight();
		return max(placeHolderHeight, root.getTotalHeight(false, true) - root.getOwnHeight());
	}
	public int getTotalInnerHeight() {
		E root = getRoot();
		int placeHolderHeight = 0;
		if (root.getSubEntries().isEmpty())
			placeHolderHeight = getPlaceHolderHeight();
		return max(placeHolderHeight, root.getTotalHeight(true, true) - root.getOwnHeight());
	}
	public int getPreferredHeight() {
		ArrangeableTreeViewCaption<E> caption = getCaption();
		return getTotalInnerHeight() + (caption != null? caption.getHeight() + getCaptionSeparation() : 0) + getPadding() * 2;
	}
	
	// Selection, focus and dragging
	
	@SuppressWarnings("unchecked") public void setSelected(ArrangeableTreeViewEntry<E> entry, boolean selected) {
		if (entry.isSelectable() && selected) {
			if (entry.canBeAddedToSelection(selection) && entry != getRoot())
				selection.add((E) entry);
		} else selection.remove((E) entry);
	}
	
	public void clearSelection() {
		selection.clear();
	}
	
	@SuppressWarnings("unchecked") public boolean isSelected(ArrangeableTreeViewEntry<E> entry) {
		E p = entry.getParent();
		return selection.contains((E) entry) || p != null && isSelected(p);
	}
	
	public boolean isDraggingEntries() {
		return draggedEntries != null;
	}
	
	public Set<E> getSelection() {
		return selection;
	}
	
	public List<E> getSortedSelection() {
		List<E> sorted = new ArrayList<>();
		for (E entry: getRoot().getSubEntries()) addSelected(sorted, entry);
		return sorted;
	}
	
	private void addSelected(List<E> list, E entry) {
		if (isSelected(entry)) {
			list.add(entry);
		} else for (E child: entry.getSubEntries()) addSelected(list, child);
	}
	
	public void startDrag(int dragOffsetX, int dragOffsetY, int button) {
		setDragged(Pair.of(button, null));
		this.draggedEntries = getSortedSelection();
		this.dragOffsetX = dragOffsetX;
		this.dragOffsetY = dragOffsetY;
		draggedOver = null;
		draggedOverParent = null;
		overlayArea.setBounds(area.x - 128, area.y - 128, area.width + 256, area.height + 256);
		overlayContainer.addOverlay(overlayArea, this);
	}
	
	public void cancelDragging() {
		dragged = null;
		draggedEntries = null;
		draggedOver = null;
		draggedOverParent = null;
		lastAutoDrag = 0L;
	}
	
	public E getFocusedEntry() {
		E entry = getRoot();
		while (entry.getFocusedSubEntry() != null) entry = entry.getFocusedSubEntry();
		return entry;
	}
	
	public void setFocusedEntry(E entry) {
		E focused = getFocusedEntry();
		focused.unFocus();
		entry.focus();
	}
	
	public boolean tryAddEntry(E entry) {
		E focused = getFocusedEntry();
		List<E> s = Collections.singletonList(entry);
		int index = 0;
		while (focused != null && !focused.canBeDroppedInto(index, s)) {
			E parent = focused.getParent();
			index = parent.getSubEntries().indexOf(focused) + 1;
			focused = parent;
		}
		if (focused != null) {
			focused.addSubEntry(index, entry);
			entry.focusAndSelect(true, true, true, null);
		}
		return focused != null;
	}
	
	public void removeSelection() {
		List<E> sortedSelection = getSortedSelection();
		E last = null;
		for (E entry: sortedSelection) {
			E parent = entry.getParent();
			if (parent != null) {
				List<E> siblings = parent.getSubEntries();
				int size = siblings.size();
				if (size == 1) {
					last = parent;
				} else {
					int index = siblings.indexOf(entry);
					index = index == size - 1? index - 1 : index + 1;
					if (index >= 0 && index < size) {
						last = siblings.get(index);
					} else last = parent;
				}
				parent.removeSubEntry(entry);
			}
		}
		if (last != null) last.focusAndSelect(true, true, true, null);
	}
	
	public @Nullable E getEntryAtPos(double mouseX, double mouseY) {
		return getEntryAtPos(mouseX, mouseY, true);
	}
	
	public @Nullable E getEntryAtPos(double mouseX, double mouseY, boolean ignoreIndent) {
		E root = getRoot();
		E entry = root;
		descent:while (entry == root || !entry.isMouseOverSelf(mouseX, mouseY)) {
			if (!entry.isMouseOver(mouseX, mouseY)) return null;
			List<E> subEntries = entry.getSubEntries();
			for (E sub: subEntries) if (sub.isMouseOver(mouseX, mouseY)) {
				entry = sub;
				continue descent;
			}
			return null;
		}
		return ignoreIndent || entry.getArea().x <= mouseX? entry : null;
	}
	
	@Override public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (isExpandDragging()) {
			E entry = getEntryAtPos(mouseX, mouseY);
			boolean state = getExpandDragState();
			if (entry != null && entry.isExpanded() != state && !entry.getSubEntries().isEmpty()) {
				entry.setExpanded(state);
				if (!state) entry.focusAndSelect(true, true, true, null);
			}
			return true;
		}
		return IDragBroadcastableControlContainer.super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}
	
	@Override public void endDrag(double mouseX, double mouseY, int button) {
		if (isExpandDragging()) endExpandDrag();
		if (isDraggingEntries()) {
			if (draggedOver != null && draggedOverParent != null) {
				E entry = draggedOver;
				List<E> parentSubEntries = draggedOverParent.getSubEntries();
				while (
				  entry != null && draggedOverParent != entry
				  && !parentSubEntries.contains(entry)
				) entry = entry.getParent();
				int index = parentSubEntries.indexOf(entry) + 1;
				if (draggedOverParent.canBeDroppedInto(index, draggedEntries)) {
					for (E dragged: draggedEntries) {
						E p = dragged.getParent();
						p.removeSubEntry(dragged);
					}
					index = parentSubEntries.indexOf(entry) + 1;
					for (E dragged: draggedEntries) {
						draggedOverParent.addSubEntry(index++, dragged);
					}
				}
			}
			cancelDragging();
		}
		IDragBroadcastableControlContainer.super.endDrag(mouseX, mouseY, button);
	}
	
	// Rendering
	
	@Override public void render(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
		int x = getX(), y = getY(), w = getWidth(), h = getHeight();
		ArrangeableTreeViewCaption<E> caption = getCaption();
		area.setBounds(x, y, w, h);
		int pad = getPadding();
		if (!transparent) renderLayout(mStack, mouseX, mouseY, delta);
		int captionHeight = caption != null? caption.getHeight() + getCaptionSeparation() : 0;
		scroller.area.setBounds(
		  x + pad, y + pad + captionHeight, w - pad * 2,
		  h - captionHeight - pad * 2);
		if (caption != null) caption.render(mStack, mouseX, mouseY, delta);
		scroller.render(mStack, mouseX, mouseY, delta);
	}
	
	public int getPadding() {
		return transparent? 0 : 1;
	}
	
	public int getCaptionSeparation() {
		return 1;
	}
	
	public void renderLayout(PoseStack mStack, int mouseX, int mouseY, float delta) {
		Rectangle area = getArea();
		fill(mStack, area.x, area.y, area.getMaxX(), area.getMaxY(), fillColor);
		drawBorderRect(mStack, area.x, area.y, area.getMaxX(), area.getMaxY(), 1, borderColor, 0);
		ArrangeableTreeViewCaption<E> caption = getCaption();
		if (caption != null) {
			int h = caption.getHeight();
			fill(mStack, area.x + 1, area.y + h + 1, area.getMaxX() - 1, area.y + h + 2, borderColor);
		}
	}
	
	@Override public boolean renderOverlay(
	  PoseStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		overlayArea.setBounds(area.x - 128, area.y - 128, area.width + 256, area.height + 256);
		if (!isDraggingEntries() || area != overlayArea) return false;
		E root = getRoot();
		Rectangle rootArea = root.getArea();
		root.updateDraggingPos(rootArea.x, rootArea.y, mouseX, mouseY, 0, false);
		if (draggedOverParent == null) draggedOver = null;
		int draggedWidth = (int) (this.area.getWidth() * 0.8);
		int draggedHeight = 48;
		int dragX = Mth.clamp(mouseX + getDragOffsetX(), overlayArea.x, overlayArea.getMaxX() - draggedWidth);
		int dragY = Mth.clamp(mouseY + getDragOffsetY(), overlayArea.y, overlayArea.getMaxY() - draggedHeight);
		renderDragged(mStack, dragX, dragY, draggedWidth, mouseX, mouseY, delta);
		
		double autoScroll = 0.0;
		int top = getY();
		int bottom = top + getHeight();
		if (mouseY < top + 24 || mouseY >= bottom - 24) {
			long t = System.currentTimeMillis();
			long dragTime = lastAutoDrag == 0? 0 : t - lastAutoDrag;
			lastAutoDrag = t;
			float dragMultiplier =
			  (mouseY < top + 24? mouseY - (top + 24) : mouseY - (bottom - 24)) * 0.01F;
			autoScroll = dragMultiplier * dragTime;
		}
		if (autoScroll != 0.0) scroller.scrollBy(autoScroll, true);
		return true;
	}
	
	public void renderDragged(
	  PoseStack mStack, int x, int y, int w, int mouseX, int mouseY, float delta
	) {
		if (isDraggingEntries()) {
			int yy = y;
			int i = 0;
			for (E cell : getDraggedEntries()) {
				boolean expanded = cell.isExpanded();
				cell.setExpanded(false, false);
				cell.cancelAnimations();
				cell.render(mStack, x, yy, w, mouseX, mouseY, delta);
				yy += cell.getTotalHeight(false, true);
				cell.setExpanded(expanded, false);
			}
		}
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return area.contains(mouseX, mouseY);
	}
	
	// Drag hooks
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isDraggingEntries()) return true;
		return IDragBroadcastableControlContainer.super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (isDraggingEntries()) return true;
		boolean r = super.keyPressed(keyCode, scanCode, modifiers);
		if (!r && (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN)) {
			setFocused(root);
			return root.handleNavigationKey(keyCode, scanCode, modifiers);
		}
		return r;
	}
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		if (isDraggingEntries()) return true;
		return super.charTyped(codePoint, modifiers);
	}
	
	@Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return IDragBroadcastableControlContainer.super.mouseReleased(mouseX, mouseY, button);
	}
	
	// Internal properties
	
	protected List<E> getDraggedEntries() {
		return draggedEntries;
	}
	
	protected int getDragOffsetX() {
		return dragOffsetX;
	}
	protected int getDragOffsetY() {
		return dragOffsetY;
	}
	protected E getDraggedOver() {
		return draggedOver;
	}
	protected void setDraggedOver(E draggedOver) {
		this.draggedOver = draggedOver;
	}
	protected E getDraggedOverParent() {
		return draggedOverParent;
	}
	protected void setDraggedOverParent(E draggedOverParent) {
		this.draggedOverParent = draggedOverParent;
	}
	
	// Inherited properties
	
	@Override public @NotNull List<? extends GuiEventListener> children() {
		return listeners;
	}
	
	@Override public Pair<Integer, GuiEventListener> getDragged() {
		return dragged;
	}
	@Override public void setDragged(Pair<Integer, GuiEventListener> dragged) {
		this.dragged = dragged;
	}
	
	// Style properties
	
	
	public boolean isTransparent() {
		return transparent;
	}
	public void setTransparent(boolean transparent) {
		this.transparent = transparent;
	}
	
	public int getBorderColor() {
		return borderColor;
	}
	public void setBorderColor(int borderColor) {
		this.borderColor = borderColor;
	}
	
	public int getFillColor() {
		return fillColor;
	}
	public void setFillColor(int fillColor) {
		this.fillColor = fillColor;
	}
	
	@Override public <W extends IPositionableRenderable> void setDragBroadcastableAction(
	  DragBroadcastableAction<W> action, DragBroadcastableControl<W> source
	) {
		dragBroadcastableAction = action;
		dragBroadcastableSource = source;
	}
	
	@Override public DragBroadcastableAction<?> getDragBroadcastableAction() {
		return dragBroadcastableAction;
	}
	
	@Override public DragBroadcastableControl<?> getDragBroadcastableSource() {
		return dragBroadcastableSource;
	}
	
	public void startExpandDrag(boolean expand) {
		draggedExpandState = expand;
	}
	
	public void endExpandDrag() {
		draggedExpandState = null;
	}
	
	public boolean isExpandDragging() {
		return draggedExpandState != null;
	}
	
	public boolean getExpandDragState() {
		return draggedExpandState != null? draggedExpandState : false;
	}
}