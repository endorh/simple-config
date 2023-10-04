package endorh.simpleconfig.ui.gui.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.ContainerEventHandlerEx;
import endorh.simpleconfig.ui.api.IExpandable;
import endorh.simpleconfig.ui.api.INavigableTarget;
import endorh.simpleconfig.ui.api.ScissorsHandler;
import endorh.simpleconfig.ui.gui.widget.DynamicEntryListWidget.ListEntry;
import endorh.simpleconfig.ui.impl.ISeekableComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.Math.min;

@OnlyIn(value = Dist.CLIENT)
public abstract class DynamicEntryListWidget<E extends ListEntry>
  extends AbstractContainerEventHandler
  implements Renderable, ContainerEventHandlerEx, NarratableEntry {
	
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
	protected boolean selectionVisible = false;
	protected boolean renderSelection;
	protected int headerHeight;
	protected double scroll;
	protected boolean scrolling;
	protected @Nullable INavigableTarget selectedTarget = null;
	protected @Nullable INavigableTarget smallestLastTarget = null;
	protected ResourceLocation backgroundLocation;
	protected Pair<Integer, GuiEventListener> dragged = null;
	protected int extraScroll;
	protected int forcedScrollMargin = 12;
	protected int preferredScrollMargin = 60;
	protected Rectangle area = new Rectangle();
	
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
		area.setBounds(left, top, right - left, bottom - top);
		this.backgroundLocation = backgroundLocation;
	}
	
	public void resize(int width, int height, int top, int bottom) {
		this.width = width;
		this.height = height;
		this.top = top;
		this.bottom = bottom;
		left = 0;
		right = width;
		area.setBounds(left, top, right - left, bottom - top);
	}
	
	@Deprecated
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
	
	public int getFieldWidth() {
		return 150;
	}
	
	public int getKeyFieldWidth() {
		return 120;
	}
	
	/**
	 * Item with keyboard focus.
	 */
	public E getFocusedItem() {
		//noinspection unchecked
		return (E) getFocused();
	}
	
	public @Nullable E getSelectedEntry() {
		INavigableTarget target = getSelectedTarget();
		while (!(target instanceof DynamicEntryListWidget.ListEntry) && target != null)
			target = target.getNavigableParent();
		//noinspection unchecked
		return (E) target;
	}
	
	public @Nullable INavigableTarget getSelectedTarget() {
		final List<INavigableTarget> targets = getNavigableTargets(true, false);
		if (selectedTarget == null) return null;
		if (targets.contains(selectedTarget))
			return selectedTarget;
		INavigableTarget target = selectedTarget;
		while (target != null && (!targets.contains(target) || target.isNavigableSubTarget()))
			target = target.getNavigableParent();
		return target;
	}
	
	public @Nullable INavigableTarget getSelectedSubTarget() {
		final List<INavigableTarget> targets = getNavigableTargets(true, false);
		if (selectedTarget == null) return null;
		if (targets.contains(selectedTarget))
			return selectedTarget;
		INavigableTarget target = selectedTarget.getNavigableParent();
		while (target != null && !targets.contains(target))
			target = target.getNavigableParent();
		if (target != null) {
			List<INavigableTarget> subTargets = target.getNavigableSubTargets();
			return selectedTarget.isNavigableSubTarget() && subTargets.contains(selectedTarget)
			       ? selectedTarget : subTargets.isEmpty()? target : null;
		}
		return null;
	}
	
	public final List<E> getEntries() {
		return entries;
	}
	
	@Override public final @NotNull List<E> children() {
		return Collections.unmodifiableList(entries);
	}
	
	protected final void clearItems() {
		entries.clear();
	}
	
	protected E getItem(int index) {
		return children().get(index);
	}
	
	protected int addItem(E item) {
		entries.add(item);
		return entries.size() - 1;
	}
	
	protected int getItemCount() {
		return children().size();
	}
	
	protected final E getItemAtPosition(double mouseX, double mouseY) {
		int listMiddleX = left + width / 2;
		int minX = listMiddleX - getItemWidth() / 2;
		int maxX = listMiddleX + getItemWidth() / 2;
		int currentY =
		  Mth.floor(mouseY - (double) top) - headerHeight + (int) getScroll() -
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
		       itemIndex < getItemCount() ? children().get(itemIndex) : null;
	}
	
	public void setLeftPos(int left) {
		this.left = left;
		area.setBounds(left, top, right - left, bottom - top);
	}
	
	public void setRightPos(int right) {
		this.right = right;
		area.setBounds(left, top, right - left, bottom - top);
	}
	
	public Rectangle getArea() {
		return area;
	}
	
	protected int getMaxScrollPosition() {
		List<Integer> list = new ArrayList<>();
		int i = headerHeight;
		for (E entry : entries) {
			if (entry.isShown()) {
				i += entry.getItemHeight();
				if (entry.getExtraScrollHeight() < 0) continue;
				list.add(i + entry.getExtraScrollHeight());
			}
		}
		list.add(i);
		return list.stream().max(Integer::compare).orElse(0) + min(bottom - top, extraScroll);
	}
	
	public int getExtraScroll() {
		return extraScroll;
	}
	
	public void setExtraScroll(int extraScroll) {
		this.extraScroll = max(0, extraScroll);
	}
	
	protected void clickedHeader(int mouseX, int mouseY) {}
	
	protected void renderHeader(
	  PoseStack matrices, int rowLeft, int startY, Tesselator tessellator
	) {}
	
	protected void drawBackground() {}
	
	protected void renderDecorations(PoseStack mStack, int mouseX, int mouseY) {}
	
	protected void renderBackBackground(
	  PoseStack mStack, BufferBuilder buffer, Tesselator tessellator
	) {
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, backgroundLocation);
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		// RenderSystem.setShaderColor(32F/255F, 32F/255F, 32F/255F, 1F);
		Matrix4f matrix = mStack.last().pose();
		int scroll = (int) getScroll();
		float div = 32F;
		buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		// @formatter:off
		buffer.vertex(matrix, (float)     0, (float) bottom, 0F).uv((float)           0, (float) (bottom + scroll) / div).color(32, 32, 32, 255).endVertex();
		buffer.vertex(matrix, (float) width, (float) bottom, 0F).uv((float) width / div, (float) (bottom + scroll) / div).color(32, 32, 32, 255).endVertex();
		buffer.vertex(matrix, (float) width, (float) top,    0F).uv((float) width / div, (float) (top    + scroll) / div).color(32, 32, 32, 255).endVertex();
		buffer.vertex(matrix, (float)     0, (float) top,    0F).uv((float)           0, (float) (top    + scroll) / div).color(32, 32, 32, 255).endVertex();
		// @formatter:on
		tessellator.end();
	}
	
	public void tick() {
		entries.forEach(ListEntry::tick);
	}
	
	@Override public void render(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
		drawBackground();
		int scrollBarPosition = getScrollBarPosition();
		int scrollBarEnd = scrollBarPosition + 6;
		
		// Render background
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bb = tessellator.getBuilder();
		renderBackBackground(mStack, bb, tessellator);
		
		// Render list
		int rowLeft = getRowLeft();
		int startY = top + 4 - (int) getScroll();
		if (renderSelection) renderHeader(mStack, rowLeft, startY, tessellator);
		
		ScissorsHandler.INSTANCE.pushScissor(new Rectangle(0, top, width, bottom - top)); {
			renderList(mStack, rowLeft, startY, mouseX, mouseY, delta);
		} ScissorsHandler.INSTANCE.popScissor();
		
		RenderSystem.disableDepthTest();
		
		// Render top and bottom segments
		renderBarBackground(mStack, 0, top, 255, 255);
		renderBarBackground(mStack, bottom, height, 255, 255);
		
		// Render scrollbar
		renderScrollBar(mStack, tessellator, bb, getMaxScroll(), scrollBarPosition, scrollBarEnd);
		
		// Render top and bottom shadows
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
		RenderSystem.setShaderTexture(0, 0);
		Matrix4f m = mStack.last().pose();
		// @formatter:off
		bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		bb.vertex(m, (float)     0, (float) (top + 4), 0F).color(0, 0, 0,   0).endVertex();
		bb.vertex(m, (float) width, (float) (top + 4), 0F).color(0, 0, 0,   0).endVertex();
		bb.vertex(m, (float) width, (float)  top     , 0F).color(0, 0, 0, 255).endVertex();
		bb.vertex(m, (float)     0, (float)  top     , 0F).color(0, 0, 0, 255).endVertex();
		tessellator.end();
		bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		bb.vertex(m, (float)     0, (float)  bottom,      0F).color(0, 0, 0, 255).endVertex();
		bb.vertex(m, (float) width, (float)  bottom,      0F).color(0, 0, 0, 255).endVertex();
		bb.vertex(m, (float) width, (float) (bottom - 4), 0F).color(0, 0, 0,   0).endVertex();
		bb.vertex(m, (float)     0, (float) (bottom - 4), 0F).color(0, 0, 0,   0).endVertex();
		tessellator.end();
		// @formatter:on
		
		// Render decorations
		renderDecorations(mStack, mouseX, mouseY);

		RenderSystem.disableBlend();
	}
	
	protected void renderScrollBar(
	  PoseStack matrices, Tesselator tessellator, BufferBuilder bb, int maxScroll,
	  int sbMinX, int sbMaxX
	) {
		if (maxScroll > 0) {
			int sbHeight = (bottom - top) * (bottom - top) / getMaxScrollPosition();
			sbHeight = Mth.clamp(sbHeight, 32, bottom - top - 8);
			int sbMinY = (int) getScroll() * (bottom - top - sbHeight) / maxScroll + top;
			if (sbMinY < top) sbMinY = top;
			int alpha = 190;
			Matrix4f m = matrices.last().pose();
			// @formatter:off
			bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			bb.vertex(m, (float) sbMinX, (float) bottom, 0F).uv(0F, 1F).color(0, 0, 0, alpha).endVertex();
			bb.vertex(m, (float) sbMaxX, (float) bottom, 0F).uv(1F, 1F).color(0, 0, 0, alpha).endVertex();
			bb.vertex(m, (float) sbMaxX, (float)    top, 0F).uv(1F, 0F).color(0, 0, 0, alpha).endVertex();
			bb.vertex(m, (float) sbMinX, (float)    top, 0F).uv(0F, 0F).color(0, 0, 0, alpha).endVertex();
			tessellator.end();
			bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			bb.vertex(m, (float) sbMinX, (float) (sbMinY + sbHeight), 0F).uv(0F, 1F).color(128, 128, 128, alpha).endVertex();
			bb.vertex(m, (float) sbMaxX, (float) (sbMinY + sbHeight), 0F).uv(1F, 1F).color(128, 128, 128, alpha).endVertex();
			bb.vertex(m, (float) sbMaxX, (float)  sbMinY,             0F).uv(1F, 0F).color(128, 128, 128, alpha).endVertex();
			bb.vertex(m, (float) sbMinX, (float)  sbMinY,             0F).uv(0F, 0F).color(128, 128, 128, alpha).endVertex();
			tessellator.end();
			bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			bb.vertex(sbMinX,     sbMinY + sbHeight - 1, 0.0).uv(0F, 1F).color(192, 192, 192, alpha).endVertex();
			bb.vertex(sbMaxX - 1, sbMinY + sbHeight - 1, 0.0).uv(1F, 1F).color(192, 192, 192, alpha).endVertex();
			bb.vertex(sbMaxX - 1, sbMinY,                0.0).uv(1F, 0F).color(192, 192, 192, alpha).endVertex();
			bb.vertex(sbMinX,     sbMinY,                0.0).uv(0F, 0F).color(192, 192, 192, alpha).endVertex();
			tessellator.end();
			// @formatter:on
		}
	}
	
	public void scrollTo(double scroll) {
		setScroll(scroll);
	}
	
	public double scrollFor(double y, double height) {
		double margin = Mth.clamp(
		  (bottom - top - height) / 2.0, forcedScrollMargin, preferredScrollMargin);
		final double relY = y - scroll;
		if (relY < margin) return max(0, y - margin);
		if (relY + height > bottom - top - margin)
			return max(0, min(y - margin, y - (bottom - top - height - margin)));
		return scroll;
	}
	
	public void scrollTo(ListEntry entry) {
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
			if (down < 0) scrollBy(-down);
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
		this.scroll = Mth.clamp(scroll, 0.0, getMaxScroll());
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
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		updateScrollingState(mouseX, mouseY, button);
		if (!isMouseOver(mouseX, mouseY))
			return false;
		E item = getItemAtPosition(mouseX, mouseY);
		if (item != null) {
			if (item.mouseClicked(mouseX, mouseY, button)) {
				setFocused(item);
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
		INavigableTarget target = null;
		ContainerEventHandler nest = this;
		while (nest.getFocused() instanceof ContainerEventHandler) {
			nest = ((ContainerEventHandler) nest.getFocused());
			if (nest instanceof INavigableTarget)
				target = ((INavigableTarget) nest);
		}
		GuiEventListener listener = nest.getFocused();
		if (listener instanceof INavigableTarget)
			target = ((INavigableTarget) listener);
		
		if (target != null)
			setSelectedTarget(target);
	}
	
	@Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (getFocusedItem() != null)
			getFocusedItem().mouseReleased(mouseX, mouseY, button);
		return false;
	}
	
	@Override public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double deltaX, double deltaY
	) {
		if (ContainerEventHandlerEx.super.mouseDragged(
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
				int int_3 = Mth.clamp((int) ((float) (int_2 * int_2) / (float) getMaxScrollPosition()), 32, int_2 - 8);
				double double_6 = max(1.0, double_5 / (double) (int_2 - int_3));
				setScroll(getScroll() + deltaY * double_6);
			}
			return true;
		}
		return false;
	}
	
	@Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		for (E entry : entries) {
			if (!entry.mouseScrolled(mouseX, mouseY, delta)) continue;
			return true;
		}
		setScroll(getScroll() - delta * (double) (getMaxScroll() / getItemCount()) / 2.0);
		return true;
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (Screen.hasAltDown()) {
			if (handleNavigationKey(keyCode, scanCode, modifiers))
				return true;
		}
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		return handleNavigationKey(keyCode, scanCode, modifiers);
	}
	
	protected boolean handleNavigationKey(int keyCode, int scanCode, int modifiers) {
		INavigableTarget target = getSelectedTarget();
		INavigableTarget subTarget = getSelectedSubTarget();
		if (subTarget != null && subTarget.handleNavigationKey(keyCode, scanCode, modifiers))
			return true;
		if (target != null && target.handleNavigationKey(keyCode, scanCode, modifiers))
			return true;
		switch (keyCode) {
			case GLFW.GLFW_KEY_DOWN -> {
				navigateEntries(1);
				return true;
			}
			case GLFW.GLFW_KEY_UP -> {
				navigateEntries(-1);
				return true;
			}
			case GLFW.GLFW_KEY_LEFT -> {
				if (subTarget instanceof IExpandable && ((IExpandable) subTarget).isExpanded()) {
					((IExpandable) subTarget).setExpanded(false, Screen.hasShiftDown());
					playFeedbackTap(0.4F);
					return true;
				}
				if (navigateSubEntries(-1)) return true;
				if (target instanceof IExpandable && ((IExpandable) target).isExpanded()) {
					((IExpandable) target).setExpanded(false, Screen.hasShiftDown());
					playFeedbackTap(0.4F);
					return true;
				} else if (subTarget != null) {
					INavigableTarget parent = subTarget.getNavigableParent();
					while (parent != null && !(parent instanceof IExpandable))
						parent = parent.getNavigableParent();
					if (parent != null) {
						parent.navigate();
						playFeedbackTap(0.4F);
						return true;
					}
				}
			}
			case GLFW.GLFW_KEY_RIGHT -> {
				if (subTarget instanceof IExpandable && !((IExpandable) subTarget).isExpanded()) {
					((IExpandable) subTarget).setExpanded(true, Screen.hasShiftDown());
					playFeedbackTap(0.4F);
					return true;
				}
				if (navigateSubEntries(1)) return true;
				if (target instanceof IExpandable && !((IExpandable) target).isExpanded()) {
					((IExpandable) target).setExpanded(true, Screen.hasShiftDown());
					playFeedbackTap(0.4F);
					return true;
				}
			}
		}
		return false;
	}
	
	private void playFeedbackTap(float volume) {
		Minecraft.getInstance().getSoundManager().play(
		  SimpleSoundInstance.forUI(SimpleConfigMod.UI_TAP, volume));
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseY >= (double) top && mouseY <= (double) bottom &&
		       mouseX >= (double) left && mouseX <= (double) right;
	}
	
	protected void renderList(
	  PoseStack mStack, int startX, int startY, int mouseX, int mouseY, float delta
	) {
		int itemCount = getItemCount();
		int yy = startY + headerHeight;
		for (int renderIndex = 0; renderIndex < itemCount; ++renderIndex) {
			E item = getItem(renderIndex);
			if (item.isShown()) {
				int itemHeight = item.getItemHeight();
				int itemWidth = getItemWidth();
				int x = getRowLeft();
				renderItem(
				  mStack, item, renderIndex, x, yy, itemWidth, itemHeight, mouseX, mouseY,
				  isMouseOver(mouseX, mouseY) && item.isMouseOver(mouseX, mouseY), delta);
				yy += item.getItemHeight();
			}
		}
		if (yy == startY + headerHeight)
			renderEmptyPlaceHolder(mStack, mouseX, mouseY, delta);
	}
	
	protected MutableComponent getEmptyPlaceHolder() {
		return Component.translatable("simpleconfig.ui.no_entries");
	}
	
	protected void renderEmptyPlaceHolder(PoseStack mStack, int mouseX, int mouseY, float delta) {
		MutableComponent text = getEmptyPlaceHolder();
		Font font = Minecraft.getInstance().font;
		drawCenteredString(mStack, font, text, (left + right) / 2, (top + bottom) / 2, 0xF0A0A0A0);
	}
	
	protected void renderItem(
	  PoseStack matrices, E item, int index, int x, int y, int entryWidth, int entryHeight,
	  int mouseX, int mouseY, boolean isHovered, float delta
	) {
		item.render(matrices, index, x, y, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
	}
	
	protected int getRowLeft() {
		return (left + right) / 2 - getItemWidth() / 2 + 2;
	}
	
	protected int getRowTop(int index) {
		int y = top + 4 - (int) getScroll() + headerHeight;
		for (int i = 0; i < entries.size() && i < index; ++i)
			if (entries.get(i).isShown()) y += entries.get(i).getItemHeight();
		return y;
	}
	
	public int getFocusedScroll() {
		int y = headerHeight;
		GuiEventListener focused = getFocusedItem();
		List<? extends ListEntry> entries = this.entries;
		final int index = entries.indexOf(focused);
		if (index < 0)
			return y;
		for (ListEntry entry : entries.subList(0, index))
			y += entry.getItemHeight();
		if (focused instanceof IExpandable)
			y += ((IExpandable) focused).getFocusedScroll();
		return y;
	}
	
	public int getFocusedHeight() {
		ListEntry focused = getFocusedItem();
		if (focused instanceof IExpandable)
			return ((IExpandable) focused).getFocusedHeight();
		if (focused != null)
			return focused.getItemHeight();
		return 24;
	}
	@Override public boolean isFocused() {
		return false;
	}
	protected void renderBarBackground(
	  PoseStack matrices, int y1, int y2, int alpha1, int alpha2
	) {
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, backgroundLocation);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		Matrix4f matrix = matrices.last().pose();
		float div = 32F;
		buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		// @formatter:off
		buffer.vertex(matrix, (float) 0,     (float) y2, 0F).uv(                 0F, (float) y2 / div).color(64, 64, 64, alpha2).endVertex();
		buffer.vertex(matrix, (float) width, (float) y2, 0F).uv((float) width / div, (float) y2 / div).color(64, 64, 64, alpha2).endVertex();
		buffer.vertex(matrix, (float) width, (float) y1, 0F).uv((float) width / div, (float) y1 / div).color(64, 64, 64, alpha1).endVertex();
		buffer.vertex(matrix, (float) 0,     (float) y1, 0F).uv(                 0F, (float) y1 / div).color(64, 64, 64, alpha1).endVertex();
		// @formatter:on
		tessellator.end();
	}
	
	protected E remove(int int_1) {
		E itemListWidget$Item_1 = entries.get(int_1);
		return removeEntry(entries.get(int_1)) ? itemListWidget$Item_1 : null;
	}
	
	protected boolean removeEntry(E entry) {
		return entries.remove(entry);
	}

	@Override public void setFocused(boolean focused) {
		if (focused)
			updateSelectedTarget();
		super.setFocused(focused);
	}

	// @Override public boolean changeFocus(boolean focus) {
	// 	boolean change = super.changeFocus(focus);
	// 	if (change)
	// 		updateSelectedTarget();
	// 	return change;
	// }
	
	public void setSelectedTarget(@Nullable INavigableTarget target) {
		if (smallestLastTarget == null) {
			smallestLastTarget = selectedTarget;
		} else if (selectedTarget != null) {
			Rectangle selectedArea = selectedTarget.getNavigableArea(),
			  smallestArea = smallestLastTarget.getNavigableArea();
			if (selectedArea.width < smallestArea.width || selectedArea.horizontalIntersection(smallestArea) <= 0)
				smallestLastTarget = selectedTarget;
		}
		if (target != null) {
			INavigableTarget parent = target;
			while (parent != null && parent.isNavigableSubTarget()) parent = parent.getNavigableParent();
			if (parent != null && parent.getNavigableSubTargets().contains(target))
				parent.setLastSelectedNavigableSubTarget(target);
		}
		selectedTarget = target;
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
			entry.setEntryList(DynamicEntryListWidget.this);
			return item;
		}
		
		@Override public void add(int index, E entry) {
			items.add(index, entry);
			entry.setEntryList(DynamicEntryListWidget.this);
		}
		
		@Override public E remove(int int_1) {
			return items.remove(int_1);
		}
	}
	
	@OnlyIn(value = Dist.CLIENT)
	public static abstract class ListEntry extends GuiComponent implements ISeekableComponent, INavigableTarget {
		private @Nullable DynamicEntryListWidget<?> entryList = null;
		
		public boolean isShown() {
			return true;
		}
		
		public void tick() {}
		
		public abstract void render(
		  PoseStack mStack, int index, int x, int y, int w, int h, int mouseX, int mouseY,
		  boolean isHovered, float delta);
		
		public DynamicEntryListWidget<?> getEntryList() {
			if (entryList == null)
				throw new IllegalStateException(
				  "Tried to get parent of orphan config entry of type " + getClass().getSimpleName() +
				  "\nThis entry hasn't been properly initialized");
			return entryList;
		}
		
		public void setEntryList(@Nullable DynamicEntryListWidget<?> parent) {
			this.entryList = parent;
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
	
	@Override public Pair<Integer, GuiEventListener> getDragged() {
		return dragged;
	}
	
	@Override public void setDragged(Pair<Integer, GuiEventListener> dragged) {
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
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSoundInstance.forUI(SimpleConfigMod.UI_DOUBLE_TAP, 1F));
			return;
		}
		matchingEntries.forEach(e -> e.setFocusedMatch(false));
		focusedMatch = matchingEntries.get(index);
		focusedMatch.setFocusedMatch(true);
		if (focusedMatch instanceof INavigableTarget)
			setSelectedTarget((INavigableTarget) focusedMatch);
		playFeedbackTap(0.4F);
	}
	
	public List<INavigableTarget> getNavigableTargets(boolean onlyVisible, boolean subTargets) {
		//noinspection RedundantTypeArguments // javac regression?
		return entries.stream()
		  .filter(INavigableTarget::isNavigable)
		  .<INavigableTarget>flatMap(
			 subTargets
			 ? e -> Stream.of(
				Stream.of(e), e.getNavigableSubTargets().stream(),
				e.getNavigableDescendantsAndSubDescendants(onlyVisible).stream()
			 ).flatMap(Function.identity())
			 : e -> Stream.concat(
				Stream.of(e), e.getNavigableDescendants(onlyVisible).stream())
		  ).collect(Collectors.toList());
	}
	
	public void navigateEntries(int step) {
		if (step == 0) return;
		final List<INavigableTarget> targets = getNavigableTargets(true, false);
		INavigableTarget selected = getSelectedTarget();
		INavigableTarget subTarget = getSelectedSubTarget();
		int i = selected != null ? targets.indexOf(selected) : -1;
		if (i == -1) i = step > 0 ? -1 : targets.size();
		int target = Mth.clamp(i + step, 0, targets.size() - 1);
		if (target != i) {
			INavigableTarget parent = targets.get(target), t = parent;
			List<INavigableTarget> subTargets = t.getNavigableSubTargets();
			if (!subTargets.isEmpty()) {
				INavigableTarget last = t.getLastSelectedNavigableSubTarget();
				t = subTarget != null ? subTarget.selectClosestTarget(
				  subTargets, last != null? last : smallestLastTarget
				) : subTargets.get(0);
				parent.setLastSelectedNavigableSubTarget(t);
			}
			t.navigate();
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSoundInstance.forUI(SimpleConfigMod.UI_TAP, 0.3F));
		}
	}
	
	public boolean navigateSubEntries(int step) {
		if (step == 0) return false;
		INavigableTarget selected = getSelectedTarget();
		INavigableTarget subTarget = getSelectedSubTarget();
		if (selected == null) return false;
		List<INavigableTarget> subTargets = selected.getNavigableSubTargets();
		if (subTargets.isEmpty()) return false;
		int i = subTarget != null? subTargets.indexOf(subTarget) : -1;
		if (i == -1) i = step > 0? -1 : subTargets.size();
		int target = Mth.clamp(i + step, 0, subTargets.size() - 1);
		if (target != i) {
			INavigableTarget t = subTargets.get(target);
			selected.setLastSelectedNavigableSubTarget(t);
			t.navigate();
			Minecraft.getInstance().getSoundManager().play(
			  SimpleSoundInstance.forUI(SimpleConfigMod.UI_TAP, 0.3F));
			return true;
		}
		return false;
	}
	
	@Override public @NotNull NarrationPriority narrationPriority() {
		return NarrationPriority.NONE;
	}
	@Override public void updateNarration(@NotNull NarrationElementOutput out) {
		// getSelectedEntry().updateNarration(out.nest());
	}
}
