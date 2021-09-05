package endorh.simple_config.clothconfig2.gui.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.clothconfig2.api.AbstractConfigEntry;
import endorh.simple_config.clothconfig2.api.IExpandable;
import endorh.simple_config.clothconfig2.api.ScissorsHandler;
import endorh.simple_config.clothconfig2.gui.IExtendedDragAwareNestedGuiEventHandler;
import endorh.simple_config.clothconfig2.gui.widget.DynamicEntryListWidget.Entry;
import endorh.simple_config.clothconfig2.impl.ISeekableComponent;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static net.minecraft.util.math.MathHelper.clamp;

@OnlyIn(value = Dist.CLIENT)
public abstract class DynamicEntryListWidget<E extends Entry>
  extends FocusableGui implements IRenderable, IExtendedDragAwareNestedGuiEventHandler {
	protected static final int DRAG_OUTSIDE = -2;
	protected final Minecraft client;
	private final List<E> entries = new Entries();
	public int width;
	public int height;
	public int top;
	public int bottom;
	public int right;
	public int left;
	protected int yDrag = DRAG_OUTSIDE;
	protected boolean selectionVisible = true;
	protected boolean renderSelection;
	protected int headerHeight;
	protected double scroll;
	protected boolean scrolling;
	protected E selectedItem;
	protected @Nullable INavigableTarget selectedTarget = null;
	protected ResourceLocation backgroundLocation;
	protected Pair<Integer, IGuiEventListener> dragged = null;
	
	protected ISeekableComponent focusedMatch = null;
	protected List<ISeekableComponent> matchingEntries = null;
	
	public DynamicEntryListWidget(
	  Minecraft client, int width, int height, int top, int bottom,
	  ResourceLocation backgroundLocation
	) {
		this.client = client;
		this.width = width;
		this.height = height;
		this.top = top;
		this.bottom = bottom;
		left = 0;
		right = width;
		this.backgroundLocation = backgroundLocation;
	}
	
	public void resize(int width, int height, int top, int bottom) {
		this.width = width;
		this.height = height;
		this.top = top;
		this.bottom = bottom;
		this.left = 0;
		this.right = width;
	}
	
	public void setRenderSelection(boolean boolean_1) {
		selectionVisible = boolean_1;
	}
	
	protected void setRenderHeader(boolean render, int headerHeight) {
		renderSelection = render;
		this.headerHeight = headerHeight;
		if (!render)
			this.headerHeight = 0;
	}
	
	public int getItemWidth() {
		return 220;
	}
	
	public E getSelectedItem() {
		return selectedItem;
	}
	
	public void selectItem(E item) {
		selectedItem = item;
	}
	
	public E getFocused() {
		//noinspection unchecked
		return (E) getListener();
	}
	
	public @Nullable INavigableTarget getSelectedTarget() {
		final List<INavigableTarget> targets = getNavigableTargets();
		if (selectedTarget != null && targets.contains(selectedTarget))
			return selectedTarget;
		if (selectedTarget != null) {
			INavigableTarget target = this.selectedTarget;
			while (target != null && !targets.contains(target))
				target = target.getNavigableParent();
			if (target != null)
				return target;
		}
		if (!targets.isEmpty())
			return selectedTarget = targets.get(0);
		return null;
	}
	
	public final List<E> getEntries() {
		return entries;
	}
	
	public final @NotNull List<E> getEventListeners() {
		return Collections.unmodifiableList(entries);
	}
	
	protected final void clearItems() {
		entries.clear();
	}
	
	protected E getItem(int index) {
		return getEventListeners().get(index);
	}
	
	protected int addItem(E item) {
		entries.add(item);
		return entries.size() - 1;
	}
	
	protected int getItemCount() {
		return getEventListeners().size();
	}
	
	protected boolean isSelected(int index) {
		return Objects.equals(getSelectedItem(), getEventListeners().get(index));
	}
	
	protected final E getItemAtPosition(double mouseX, double mouseY) {
		int listMiddleX = left + width / 2;
		int minX = listMiddleX - getItemWidth() / 2;
		int maxX = listMiddleX + getItemWidth() / 2;
		int currentY =
		  MathHelper.floor(mouseY - (double) top) - headerHeight + (int) getScroll() -
		  4;
		int itemY = 0;
		int itemIndex = -1;
		for (int i = 0; i < entries.size(); ++i) {
			E item = getItem(i);
			if ((itemY += item.getItemHeight()) <= currentY) continue;
			itemIndex = i;
			break;
		}
		return mouseX < (double) getScrollBarPosition() && mouseX >= (double) minX &&
		       mouseX <= (double) maxX && itemIndex >= 0 && currentY >= 0 &&
		       itemIndex < getItemCount() ? getEventListeners().get(itemIndex) : null;
	}
	
	public void updateSize(int width, int height, int top, int bottom) {
		this.width = width;
		this.height = height;
		this.top = top;
		this.bottom = bottom;
		left = 0;
		right = width;
	}
	
	public void setLeftPos(int left) {
		this.left = left;
		right = left + width;
	}
	
	protected int getMaxScrollPosition() {
		ArrayList<Integer> list = new ArrayList<>();
		int i = headerHeight;
		for (E entry : entries) {
			i += entry.getItemHeight();
			if (entry.getExtraScrollHeight() < 0) continue;
			list.add(i + entry.getExtraScrollHeight());
		}
		list.add(i);
		return list.stream().max(Integer::compare).orElse(0);
	}
	
	protected void clickedHeader(int mouseX, int mouseY) {}
	
	protected void renderHeader(
	  MatrixStack matrices, int rowLeft, int startY, Tessellator tessellator
	) {}
	
	protected void drawBackground() {}
	
	protected void renderDecorations(MatrixStack mStack, int mouseX, int mouseY) {}
	
	@Deprecated
	protected void renderBackBackground(
	  MatrixStack mStack, BufferBuilder buffer, Tessellator tessellator
	) {
		client.getTextureManager().bindTexture(backgroundLocation);
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
		Matrix4f matrix = mStack.getLast().getMatrix();
		float float_2 = 32.0f;
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.pos(matrix, (float) left, (float) bottom, 0.0f).tex((float) left / 32.0f, (float) (bottom + (int) getScroll()) / 32.0f).color(32, 32, 32, 255).endVertex();
		buffer.pos(matrix, (float) right, (float) bottom, 0.0f).tex((float) right / 32.0f, (float) (bottom + (int) getScroll()) / 32.0f).color(32, 32, 32, 255).endVertex();
		buffer.pos(matrix, (float) right, (float) top, 0.0f).tex((float) right / 32.0f, (float) (top + (int) getScroll()) / 32.0f).color(32, 32, 32, 255).endVertex();
		buffer.pos(matrix, (float) left, (float) top, 0.0f).tex((float) left / 32.0f, (float) (top + (int) getScroll()) / 32.0f).color(32, 32, 32, 255).endVertex();
		tessellator.draw();
	}
	
	public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		drawBackground();
		int scrollBarPosition = getScrollBarPosition();
		int scrollBarEnd = scrollBarPosition + 6;
		RenderSystem.disableLighting();
		RenderSystem.disableFog();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		renderBackBackground(mStack, buffer, tessellator);
		int rowLeft = getRowLeft();
		int startY = top + 4 - (int) getScroll();
		if (renderSelection)
			renderHeader(mStack, rowLeft, startY, tessellator);
		ScissorsHandler.INSTANCE.scissor(
		  new Rectangle(left, top, width, bottom - top));
		renderList(mStack, rowLeft, startY, mouseX, mouseY, delta);
		ScissorsHandler.INSTANCE.removeLastScissor();
		RenderSystem.disableDepthTest();
		renderHoleBackground(mStack, 0, top, 255, 255);
		renderHoleBackground(mStack, bottom, height, 255, 255);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(770, 771, 0, 1);
		RenderSystem.disableAlphaTest();
		RenderSystem.shadeModel(7425);
		RenderSystem.disableTexture();
		Matrix4f matrix = mStack.getLast().getMatrix();
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.pos(matrix, (float) left, (float) (top + 4), 0.0f).tex(0.0f, 1.0f).color(0, 0, 0, 0).endVertex();
		buffer.pos(matrix, (float) right, (float) (top + 4), 0.0f).tex(1.0f, 1.0f).color(0, 0, 0, 0).endVertex();
		buffer.pos(matrix, (float) right, (float) top, 0.0f).tex(1.0f, 0.0f).color(0, 0, 0, 255).endVertex();
		buffer.pos(matrix, (float) left, (float) top, 0.0f).tex(0.0f, 0.0f).color(0, 0, 0, 255).endVertex();
		tessellator.draw();
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.pos(matrix, (float) left, (float) bottom, 0.0f).tex(0.0f, 1.0f).color(0, 0, 0, 255).endVertex();
		buffer.pos(matrix, (float) right, (float) bottom, 0.0f).tex(1.0f, 1.0f).color(0, 0, 0, 255).endVertex();
		buffer.pos(matrix, (float) right, (float) (bottom - 4), 0.0f).tex(1.0f, 0.0f).color(0, 0, 0, 0).endVertex();
		buffer.pos(matrix, (float) left, (float) (bottom - 4), 0.0f).tex(0.0f, 0.0f).color(0, 0, 0, 0).endVertex();
		tessellator.draw();
		int maxScroll = getMaxScroll();
		renderScrollBar(mStack, tessellator, buffer, maxScroll, scrollBarPosition, scrollBarEnd);
		renderDecorations(mStack, mouseX, mouseY);
		RenderSystem.enableTexture();
		RenderSystem.shadeModel(7424);
		RenderSystem.enableAlphaTest();
		RenderSystem.disableBlend();
	}
	
	protected void renderScrollBar(
	  MatrixStack matrices, Tessellator tessellator, BufferBuilder buffer, int maxScroll,
	  int sbMinX, int sbMaxX
	) {
		if (maxScroll > 0) {
			int sbHeight =
			  (bottom - top) * (bottom - top) / getMaxScrollPosition();
			sbHeight = clamp(sbHeight, 32, bottom - top - 8);
			int sbMinY = (int) getScroll() * (bottom - top - sbHeight) / maxScroll + top;
			if (sbMinY < top) sbMinY = top;
			Matrix4f matrix = matrices.getLast().getMatrix();
			buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			buffer.pos(matrix, (float) sbMinX, (float) bottom, 0.0f).tex(0.0f, 1.0f).color(0, 0, 0, 255).endVertex();
			buffer.pos(matrix, (float) sbMaxX, (float) bottom, 0.0f).tex(1.0f, 1.0f).color(0, 0, 0, 255).endVertex();
			buffer.pos(matrix, (float) sbMaxX, (float) top, 0.0f).tex(1.0f, 0.0f).color(0, 0, 0, 255).endVertex();
			buffer.pos(matrix, (float) sbMinX, (float) top, 0.0f).tex(0.0f, 0.0f).color(0, 0, 0, 255).endVertex();
			tessellator.draw();
			buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			buffer.pos(matrix, (float) sbMinX, (float) (sbMinY + sbHeight), 0.0f).tex(0.0f, 1.0f).color(128, 128, 128, 255).endVertex();
			buffer.pos(matrix, (float) sbMaxX, (float) (sbMinY + sbHeight), 0.0f).tex(1.0f, 1.0f).color(128, 128, 128, 255).endVertex();
			buffer.pos(matrix, (float) sbMaxX, (float) sbMinY, 0.0f).tex(1.0f, 0.0f).color(128, 128, 128, 255).endVertex();
			buffer.pos(matrix, (float) sbMinX, (float) sbMinY, 0.0f).tex(0.0f, 0.0f).color(128, 128, 128, 255).endVertex();
			tessellator.draw();
			buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			buffer.pos(sbMinX, sbMinY + sbHeight - 1, 0.0).tex(0.0f, 1.0f).color(192, 192, 192, 255).endVertex();
			buffer.pos(sbMaxX - 1, sbMinY + sbHeight - 1, 0.0).tex(1.0f, 1.0f).color(192, 192, 192, 255).endVertex();
			buffer.pos(sbMaxX - 1, sbMinY, 0.0).tex(1.0f, 0.0f).color(192, 192, 192, 255).endVertex();
			buffer.pos(sbMinX, sbMinY, 0.0).tex(0.0f, 0.0f).color(192, 192, 192, 255).endVertex();
			tessellator.draw();
		}
	}
	
	public void scrollTo(double scroll) {
		setScroll(scroll);
	}
	
	public void scrollTo(Entry entry) {
		entry.expandParents();
		setScroll(entry.getScrollY());
	}
	
	public void ensureFocusedVisible() {
		final int focusedTop = getFocusedScroll();
		final int height = getFocusedHeight();
		int up = focusedTop - 12 - (int) getScroll();
		if (up < 0) {
			scrollBy(up);
		} else {
			int down = bottom - top - focusedTop - height - 12 + (int) getScroll();
			if (down < 0)
				scrollBy(-down);
		}
	}
	
	protected void scrollBy(int amount) {
		setScroll(getScroll() + (double) amount);
		yDrag = DRAG_OUTSIDE;
	}
	
	public double getScroll() {
		return scroll;
	}
	
	public void setScroll(double scroll) {
		this.scroll = clamp(scroll, 0.0, getMaxScroll());
	}
	
	protected int getMaxScroll() {
		return max(0, getMaxScrollPosition() - (bottom - top - 4));
	}
	
	public int getScrollBottom() {
		return (int) getScroll() - height - headerHeight;
	}
	
	protected void updateScrollingState(double mouseX, double mouseY, int button) {
		scrolling = button == 0 && mouseX >= (double) getScrollBarPosition() &&
		            mouseX < (double) (getScrollBarPosition() + 6);
	}
	
	protected int getScrollBarPosition() {
		return width / 2 + 124;
	}
	
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		updateScrollingState(mouseX, mouseY, button);
		if (!isMouseOver(mouseX, mouseY))
			return false;
		E item = getItemAtPosition(mouseX, mouseY);
		if (item != null) {
			if (item.mouseClicked(mouseX, mouseY, button)) {
				setListener(item);
				setDragging(true);
				updateSelectedTarget();
				return true;
			}
		} else if (button == 0) {
			clickedHeader(
			  (int) (mouseX - (double) (left + width / 2 - getItemWidth() / 2)),
			  (int) (mouseY - (double) top) + (int) getScroll() - 4);
			return true;
		}
		return scrolling;
	}
	
	protected void updateSelectedTarget() {
		INavigableTarget lastTarget = null;
		INestedGuiEventHandler nest = this;
		while (nest.getListener() instanceof INestedGuiEventHandler) {
			nest = ((INestedGuiEventHandler) nest.getListener());
			if (nest instanceof INavigableTarget)
				lastTarget = ((INavigableTarget) nest);
		}
		IGuiEventListener listener = nest.getListener();
		if (listener instanceof INavigableTarget)
			lastTarget = ((INavigableTarget) listener);
		
		if (lastTarget != null)
			selectedTarget = lastTarget;
	}
	
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (getFocused() != null)
			getFocused().mouseReleased(mouseX, mouseY, button);
		return false;
	}
	
	public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double deltaX, double deltaY
	) {
		if (IExtendedDragAwareNestedGuiEventHandler.super.mouseDragged(
		  mouseX, mouseY, button, deltaX, deltaY))
			return true;
		if (button == 0 && scrolling) {
			if (mouseY < (double) top) {
				setScroll(0.0);
			} else if (mouseY > (double) bottom) {
				setScroll(getMaxScroll());
			} else {
				double double_5 = max(1, getMaxScroll());
				int int_2 = bottom - top;
				int int_3 = clamp(
				  (int) ((float) (int_2 * int_2) / (float) getMaxScrollPosition()), 32, int_2 - 8);
				double double_6 = max(1.0, double_5 / (double) (int_2 - int_3));
				setScroll(getScroll() + deltaY * double_6);
			}
			return true;
		}
		return false;
	}
	
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		for (E entry : entries) {
			if (!entry.mouseScrolled(mouseX, mouseY, delta)) continue;
			return true;
		}
		setScroll(getScroll() - delta * (double) (getMaxScroll() / getItemCount()) / 2.0);
		return true;
	}
	
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (Screen.hasAltDown()) {
			if (handleNavigationKey(keyCode, scanCode, modifiers))
				return true;
		}
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		return handleNavigationKey(keyCode, scanCode, modifiers);
	}
	
	protected boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		final INavigableTarget target = getSelectedTarget();
		if (target != null && target.handleNavigationKey(keyCode, scanCode, modifiers))
			return true;
		switch (keyCode) {
			case 264: // Arrow down
				navigateEntries(1);
				return true;
			case 265: // Arrow up
				navigateEntries(-1);
				return true;
			case 263: // Arrow left
				if (target != null) {
					navigateTo(target.getNavigableParent());
					return true;
				}
		}
		return false;
	}
	
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseY >= (double) top && mouseY <= (double) bottom &&
		       mouseX >= (double) left && mouseX <= (double) right;
	}
	
	protected void renderList(
	  MatrixStack mStack, int startX, int startY, int mouseX, int mouseY, float delta
	) {
		int itemCount = getItemCount();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		for (int renderIndex = 0; renderIndex < itemCount; ++renderIndex) {
			E item = getItem(renderIndex);
			int itemY = startY + headerHeight;
			for (int i = 0; i < entries.size() && i < renderIndex; ++i) {
				itemY += entries.get(i).getItemHeight();
			}
			int itemHeight = item.getItemHeight() - 4;
			int itemWidth = getItemWidth();
			if (selectionVisible && isSelected(renderIndex)) {
				int itemMinX = left + width / 2 - itemWidth / 2;
				int itemMaxX = itemMinX + itemWidth;
				RenderSystem.disableTexture();
				float sat = isFocused() ? 1.0f : 0.5f;
				Matrix4f matrix = mStack.getLast().getMatrix();
				RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
				buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
				buffer.pos(matrix, (float) itemMinX, (float) (itemY + itemHeight + 2), 0.0f)
				  .color(sat, sat, sat, 1.0f).endVertex();
				buffer.pos(matrix, (float) itemMaxX, (float) (itemY + itemHeight + 2), 0.0f)
				  .color(sat, sat, sat, 1.0f).endVertex();
				buffer.pos(matrix, (float) itemMaxX, (float) (itemY - 2), 0.0f)
				  .color(sat, sat, sat, 1.0f).endVertex();
				buffer.pos(matrix, (float) itemMinX, (float) (itemY - 2), 0.0f)
				  .color(sat, sat, sat, 1.0f).endVertex();
				tessellator.draw();
				buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
				buffer.pos(matrix, (float) (itemMinX + 1), (float) (itemY + itemHeight + 1), 0.0f)
				  .color(0.0f, 0.0f, 0.0f, 1.0f).endVertex();
				buffer.pos(matrix, (float) (itemMaxX - 1), (float) (itemY + itemHeight + 1), 0.0f)
				  .color(0.0f, 0.0f, 0.0f, 1.0f).endVertex();
				buffer.pos(matrix, (float) (itemMaxX - 1), (float) (itemY - 1), 0.0f)
				  .color(0.0f, 0.0f, 0.0f, 1.0f).endVertex();
				buffer.pos(matrix, (float) (itemMinX + 1), (float) (itemY - 1), 0.0f)
				  .color(0.0f, 0.0f, 0.0f, 1.0f).endVertex();
				tessellator.draw();
				RenderSystem.enableTexture();
			}
			int y = getRowTop(renderIndex);
			int x = getRowLeft();
			RenderHelper.disableStandardItemLighting();
			renderItem(
			  mStack, item, renderIndex, y, x, itemWidth, itemHeight, mouseX, mouseY,
			  isMouseOver(mouseX, mouseY) && item.isMouseOver(mouseX, mouseY), delta);
		}
	}
	
	protected void renderItem(
	  MatrixStack matrices, E item, int index, int y, int x, int entryWidth, int entryHeight,
	  int mouseX, int mouseY, boolean isHovered, float delta
	) {
		item.render(
		  matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
	}
	
	protected int getRowLeft() {
		return left + width / 2 - getItemWidth() / 2 + 2;
	}
	
	protected int getRowTop(int index) {
		int y = top + 4 - (int) getScroll() + headerHeight;
		for (int i = 0; i < entries.size() && i < index; ++i)
			y += entries.get(i).getItemHeight();
		return y;
	}
	
	public int getFocusedScroll() {
		int y = headerHeight;
		IGuiEventListener focused = getFocused();
		List<? extends Entry> entries = this.entries;
		final int index = entries.indexOf(focused);
		if (index < 0)
			return y;
		for (Entry entry : entries.subList(0, index))
			y += entry.getItemHeight();
		if (focused instanceof IExpandable)
			y += ((IExpandable) focused).getFocusedScroll();
		return y;
	}
	
	public int getFocusedHeight() {
		Entry focused = getFocused();
		if (focused instanceof IExpandable)
			return ((IExpandable) focused).getFocusedHeight();
		if (focused != null)
			return focused.getItemHeight();
		return 24;
	}
	
	protected boolean isFocused() {
		return false;
	}
	
	protected void renderHoleBackground(
	  MatrixStack matrices, int y1, int y2, int alpha1, int alpha2
	) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		client.getTextureManager().bindTexture(backgroundLocation);
		Matrix4f matrix = matrices.getLast().getMatrix();
		RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
		float float_1 = 32.0f;
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.pos(matrix, (float) left, (float) y2, 0.0f).tex(0.0f, (float) y2 / 32.0f)
		  .color(64, 64, 64, alpha2).endVertex();
		buffer.pos(matrix, (float) (left + width), (float) y2, 0.0f)
		  .tex((float) width / 32.0f, (float) y2 / 32.0f).color(64, 64, 64, alpha2).endVertex();
		buffer.pos(matrix, (float) (left + width), (float) y1, 0.0f)
		  .tex((float) width / 32.0f, (float) y1 / 32.0f).color(64, 64, 64, alpha1).endVertex();
		buffer.pos(matrix, (float) left, (float) y1, 0.0f).tex(0.0f, (float) y1 / 32.0f)
		  .color(64, 64, 64, alpha1).endVertex();
		tessellator.draw();
	}
	
	protected E remove(int int_1) {
		E itemListWidget$Item_1 = entries.get(int_1);
		return removeEntry(entries.get(int_1)) ? itemListWidget$Item_1 : null;
	}
	
	protected boolean removeEntry(E entry) {
		boolean removed = entries.remove(entry);
		if (removed && entry == getSelectedItem())
			selectItem(null);
		return removed;
	}
	
	@Override public boolean changeFocus(boolean focus) {
		boolean change = super.changeFocus(focus);
		if (change)
			updateSelectedTarget();
		return change;
	}
	
	public void setSelectedTarget(@Nullable INavigableTarget target) {
		this.selectedTarget = target;
	}
	
	@OnlyIn(value = Dist.CLIENT)
	class Entries extends AbstractList<E> {
		private final ArrayList<E> items = Lists.newArrayList();
		
		private Entries() {}
		
		@Override public void clear() {
			items.clear();
		}
		
		@Override public E get(int index) {
			return items.get(index);
		}
		
		@Override public int size() {
			return items.size();
		}
		
		@Override public E set(int index, E entry) {
			E item = items.set(index, entry);
			entry.setParent(DynamicEntryListWidget.this);
			return item;
		}
		
		@Override public void add(int index, E entry) {
			items.add(index, entry);
			entry.setParent(DynamicEntryListWidget.this);
		}
		
		@Override public E remove(int int_1) {
			return items.remove(int_1);
		}
	}
	
	@OnlyIn(value = Dist.CLIENT)
	public static abstract class Entry
	  extends AbstractGui implements IGuiEventListener, ISeekableComponent, INavigableTarget {
		private WeakReference<DynamicEntryListWidget<?>> parent = new WeakReference<>(null);
		
		public abstract void render(
		  MatrixStack mStack, int index, int y, int x, int w, int h, int mouseX, int mouseY,
		  boolean isHovered, float delta
		);
		
		public DynamicEntryListWidget<?> getParent() {
			final DynamicEntryListWidget<?> parent = this.parent.get();
			if (parent == null)
				throw new IllegalStateException(
				  "Tried to get parent of orphan config entry of type " + getClass().getSimpleName() +
				  "\nThis entry hasn't been properly initialized");
			return parent;
		}
		
		@Internal protected @Nullable DynamicEntryListWidget<?> getParentOrNull() {
			return parent.get();
		}
		
		public void setParent(DynamicEntryListWidget<?> parent) {
			this.parent = new WeakReference<>(parent);
		}
		
		protected abstract void expandParents();
		
		protected abstract void claimFocus();
		
		public abstract int getItemHeight();
		
		public int getCaptionHeight() {
			return 20;
		}
		
		public int getExtraScrollHeight() {
			return -1;
		}
		
		public abstract int getScrollY();
	}
	
	public static final class SmoothScrollingSettings {
		public static final double CLAMP_EXTENSION = 200.0;
		
		private SmoothScrollingSettings() {}
	}
	
	@Override public Pair<Integer, IGuiEventListener> getDragged() {
		return dragged;
	}
	
	@Override public void setDragged(Pair<Integer, IGuiEventListener> dragged) {
		this.dragged = dragged;
	}
	
	// Search
	
	public Pair<Integer, Integer> search(Pattern query) {
		if (focusedMatch != null)
			focusedMatch.setFocusedMatch(false);
		matchingEntries =
		  entries.stream().flatMap(e -> e.search(query).stream()).collect(Collectors.toList());
		if (focusedMatch != null && !matchingEntries.contains(focusedMatch))
			focusedMatch = null;
		if (focusedMatch == null) changeFocusedMatch(0);
		else focusedMatch.setFocusedMatch(true);
		return Pair.of(
		  focusedMatch != null ? matchingEntries.indexOf(focusedMatch) : -1, matchingEntries.size());
	}
	
	public void changeFocusedMatch(int index) {
		if (matchingEntries == null || matchingEntries.isEmpty()) {
			focusedMatch = null;
			return;
		}
		if (focusedMatch != null)
			focusedMatch.setFocusedMatch(false);
		focusedMatch = matchingEntries.get(index);
		focusedMatch.setFocusedMatch(true);
		if (focusedMatch instanceof INavigableTarget)
			selectedTarget = ((INavigableTarget) focusedMatch);
	}
	
	public List<INavigableTarget> getNavigableTargets() {
		return entries.stream().flatMap(e -> e.getNavigableChildren().stream())
		  .collect(Collectors.toList());
	}
	
	public void navigateEntries(int step) {
		if (step == 0)
			return;
		final List<INavigableTarget> targets = getNavigableTargets();
		INavigableTarget selectedTarget = getClosestNavigableTarget();
		if (selectedTarget instanceof AbstractConfigEntry
		    && !((AbstractConfigEntry<?>) selectedTarget).getCategory().equals(
				((AbstractConfigEntry<?>) selectedTarget).getConfigScreen().getSelectedCategoryName())) {
			selectedTarget = null;
		}
		int i = selectedTarget != null ? targets.indexOf(selectedTarget) : -1;
		if (i == -1)
			i = step > 0 ? -1 : targets.size();
		int target = clamp(i + step, 0, targets.size() - 1);
		final INavigableTarget t = targets.get(target);
		t.onNavigate();
	}
	
	protected @Nullable INavigableTarget getClosestNavigableTarget() {
		final List<INavigableTarget> targets = getNavigableTargets();
		INavigableTarget target = this.selectedTarget;
		while (target != null && !targets.contains(target))
			target = target.getNavigableParent();
		return target;
	}
	
	public void navigateTo(INavigableTarget target) {
		while (target != null && !getNavigableTargets().contains(target))
			target = target.getNavigableParent();
		if (target != null) target.onNavigate();
	}
	
	public interface INavigableTarget {
		default @Nullable INavigableTarget getNavigableParent() {
			return null;
		}
		
		default List<INavigableTarget> getNavigableChildren() {
			return Lists.newArrayList(this);
		}
		
		void onNavigate();
		
		default boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
			if (keyCode == 263 && this instanceof IExpandable) {
				final IExpandable ex = (IExpandable) this;
				if (ex.isExpanded()) {
					ex.setExpanded(false, Screen.hasShiftDown());
					return true;
				}
			}
			return false;
		}
	}
}
