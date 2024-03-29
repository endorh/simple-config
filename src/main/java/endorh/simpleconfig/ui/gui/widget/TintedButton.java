package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons.Backgrounds;
import endorh.simpleconfig.api.ui.math.Point;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.IMultiTooltipScreen;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.api.ScissorsHandler;
import endorh.simpleconfig.ui.api.Tooltip;
import endorh.simpleconfig.ui.gui.OverlayInjector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static java.lang.Math.max;

public class TintedButton extends Button {
	
	protected int tintColor = 0x00000000;
	protected Rectangle area = new Rectangle();
	protected @Nullable Rectangle overlayArea = null;
	protected Rectangle contentArea = new Rectangle();
	protected ButtonOverlay overlay = new ButtonOverlay(this);
	
	public static TintedButton of(
	  Component title, OnPress pressedAction
	) {
		return of(80, 20, title, pressedAction);
	}
	
	public static TintedButton of(
	  Component title, int tint, OnPress pressedAction
	) {
		return of(80, 20, title, tint, pressedAction);
	}
	
	public static TintedButton of(
	  int width, int height, Component title, OnPress pressedAction
	) {
		return of(width, height, title, 0, pressedAction);
	}
	
	public static TintedButton of(
	  int width, int height, Component title, int tint, OnPress pressedAction
	) {
		TintedButton button = new TintedButton(0, 0, width, height, title, pressedAction);
		button.setTintColor(tint);
		return button;
	}
	
	public TintedButton(
	  int x, int y, int width, int height, Component title, OnPress pressedAction
	) {
		super(new Button.Builder(title, pressedAction).bounds(x, y, width, height));
	}
	
	public TintedButton(
	  int x, int y, int width, int height, Component title, OnPress pressedAction, CreateNarration createNarration
	) {
		super(x, y, width, height, title, pressedAction, createNarration);
	}
	
	@Override public void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float delta) {
		area.setBounds(getX(), getY(), getWidth(), getHeight());
		super.render(gg, mouseX, mouseY, delta);
	}
	
	@Override public void renderWidget(@NotNull GuiGraphics gg, int mouseX, int mouseY, float delta) {
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		int level = getTextureLevel();
		// TODO: Simplify rendering logic
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		Backgrounds.BUTTON_BACKGROUND.renderStretch(gg, getX(), getY(), width, height, level);
		renderTint(gg, getX(), getY(), getX() + width / 2 * 2, getY() + height);
		int fgColor = getFGColor();
		Component message = getMessage();
		int contentWidth = font.width(message);
		contentArea.setBounds(area.x + 4, area.y, area.width - 8, area.height);
		PoseStack mStack = gg.pose();
		mStack.pushPose(); {
			if (contentWidth < width - 8) mStack.translate((width - 8 - contentWidth) / 2.0, 0.0, 0.0);
			if (contentWidth > width - 8) {
				ScissorsHandler.INSTANCE.withScissor(
				  contentArea, () -> gg.drawString(
					 font, message, getX() + 4, getY() + (height - 8) / 2,
					 fgColor | Mth.ceil(alpha * 255F) << 24));
				if (isMouseOver(mouseX, mouseY) && !overlay.isRendering()) {
					Screen screen = mc.screen;
					if (overlayArea == null) {
						overlayArea = new Rectangle();
						OverlayInjector.injectVisualOverlay(overlayArea, overlay, 10);
					}
					overlayArea.setBounds(getX(), getY(), contentWidth + 8, height + 1);
					if (screen != null && overlayArea.getMaxX() > screen.width)
						overlayArea.x = max(4, screen.width - 4 - overlayArea.getWidth());
				}
			} else {
				if (overlayArea != null) overlayArea.setBounds(getX(), getY(), width, height + 1);
				gg.drawString(
				  font, message, getX() + 4, getY() + (height - 8) / 2,
				  fgColor | Mth.ceil(alpha * 255F) << 24);
			}
		} mStack.popPose();
		if (isHoveredOrFocused()) renderToolTip(gg, mouseX, mouseY);
	}

	protected int getTextureLevel() {
		return !active? 0 : isHoveredOrFocused()? 2 : 1;
	}

	protected void renderTint(@NotNull GuiGraphics gg, int x, int y, int xm, int ym) {
		// The 2-patch button texture blit implementation floors width to even numbers
		if (tintColor != 0) {
			gg.fill(x, y, xm, ym,
				getEffectiveTintColor());
		}
	}

	protected int getEffectiveTintColor() {
		return active ? tintColor : tintColor & 0xFFFFFF | (tintColor >> 24 & 0xFF) / 4 << 24;
	}

	public List<Component> getTooltipContents() {
		return Collections.emptyList();
	}

	public void renderToolTip(@NotNull GuiGraphics gg, int mouseX, int mouseY) {
		// TODO: Adapt to the new Mojang way to position tooltips
		final List<Component> ls = getTooltipContents();
		if (!ls.isEmpty()) {
			final Screen screen = Minecraft.getInstance().screen;
			boolean hovered = isMouseOver(mouseX, mouseY);
			int tooltipX = hovered ? mouseX : getX() + width / 2;
			int tooltipY = hovered ? mouseY : getY() < 64 ? getY() + height : getY();
			if (screen instanceof IMultiTooltipScreen ts) {
				ts.addTooltip(Tooltip.of(
					Rectangle.of(getX(), getY(), width, height),
					Point.of(tooltipX, tooltipY), ls
				).asKeyboardTooltip(!hovered));
			} else if (screen != null) gg.renderComponentTooltip(Minecraft.getInstance().font, ls, tooltipX, tooltipY);
		}
	}
	
	public int getTintColor() {
		return tintColor;
	}
	
	public void setTintColor(int color) {
		tintColor = color;
	}
	
	public static class ButtonOverlay implements IOverlayRenderer {
		protected final TintedButton button;
		protected final ToggleAnimator animator = new ToggleAnimator(140L);
		protected final Rectangle area = new Rectangle();
		protected boolean rendering;
		protected int lastWidth = -1;
		
		public ButtonOverlay(TintedButton button) {
			this.button = button;
		}
		
		@Override public boolean renderOverlay(
			GuiGraphics gg, Rectangle area, int mouseX, int mouseY, float delta
		) {
			if (!button.isMouseOver(mouseX, mouseY)) {
				button.overlayArea = null;
				animator.stopAndSet(0F);
				return false;
			}
			if (animator.getTarget() == 0F) {
				animator.resetTarget();
				animator.setOutputRange(button.width, area.width);
			}
			if (lastWidth != area.width) {
				animator.setOutputRange(animator.getEaseOut(), area.width);
				animator.resetTarget();
				lastWidth = area.width;
			}
			rendering = true;
			int x = button.getX();
			int y = button.getY();
			int w = button.width;
			int h = button.height;
			int ww = (int) animator.getEaseOut();
			button.setPosition(area.x, area.y);
			button.setWidth(ww);
			button.setHeight(area.height);
			this.area.setBounds(area.x, area.y, ww, area.height);
			ScissorsHandler.INSTANCE.withSingleScissor(
			  this.area, () -> button.render(gg, mouseX, mouseY, delta));
			button.render(gg, mouseX, mouseY, delta);
			button.setPosition(x, y);
			button.setWidth(w);
			button.setHeight(h);
			rendering = false;
			return true;
		}
		
		public boolean isRendering() {
			return rendering;
		}
	}
}
