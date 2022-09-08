package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.api.ScissorsHandler;
import endorh.simpleconfig.ui.gui.OverlayInjector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static endorh.simpleconfig.ui.gui.WidgetUtils.pos;
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
	  int width, int height,  Component title, OnPress pressedAction
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
		super(x, y, width, height, title, pressedAction);
	}
	
	public TintedButton(
	  int x, int y, int width, int height, Component title, OnPress pressedAction, OnTooltip onTooltip
	) {
		super(x, y, width, height, title, pressedAction, onTooltip);
	}
	
	@Override public void render(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
		area.setBounds(x, y, getWidth(), getHeight());
		super.render(mStack, mouseX, mouseY, delta);
	}
	
	@Override public void renderButton(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
		int level = getYImage(isHoveredOrFocused());
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		blit(mStack, x, y, 0, 46 + level * 20, width / 2, height);
		blit(mStack, x + width / 2, y, 200 - width / 2, 46 + level * 20, width / 2, height);
		renderBg(mStack, mc, mouseX, mouseY);
		int fgColor = getFGColor();
		Component message = getMessage();
		int contentWidth = font.width(message);
		contentArea.setBounds(area.x + 4, area.y, area.width - 8, area.height);
		mStack.pushPose(); {
			if (contentWidth < width - 8) mStack.translate((width - 8 - contentWidth) / 2.0, 0.0, 0.0);
			if (contentWidth > width - 8) {
				ScissorsHandler.INSTANCE.withScissor(
				  contentArea, () -> drawString(
					 mStack, font, message, x + 4, y + (height - 8) / 2,
					 fgColor | Mth.ceil(alpha * 255F) << 24));
				if (isMouseOver(mouseX, mouseY) && !overlay.isRendering()) {
					Screen screen = mc.screen;
					if (overlayArea == null) {
						overlayArea = new Rectangle();
						OverlayInjector.injectVisualOverlay(overlayArea, overlay, 10);
					}
					overlayArea.setBounds(x, y, contentWidth + 8, height + 1);
					if (screen != null && overlayArea.getMaxX() > screen.width)
						overlayArea.x = max(4, screen.width - 4 - overlayArea.getWidth());
				}
			} else {
				if (overlayArea != null) overlayArea.setBounds(x, y, width, height + 1);
				drawString(
				  mStack, font, message, x + 4, y + (height - 8) / 2,
				  fgColor | Mth.ceil(alpha * 255F) << 24);
			}
		} mStack.popPose();
		if (isHoveredOrFocused()) renderToolTip(mStack, mouseX, mouseY);
	}
	
	@Override protected void renderBg(
	  @NotNull PoseStack mStack, @NotNull Minecraft minecraft, int mouseX, int mouseY
	) {
		super.renderBg(mStack, minecraft, mouseX, mouseY);
		// The 2-patch button texture blit implementation floors width to even numbers
		if (tintColor != 0) {
			fill(mStack, x, y, x + width / 2 * 2, y + height,
			     active ? tintColor : tintColor & 0xFFFFFF | (tintColor >> 24 & 0xFF) / 4 << 24);
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
		  PoseStack mStack, Rectangle area, int mouseX, int mouseY, float delta
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
			int x = button.x;
			int y = button.y;
			int w = button.width;
			int h = button.height;
			int ww = (int) animator.getEaseOut();
			pos(button, area.x, area.y, ww, area.height);
			this.area.setBounds(area.x, area.y, ww, area.height);
			ScissorsHandler.INSTANCE.withSingleScissor(
			  this.area, () -> button.render(mStack, mouseX, mouseY, delta));
			button.render(mStack, mouseX, mouseY, delta);
			pos(button, x, y, w, h);
			rendering = false;
			return true;
		}
		
		public boolean isRendering() {
			return rendering;
		}
	}
}
